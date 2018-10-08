package com.cwt.liaohs.cwtdvrplus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cwt.liaohs.bus.CrashFinish;
import com.cwt.liaohs.bus.CrashStart;
import com.cwt.liaohs.bus.Crashing;
import com.cwt.liaohs.bus.SdcardNotMounted;
import com.cwt.liaohs.bus.StartRecord;
import com.cwt.liaohs.bus.StopRecord;
import com.cwt.liaohs.recorder.CrashGsensorManager;
import com.cwt.liaohs.recorder.OnPicTakeListener;
import com.cwt.liaohs.recorder.RecordSettings;
import com.cwt.liaohs.recorder.RecordStorage;
import com.cwt.liaohs.state.CameraErrorState;
import com.cwt.liaohs.state.CrashState;
import com.cwt.liaohs.state.DriveState;
import com.cwt.liaohs.state.IdleState;
import com.cwt.liaohs.state.RecordStateManager;
import com.cwt.liaohs.state.WechatPicState;
import com.cwt.liaohs.state.WechatVedioState;
import com.squareup.otto.Subscribe;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.cwt.liaohs.cwtdvrplus.ContextUtil.BUS;

/**
 * Created by liaohs on 2018/9/14.
 */

public class DoubleRecordActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener,OnPicTakeListener {
    private boolean mNeedGrantedPermission;
    public static final int REQUEST_CAMERA_PERMISSION = 1001;

    private Button switch_camera;
    private Button tack_pic;
    private Button vedio_setting;
    private CheckBox switch_record;
    private CheckBox voice_switch;
    private CheckBox vedio_lock;
    private TextView textRecordTick;
    private RelativeLayout unloadpanel;
    private ImageView pic;


    private TextureView textureView_front;
    private TextureView textureView_back;

    private final int frontCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private final int backCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;

    private RecordStateManager backStateManager;
    private RecordStateManager frontStateManager;

    private Handler mHandler;
    private Handler mainHandler;
    private HandlerThread mHandlerThread;

    private final TestBroadCast broadCast = new TestBroadCast();
    private final SdcardReceiver sdcardReceiver = new SdcardReceiver();
    private final WechatReceiver wechatReceiver = new WechatReceiver();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);

        setContentView(R.layout.activity_double_record);
        BUS.register(this);

        IntentFilter filter = new IntentFilter("com.cwt.test");
        registerReceiver(broadCast,filter);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED
                ) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
            mNeedGrantedPermission = true;
            return;
        } else {
            // resume..
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mNeedGrantedPermission) {
            Log.d("cwt", "onResume()");
            goonWithPermissionGranted();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Camera.getNumberOfCameras() == 2) {
            if (frontStateManager != null) {
                frontStateManager.getRecordManager().stopPreview();
                if(frontStateManager.getRecordManager().isRecording()){
                    stopFrontRecord();
                }
            }
        }

        if (backStateManager != null) {
            backStateManager.getRecordManager().stopPreview();
            if(backStateManager.getRecordManager().isRecording()){
                stopBackRecord();
            }
        }

        if (!mNeedGrantedPermission) {
            if(switch_record.isChecked()){
                switch_record.setChecked(false);
            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BUS.unregister(this);
        unregisterReceiver(sdcardReceiver);
        unregisterReceiver(wechatReceiver);
        unregisterReceiver(broadCast);

        if (frontStateManager != null) {
            frontStateManager.getRecordManager().stopPreview();
            if(frontStateManager.getRecordManager().isRecording()){
                stopFrontRecord();
            }
            frontStateManager = null;
        }

        if (backStateManager != null) {
            backStateManager.getRecordManager().stopPreview();
            if(backStateManager.getRecordManager().isRecording()){
                stopBackRecord();
            }
            backStateManager = null;
        }
    }

    private void goonWithPermissionGranted() {
        switch_camera = (Button) findViewById(R.id.switch_camera);
        tack_pic = (Button) findViewById(R.id.tack_pic);
        vedio_setting = (Button) findViewById(R.id.vedio_setting);
        switch_record = (CheckBox) findViewById(R.id.vedio_switch);
        vedio_lock = (CheckBox) findViewById(R.id.vedio_lock);
        voice_switch = (CheckBox) findViewById(R.id.voice_switch);
        textRecordTick = (TextView) findViewById(R.id.tv_start_record);

        unloadpanel = (RelativeLayout) findViewById(R.id.unloadpanel);
        pic = (ImageView) findViewById(R.id.pic);

        textureView_front = (TextureView) findViewById(R.id.textureview_front);
        textureView_back = (TextureView) findViewById(R.id.textureview_back);

        textureView_front.setSurfaceTextureListener(new FrontSurfaceTextureListener());
        textureView_front.setOnClickListener(this);

        textureView_back.setSurfaceTextureListener(new BackSurfaceTextureListener());
        textureView_back.setOnClickListener(this);

        backStateManager = new RecordStateManager(backCameraId,textureView_back);
        ContextUtil.getInstance().setRecordStateManager(backStateManager,false);

        if (textureView_back.isAvailable()) {
            if(backStateManager != null){
                backStateManager.getRecordManager().startPreview();
            }
        }

        if (Camera.getNumberOfCameras() == 2) {
            frontStateManager = new RecordStateManager(frontCameraId,textureView_front);
            ContextUtil.getInstance().setRecordStateManager(frontStateManager,true);

            if (textureView_front.isAvailable()) {
                if(frontStateManager != null){
                    frontStateManager.getRecordManager().startPreview();
                }
            }
        }

        voice_switch.setChecked(RecordSettings.isVoiceDisable());
        vedio_lock.setChecked(RecordSettings.isLock());

        switch_camera.setOnClickListener(this);
        tack_pic.setOnClickListener(this);
        vedio_setting.setOnClickListener(this);
        switch_record.setOnCheckedChangeListener(this);
        voice_switch.setOnCheckedChangeListener(this);
        vedio_lock.setOnCheckedChangeListener(this);

        mHandlerThread = new HandlerThread("camera");
        mHandlerThread.start();

        IntentFilter mediafilter = new IntentFilter();
        mediafilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        mediafilter.addAction(Intent.ACTION_MEDIA_EJECT);
        mediafilter.addDataScheme("file");
        registerReceiver(sdcardReceiver,mediafilter);

        IntentFilter wechatfilter = new IntentFilter("com.spreadwin.camera.snapshot");
        registerReceiver(wechatReceiver,wechatfilter);

        mHandler = new Handler(mHandlerThread.getLooper());
        mainHandler = new Handler();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        switch_camera.setVisibility(Camera.getNumberOfCameras() == 2 ? View.VISIBLE : View.GONE);
                        if(switch_camera.getVisibility() == View.VISIBLE){
                            if(frontStateManager == null){
                                frontStateManager = new RecordStateManager(frontCameraId,textureView_front);
                                ContextUtil.getInstance().setRecordStateManager(frontStateManager,true);

                                if (textureView_front.isAvailable()) {
                                    if(frontStateManager != null){
                                        frontStateManager.getRecordManager().startPreview();
                                    }
                                }
                                if(RecordSettings.isBackCrashedOn()){
                                    return;
                                }else if(RecordSettings.isBackWechatVedioOn()){
                                    return;
                                }else{
                                    if(CameraErrorState.class.isInstance(frontStateManager.getCurState())){
                                        return;
                                    }else{
                                        if(switch_record.isChecked()){
                                            startFrontRecord();
                                        }
                                    }

                                }
                            }
                        }else{
                            textureView_back.setVisibility(View.VISIBLE);
                            if (frontStateManager != null) {
                                frontStateManager.getRecordManager().stopPreview();
                                if(frontStateManager.getRecordManager().isRecording()){
                                    stopFrontRecord();
                                }
                                if(RecordSettings.isFrontCrashedOn()){
                                    RecordSettings.setFrontCrashed(false);
                                }
                                if(RecordSettings.isFrontWechatVedioOn()){
                                    RecordSettings.setFrontWechatVedio(false);
                                }
                            }
                            frontStateManager = null;
                            ContextUtil.getInstance().setRecordStateManager(frontStateManager,true);
                        }
                    }
                });
                mHandler.postDelayed(this, 500);
            }
        });

    }


    private void switchCamera() {
        if (textureView_back.getVisibility() == View.VISIBLE) {
            textureView_back.setVisibility(View.INVISIBLE);
        } else {
            textureView_back.setVisibility(View.VISIBLE);
        }
    }

    private long mLastPicTime = 0;

    private void takePicTask() {

        if (System.currentTimeMillis() - mLastPicTime < 2000) {
            return;
        }

        if(!RecordStorage.isSdcardAvailable()){
            Toast.makeText(DoubleRecordActivity.this,"sdcard not mounted",Toast.LENGTH_SHORT).show();
            return;
        }

        if(backStateManager != null){
            if(backStateManager.getRecordManager().isRecording()){
                return;
            }
        }

        if(frontStateManager != null){
            if(frontStateManager.getRecordManager().isRecording()){
                return;
            }
        }

        if (textureView_back.getVisibility() == View.VISIBLE) {
            if (backStateManager != null) {
                backStateManager.getRecordManager().takePic(this);
            }
        } else {
            if (frontStateManager != null) {
                frontStateManager.getRecordManager().takePic(this);
            }
        }

        mLastPicTime = System.currentTimeMillis();
    }

    private void vedioSetting() {
        startActivity(new Intent(DoubleRecordActivity.this, SettingsActivity.class));
    }

    private void overlay(byte[] data) {
        unloadpanel.setVisibility(View.VISIBLE);
        mainHandler.removeCallbacks(hidePicRunnable);
        mainHandler.postDelayed(hidePicRunnable, 2000);
        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
        pic.setBackgroundDrawable(new BitmapDrawable(bmp));
    }

    private final Runnable hidePicRunnable = new Runnable() {
        @Override
        public void run() {
            unloadpanel.setVisibility(View.INVISIBLE);
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION: {
                if (grantResults.length > 2
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    mNeedGrantedPermission = false;
                    goonWithPermissionGranted();

                } else {
                    finish();
                }
                break;
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.vedio_switch:
                if (isChecked) {
                    if(RecordSettings.isBackCrashedOn()){
                        return;
                    }

                    if(ContextUtil.getInstance().isDriveAfterCrash){
                        ContextUtil.getInstance().isDriveAfterCrash = false;
                        return;
                    }

                    if(RecordSettings.isBackWechatVedioOn()){
                        return;
                    }

                    if(ContextUtil.getInstance().isDriveAfterWechatVedio){
                        ContextUtil.getInstance().isDriveAfterWechatVedio = false;
                        return;
                    }

                    if(backStateManager != null){
                        startBackRecord();
                    }

                    if(frontStateManager != null){
                        startFrontRecord();
                    }
                    if(RecordStorage.isSdcardAvailable()){
                        BUS.post(new StartRecord());
                    }
                } else {
                    if(RecordSettings.isBackCrashedOn()){
                        return;
                    }

                    if(RecordSettings.isBackWechatVedioOn()){
                        return;
                    }

                    if (ContextUtil.getInstance().isRecording) {
                        if(backStateManager != null){
                            stopBackRecord();
                        }

                        if(frontStateManager != null){
                            stopFrontRecord();
                        }

                        BUS.post(new StopRecord());
                    }
                }
                break;
            case R.id.voice_switch:
                RecordSettings.setVoiceDisable(isChecked);
                break;
            case R.id.vedio_lock:
                RecordSettings.setLock(isChecked);
                break;
        }
    }

    private void startFrontRecord() {
        new StartRecordAsyncTask(frontStateManager,typeFront).execute();
    }

    private void startBackRecord() {
        new StartRecordAsyncTask(backStateManager,typeBack).execute();
    }

    private void stopFrontRecord() {
        new StopRecordAsyncTask(frontStateManager,typeFront).execute();
    }

    private void stopBackRecord() {
        new StopRecordAsyncTask(backStateManager,typeBack).execute();
    }

    @Override
    public void onPicTakeFinish(byte[] data) {
        if(data != null){
            overlay(data);
        }
    }

    class FrontSurfaceTextureListener implements TextureView.SurfaceTextureListener {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            if (frontStateManager != null) {
                frontStateManager.getRecordManager().startPreview();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    }

    class BackSurfaceTextureListener implements TextureView.SurfaceTextureListener {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            if (backStateManager != null) {
                backStateManager.getRecordManager().startPreview();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    }

    @Subscribe
    public void onStartRecord(StartRecord startRecord) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(RecordSettings.isBackCrashedOn()){
                    switch_record.setChecked(true);
                    switch_record.setClickable(false);
                }

                if(RecordSettings.isBackWechatVedioOn()){
                    switch_record.setChecked(true);
                    switch_record.setClickable(false);
                }

                if(ContextUtil.getInstance().isDriveAfterCrash){
                    switch_record.setChecked(true);
                }

                if(ContextUtil.getInstance().isDriveAfterWechatVedio){
                    switch_record.setChecked(true);
                }

                textRecordTick.setVisibility(View.VISIBLE);
                textRecordTick.removeCallbacks(mRecordTickRunnable);
                textRecordTick.post(mRecordTickRunnable);
            }
        });
    }

    @Subscribe
    public void onStopRecord(StopRecord stopRecord) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(RecordSettings.isBackCrashedOn()){
                    switch_record.setChecked(false);
                    switch_record.setClickable(true);
                }

                if(RecordSettings.isBackWechatVedioOn()){
                    switch_record.setChecked(false);
                    switch_record.setClickable(true);
                }

                textRecordTick.setVisibility(View.GONE);
                textRecordTick.removeCallbacks(mRecordTickRunnable);
            }
        });
    }

    @Subscribe
    public void onSdcardNotMounted(SdcardNotMounted sdcardNotMounted) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(DoubleRecordActivity.this,"sdcard not mounted!",Toast.LENGTH_SHORT).show();
                switch_record.setChecked(false);
                textRecordTick.setVisibility(View.GONE);
                textRecordTick.removeCallbacks(mRecordTickRunnable);
            }
        });
    }


    @Subscribe
    public void onCrashStart(CrashStart crashStart) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
               Toast.makeText(DoubleRecordActivity.this,"疑似汽车发生碰撞，启动紧急录影！",Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Subscribe
    public void onCrashing(Crashing crashing) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(DoubleRecordActivity.this,"视频已加锁！",Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Subscribe
    public void onCrashFinish(CrashFinish crashFinish) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(DoubleRecordActivity.this,"紧急录影结束！",Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Runnable mRecordTickRunnable = new Runnable() {
        @Override
        public void run() {
            long duration = System.currentTimeMillis() - ContextUtil.getInstance().mRecordingBegin;
            if (duration >= RecordSettings.getRecordInterval() * 60 * 1000) {
                ContextUtil.getInstance().mRecordingBegin = System.currentTimeMillis();
            }
            duration /= 1000;
            textRecordTick.setText(String.format("%02d:%02d", duration / 60, (duration) % 60));
            if (duration % 2 == 0) {
                textRecordTick.setCompoundDrawablesWithIntrinsicBounds(R.drawable.recording_marker_shape, 0, 0, 0);
            } else {
                textRecordTick.setCompoundDrawablesWithIntrinsicBounds(R.drawable.recording_marker_interval_shape, 0, 0, 0);
            }

            textRecordTick.removeCallbacks(this);
            textRecordTick.postDelayed(this, 1000);
        }
    };


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.switch_camera:
                switchCamera();
                break;
            case R.id.tack_pic:
                takePicTask();
                break;
            case R.id.vedio_setting:
                if(RecordSettings.isFrontCrashedOn() || RecordSettings.isBackCrashedOn()){
                    Toast.makeText(DoubleRecordActivity.this,"正在紧急录像，稍后再点击",Toast.LENGTH_SHORT).show();
                }else if(RecordSettings.isFrontWechatVedioOn() || RecordSettings.isBackWechatVedioOn()){
                    Toast.makeText(DoubleRecordActivity.this,"正在远程微视，稍后再点击",Toast.LENGTH_SHORT).show();
                }else{
                    vedioSetting();
                }
                break;
            case R.id.textureview_front:
                if (frontStateManager != null) {
                    frontStateManager.getRecordManager().autoFocus();
                }
                break;
            case R.id.textureview_back:
                if (backStateManager != null) {
                    backStateManager.getRecordManager().autoFocus();
                }
                break;
        }
    }


    private static final int typeFront = 0;
    private static final int typeBack = 1;

    class StartRecordAsyncTask extends AsyncTask<String, String, Boolean> {
        private RecordStateManager stateManager;
        private int type;
        public StartRecordAsyncTask(RecordStateManager stateManager,int type){
            this.stateManager = stateManager;
            this.type = type;
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Boolean doInBackground(String... arg0) {
            if(stateManager != null){

                if(type == typeFront){
                    stateManager.changeToState(new DriveState(stateManager,typeFront));
                }else if(type == typeBack){
                    stateManager.changeToState(new DriveState(stateManager,typeBack));
                }

            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {

        }
    }

    class StopRecordAsyncTask extends AsyncTask<String, String, Boolean> {
        private RecordStateManager stateManager;
        private int type;
        public StopRecordAsyncTask(RecordStateManager stateManager,int type){
            this.stateManager = stateManager;
            this.type = type;
        }
        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Boolean doInBackground(String... arg0) {
            if(stateManager != null){
                stateManager.changeToState(new IdleState(stateManager,type));
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {

        }
    }


    class SdcardReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                ContextUtil.getInstance().isSdcardRemove = true;
                if(RecordSettings.isBackCrashedOn() || RecordSettings.isFrontCrashedOn()){
                    if(RecordSettings.isBackCrashedOn()){
                        BUS.post(new StopRecord());
                        RecordSettings.setRecordInterval(CrashState.getPreRecordInterval());
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                backStateManager.changeToState(new IdleState(backStateManager,typeBack));
                            }
                        }).start();

                    }

                    if(RecordSettings.isFrontCrashedOn()){
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                frontStateManager.changeToState(new IdleState(frontStateManager,typeFront));
                            }
                        }).start();
                    }
                }else{
                    switch_record.setChecked(false);
                }

            } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                ContextUtil.getInstance().isSdcardRemove = false;
                switch_record.setChecked(true);
            }

        }

    }

    class WechatReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent == null){
                return;
            }

            if(intent.getBooleanExtra("video", false)){
                //vedio
                if(RecordSettings.isBackCrashedOn()){
                    Toast.makeText(DoubleRecordActivity.this,"正在紧急录像，请稍后再试。",Toast.LENGTH_SHORT).show();
                    return;
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if(frontStateManager != null){
                            frontStateManager.changeToState(new WechatVedioState(frontStateManager,typeFront));
                        }
                    }
                }).start();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if(backStateManager != null){
                            backStateManager.changeToState(new WechatVedioState(backStateManager,typeBack));
                        }
                    }
                }).start();

            }else{
                //pic
                if(!RecordStorage.isSdcardAvailable()){
                    Toast.makeText(DoubleRecordActivity.this,"sdcard not mount!",Toast.LENGTH_SHORT).show();
                    return;
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if(frontStateManager != null){
                            new WechatPicState(frontStateManager,typeFront).onStart();
                        }
                    }
                }).start();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if(backStateManager != null){
                            new WechatPicState(backStateManager,typeBack).onStart();
                        }
                    }
                }).start();

            }
        }
    }


    class TestBroadCast extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            CrashGsensorManager.getInstance().test();
        }
    }


}
