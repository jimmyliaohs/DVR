package com.cwt.liaohs.state;

import android.util.Log;

import com.cwt.liaohs.bus.CrashFinish;
import com.cwt.liaohs.bus.CrashStart;
import com.cwt.liaohs.bus.Crashing;
import com.cwt.liaohs.bus.StartRecord;
import com.cwt.liaohs.bus.StopRecord;
import com.cwt.liaohs.cwtdvrplus.ContextUtil;
import com.cwt.liaohs.recorder.OnRecordFinishListener;
import com.cwt.liaohs.recorder.RecordSettings;
import com.cwt.liaohs.recorder.RecordStorage;

import static com.cwt.liaohs.cwtdvrplus.ContextUtil.BUS;

public class CrashState extends RecordState implements OnRecordFinishListener{
	private static final int typeFront = 0;
	private static final int typeBack = 1;

	private static int preRecordInterval = 1;
	private int type;
	public CrashState(RecordStateManager stateManager,int type) {
		super(stateManager);
		this.type = type;
	}

	public static int getPreRecordInterval(){
		return preRecordInterval;
	}

	@Override
	public void onStart() {
		Log.d("cwt","CrashState##stateManager-->"+stateManager+":onStart()");

		if(!RecordStorage.isSdcardAvailable()){
			Log.d("cwt","CrashState##stateManager-->"+stateManager+":sdcard not mounted!");
			if(type == typeFront){
				stateManager.changeToState(new IdleState(stateManager,typeFront));
			}else if(type == typeBack){
				stateManager.changeToState(new IdleState(stateManager,typeBack));
			}
		}else{

			if(type == typeFront){
				RecordSettings.setFrontCrashed(true);
			}else if(type == typeBack){
				RecordSettings.setBackCrashed(true);
			}

			preState = stateManager.getPreState();
			if(DriveState.class.isInstance(preState)){
				Log.d("cwt","CrashState##stateManager-->"+stateManager+":DriveState.class");
				stateManager.getRecordManager().stopRecordTask(null);
				if(type == typeBack){
					BUS.post(new StopRecord());
				}
			}else if(CrashState.class.isInstance(preState)){
				Log.d("cwt","Crash again!");
				return;
			}

			if(type == typeBack){
				preRecordInterval = RecordSettings.getRecordInterval();
				RecordSettings.setRecordInterval(2);
			}

			stateManager.getRecordManager().startRecordTask(CrashState.this);
			if(type == typeBack){
				BUS.post(new CrashStart());
				BUS.post(new StartRecord());
			}
		}

	}

	@Override
	public void onStop() {
		Log.d("cwt","CrashState##stateManager-->"+stateManager+":onStop()");
	}

	private RecordState preState;
	private RecordState curState;

	@Override
	public void onRecordFinish() {
		Log.d("cwt","type="+type+",紧急录像已结束！");

		if(type == typeBack){
			BUS.post(new Crashing());
			BUS.post(new StopRecord());
			RecordSettings.setRecordInterval(preRecordInterval);
		}

		if(type == typeFront){
			RecordSettings.setFrontCrashed(false);
		}else if(type == typeBack){
			RecordSettings.setBackCrashed(false);
		}

		if(DriveState.class.isInstance(preState)){
			Log.d("cwt","11111111#type="+type+":preState="+preState.getClass().toString());
			if(type == typeFront){
				stateManager.changeToState(new DriveState(stateManager,typeFront));
			}else if(type == typeBack){
				ContextUtil.getInstance().isDriveAfterCrash = true;
				BUS.post(new CrashFinish());
				BUS.post(new StartRecord());
				stateManager.changeToState(new DriveState(stateManager,typeBack));
			}

		}else{
			Log.d("cwt","222222#type="+type+":preState="+preState.getClass().toString());
			if(type == typeFront){
				stateManager.changeToState(new IdleState(stateManager,typeFront));
			}else if(type == typeBack){
				BUS.post(new CrashFinish());
				stateManager.changeToState(new IdleState(stateManager,typeBack));
			}
		}

	}
}
