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
import com.cwt.liaohs.recorder.RecordManager;
import com.cwt.liaohs.recorder.RecordSettings;
import com.cwt.liaohs.recorder.RecordStorage;
import com.cwt.liaohs.state.CrashState;
import com.cwt.liaohs.state.DriveState;
import com.cwt.liaohs.state.IdleState;
import com.cwt.liaohs.state.RecordStateManager;
import com.squareup.otto.Subscribe;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.cwt.liaohs.cwtdvrplus.ContextUtil.BUS;

/**
 * Created by liaohs on 2018/9/14.
 */

public class DoubleRecordActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
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

    private RecordManager backRecordManager;
    private RecordManager frontRecordManager;

    private RecordStateManager backStateManager;
    private RecordStateManager frontStateManager;

    private Handler mHandler;
    private Handler mainHandler;
    private HandlerThread mHandlerThread;

    TestBroadCast broadCast;
    private final SdcardReceiver sdcardReceiver = new SdcardReceiver();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);

        setContentView(R.layout.activity_double_record);
        BUS.register(this);

        IntentFilter filter = new IntentFilter("com.cwt.test");
        broadCast = new TestBroadCast();
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
            if (frontRecordManager != null) {
                frontRecordManager.stopPreview();
                if(frontRecordManager.isRecording()){
                    stopFrontRecord();
                }
            }
        }

        if (backRecordManager != null) {
            backRecordManager.stopPreview();
            if(backRecordManager.isRecording()){
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

        if (frontRecordManager != null) {
            frontRecordManager.stopPreview();
            if(frontRecordManager.isRecording()){
                stopFrontRecord();
            }
            frontRecordManager = null;
        }

        if (backRecordManager != null) {
            backRecordManager.stopPreview();
            if(backRecordManager.isRecording()){
                stopBackRecord();
            }
            backRecordManager = null;
        }

        if(frontStateManager != null){
            frontStateManager = null;
        }

        if(backStateManager != null){
            backStateManager = null;
        }

        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
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

        RecordSettings.setFrontCrashed(false);
        RecordSettings.setBackCrashed(false);
        RecordSettings.setRecordInterval(CrashState.getPreRecordInterval());

        backStateManager = new RecordStateManager(backCameraId,textureView_back);
        backRecordManager = backStateManager.getRecordManager();
        ContextUtil.getInstance().setRecordStateManager(backStateManager,false);

        if (textureView_back.isAvailable()) {
            if(backRecordManager != null){
                backRecordManager.startPreview();
            }
        }

        if (Camera.getNumberOfCameras() == 2) {
            frontStateManager = new RecordStateManager(frontCameraId,textureView_front);
            frontRecordManager = frontStateManager.getRecordManager();
            ContextUtil.getInstance().setRecordStateManager(frontStateManager,true);

            if (textureView_front.isAvailable()) {
                if(frontRecordManager != null){
                    frontRecordManager.startPreview();
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


        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addDataScheme("file");
        registerReceiver(sdcardReceiver,filter);

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
                                frontRecordManager = frontStateManager.getRecordManager();
                                ContextUtil.getInstance().setRecordStateManager(frontStateManager,true);

                                if (textureView_front.isAvailable()) {
                                    if(frontRecordManager != null){
                                        frontRecordManager.startPreview();
                                    }
                                }

                                if(switch_record.isChecked()){
                                    startFrontRecord();
                                }

                            }
                        }else{
                            textureView_back.setVisibility(View.VISIBLE);
                            if (frontRecordManager != null) {
                                frontRecordManager.stopPreview();
                                if(frontRecordManager.isRecording()){
                                    stopFrontRecord();
                                }
                                frontRecordManager = null;
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

    private Camera mCamera;
    private long mLastPicTime = 0;

    private void takePic() {

        if (System.currentTimeMillis() - mLastPicTime < 2000) {
            return;
        }

        if(!RecordStorage.isSdcardAvailable()){
            Toast.makeText(DoubleRecordActivity.this,"sdcard not mounted",Toast.LENGTH_SHORT).show();
            return;
        }

        if(backRecordManager != null){
            if(backRecordManager.isRecording()){
                return;
            }
        }

        if(frontRecordManager != null){
            if(frontRecordManager.isRecording()){
                return;
            }
        }

        if (textureView_back.getVisibility() == View.VISIBLE) {
            if (backRecordManager != null) {
                mCamera = backRecordManager.mCamera;
            }
        } else {
            if (frontRecordManager != null) {
                mCamera = frontRecordManager.mCamera;
            }
        }

        if (mCamera == null) {
            return;
        }

        mCamera.takePicture(null, null, new Camera.PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

                if(camera != null){
                    camera.startPreview();
                }

                if (data == null) {
                    Log.e("cwt", "pic taken ,data is null!");
                    return;
                }

                Bitmap bmp;
                Bitmap scaledBitmap;
                ByteArrayOutputStream bos;

                bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                if (bmp == null) {
                    Log.e("cwt", "pic taken, bitmap is null!");
                    return;
                }

                scaledBitmap = Bitmap.createScaledBitmap(bmp, 800, 480, true);
                bos = new ByteArrayOutputStream();
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);

                if (bos == null)
                    return;

                overlay(bos.toByteArray());

                FileOutputStream out = null;

                try {
                    out = new FileOutputStream(new File(RecordStorage.getCurrentPicPath()));
                    out.write(bos.toByteArray());
                    out.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d("cwt", "takePic()-->e" + e.toString());
                } finally {

                    if (bmp != null) {
                        bmp.recycle();
                    }
                    if (scaledBitmap != null) {
                        scaledBitmap.recycle();
                    }
                    try {
                        if (out != null) {
                            out.close();
                        }
                        if (bos != null) {
                            bos.close();
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }


            }
        });

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

                    if(backRecordManager != null){
                        startBackRecord();
                    }

                    if(frontRecordManager != null){
                        startFrontRecord();
                    }
                    if(RecordStorage.isSdcardAvailable()){
                        BUS.post(new StartRecord());
                    }
                } else {
                    if(RecordSettings.isBackCrashedOn()){
                        return;
                    }

                    if (ContextUtil.getInstance().isRecording) {
                        if(backRecordManager != null){
                            stopBackRecord();
                        }

                        if(frontRecordManager != null){
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

    class FrontSurfaceTextureListener implements TextureView.SurfaceTextureListener {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            if (frontRecordManager != null) {
                frontRecordManager.startPreview();
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
            if (backRecordManager != null) {
                backRecordManager.startPreview();
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

                if(ContextUtil.getInstance().isDriveAfterCrash){
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
                takePic();
                break;
            case R.id.vedio_setting:
                if(RecordSettings.isFrontCrashedOn() || RecordSettings.isBackCrashedOn()){
                    Toast.makeText(DoubleRecordActivity.this,"正在紧急录像，稍后再点击",Toast.LENGTH_SHORT).show();
                }else{
                    vedioSetting();
                }
                break;
            case R.id.textureview_front:
                if (frontRecordManager != null) {
                    frontRecordManager.autoFocus();
                }
                break;
            case R.id.textureview_back:
                if (backRecordManager != null) {
                    backRecordManager.autoFocus();
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


    class TestBroadCast extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            CrashGsensorManager.getInstance().test();
        }
    }


}
