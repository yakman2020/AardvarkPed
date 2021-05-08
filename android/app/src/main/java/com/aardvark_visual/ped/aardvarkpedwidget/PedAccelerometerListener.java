
/*
 *  Copyright (c) 2014-2015 Aardvark Visual Software Inc,
 *
 *  All Rights Reserved.
 *
 */

package com.aardvark_visual.ped.aardvarkped;

import android.content.res.Configuration;
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
import java.util.Arrays;
import java.io.IOException.*;


public class PedAccelerometerListener extends com.aardvark_visual.ped.aardvarkped.PedLinearAccelListener {

     protected static double[] m_GravityVector = new double[3];


     public PedAccelerometerListener(Context context, double[] gravityVector) {
             super(context, m_GravityVector);
         };

     static double g = 0.0;  // at the equator.

    @Override
     public void onSensorChanged(SensorEvent event) {
         float [] event_values = event.values.clone();
         double accel_x = 0.0;
         double accel_y = 0.0;
         double accel_z = 0.0;
         double up_x = 0.0;
         double up_y = 0.0;
         double up_z = 0.0;
         double magnitude = 0.0;
         double latitude = 47.61; // We should actually get this in future, I suppose.

         if (g == 0.0) {
             double sin_sq = Math.sin(Math.toRadians(latitude));
             sin_sq *= sin_sq;
             g = 9.7803267714;
             g *= (1.0 + (0.00193185138639 * sin_sq)) / Math.sqrt(1.0 - (0.00669437999013 * sin_sq)); // latitude correction
         }

          up_x = event_values[0];
          up_y = event_values[1];
          up_z = event_values[2];
          magnitude = Math.sqrt(up_x*up_x+up_y*up_y+up_z*up_z);
          magnitude = 1.0/magnitude;
          up_x *= magnitude;
          up_y *= magnitude;
          up_z *= magnitude;

          // This gives us a rough orientation.
          up_x *= g;
          up_y *= g;
          up_z *= g;

          this.m_GravityVector[0] = (this.m_GravityVector[0]*0.5)+(up_x*0.5);
          this.m_GravityVector[1] = (this.m_GravityVector[1]*0.5)+(up_y*0.5);
          this.m_GravityVector[2] = (this.m_GravityVector[2]*0.5)+(up_z*0.5);

          accel_x = event_values[0] - this.m_GravityVector[0] ;
          accel_y = event_values[1] - this.m_GravityVector[1] ;
          accel_z = event_values[2] - this.m_GravityVector[2] ;

          event.values[0] = (float)accel_x;
          event.values[1] = (float)accel_y;
          event.values[2] = (float)accel_z;

          super.onSensorChanged(event);
      }
 }
 


