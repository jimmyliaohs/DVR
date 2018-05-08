package com.cwt.liaohs.recorder;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 * Created by liaohs on 2018/4/24.
 */

public class RecordManager implements MediaRecorder.OnInfoListener, MediaRecorder.OnErrorListener {

    public static final int BITRATE_LOW = 8 * 1024 * 1024;
    public static final int BITRATE_MID = 10 * 1024 * 1024;
    public static final int BITRATE_HIG = 12 * 1024 * 1024;


    public static final int CAMERA_FACING_FRONT = 1;//前摄
    public static final int CAMERA_FACING_BACK = 0;//后摄
    public static int currentCameraType = -1;//当前打开的摄像头标记

    public Camera openCamera(int type) {
        currentCameraType = -1;

        int frontIndex = -1;
        int backIndex = -1;

        int cameraCount = Camera.getNumberOfCameras();
        Camera.CameraInfo info = new Camera.CameraInfo();

        Log.v("cwt", "info:" + info.toString() + ",cameraCount=" + cameraCount);

        for (int cameraIndex = 0; cameraIndex < cameraCount; cameraIndex++) {
            Camera.getCameraInfo(cameraIndex, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                frontIndex = cameraIndex;
            } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                backIndex = cameraIndex;
            }
        }

        if (type == CAMERA_FACING_FRONT && frontIndex != -1) {
            currentCameraType = type;
            return Camera.open(frontIndex);
        } else if (type == CAMERA_FACING_BACK && backIndex != -1) {
            currentCameraType = type;
            return Camera.open(backIndex);
        }
        return null;
    }

    public boolean isDoubleCamears() {
        return 2 == Camera.getNumberOfCameras();
    }


    public MediaRecorder startRecord(Camera camera, MediaRecorder mediaRecorder, SurfaceHolder holder,
                                     final RecordDbHelper dbHelper, final int bitRate, int frameRate,
                                     final String recordSavePath) {

        if (dbHelper == null) {
            Log.v("cwt","dbHelper:null");
            return null;
        }

        if ((recordSavePath == null || recordSavePath.isEmpty())) {
            Log.v("cwt","recordSavePath:null");
            return null;
        }

        if (camera == null) {
            Log.v("cwt","camera:null");
            return null;
        }

        try {

            if (mediaRecorder != null) {
                releaseMediaRecorder(camera, mediaRecorder);
            }

            mediaRecorder = new MediaRecorder();

            camera.unlock();

            mediaRecorder.setCamera(camera);

//            mediaRecorder.setOrientationHint(90);
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

            CamcorderProfile profile = CamcorderProfile
                    .get(CamcorderProfile.QUALITY_HIGH);
            mediaRecorder.setVideoEncodingBitRate(bitRate);

            mediaRecorder.setVideoSize(profile.videoFrameWidth,
                    profile.videoFrameHeight);
            mediaRecorder.setVideoFrameRate(frameRate);

//            mediaRecorder.setMaxDuration(MAX_RECORD_DUR);

            mediaRecorder.setOnInfoListener(this);
            mediaRecorder.setOnErrorListener(this);

            mediaRecorder.setOutputFile(recordSavePath);

            Log.v("cwt", "startRecord-->" + recordSavePath);

            mediaRecorder.setPreviewDisplay(holder.getSurface());

            mediaRecorder.prepare();
            mediaRecorder.start();

            RecordItem recordItem = new RecordItem();
            recordItem.setRecord_lock(RecordSettings.getLock());
            recordItem.setRecord_name(recordSavePath);
            recordItem.setRecord_resolution(bitRate);
            dbHelper.addRecordItem(recordItem);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.v("cwt", "startRecord-->e:" + e.toString());
            return null;
        }

        return mediaRecorder;
    }

    public void stopRecord(Camera camera, MediaRecorder mediaRecorder) {
        if (mediaRecorder != null && camera != null) {
            try {
                mediaRecorder.setOnErrorListener(null);
                mediaRecorder.setOnInfoListener(null);
                mediaRecorder.setPreviewDisplay(null);
                mediaRecorder.stop();
                Log.v("cwt", "======stopRecord======");
            } catch (Exception e) {
                Log.v("cwt", "stopRecord-->e:" + e.toString());
            } finally {
                releaseMediaRecorder(camera, mediaRecorder);
            }
        }
    }

    public void releaseMediaRecorder(Camera camera, MediaRecorder mediaRecorder) {
        if (mediaRecorder != null && camera != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            camera.lock();
            Log.v("cwt", "======releaseMediaRecorder======");
        }
    }


    private void releaseCamera(Camera camera) {
        if (camera != null) {
            camera.release();
            camera = null;
            Log.v("cwt", "======releaseCamera======");
        }
    }


    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        if (what == MediaRecorder.MEDIA_ERROR_SERVER_DIED) {
            // Media server died. In this case,
            // the application must release the MediaRecorder object and
            // instantiate a new one.
            Log.e("cwt", "MediaRecorder:" + mr.toString() + ",onError()-->Media server died");
            // restartRecord();
        } else {
            Log.e("cwt", "MediaRecorder:" + mr.toString() + ",onError()-->Unspecified media recorder error.");
        }
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        switch (what) {
            case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                Log.v("cwt", "MediaRecorder:" + mr.toString() + ",onInfo()-->Max Duration reached");
            case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
                Log.e("cwt", "MediaRecorder:" + mr.toString() + ",onInfo()-->Max File size reached");
                break;
            case MediaRecorder.MEDIA_RECORDER_INFO_UNKNOWN:
                Log.e("cwt", "MediaRecorder:" + mr.toString() + ",onInfo()-->Unkown record info error");
                break;
        }
    }
}
