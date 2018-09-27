package com.cwt.liaohs.state;

import android.util.Log;

import com.cwt.liaohs.recorder.OnRecordFinishListener;
import com.cwt.liaohs.recorder.RecordSettings;

public class DriveState extends RecordState implements OnRecordFinishListener {
	private static final int typeFront = 0;
	private static final int typeBack = 1;

	private int type;
	public DriveState(RecordStateManager stateManager,int type) {
		super(stateManager);
		this.type = type;
	}

	@Override
	public void onStart() {

		if(type == typeFront){
			if(RecordSettings.isFrontCrashedOn()){
				Log.d("cwt","DriveState##Front"+":onStart()-->1");
				return;
			}else{
				Log.d("cwt","DriveState##Front"+":onStart()-->2");
				stateManager.getRecordManager().startRecordTask(this);
			}
		}else if(type == typeBack){
			if(RecordSettings.isBackCrashedOn()){
				Log.d("cwt","DriveState##Back"+":onStart()-->1");
				return;
			}else{
				Log.d("cwt","DriveState##Back"+":onStart()-->2");
				stateManager.getRecordManager().startRecordTask(this);
			}

		}

	}

	@Override
	public void onStop() {
		Log.d("cwt","DriveState##stateManager-->"+stateManager+":onStop()");
	}

	@Override
	public void onRecordFinish() {
		Log.d("cwt","DriveState-->onRecordFinish()");
		stateManager.getRecordManager().startRecordTask(this);
	}
}
