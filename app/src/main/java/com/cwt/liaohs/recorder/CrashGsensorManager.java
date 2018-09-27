package com.cwt.liaohs.recorder;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;

import com.cwt.liaohs.cwtdvrplus.ContextUtil;
import com.cwt.liaohs.state.CrashState;
import com.cwt.liaohs.state.RecordStateManager;

public class CrashGsensorManager implements SensorEventListener {
    private static CrashGsensorManager mInstance;
    private SensorManager mSensorManager;
    private Sensor mSensor;

    private float sensor_x = -1000;
    private float sensor_z = -1000;
    private float sensor_y = -1000;

    private long mLastDetectedTime;
    private long mLastCrashTime;

    private final int BASE_NUMBERS = 100;

    private int detectIntervel;
    private int mRecordIndex;
    private float mTotal_x;
    private float mTotal_z;
    private float mTotal_y;
    private float mG_x;
    private float mG_z;
    private float mG_y;

    private float mAllGsensors_x[];
    private float mAllGsensors_z[];
    private float mAllGsensors_y[];

    private boolean goodPoints[];

    long crashAgain = 30 * 1000;

    public static CrashGsensorManager getInstance() {
        if (mInstance == null) {
            mInstance = new CrashGsensorManager();
        }
        return mInstance;
    }

    private CrashGsensorManager() {
        mSensorManager = (SensorManager) ContextUtil.getInstance().getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        initValues();
    }

    public void startGSensor() {
        mSensorManager.registerListener(this, mSensor,
                SensorManager.SENSOR_DELAY_GAME);
    }

    public void stopGSensor() {
        mSensorManager.unregisterListener(this);
    }

    public void test(){
        startCrashTask();
    }

    private void initValues() {
        detectIntervel = 100;
        mRecordIndex = 0;
        mTotal_x = 0;
        mTotal_z = 0;
        mTotal_y = 0;
        mG_x = 0;
        mG_z = 0;
        mG_y = 0;

        mAllGsensors_x = new float[BASE_NUMBERS];
        mAllGsensors_z = new float[BASE_NUMBERS];
        mAllGsensors_y = new float[BASE_NUMBERS];
        goodPoints = new boolean[BASE_NUMBERS];

        for (int i = 0; i < goodPoints.length; i++) {
            goodPoints[i] = false;
        }

    }

    private RecordStateManager frontStateManager;
    private RecordStateManager backStateManager;

    private static final int typeFront = 0;
    private static final int typeBack = 1;

    private void startCrashTask(){
        backStateManager = ContextUtil.getInstance().getStateManager(false);
        frontStateManager = ContextUtil.getInstance().getStateManager(true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                if(backStateManager != null){
                    backStateManager.changeToState(new CrashState(backStateManager,typeBack));
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                if(frontStateManager != null){
                    frontStateManager.changeToState(new CrashState(frontStateManager,typeFront));
                }
            }
        }).start();

        mLastCrashTime = System.currentTimeMillis();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (System.currentTimeMillis() - mLastDetectedTime < detectIntervel
                || !RecordSettings.isCrashToggle() || !RecordStorage.isSdcardAvailable()) {
            return;
        }

        mLastDetectedTime = System.currentTimeMillis();

        sensor_x = event.values[SensorManager.DATA_X];
        sensor_z = event.values[SensorManager.DATA_Z];
        sensor_y = event.values[SensorManager.DATA_Y];

        if (mRecordIndex < 100) {
            mAllGsensors_x[mRecordIndex] = sensor_x;
            mTotal_x += sensor_x;
            mAllGsensors_y[mRecordIndex] = sensor_y;
            mTotal_y += sensor_y;
            mAllGsensors_z[mRecordIndex] = sensor_z;
            mTotal_z += sensor_z;
            mRecordIndex++;
            return;
        }

        int arrayIndex = mRecordIndex % BASE_NUMBERS;
        mTotal_x -= mAllGsensors_x[arrayIndex];
        mTotal_z -= mAllGsensors_z[arrayIndex];
        mTotal_y -= mAllGsensors_y[arrayIndex];

        mTotal_x += sensor_x;
        mTotal_z += sensor_z;
        mTotal_y += sensor_y;

        mG_x = mTotal_x / BASE_NUMBERS;
        mG_z = mTotal_z / BASE_NUMBERS;
        mG_y = mTotal_y / BASE_NUMBERS;

        mAllGsensors_x[arrayIndex] = sensor_x;
        mAllGsensors_z[arrayIndex] = sensor_z;
        mAllGsensors_y[arrayIndex] = sensor_y;

        float deta_x = sensor_x - mG_x;
        float deta_z = sensor_z - mG_z;
        float deta_y = sensor_y - mG_y;

        int crashLevel;

        crashLevel = ((2 * RecordSettings.getCrashSenseLevel()) + 1) * 5;

        if (calForwardForce(deta_x, deta_z) > crashLevel || calForwardForce(deta_y, deta_z) > crashLevel) {
            goodPoints[arrayIndex] = true;
        } else {
            goodPoints[arrayIndex] = false;
        }

        if (System.currentTimeMillis() - mLastCrashTime > crashAgain) {
            int goodPointNum = 0;
            for (int i = 0; i < 15; i++) {
                if (goodPoints[(BASE_NUMBERS + arrayIndex - i)
                        % BASE_NUMBERS]) {
                    goodPointNum++;
                }
            }

            if (goodPointNum > 6) {//10
                startCrashTask();
                //把之前的mIsGoodPoints重新置为原始值
                for (int i = 0; i < goodPoints.length; i++) {
                    goodPoints[i] = false;
                }

            }

        }
        mRecordIndex++;
    }

    private double calForwardForce(float force_x, float force_z) {
        float squreForce = force_x * force_x + force_z * force_z;
        float squreG = mG_x * mG_x + mG_z * mG_z;
        double cosin_force_g = (force_x * mG_x + force_z * mG_z) / Math.sqrt(squreForce * squreG);
        return squreForce * (1 - cosin_force_g * cosin_force_g);
    }

}
