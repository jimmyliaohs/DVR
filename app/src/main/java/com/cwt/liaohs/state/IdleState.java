package com.cwt.liaohs.state;

import android.util.Log;

import com.cwt.liaohs.cwtdvrplus.ContextUtil;
import com.cwt.liaohs.recorder.RecordSettings;
import com.cwt.liaohs.recorder.RecordStorage;

public class IdleState extends RecordState {
	private static final int typeFront = 0;
	private static final int typeBack = 1;
	private int type;
	public IdleState(RecordStateManager stateManager,int type) {
		super(stateManager);
		this.type = type;
	}

	@Override
	public void onStart() {
		Log.d("cwt","IdleState##stateManager-->"+stateManager+":onStart()-->1");
		if(stateManager.getRecordManager().isRecording()){
			Log.d("cwt","IdleState##stateManager-->"+stateManager+":onStart()-->2");

			if(type == typeFront){
				if(RecordSettings.isFrontCrashedOn()){
					Log.d("cwt","IdleState##front-->##################3333333333");
					if(!RecordStorage.isSdcardAvailable() || ContextUtil.getInstance().isSdcardRemove){
						Log.d("cwt","IdleState##front-->Sdcard not mounted3333333333");
						RecordSettings.setFrontCrashed(false);
						stateManager.getRecordManager().stopRecordTask(null);
					}
					return;
				}
			}else if(type == typeBack){
				if(RecordSettings.isBackCrashedOn()){
					Log.d("cwt","IdleState##bacj-->####################44444444444444");
					if(!RecordStorage.isSdcardAvailable() || ContextUtil.getInstance().isSdcardRemove){
						Log.d("cwt","IdleState##front-->Sdcard not mounted44444444444444");
						RecordSettings.setBackCrashed(false);
						stateManager.getRecordManager().stopRecordTask(null);
					}
					return;
				}
			}

			stateManager.getRecordManager().stopRecordTask(null);
		}
	}

	@Override
	public void onStop() {
		Log.d("cwt","IdleState##stateManager-->"+stateManager+":onStop()");
	}
}
