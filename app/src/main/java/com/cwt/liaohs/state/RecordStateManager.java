package com.cwt.liaohs.state;

import android.hardware.Camera;
import android.view.TextureView;

import com.cwt.liaohs.recorder.RecordManager;

public class RecordStateManager {

    private RecordState mCurState;
    private RecordState mPreState;

    private RecordManager recordManager;
    private static final int backCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private static final int typeFront = 0;
    private static final int typeBack = 1;

    public RecordStateManager(int cameraId, TextureView textureView) {
        recordManager = new RecordManager(cameraId, textureView);
        if (recordManager.isCameraValid()) {
            mCurState = new IdleState(this,recordManager.cameraId == backCameraId  ?  typeBack : typeFront);
        } else {
            mCurState = new CameraErrorState(this);
        }
        mPreState = mCurState;
    }

    public RecordManager getRecordManager() {
        return recordManager;
    }

    public RecordState getCurState() {
        return mCurState;
    }

    public RecordState getPreState() {
        return mPreState;
    }

    public void changeToState(RecordState recordState) {
        mCurState.onStop();
        mPreState = mCurState;
        mCurState = recordState;
        mCurState.onStart();
    }

}
