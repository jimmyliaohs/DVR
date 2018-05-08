package com.cwt.liaohs.recorder;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

/**
 * Created by liaohs on 2018/4/23.
 */

public class CamearView extends SurfaceView implements SurfaceHolder.Callback {
    private Camera mCamera;
    private SurfaceHolder mHolder;

    public CamearView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void init(Camera camera) {
        this.mCamera = camera;
        this.mHolder = getHolder();
        this.mHolder.addCallback(this);
    }

    public SurfaceHolder getSurfaceHolder() {
        return mHolder;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.v("cwt", "surfaceCreated");
        if (mCamera == null || mHolder == null) {
            return;
        }
        startPreview(mCamera, mHolder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.v("cwt", "surfaceChanged");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.v("cwt", "surfaceDestroyed");
    }


    private void startPreview(Camera camera, SurfaceHolder holder) {
        if (camera != null && holder != null) {

            try {
                camera.setPreviewDisplay(holder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Camera.Parameters parameters = camera.getParameters();
            List<String> focusModes = parameters.getSupportedFocusModes();
//            parameters.setPreviewSize(prevWidth, prevHeight);
            if (focusModes != null) {
                for (String mode : focusModes) {
                    mode.contains("continuous-video");
                    parameters.setFocusMode("continuous-video");
                }
            }
            camera.setParameters(parameters);
            camera.startPreview();
            Log.v("cwt","startPreview");
        }
    }

}
