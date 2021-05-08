
/*
 *  Copyright (c) 2014-2015 Aardvark Visual Software Inc,
 *
 *  All Rights Reserved.
 *
 */

package com.aardvark_visual.ped.aardvarkped;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.app.Activity;

import android.os.Build;
import android.os.Bundle;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;
import android.hardware.SensorEvent;



public class PedStepDetectorListener  implements SensorEventListener {
     private  SensorManager mSensorManager;
     private  Sensor mAccelerometer;

     private CounterUpdateCallback mCallback = null;
     private int mNumSteps;
     private boolean mCounting;

     public void setStepCountCallback(CounterUpdateCallback counterCallback) {
              mCallback = counterCallback;
          };

    public int  getStepCount() {
        return mNumSteps;
    };

    public int  setStepCount(int numSteps) { // Used to restore the step state
        mNumSteps = numSteps;
        return mNumSteps;
    };

    public void ResetCount() {
             mNumSteps = 0;
         };

     public void StartCount() {
             mCounting = true;
         };

     public void StopCount() {
             mCounting = false;
         };

     public PedStepDetectorListener() {
         System.err.println("zoot32");
         mCounting = false;
         mNumSteps = 0;
     }

     public void onAccuracyChanged(Sensor sensor, int accuracy) {
          System.err.println("Step counter accuracy changed. new accuracy " + accuracy);
     }

     public void onSensorChanged(SensorEvent event) {
         System.err.println("step detector event");
         // Only see one value on the step detector

         if (mCounting) {
             mNumSteps += (int)event.values[0];
             if (mCallback != null) {

              // Update whatever

                  mCallback.onCounterUpdate(mNumSteps, (int)event.values[0]);
             }
         }
     }
 }
 


