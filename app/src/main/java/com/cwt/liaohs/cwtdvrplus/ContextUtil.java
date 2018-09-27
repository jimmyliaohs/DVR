package com.cwt.liaohs.cwtdvrplus;

import android.app.Application;
import android.view.View;
import android.widget.Toast;

import com.cwt.liaohs.bus.SdcardNotMounted;
import com.cwt.liaohs.bus.StartRecord;
import com.cwt.liaohs.bus.StopRecord;
import com.cwt.liaohs.recorder.CrashGsensorManager;
import com.cwt.liaohs.recorder.RecordDatabaseManager;
import com.cwt.liaohs.recorder.RecordDbHelper;
import com.cwt.liaohs.state.RecordStateManager;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.squareup.otto.ThreadEnforcer;

public class ContextUtil extends Application {
    private static ContextUtil contextUtil;
    public long mRecordingBegin;
    public boolean isRecording = false;
    public boolean isDriveAfterCrash = false;
    public boolean isSdcardRemove = false;
    public RecordStateManager frontStateManager = null;
    public RecordStateManager backStateManager = null;

    public static final int BITRATE_LOW = 8 * 1024 * 1000;
    public static final int BITRATE_MID = 10 * 1024 * 1000;
    public static final int BITRATE_HIG = 12 * 1024 * 1000;

    public static final int CLASH_LEVEL_LOW = 8;
    public static final int CLASH_LEVEL_MID = 4;
    public static final int CLASH_LEVEL_HIG = 2;

    public static final Bus BUS = new Bus(ThreadEnforcer.ANY);

    public static ContextUtil getInstance() {
        return contextUtil;
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        contextUtil = this;
        BUS.register(this);
        CrashGsensorManager.getInstance().startGSensor();
        RecordDatabaseManager.initializeInstance(RecordDbHelper.getInstance(this));
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        BUS.unregister(this);
        CrashGsensorManager.getInstance().stopGSensor();
    }

    public void setRecordStateManager(RecordStateManager stateManager,boolean isFront){
        if(isFront){
            frontStateManager = stateManager;
        }else{
            backStateManager = stateManager;
        }
    }

    public RecordStateManager getStateManager(boolean isFront){
        if(isFront){
            return frontStateManager;
        }else{
            return backStateManager;
        }
    }

    @Subscribe
    public void onStartRecord(StartRecord startRecord) {
        isRecording = true;
        mRecordingBegin = System.currentTimeMillis();
    }

    @Subscribe
    public void onStopRecord(StopRecord stopRecord) {
        isRecording = false;
        mRecordingBegin = 0;
    }

    @Subscribe
    public void onSdcardNotMounted(SdcardNotMounted sdcardNotMounted) {
        isRecording = false;
        mRecordingBegin = 0;
    }

}
