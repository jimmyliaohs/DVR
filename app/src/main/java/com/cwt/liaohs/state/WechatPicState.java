package com.cwt.liaohs.state;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.cwt.liaohs.cwtdvrplus.ContextUtil;
import com.cwt.liaohs.recorder.OnPicTakeListener;

/**
 * Created by liaohs on 2018/9/29.
 */

public class WechatPicState extends RecordState implements OnPicTakeListener{

    private static final int typeFront = 0;
    private static final int typeBack = 1;
    private int type;

    public WechatPicState(RecordStateManager stateManager,int type){
        super(stateManager);
        this.type = type;
    }

    @Override
    public void onStart() {
        stateManager.getRecordManager().takePic(this);
    }

    @Override
    public void onStop() {

    }

    @Override
    public void onPicTakeFinish(byte[] data) {
        String  picPath = stateManager.getRecordManager().getCurrentPicPath();
        if(!TextUtils.isEmpty(picPath)){
            Log.d("cwt","onPicTakeFinish()-->picPath="+picPath);
            Intent intent = new Intent("com.spreadwin.camera.snapover");
            intent.putExtra("video", false);

            if(type == typeFront){
                intent.putExtra("csi_file", "null");
                intent.putExtra("usb_file", picPath);
            }else if(type == typeBack){
                intent.putExtra("csi_file", picPath);
                intent.putExtra("usb_file", "null");
            }
            intent.putExtra("usb_ver", "2");
            intent.putExtra("error_code", 1);
            ContextUtil.getInstance().sendBroadcast(intent);
        }
    }
}
