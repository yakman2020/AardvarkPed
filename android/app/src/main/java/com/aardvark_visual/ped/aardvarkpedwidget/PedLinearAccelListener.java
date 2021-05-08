
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

import com.aardvark_visual.ped.aardvarkped.KalmanState;

public class PedLinearAccelListener extends AardvarkPedListener {

     private PrintStream log;
     private File f;
     private FileOutputStream fOut;
     private OutputStreamWriter out;
     private Logger logger;
     private KalmanState filterx;
     private KalmanState filtery;
     private KalmanState filterz;

     private KalmanState filter2x;
     private KalmanState filter2y;
     private KalmanState filter2z;

     private com.aardvark_visual.filters.InfiniteImpulseResponseFilter bandpassx;
     private com.aardvark_visual.filters.InfiniteImpulseResponseFilter bandpassy;
     private com.aardvark_visual.filters.InfiniteImpulseResponseFilter bandpassz;

     private boolean mCounting;
     private boolean mCalibrating;
     private double prevtimestamp;
     private double packet_start_time = 0.0;

     private final int NumSamples = 512;
     private double [] timestep     = new double [NumSamples];     // We keep all the measurments for smoothing
     private double [] m_AccelXAxis = new double [NumSamples];
     private double [] m_AccelYAxis = new double [NumSamples];
     private double [] m_AccelZAxis = new double [NumSamples];

     private double [] m_FilteredXAxis = new double [NumSamples];
     private double [] m_FilteredYAxis = new double [NumSamples];
     private double [] m_FilteredZAxis = new double [NumSamples];

     private double [] m_Filtered1XAxis = new double [NumSamples];
     private double [] m_Filtered1YAxis = new double [NumSamples];
     private double [] m_Filtered1ZAxis = new double [NumSamples];

     private double [] m_Filtered2XAxis = new double [NumSamples];
     private double [] m_Filtered2YAxis = new double [NumSamples];
     private double [] m_Filtered2ZAxis = new double [NumSamples];

     private int    [] needed = new int[1];

     private StepBuffer mStepCounter;

     private int mBufIdx;
     private int mBufNumEntries;
     private int mNumSteps = 0;
     protected int mSampleCount = 0;
     protected double [] m_GravityVector = null; // Kept up by the gravity listener
     private CounterUpdateCallback mCallback = null;


     public void setStepCountCallback(CounterUpdateCallback counterCallback) {
              mCallback = counterCallback;
          };

     // We get the measurement, aligned with gravity.  We select the measurement from the axis that 
     // has the largest value in the gravity vector, and invert it if the vector is negative. 
     // That way we can compensate for the orientation of the device, esp orientation changes. 

     protected int IncrementStepCount() {

               // We will put a call back in here so steps are updated when received. 
               // This will allow the callback to, for example, generate an update intent for the app or 
               // widget (or both) without requiring constant update events or requring the PedLinearAccelListener or PedStepCounterListener 
               // commit to intimate knowlege of the insides of the app.  Similarly it allows the app or widget to use the same logic regardless 
               // of whether the step count is coming from the LinearAccel listener or the step counter listener.  I expect we will use the 
               // step counter listener to count steps if it is available.  The same callback class will be set by both.
               
                mNumSteps++;
                if (mCallback != null) {
                    mCallback.onCounterUpdate(mNumSteps, 1);
                }
                return mNumSteps;
           };

     public int  getStepCount() {
                return mNumSteps;
           };

     public int  setStepCount(int numSteps) { // Used to restore the step state
                mNumSteps = numSteps;
                return mNumSteps;
           };

     public void ResetCount() {
             prevtimestamp  = 0.0;
             mBufIdx        = 0;
             mBufNumEntries = 0;
             mNumSteps = 0;

             // We prime the arrayList so we can just set, rather than add all the time.
             // Setting the initial capacity doesn't do that. Otherwise every time we did a set() we would get an 
             // array out of bounds exception. 

             for (int i = 0; i < NumSamples; i++ ) {
                 timestep[i] = 0.0;     // We keep all the measurments for smoothing
    
                 m_AccelXAxis[i] = 0.0;
                 m_AccelYAxis[i] = 0.0;
                 m_AccelZAxis[i] = 0.0;

                 m_FilteredXAxis[i] = 0.0;
                 m_FilteredYAxis[i] = 0.0;
                 m_FilteredZAxis[i] = 0.0;

                 m_Filtered1XAxis[i] = 0.0;
                 m_Filtered1YAxis[i] = 0.0;
                 m_Filtered1ZAxis[i] = 0.0;

                 m_Filtered2XAxis[i] = 0.0;
                 m_Filtered2YAxis[i] = 0.0;
                 m_Filtered2ZAxis[i] = 0.0;
             }
         };

     public void StartCount() {
             mCounting = true;
         };

     public void StopCount() {
             mCounting = false;
         };

     public PedLinearAccelListener(Context context, double[] gravityVector) {
             m_GravityVector = gravityVector;
             needed[0] = 0;
             File file_dir = context.getExternalFilesDir(null);

             try {
                 if (file_dir == null) {
                     file_dir = new File(context.getExternalFilesDir(null)+"/Documents/com.aardvark_visual.ped.aardvarkped/logs");
                     file_dir.mkdirs();
                 }
                 String full_filename = file_dir+"/aardvark.log";

                 f = new File(full_filename);
             }
             catch(Exception e) {
                 // do nothing
                 System.err.println("could not open log file");
             }

    
             try {
                 fOut = new FileOutputStream(f);
             }
             catch (Exception e3) {
                 System.err.println("could not open log file");
                 try {
                     f.createNewFile();
                     try {
                         fOut = new FileOutputStream(f);
                     }
                     catch (Exception e5) {
                         System.err.println("*2could not open log file");
                     }
                 }
                 catch (Exception e4) {
                     System.err.println("*3could not open log file");
                 }
             }
    
             System.err.println("log file = "+file_dir+"/aardvark.log");
             log = new PrintStream(fOut);
             logger = new Logger(log, Logger.LoggingLevel.WARNING);
    
             logger.print(Logger.LoggingLevel.VERBOSE, "zoot. JustZoot\n");
             logger.flush();
             mStepCounter = new StepBuffer(NumSamples, logger);
             mCounting      = false;
             mCalibrating   = false;
             mBufNumEntries = 0;
             mBufIdx = 0;
    
             for (int i = 0; i < NumSamples; i++ ) {
                 timestep[i] = 0.0;     // We keep all the measurments for smoothing
    
                 m_AccelXAxis[i] = 0.0;
                 m_AccelYAxis[i] = 0.0;
                 m_AccelZAxis[i] = 0.0;

                 m_FilteredXAxis[i] = 0.0;
                 m_FilteredYAxis[i] = 0.0;
                 m_FilteredZAxis[i] = 0.0;

                 m_Filtered1XAxis[i] = 0.0;
                 m_Filtered1YAxis[i] = 0.0;
                 m_Filtered1ZAxis[i] = 0.0;

                 m_Filtered2XAxis[i] = 0.0;
                 m_Filtered2YAxis[i] = 0.0;
                 m_Filtered2ZAxis[i] = 0.0;
             }
    
             // filterx = new KalmanState(0.00007, 0.17, 1.0, 1.0);
             // filtery = new KalmanState(0.00007, 0.17, 1.0, 1.0);
             // filterz = new KalmanState(0.00007, 0.17, 1.0, 1.0);

             // filter2x = new KalmanState(0.00003, 0.010, 1.0, 1.0);
             // filter2y = new KalmanState(0.00003, 0.010, 1.0, 1.0);
             // filter2z = new KalmanState(0.00003, 0.010, 1.0, 1.0);


             bandpassx = new com.aardvark_visual.filters.InfiniteImpulseResponseFilter();
             bandpassy = new com.aardvark_visual.filters.InfiniteImpulseResponseFilter();
             bandpassz = new com.aardvark_visual.filters.InfiniteImpulseResponseFilter();
             bandpassx.SetCoefficients(new com.aardvark_visual.filters.Order6ChebyshevLPF_SampleRate50_Fc_4_5());
             bandpassy.SetCoefficients(new com.aardvark_visual.filters.Order6ChebyshevLPF_SampleRate50_Fc_4_5());
             bandpassz.SetCoefficients(new com.aardvark_visual.filters.Order6ChebyshevLPF_SampleRate50_Fc_4_5());
         };

     public void onAccuracyChanged(Sensor sensor, int accuracy) {
          System.err.println("Step counter accuracy changed. new accuracy" + accuracy);
     }

     public void onSensorChanged(SensorEvent event) {
         float [] event_values = event.values.clone(); // Apparently the values are liable to change otherwise

          int fltridx = 0;

          // initial conditions. We need to get a timestemp to get the dt
           if (prevtimestamp == 0.0) {
               prevtimestamp  = (double)System.nanoTime();
           }
           try {
              // timestep[mBufIdx] = (event.timestamp - prevtimestamp) * (1.0/1000000000.0);
               timestep[mBufIdx] = event.timestamp * 1e-9;
     
              // We keep everything in separate axises unless reporting
    
                m_AccelXAxis[mBufIdx] = (double) event_values[0];
                m_AccelYAxis[mBufIdx] = (double) event_values[1];
                m_AccelZAxis[mBufIdx] = (double) event_values[2];
            
                // double filter. It cleans the signal up very nicely
            
       //     logger.SetLogLevel(Logger.LoggingLevel.INFO);

                logger.print(Logger.LoggingLevel.INFO, "dx[" + mSampleCount + "] = " + m_AccelXAxis[mBufIdx] + "\n");
                logger.print(Logger.LoggingLevel.INFO, "dy[" + mSampleCount + "] = " + m_AccelYAxis[mBufIdx] + "\n");
                logger.print(Logger.LoggingLevel.INFO, "dz[" + mSampleCount + "] = " + m_AccelZAxis[mBufIdx] + "\n");
                logger.print(Logger.LoggingLevel.INFO, "dt[" + mSampleCount + "] = " + timestep[mBufIdx]+"\n");

                mSampleCount++;
      //      logger.SetLogLevel(Logger.LoggingLevel.WARNING);
           }
           catch(Exception e) {
                System.err.println("onSensorChanged: exception in log and filter"+e);
           }

           mBufIdx++;
           if (mBufIdx >= NumSamples-1) {
              int sample_base = mSampleCount-mBufIdx;

              double maxX = 0.0;
              double maxY = 0.0;
              double maxZ = 0.0;
              double minX = 0.0;
              double minY = 0.0;
              double minZ = 0.0;
              double Signal_Threshold = 1.0;
              for (int i = 0; i < NumSamples; i++ ) {
                  if (m_AccelXAxis[i] > maxX ) {
                     maxX =  m_AccelXAxis[i];
                  }
                  if (m_AccelXAxis[i] < minX ) {
                     minX =  m_AccelXAxis[i];
                  }
                  if (m_AccelYAxis[i] > maxY ) {
                     maxY =  m_AccelYAxis[i];
                  }
                  if (m_AccelYAxis[i] < minY ) {
                     minY =  m_AccelYAxis[i];
                  }
                  if (m_AccelZAxis[i] > maxZ ) {
                     maxZ =  m_AccelZAxis[i];
                  }
                  if (m_AccelZAxis[i] < minZ ) {
                     minZ =  m_AccelZAxis[i];
                  }
              }

              if ((maxX-minX) < Signal_Threshold &&
                  (maxY-minY) < Signal_Threshold &&
                  (maxZ-minZ) < Signal_Threshold ) {

                // Nothing going on. clear the buffer and move on with life

                  Arrays.fill(m_AccelXAxis, 0.0);
                  Arrays.fill(m_AccelYAxis, 0.0);
                  Arrays.fill(m_AccelZAxis, 0.0);
                  Arrays.fill(timestep, 0.0);

                  mBufIdx = 0;
                  return;
              }


              bandpassx.FilterIIRBiquadForm2(m_AccelXAxis, m_Filtered1XAxis, m_AccelXAxis.length);
              bandpassy.FilterIIRBiquadForm2(m_AccelYAxis, m_Filtered1YAxis, m_AccelYAxis.length);
              bandpassz.FilterIIRBiquadForm2(m_AccelZAxis, m_Filtered1ZAxis, m_AccelZAxis.length);

/*
              bandpassx.FilterIIRBiquadForm2(m_FilteredXAxis, m_Filtered1XAxis, m_AccelXAxis.length);
              bandpassy.FilterIIRBiquadForm2(m_FilteredYAxis, m_Filtered1YAxis, m_AccelYAxis.length);
              bandpassz.FilterIIRBiquadForm2(m_FilteredZAxis, m_Filtered1ZAxis, m_AccelZAxis.length);

              bandpassx.FilterIIRBiquadForm2(m_Filtered1XAxis, m_Filtered2XAxis, m_AccelXAxis.length);
              bandpassy.FilterIIRBiquadForm2(m_Filtered1YAxis, m_Filtered2YAxis, m_AccelYAxis.length);
              bandpassz.FilterIIRBiquadForm2(m_Filtered1ZAxis, m_Filtered2ZAxis, m_AccelZAxis.length);
 */

              try {
                  int new_steps = 0;
            
                  mStepCounter.Fill(NumSamples, timestep, m_Filtered1XAxis, m_Filtered1YAxis, m_Filtered1ZAxis, m_GravityVector);
                  try {
                      new_steps = mStepCounter.RecogniseSteps(needed, sample_base);
                  }
                  catch(Exception e) {
                      System.err.println("onSensorChanged: exception recognise steps"+e);
                  }
            
                  if (new_steps > 0 ) {
                      mNumSteps += new_steps;
                      if (mCallback != null) {
            
                          // Update whatever
            
                          mCallback.onCounterUpdate(mNumSteps, new_steps);
                      }
                  }
              }
              catch(Exception e) {
                  System.err.println("onSensorChanged: exception in step counter call"+e);
              }
              try {
                  // System.err.printf(" mNumSteps = %d needed = %d\n", mNumSteps, needed[0]);
                  if (needed[0] < m_FilteredXAxis.length) {
                      System.arraycopy(m_AccelXAxis, needed[0], m_AccelXAxis, 0, m_AccelXAxis.length-needed[0]);
                      System.arraycopy(m_AccelYAxis, needed[0], m_AccelYAxis, 0, m_AccelYAxis.length-needed[0]);
                      System.arraycopy(m_AccelZAxis, needed[0], m_AccelZAxis, 0, m_AccelZAxis.length-needed[0]);
                      System.arraycopy(timestep, needed[0], timestep, 0, timestep.length-needed[0]);
                  }
                  else {
                      Arrays.fill(m_AccelXAxis, 0.0);
                      Arrays.fill(m_AccelYAxis, 0.0);
                      Arrays.fill(m_AccelZAxis, 0.0);
                      Arrays.fill(timestep, 0.0);
                  }
              } 
              catch(Exception e) { 
                  System.err.println("onSensorChanged: *2exception in step counter call");
              }

              mBufIdx = needed[0] % NumSamples;
              // System.err.printf("mBufIdx = %d\n", mBufIdx);
           }



           // A full buffer will always have the max number of entries;
           mBufNumEntries++;
           if ( mBufNumEntries > NumSamples ) {
               mBufNumEntries = NumSamples;
           }
      }
 }
 


