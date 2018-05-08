package com.cwt.liaohs.cwtdvrplus;

import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.cwt.liaohs.recorder.CamearView;
import com.cwt.liaohs.recorder.ISdcardCheckoutListener;
import com.cwt.liaohs.recorder.RecordDbHelper;
import com.cwt.liaohs.recorder.RecordManager;
import com.cwt.liaohs.recorder.RecordSettings;
import com.cwt.liaohs.recorder.RecordStorage;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ISdcardCheckoutListener {
    private CamearView camearView;
    private Camera defaultCamera;

    private Button switch_camera;
    private CheckBox startOrStopRecord;
    private RecordDbHelper recordDbHelper;
    private RecordManager recordManager;

    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private Handler mainHandler;

    private Camera frontCamera;
    private Camera backCamera;
    private MediaRecorder frontMediaRecorder;
    private MediaRecorder backMediaRecorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);

        setContentView(R.layout.activity_main);

        ContextUtil.requestPermissions(MainActivity.this);
        RecordSettings.setRecord(false);

        recordManager = new RecordManager();

        defaultCamera = recordManager.openCamera(RecordManager.CAMERA_FACING_BACK);

        camearView = (CamearView) findViewById(R.id.surfaceView);
        switch_camera = (Button) findViewById(R.id.switch_camera);
        startOrStopRecord = (CheckBox) findViewById(R.id.vedio_switch);

        camearView.init(defaultCamera);
        recordDbHelper = new RecordDbHelper(MainActivity.this);

        frontMediaRecorder = new MediaRecorder();
        backMediaRecorder = new MediaRecorder();

        mainHandler = new Handler();

        mHandlerThread = new HandlerThread("camera");
        mHandlerThread.start();

        mHandler = new Handler(mHandlerThread.getLooper());

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        switch_camera.setVisibility(recordManager.isDoubleCamears() ? View.VISIBLE : View.GONE);
                    }
                });

                mHandler.postDelayed(this, 100);
            }
        });

        switch_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeCamera();
            }
        });

        startOrStopRecord.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                RecordStorage.checkSdcard(recordDbHelper, MainActivity.this);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();

                } else {
                    mainHandler.removeCallbacks(recodRunnable);
                    new StopRecordAsyncTask(null).execute();
                }

            }
        });


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void changeCamera() {

        if (defaultCamera != null) {
            defaultCamera.stopPreview();
            defaultCamera.release();
            defaultCamera = null;
        }

        if (recordManager.currentCameraType == RecordManager.CAMERA_FACING_FRONT) {
            defaultCamera = recordManager.openCamera(RecordManager.CAMERA_FACING_BACK);
        } else if (recordManager.currentCameraType == RecordManager.CAMERA_FACING_BACK) {
            defaultCamera = recordManager.openCamera(RecordManager.CAMERA_FACING_FRONT);
        }

        Camera.Parameters parameters = defaultCamera.getParameters();
        List<String> focusModes = parameters.getSupportedFocusModes();
//            parameters.setPreviewSize(prevWidth, prevHeight);
        if (focusModes != null) {
            for (String mode : focusModes) {
                mode.contains("continuous-video");
                parameters.setFocusMode("continuous-video");
            }
        }

        defaultCamera.setParameters(parameters);

        try {
            defaultCamera.setPreviewDisplay(camearView.getHolder());
        } catch (IOException e) {
            e.printStackTrace();
        }
        defaultCamera.startPreview();
    }

    private void startFrontRecord() {
        if (recordManager.currentCameraType != -1 && recordManager.currentCameraType == RecordManager.CAMERA_FACING_FRONT) {
            if (defaultCamera != null) {
                frontMediaRecorder = recordManager.startRecord(defaultCamera, frontMediaRecorder,
                        camearView.getSurfaceHolder(), recordDbHelper, RecordManager.BITRATE_HIG,
                        30, RecordStorage.getCurrentFrontRecordPath());
            }
        } else {
            frontCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            frontMediaRecorder = recordManager.startRecord(frontCamera, frontMediaRecorder,
                    camearView.getSurfaceHolder(), recordDbHelper, RecordManager.BITRATE_HIG,
                    30, RecordStorage.getCurrentFrontRecordPath());
        }

    }


    private void startBackRecord() {

        if (recordManager.currentCameraType != -1 && recordManager.currentCameraType == RecordManager.CAMERA_FACING_BACK) {
            if (defaultCamera != null) {
                backMediaRecorder = recordManager.startRecord(defaultCamera, backMediaRecorder,
                        camearView.getSurfaceHolder(), recordDbHelper, RecordManager.BITRATE_HIG,
                        30, RecordStorage.getCurrentBackRecordPath());
            }
        } else {
            backCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            backMediaRecorder = recordManager.startRecord(backCamera, backMediaRecorder,
                    camearView.getSurfaceHolder(), recordDbHelper, RecordManager.BITRATE_HIG,
                    30, RecordStorage.getCurrentBackRecordPath());

        }

    }

    private void stopFrontRecord() {
        recordManager.stopRecord(frontCamera, frontMediaRecorder);
    }

    private void stopBackRecord() {
        recordManager.stopRecord(backCamera, backMediaRecorder);
    }

    class StartRecordAsyncTask extends AsyncTask<Void, Void, Boolean> {
        private long delay;

        public StartRecordAsyncTask(long delay) {
            Log.v("cwt", "===StartRecordAsyncTask===");
            this.delay = delay;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.v("cwt", "StartRecordAsyncTask--->onPreExecute()");
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            Log.v("cwt", "StartRecordAsyncTask--->doInBackground()");
            if (recordManager.isDoubleCamears()) {
                Log.v("cwt", "===============double===============");
                startFrontRecord();
                startBackRecord();
            } else {
                Log.v("cwt", "===============single===============");
                startBackRecord();
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            Log.v("cwt", "StartRecordAsyncTask--->onPostExecute()");
            super.onPostExecute(aBoolean);

            mainHandler.postDelayed(recodRunnable, delay);
        }

    }

    private final Runnable recodRunnable = new Runnable() {
        @Override
        public void run() {
            new StopRecordAsyncTask(new OnRecordFinishListener() {
                @Override
                public void onRecordFinish() {
                    Log.v("cwt", "===onRecordFinish===");
                    new StartRecordAsyncTask(60 * 1000 + 1000).execute();
                }
            }).execute();
        }
    };

    class StopRecordAsyncTask extends AsyncTask<Void, Void, Boolean> {
        private OnRecordFinishListener recordFinishListener;

        public StopRecordAsyncTask(OnRecordFinishListener recordFinishListener) {
            Log.v("cwt", "===StopRecordAsyncTask===");
            this.recordFinishListener = recordFinishListener;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.v("cwt", "StopRecordAsyncTask--->onPreExecute()");
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            Log.v("cwt", "StopRecordAsyncTask--->doInBackground()");
            if (recordManager.isDoubleCamears()) {
                stopFrontRecord();
                stopBackRecord();
            } else {
                stopBackRecord();
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            Log.v("cwt", "StopRecordAsyncTask--->onPostExecute()");
            if (recordFinishListener != null) {
                recordFinishListener.onRecordFinish();
            }
        }
    }

    @Override
    public void sdcardNoMounted() {
        Log.v("cwt", "sdcard no mounted");
    }

    @Override
    public void sdcardStorageEnough() {
        Log.v("cwt", "sdcard storage enough");

        new StartRecordAsyncTask(60 * 1000 + 1000).execute();
    }

    @Override
    public void sdcardStorageNotEnough() {
        Log.v("cwt", "sdcard storage not enough");
    }

    public interface OnRecordFinishListener {
        void onRecordFinish();
    }
}
