package com.cwt.liaohs.state;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.cwt.liaohs.bus.StartRecord;
import com.cwt.liaohs.bus.StopRecord;
import com.cwt.liaohs.cwtdvrplus.ContextUtil;
import com.cwt.liaohs.recorder.OnRecordFinishListener;
import com.cwt.liaohs.recorder.RecordSettings;
import com.cwt.liaohs.recorder.RecordStorage;

import static com.cwt.liaohs.cwtdvrplus.ContextUtil.BUS;

public class WechatVedioState extends RecordState implements OnRecordFinishListener{
	private static final int typeFront = 0;
	private static final int typeBack = 1;

	private static int preRecordInterval = 1;
	private int type;
	public WechatVedioState(RecordStateManager stateManager, int type) {
		super(stateManager);
		this.type = type;
	}

	@Override
	public void onStart() {
		Log.d("cwt","WechatVedioState##stateManager-->"+stateManager+":onStart()");
		if(!RecordStorage.isSdcardAvailable()){
			if(type == typeFront){
				stateManager.changeToState(new IdleState(stateManager,typeFront));
			}else if(type == typeBack){
				stateManager.changeToState(new IdleState(stateManager,typeBack));
			}
		}else{
			if(type == typeFront){
				RecordSettings.setFrontWechatVedio(true);
			}else if(type == typeBack){
				RecordSettings.setBackWechatVedio(true);
			}

			preState = stateManager.getPreState();
			if(DriveState.class.isInstance(preState)){
				stateManager.getRecordManager().stopRecordTask(null);
				if(type == typeBack){
					BUS.post(new StopRecord());
				}
			}else if(WechatVedioState.class.isInstance(preState)){
				stateManager.getRecordManager().stopRecordTask(null);
				if(type == typeBack){
					BUS.post(new StopRecord());
				}
			}

			if(DriveState.class.isInstance(preState)){
				if(type == typeBack){
//					preRecordInterval = RecordSettings.getRecordInterval();
//					RecordSettings.setRecordInterval(2);
				}
			}

			stateManager.getRecordManager().startRecordTask(WechatVedioState.this);
			if(type == typeBack){
				BUS.post(new StartRecord());
			}
		}

	}

	@Override
	public void onStop() {
		Log.d("cwt","WechatVedioState##stateManager-->"+stateManager+":onStop()");
	}

	private RecordState preState;
	private RecordState curState;

	@Override
	public void onRecordFinish() {
		Log.d("cwt","WechatVedioState##stateManager-->"+stateManager+":onRecordFinish()");

		String  vedioPath = stateManager.getRecordManager().getCurrentVedioPath();
		if(!TextUtils.isEmpty(vedioPath)){
			Log.d("cwt","WechatVedioState##stateManager-->"+stateManager+":upload vedio");
			Intent intent = new Intent("com.spreadwin.camera.snapover");
			intent.putExtra("video", true);

			if(type == typeFront){
				intent.putExtra("csi_file", "null");
				intent.putExtra("usb_file", vedioPath);
			}else if(type == typeBack){
				intent.putExtra("csi_file", vedioPath);
				intent.putExtra("usb_file", "null");
			}
			intent.putExtra("usb_ver", "2");
			intent.putExtra("error_code", 1);
			ContextUtil.getInstance().sendBroadcast(intent);
		}

		if(type == typeBack){
			BUS.post(new StopRecord());
//			RecordSettings.setRecordInterval(preRecordInterval);
		}

		if(type == typeFront){
			RecordSettings.setFrontWechatVedio(false);
		}else if(type == typeBack){
			RecordSettings.setBackWechatVedio(false);
		}

		if(DriveState.class.isInstance(preState)){
			if(type == typeFront){
				stateManager.changeToState(new DriveState(stateManager,typeFront));
			}else if(type == typeBack){
				stateManager.changeToState(new DriveState(stateManager,typeBack));
				ContextUtil.getInstance().isDriveAfterWechatVedio = true;
				BUS.post(new StartRecord());
			}
		}else{
			if(type == typeFront){
				stateManager.changeToState(new IdleState(stateManager,typeFront));
			}else if(type == typeBack){
				stateManager.changeToState(new IdleState(stateManager,typeBack));
			}
		}

	}
}
