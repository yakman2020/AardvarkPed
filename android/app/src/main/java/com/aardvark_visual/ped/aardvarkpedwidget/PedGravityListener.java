
/*
 *  Copyright (c) 2014-2015 Aardvark Visual Software Inc,
 *
 *  All Rights Reserved.
 *
 */

package com.aardvark_visual.ped.aardvarkped;

import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.app.Activity;

import android.os.Build;
import android.os.Bundle;
import android.content.*;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;
import android.hardware.SensorEvent;

import java.io.*;
import java.io.IOException.*;



public class PedGravityListener  implements SensorEventListener {

     // Constants for the WGS 84 Ellipsoidal gravity formula. 
     // we need to know our latitutde to find the bias. Gravity varies by up to .06 m/sec^2 from pole to equator. 

     private final double Gravity(double latitude) {
                 double g = 9.7803267714;  // at the equator.
                 double sin_sq = Math.sin(Math.toRadians(latitude));
                 sin_sq *= sin_sq;

                 g *= (1.0+(0.00193185138639*sin_sq))/Math.sqrt(1.0-(0.00669437999013*sin_sq)); // latitude correction
                
                 return g;
            };


     // First we calibrate face up, then on edge

     private boolean mCalibrateXY = false;
     private CalibrationData mCalibrationXY = null;

     private boolean mCalibrateYZ = false;
     private CalibrationData mCalibrationYZ = null;

     private boolean mCalibrateXZ = false;
     private CalibrationData mCalibrationXZ = null;

     private double prevtimestamp = 0.0;

     private final int NumCalSamples = 1024;
     private final int NumPreSamples = 100;

     private double[] timestep     = new double[NumCalSamples];     // We keep all the measurments for smoothing
     private double[] m_GravityXAxis = new double[NumCalSamples];
     private double[] m_GravityYAxis = new double[NumCalSamples];
     private double[] m_GravityZAxis = new double[NumCalSamples];

     private int mCalSamples;
     private int mPreSamples;
     private int SampleCount = 0;

     double max_x = -Double.MAX_VALUE;
     double max_y = -Double.MAX_VALUE;
     double max_z = -Double.MAX_VALUE;

     double min_x = Double.MAX_VALUE;
     double min_y = Double.MAX_VALUE;
     double min_z = Double.MAX_VALUE;

     double average_x = 0.0;
     double average_y = 0.0;
     double average_z = 0.0;

     double mStandardGravity = 0.0;

     double[] mCurrentGravityVector;

/*
    protected double lowPass( double input, double output ) {
        output = output + ALPHA * (input - output);
        return output;
    }
 */

     public PedGravityListener(Context context, double latitude, double [] gravityVector) {

         System.err.println("zoot62");

         mCalibrateXY = false; //false;
         mCalSamples    = 0;

         mStandardGravity = Gravity(latitude);
         mCurrentGravityVector = gravityVector;
     }

     public void onAccuracyChanged(Sensor sensor, int accuracy) {
          System.err.println("Gravity sensor accuracy changed. new accuracy" + accuracy);
     }

     public void onSensorChanged(SensorEvent event) {
         float [] event_values = event.values.clone(); // Apparently the values are liable to change otherwise

         mCurrentGravityVector[0] = event_values[0];
         mCurrentGravityVector[1] = event_values[1];
         mCurrentGravityVector[2] = event_values[2];
         if (mCalibrateXY) {
              double vec_x, vec_y, vec_z;
              double magnitude, invmagnitude;

              if (mPreSamples++ < NumPreSamples) {
                  return;
              }
              
              if (prevtimestamp == 0.0) {
                  prevtimestamp  = event.timestamp;
              }

              timestep[mCalSamples] = (event.timestamp - prevtimestamp) * (1.0/1.0e9);
 
              // We keep everything in separate axises

              m_GravityXAxis[mCalSamples] = event_values[0];
              m_GravityYAxis[mCalSamples] = event_values[1];
              m_GravityZAxis[mCalSamples] = event_values[2];


              mCalSamples++;
              if (mCalSamples == NumCalSamples ) {

                  mCalibrationXY = new CalibrationData();

                  for (int i = 0; i < m_GravityXAxis.length; i++ ) {
                      average_x += m_GravityXAxis[i];
                      if (min_x > m_GravityXAxis[i]) {
                          min_x = m_GravityXAxis[i];
                      }
                      if (max_x < m_GravityXAxis[i]) {
                          max_x = m_GravityXAxis[i];
                      }
                  }
                  average_x *= 1.0/(double)m_GravityXAxis.length;

                  for (int i = 0; i < m_GravityYAxis.length; i++ ) {
                      average_y += m_GravityYAxis[i];
                      if (min_y > m_GravityYAxis[i]) {
                          min_y = m_GravityYAxis[i];
                      }
                      if (max_y < m_GravityYAxis[i]) {
                          max_y = m_GravityYAxis[i];
                      }
                  }
                  average_y *= 1.0/(double)m_GravityYAxis.length;

                  for (int i = 0; i < m_GravityZAxis.length; i++ ) {
                      average_z += m_GravityZAxis[i];
                      if (min_z > m_GravityZAxis[i]) {
                          min_z = m_GravityZAxis[i];
                      }
                      if (max_z < m_GravityZAxis[i]) {
                          max_z = m_GravityZAxis[i];
                      }
                  }
                  average_z *= 1.0/(double)m_GravityZAxis.length;

                  // Now, we need to estimate our rotation vector using admittedly biased, noisy data.
                  // but we figure the bias will cancel out, or at any event have far less significance.
                  
                  magnitude = Math.sqrt(average_x*average_x+
                                   average_y*average_y+
                                   average_z*average_z);

                  invmagnitude = 1.0/magnitude;

                  vec_x = average_x*invmagnitude;
                  vec_y = average_y*invmagnitude;
                  vec_z = average_z*invmagnitude;
                  
                  mCalibrationXY.mBiasX = average_x-(vec_x*mStandardGravity);
                  mCalibrationXY.mBiasY = average_y-(vec_y*mStandardGravity);
                  mCalibrationXY.mBiasZ = average_z-(vec_z*mStandardGravity);

                  System.err.println("bias x="+mCalibrationXY.mBiasX+", y="+mCalibrationXY.mBiasY+"z="+mCalibrationXY.mBiasZ);

                  mCalibrationXY.mNoiseX = max_x-min_x;
                  mCalibrationXY.mNoiseY = max_y-min_y;
                  mCalibrationXY.mNoiseZ = max_z-min_z;

                  System.err.println("noise x="+mCalibrationXY.mNoiseX+", y="+mCalibrationXY.mNoiseY+"z="+mCalibrationXY.mNoiseZ);


                  mCalSamples = 0; // We cal'ed the first axis, now do the second.
                  mPreSamples = 0; 
                  prevtimestamp = 0;
                  max_x = -Double.MAX_VALUE;
                  max_y = -Double.MAX_VALUE;
                  max_z = -Double.MAX_VALUE;

                  min_x = Double.MAX_VALUE;
                  min_y = Double.MAX_VALUE;
                  min_z = Double.MAX_VALUE;

                  average_x = 0.0;
                  average_y = 0.0;
                  average_z = 0.0;

                  mCalibrateXY = false;
                  mCalibrateYZ = true;
              }
         }
         else if (mCalibrateYZ) {
              double vec_x, vec_y, vec_z;
              double magnitude, invmagnitude;
              

              // Settling time; 
              if (mPreSamples++ < NumPreSamples) {
                  return;
              }

              if (prevtimestamp == 0.0) {
                  prevtimestamp  = event.timestamp;
              }

              timestep[mCalSamples] = (event.timestamp - prevtimestamp) * (1.0/1.0e9);
 
              // We keep everything in separate axises

              m_GravityXAxis[mCalSamples] = event_values[0];
              m_GravityYAxis[mCalSamples] = event_values[1];
              m_GravityZAxis[mCalSamples] = event_values[2];

              mCalSamples++;
              if (mCalSamples == NumCalSamples ) {

                  mCalibrationYZ = new CalibrationData();

                  for (int i = 0; i < m_GravityXAxis.length; i++ ) {
                      average_x += m_GravityXAxis[i];
                      if (min_x > m_GravityXAxis[i]) {
                          min_x = m_GravityXAxis[i];
                      }
                      if (max_x < m_GravityXAxis[i]) {
                          max_x = m_GravityXAxis[i];
                      }
                  }
                  average_x *= 1.0/(double)mCalSamples;

                  for (int i = 0; i < m_GravityYAxis.length; i++ ) {
                      average_y += m_GravityYAxis[i];
                      if (min_y > m_GravityYAxis[i]) {
                          min_y = m_GravityYAxis[i];
                      }
                      if (max_y < m_GravityYAxis[i]) {
                          max_y = m_GravityYAxis[i];
                      }
                  }
                  average_y *= 1.0/(double)m_GravityYAxis.length;

                  for (int i = 0; i < m_GravityZAxis.length; i++ ) {
                      average_z += m_GravityZAxis[i];
                      if (min_z > m_GravityZAxis[i]) {
                          min_z = m_GravityZAxis[i];
                      }
                      if (max_z < m_GravityZAxis[i]) {
                          max_z = m_GravityZAxis[i];
                      }
                  }
                  average_z *= 1.0/(double)m_GravityZAxis.length;

                  // Now, we need to estimate our rotation vector using admittedly biased, noisy data.
                  // but we figure the bias will cancel out, or at any event have far less significance.
                  
                  magnitude = Math.sqrt(average_x*average_x+
                                   average_y*average_y+
                                   average_z*average_z);

                  invmagnitude = 1.0/magnitude;

                  vec_x = average_x*invmagnitude;
                  vec_y = average_y*invmagnitude;
                  vec_z = average_z*invmagnitude;
                  
                  mCalibrationYZ.mBiasX = average_x-(vec_x*mStandardGravity);
                  mCalibrationYZ.mBiasY = average_y-(vec_y*mStandardGravity);
                  mCalibrationYZ.mBiasZ = average_z-(vec_z*mStandardGravity);

                  System.err.println("bias x="+mCalibrationYZ.mBiasX+", y="+mCalibrationYZ.mBiasY+"z="+mCalibrationYZ.mBiasZ);

                  mCalibrationYZ.mNoiseX = max_x-min_x;
                  mCalibrationYZ.mNoiseY = max_y-min_y;
                  mCalibrationYZ.mNoiseZ = max_z-min_z;

                  System.err.println("noise x="+mCalibrationYZ.mNoiseX+", y="+mCalibrationYZ.mNoiseY+"z="+mCalibrationYZ.mNoiseZ);

                  mCalSamples = 0; // We cal'ed the first axis, now do the second.
                  mPreSamples = 0; 
                  prevtimestamp = 0;
                  max_x = -Double.MAX_VALUE;
                  max_y = -Double.MAX_VALUE;
                  max_z = -Double.MAX_VALUE;

                  min_x = Double.MAX_VALUE;
                  min_y = Double.MAX_VALUE;
                  min_z = Double.MAX_VALUE;

                  average_x = 0.0;
                  average_y = 0.0;
                  average_z = 0.0;

                  mCalibrateYZ = false;
                  mCalibrateXZ = true;
              }
         }
         else if (mCalibrateXZ) {
              double vec_x, vec_y, vec_z;
              double magnitude, invmagnitude;
              
              // Settling time; 
              if (mPreSamples++ < NumPreSamples) {
                  return;
              }

              if (prevtimestamp == 0.0) {
                  prevtimestamp  = event.timestamp;
              }

              timestep[mCalSamples] = (event.timestamp - prevtimestamp) * (1.0/1.0e9);
 
              // We keep everything in separate axises

              m_GravityXAxis[mCalSamples] = event_values[0];
              m_GravityYAxis[mCalSamples] = event_values[1];
              m_GravityZAxis[mCalSamples] = event_values[2];

              mCalSamples++;
              if (mCalSamples == NumCalSamples ) {

                  mCalibrationXZ = new CalibrationData();

                  for (int i = 0; i < m_GravityXAxis.length; i++ ) {
                      average_x += m_GravityXAxis[i];
                      if (min_x > m_GravityXAxis[i]) {
                          min_x = m_GravityXAxis[i];
                      }
                      if (max_x < m_GravityXAxis[i]) {
                          max_x = m_GravityXAxis[i];
                      }
                  }
                  average_x *= 1.0/(double)mCalSamples;

                  for (int i = 0; i < m_GravityYAxis.length; i++ ) {
                      average_y += m_GravityYAxis[i];
                      if (min_y > m_GravityYAxis[i]) {
                          min_y = m_GravityYAxis[i];
                      }
                      if (max_y < m_GravityYAxis[i]) {
                          max_y = m_GravityYAxis[i];
                      }
                  }
                  average_y *= 1.0/(double)m_GravityYAxis.length;

                  for (int i = 0; i < m_GravityZAxis.length; i++ ) {
                      average_z += m_GravityZAxis[i];
                      if (min_z > m_GravityZAxis[i]) {
                          min_z = m_GravityZAxis[i];
                      }
                      if (max_z < m_GravityZAxis[i]) {
                          max_z = m_GravityZAxis[i];
                      }
                  }
                  average_z *= 1.0/(double)m_GravityZAxis.length;

                  // Now, we need to estimate our rotation vector using admittedly biased, noisy data.
                  // but we figure the bias will cancel out, or at any event have far less significance.
                  
                  magnitude = Math.sqrt(average_x*average_x+
                                   average_y*average_y+
                                   average_z*average_z);

                  invmagnitude = 1.0/magnitude;

                  vec_x = average_x*invmagnitude;
                  vec_y = average_y*invmagnitude;
                  vec_z = average_z*invmagnitude;
                  
                  mCalibrationXZ.mBiasX = average_x-(vec_x*mStandardGravity);
                  mCalibrationXZ.mBiasY = average_y-(vec_y*mStandardGravity);
                  mCalibrationXZ.mBiasZ = average_z-(vec_z*mStandardGravity);

                  System.err.println("bias x="+mCalibrationXZ.mBiasX+", y="+mCalibrationXZ.mBiasY+"z="+mCalibrationXZ.mBiasZ);

                  mCalibrationXZ.mNoiseX = max_x-min_x;
                  mCalibrationXZ.mNoiseY = max_y-min_y;
                  mCalibrationXZ.mNoiseZ = max_z-min_z;

                  System.err.println("noise x="+mCalibrationXZ.mNoiseX+", y="+mCalibrationXZ.mNoiseY+"z="+mCalibrationXZ.mNoiseZ);

                  mCalSamples = 0; // We cal'ed the first axis, now do the second.
                  mPreSamples = 0; 
                  prevtimestamp = 0;
                  max_x = -Double.MAX_VALUE;
                  max_y = -Double.MAX_VALUE;
                  max_z = -Double.MAX_VALUE;

                  min_x = Double.MAX_VALUE;
                  min_y = Double.MAX_VALUE;
                  min_z = Double.MAX_VALUE;

                  average_x = 0.0;
                  average_y = 0.0;
                  average_z = 0.0;

                  mCalibrateXZ = false;
              }
         }
     }
 }
 


