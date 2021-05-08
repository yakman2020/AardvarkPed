/*
 *  Copyright (c) 2014-2015 Aardvark Visual Software Inc,
 *
 *  All Rights Reserved.
 *
 */

package com.aardvark_visual.ped.aardvarkped;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;
import android.hardware.SensorEvent;



public abstract class AardvarkPedListener implements SensorEventListener {

    public abstract void setStepCountCallback(com.aardvark_visual.ped.aardvarkped.CounterUpdateCallback counterCallback);
    public abstract int  getStepCount();

    public abstract int  setStepCount(int numSteps);
    public abstract void ResetCount();
    public abstract void StartCount();
    public abstract void StopCount();
    public abstract void onAccuracyChanged(Sensor sensor, int accuracy);
    public abstract void onSensorChanged(SensorEvent event);
}
 


