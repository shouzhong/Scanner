package com.shouzhong.scanner;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 *
 * 加速感应器
 */

class SensorController implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mSensor;

    private Callback callback;

    private float mX, mY, mZ;
    private long currentTime = 0;
    private long delay;
    private boolean isMove;

    SensorController(Context context) {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager == null ? null : mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);// TYPE_GRAVITY
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == null || event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        if (!isMove) isMove = (mX - x) * (mX - x) + (mY - y) * (mY - y) + (mZ - z) * (mZ - z) > 2;
        long time = System.currentTimeMillis();
        if (isMove && time - currentTime > delay) {
            currentTime = time;
            mX = x;
            mY = y;
            mZ = z;
            isMove = false;
            if (callback != null) callback.onChanged();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    void onStart() {
        currentTime = System.currentTimeMillis();
        isMove = true;
        if (mSensorManager == null || mSensor == null) return;
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    void onStop() {
        if (mSensorManager == null || mSensor == null) return;
        mSensorManager.unregisterListener(this, mSensor);
    }

    void setDelay(long l) {
        this.delay = l;
    }

    void setCallback(Callback callback) {
        this.callback = callback;
    }

    public interface Callback {
        void onChanged();
    }
}
