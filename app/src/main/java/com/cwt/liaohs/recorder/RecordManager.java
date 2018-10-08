package com.cwt.liaohs.recorder;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.TextureView;
import android.widget.Toast;

import com.cwt.liaohs.bus.SdcardNotMounted;
import com.cwt.liaohs.cwtdvrplus.ContextUtil;
import com.cwt.liaohs.state.IdleState;
import com.cwt.liaohs.state.RecordStateManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.cwt.liaohs.cwtdvrplus.ContextUtil.BUS;

/**
 * Created by liaohs on 2018/9/15.
 */

public class RecordManager implements Runnable {

    public MediaRecorder mediaRecorder;
    public Camera mCamera;
    public TextureView mTextureView;
    public int cameraId;


    public MediaRecordErrorListener mMediaRecordErrorListener;
    public MediaRecordInfoListener mMediaRecordInfoListener;

    private Handler mainHandler;
    private OnRecordFinishListener onRecordFinishListener;
    private boolean isRecording = false;

    private static final int frontCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private static final int backCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;

    public RecordManager(int cameraId, TextureView mTextureView) {
        this.cameraId = cameraId;
        this.mTextureView = mTextureView;
        this.mCamera = getCameraInstance();
        this.mainHandler = new Handler();
        this.mMediaRecordErrorListener = new MediaRecordErrorListener();
        this.mMediaRecordInfoListener = new MediaRecordInfoListener();
    }

    public boolean isCameraValid() {
        return null != getCameraInstance();
    }

    public boolean isRecording(){
        return isRecording;
    }


    private static int openCount = 0;
    private static int prevCount = 0;

    public Camera getCameraInstance() {
        if (mCamera == null) {
            try {
                mCamera = Camera.open(cameraId);
                mCamera.setErrorCallback(new Camera.ErrorCallback() {
                    @Override
                    public void onError(int error, Camera camera) {
                        if (Camera.CAMERA_ERROR_SERVER_DIED == error) {
                            Log.d("cwt", "CAMERA_ERROR_SERVER_DIED");
                            if (mCamera != null) {
                                stopPreview();
                                mCamera.release();
                                mCamera = null;
                            }
                            getCameraInstance();
                        }
                        Log.d("cwt", "Camera Error:" + "error+-->cameraId:" + cameraId);
                    }
                });
                openCount = 0;
            } catch (Exception e) {
                e.printStackTrace();
                openCount++;
                if(openCount > 3){
                    openCount = 0;
                    Toast.makeText(ContextUtil.getInstance(),"打开摄像头失败！",Toast.LENGTH_SHORT).show();
                }else{
                    getCameraInstance();
                }
            }
        }
        return mCamera;
    }

    public void takePic(final OnPicTakeListener onPicTakeListener){

        if(onPicTakeListener == null){
            return;
        }

        if(mCamera == null){
            mCamera = getCameraInstance();
            if(mCamera == null){
                onPicTakeListener.onPicTakeFinish(null);
            }
            return;
        }

        mCamera.takePicture(null, null, new Camera.PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                if(camera != null){
                    camera.startPreview();
                }

                if (data == null) {
                    onPicTakeListener.onPicTakeFinish(null);
                    return;
                }

                byte[] bytes = compressPic(data);

                if(bytes != null){
                    currentPicPath = savePic(bytes);
                    onPicTakeListener.onPicTakeFinish(bytes);
                }else{
                    onPicTakeListener.onPicTakeFinish(null);
                }
            }
        });

    }

    private byte[] compressPic(byte[] data){
        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bmp, 800, 480, true);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);
        bmp.recycle();
        scaledBitmap.recycle();
        return bos.toByteArray();
    }

    private String savePic(byte[] data) {
        if (data == null) {
            return null;
        }
        FileOutputStream out;
        String picPath;
        try {
            picPath = RecordStorage.getCurrentPicPath();
            out = new FileOutputStream(new File(picPath));
            out.write(data);
            out.flush();
            out.close();
            return picPath;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String currentVedioPath = null;
    public String getCurrentVedioPath(){
        return currentVedioPath;
    }

    private String currentPicPath = null;
    public String getCurrentPicPath(){
        return currentPicPath;
    }

    public void startPreview() {
        if (mCamera != null) {

            try {
                mCamera.setPreviewTexture(mTextureView.getSurfaceTexture());
                mCamera.startPreview();
                mCamera.autoFocus(null);
                prevCount = 0;
            } catch (IOException e) {
                Log.d("cwt","preview failed:"+e.toString());
                prevCount++;
                if(prevCount > 3){
                    prevCount = 0;
                    Toast.makeText(ContextUtil.getInstance(),"预览失败！",Toast.LENGTH_SHORT).show();
                }else{
                    startPreview();
                }
            }

        }

    }


    public void stopPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    public void autoFocus() {
        if (mCamera != null) {
            try {
                mCamera.autoFocus(null);
            }catch (Exception e){
                Log.d("cwt","autoFocus failed:"+e.toString());
            }
        }
    }

    private void startRecord(OnRecordFinishListener onRecordFinishListener) {
        Log.d("cwt", "################startRecord()-->cameraId:" + cameraId);
        if (mCamera == null) {
            return;
        }

        if (mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
        }

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setOnInfoListener(mMediaRecordInfoListener);
        mediaRecorder.setOnErrorListener(mMediaRecordErrorListener);

        mCamera.unlock();
        mediaRecorder.setCamera(mCamera);

        if (!RecordSettings.isVoiceDisable()) {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        }

        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_2_TS);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

        if (!RecordSettings.isVoiceDisable()) {
            mediaRecorder.setAudioEncodingBitRate(128000);
            mediaRecorder.setAudioChannels(2);
            mediaRecorder.setAudioSamplingRate(48000);//8000
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);//AAC AMR_NB
        }


        if(RecordSettings.isQuality720p()){
            mediaRecorder.setVideoSize(1280, 720);//720p
            mediaRecorder.setVideoEncodingBitRate(9000000);
        }else{
            mediaRecorder.setVideoSize(1920, 1088);//1080p
            mediaRecorder.setVideoEncodingBitRate(4500000);
        }

        mediaRecorder.setVideoFrameRate(30/*frameRate*/);

        if (cameraId == frontCameraId) {
            if (RecordSettings.isLock()) {
                currentVedioPath = RecordStorage.getCurrentFrontRecordPath(true);
            } else {
                currentVedioPath = RecordStorage.getCurrentFrontRecordPath(false);
            }
        } else {
            if (RecordSettings.isLock()) {
                currentVedioPath = RecordStorage.getCurrentBackRecordPath(true);
            } else {
                currentVedioPath = RecordStorage.getCurrentBackRecordPath(false);
            }
        }

        mediaRecorder.setOutputFile(currentVedioPath);

        RecordItem recordItem = new RecordItem();
        if(RecordSettings.isBackCrashedOn() || RecordSettings.isFrontCrashedOn()){
            recordItem.setRecord_lock(1);
        }else{
            recordItem.setRecord_lock(RecordSettings.isLock() ? 1 : 0);
        }
        recordItem.setRecord_name(currentVedioPath);
        recordItem.setRecord_resolution(ContextUtil.BITRATE_HIG);
        RecordDatabaseManager.getInstance().addRecordItem(recordItem);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            Log.d("cwt", "startRecord-->e:" + e.toString() + ",cameraId:" + cameraId);
            if (!TextUtils.isEmpty(currentVedioPath)) {
                RecordDatabaseManager.getInstance().deleteRecordItemByName(currentVedioPath);
                RecordStorage.deleteFile(currentVedioPath);
            }
        }
        isRecording = true;
        this.onRecordFinishListener = onRecordFinishListener;
        if(RecordSettings.isBackWechatVedioOn()){
            mainHandler.postDelayed(this, 15 * 1000 + 1000);
        }else{
            mainHandler.postDelayed(this, RecordSettings.getRecordInterval() * 60 * 1000 + 1000);
        }

    }

    @Override
    public void run() {
        stopRecordTask(onRecordFinishListener);
    }

    private void stopRecord() {
        Log.d("cwt", "###################stopRecord()-->cameraId:" + cameraId);

        if (mediaRecorder != null) {
            try {
                mediaRecorder.setOnErrorListener(null);
                mediaRecorder.setOnInfoListener(null);
                mediaRecorder.stop();
            } catch (Exception e) {
                Log.d("cwt", "stopRecord-->e:" + e.toString() + ",cameraId:" + cameraId);
                if (!TextUtils.isEmpty(currentVedioPath)) {
                    RecordDatabaseManager.getInstance().deleteRecordItemByName(currentVedioPath);
                    RecordStorage.deleteFile(currentVedioPath);
                }
            } finally {
                mediaRecorder.reset();
                mediaRecorder.release();
                mediaRecorder = null;
                if (mCamera != null) {
                    mCamera.lock();
                }
            }
            isRecording = false;
        }

    }

    private RecordStateManager stateManager;
    private static final int typeFront = 0;
    private static final int typeBack = 1;
    public void startRecordTask(final OnRecordFinishListener onRecordFinishListener) {
        try {
            RecordStorage.checkSdcard(new OnSdcardCheckoutListener() {
                @Override
                public void sdcardNoMounted() {
                    if (cameraId == frontCameraId) {
                        stateManager = ContextUtil.getInstance().getStateManager(true);
                        stateManager.changeToState(new IdleState(stateManager,typeFront));
                    } else {
                        stateManager = ContextUtil.getInstance().getStateManager(false);
                        stateManager.changeToState(new IdleState(stateManager,typeBack));
                        BUS.post(new SdcardNotMounted());
                    }

                }

                @Override
                public void sdcardStorageEnough() {
                    startRecord(onRecordFinishListener);
                }

                @Override
                public void sdcardStorageNotEnough() {
                    Log.d("cwt","sdcardStorageNotEnough()");
                }
            });
        } catch (Exception e) {

        }

    }

    public void stopRecordTask(OnRecordFinishListener onRecordFinishListener) {
        stopRecord();
        mainHandler.removeCallbacks(this);
        if (onRecordFinishListener != null) {
            onRecordFinishListener.onRecordFinish();
        }
    }

    class MediaRecordErrorListener implements MediaRecorder.OnErrorListener {
        @Override
        public void onError(MediaRecorder mr, int what, int extra) {
            if (what == MediaRecorder.MEDIA_ERROR_SERVER_DIED) {
                Log.e("cwt", "Media server died-->cameraId:" + cameraId);
            } else {
                Log.e("cwt", "Unspecified media recorder error-->cameraId:" + cameraId + ",what=" + what + ",extra=" + extra);
            }
        }
    }

    class MediaRecordInfoListener implements MediaRecorder.OnInfoListener {
        @Override
        public void onInfo(MediaRecorder mr, int what, int extra) {
            // TODO Auto-generated method stub
            switch (what) {
                case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                    Log.e("cwt", "Max Duration reached-->cameraId:" + cameraId);
                case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
                    Log.e("cwt", "Max File size reached-->cameraId:" + cameraId);
                    break;
                case MediaRecorder.MEDIA_RECORDER_INFO_UNKNOWN:
                    Log.e("cwt", "Unkown record info error-->cameraId:" + cameraId);
            }
        }
    }

}
