
/*
 *  Copyright (c) 2014-2015 Aardvark Visual Software Inc,
 *
 *  All Rights Reserved.
 *
 */

package com.aardvark_visual.ped.aardvarkped;

import java.util.Arrays;
import java.util.ArrayList;
import com.aardvark_visual.ped.aardvarkped.Feature;
import java.io.*;



public class StepBuffer {

      // If we know what we are doing, we can select the most likely step rate 
      // to look for.

    protected final int SAMPLES_PER_SECOND       = 50;
    protected final int SAMPLES_PER_WINDOW       = 200;
    // protected final int THRESHOLD_UPDATE_SAMPLES = 60;

    protected final double MIN_STEPS_PER_SECOND    = 0.5;
    protected final double MAX_STEPS_PER_SECOND    = 3.5;

    protected final int WALKING_PACE_SAMPLES_PER_STEP = (int)(SAMPLES_PER_SECOND/2.0);  // 120 steps per second is a normal walking pace
    protected final int JOGGING_PACE_SAMPLES_PER_STEP = (int)(SAMPLES_PER_SECOND/2.5);  // 150 steps per second is a normal jogging pace
    protected final int RUNNING_PACE_SAMPLES_PER_STEP = (int)(SAMPLES_PER_SECOND/6.0);  // 180 steps per second is a normal running pace

    protected final int MAX_SAMPLES_PER_STEP = (int)(SAMPLES_PER_SECOND/MIN_STEPS_PER_SECOND);
    protected final int MIN_SAMPLES_PER_STEP = (int)(SAMPLES_PER_SECOND/MAX_STEPS_PER_SECOND);

      enum ActivityCode {
              Stationary,
              SlowWalking,
              Walking,
              FastWalking,
              Running
          };

      // Size of the step buffer is determined by the sample rate and expected step rates.

      public StepBuffer(int size, Logger logger) {
               mSampleCount = 0;
               mLog        = logger;
               mActivity   = ActivityCode.Stationary;
               mSize       = size;
               mMinVal     = Double.NaN;
               mMaxVal     = Double.NaN;
               mAvgVal     = Double.NaN;
               mTimevals   = new double[mSize];
               mDataBuffer = new double[mSize];
               mFeatures   = new ArrayList();
          };

      public void Fill(int numSamples, double[] times, double[] accelBuffX, double[] accelBuffY, double[] accelBuffZ, double [] gravityVector) {

               // We eat all of the data that the accelBuffs give us.  They must be the same size, btw.
               // we provide info so ensure the sample window is the same size from buffer to buffer, but that 
               // is the duty of the caller.

               for (int i = 0; i < numSamples;  i++ ) {

                   // For steps, we are only interest in one axis, which is the one that the gravity vector is pointing at.
                   // We only care about the dominant axis, we aren't interested in the absolute value, as the waveform is the thing. 
                   // Worst case is that we see readings 0.7 times the best possible alignment, which is acceptable for our porpoises.

                   double measurement = GetGravityAlignedMeasurement(accelBuffX[i], accelBuffY[i], accelBuffZ[i],
                                                                     gravityVector[0], gravityVector[1], gravityVector[2]);
                   mDataBuffer[i] = measurement;
                   mTimevals[i]   = times[i];
               }
               mLog.flush();
          };

     protected double GetGravityAlignedMeasurement(double x, double y, double z, double gx, double gy, double gz) {
               if (Math.abs(gx) > Math.abs(gy)) {
                   if (Math.abs(gx) > Math.abs(gz)) {
                       // Largest is X. We will use that measurmement. We only need to
                       // consider walking motions, and only relative, not absolute values
                       // gx > gy
                       // gx > gz
                       // gx is greatest
                       if (gx > 0.0) {
                           return x;
                       }
                       else {
                           return -x;
                       }

                   } 
                   else {
                       // Largest is Z
                       // gx > gy
                       // gz > gx > gy
                       // gz is greatest

                       if (gz > 0.0) {
                           return z;
                       }
                       else {
                           return -z;
                       }
                   }
               }
               else if (Math.abs(gy) > Math.abs(gz)) {
                   // gy > gx
                   // gy > gz
                   // gz is biggest

                   if (gy > 0.0) {
                       return y;
                   }
                   else {
                       return -y;
                   }
               }
               else {
                   // gy > gx 
                   // gz > gy > gx
                   // gz is largest
                   if (gz > 0.0) {
                       return z;
                   }
                   else {
                       return -z;
                   }
               }
               //return Double.NaN; // Should be unreachable.
         };
     


     /*
      * Given a circular buffer, finds a step crossing of sufficient amplitude in the buffer, if any. 
      * when it find the step crossing, it returns the index of the end of the downslope indicating a step.
      *. Start scan must be 1 or greater, btw
      */

      protected final class Slope {
                 double startVal;
                 double endVal;
                 double startTime;
                 double endTime;
                 int idxStart;
                 int idxEnd;
                 int direction; // -1 down, 0 flat, 1 up
           };


      // Args:
      //   sampleBase : base running sequence value of this buffer. The sequence value allows use to identify samples accross multiple buffers
      //
      // returns:
      //   int stepcount
      //   
      //
      protected int ExtractFeatures(int[] needed, int sampleBase) {
           final int UP = 1;
           final int DOWN = -1;

           int down_count = 0;
           
           ArrayList<Slope> all_slopes    = new ArrayList<Slope>();
           ArrayList<Slope> pruned_slopes = new ArrayList<Slope>();
           ArrayList<Slope> final_slopes  = new ArrayList<Slope>();
           

           int lastfeatureidx = 0;
           // We calculate the threshold and the min and max.  This allows us to ignore low frequency noise. The window should be 
           // just big enough to guarantee two steps.  We extract the features, then can examine the features list to find the step pattern,
           // which is to say a big downslope pretty near the same step to step distance, within a range of 0.2 and 5 steps per second.
       
           // Slope = -1, 0, 0r 1)

           // Pass 1. Convert the samples to a list of slopes. We will prune the small bumps out of the list next.

           int i = 1;
           int idx = 0;
           while( i < mDataBuffer.length ) {
               if (Math.abs(mDataBuffer[i] - mDataBuffer[i - 1]) < 0.019) {
                   Slope slope = new Slope();
                   slope.direction = 0;
                   slope.idxStart = i-1;
                   slope.idxEnd   = i;
                   while (i < mDataBuffer.length && Math.abs(mDataBuffer[i] - mDataBuffer[i - 1]) < 0.019  ) {
                       slope.idxEnd = i;
                       i++;
                   }
                   slope.startVal  = mDataBuffer[slope.idxStart];
                   slope.endVal    = mDataBuffer[slope.idxEnd];
                   slope.startTime = mTimevals[slope.idxStart];
                   slope.endTime   = mTimevals[slope.idxEnd];
                   slope.idxStart += sampleBase;
                   slope.idxEnd   += sampleBase;
                   if (Math.abs(slope.startVal-slope.endVal) > 0.2 || (slope.endTime-slope.startTime)> 0.1 ) {
                       all_slopes.add(slope);
                   }
               }
               else if (mDataBuffer[i] > mDataBuffer[i - 1]) {
                   Slope slope = new Slope();
                   slope.direction = UP;
                   slope.idxStart = i-1;
                   slope.idxEnd   = i;
                   while (i < mDataBuffer.length && (mDataBuffer[i] - mDataBuffer[i - 1]) > 0.0  ) {
                       slope.idxEnd = i;
                       i++;
                   }
                   slope.startVal = mDataBuffer[slope.idxStart];
                   slope.endVal   = mDataBuffer[slope.idxEnd];
                   slope.startTime = mTimevals[slope.idxStart];
                   slope.endTime   = mTimevals[slope.idxEnd];
                   slope.idxStart += sampleBase;
                   slope.idxEnd   += sampleBase;
                   if (Math.abs(slope.startVal-slope.endVal) > 0.2 || (slope.endTime-slope.startTime)> 0.1 ) {
                       all_slopes.add(slope);
                   }

               }
               else {
                   Slope slope = new Slope();
                   slope.direction = DOWN;
                   slope.idxStart = i-1;
                   slope.idxEnd   = i;
                   while (i < mDataBuffer.length && (mDataBuffer[i] - mDataBuffer[i - 1]) < 0.0 ) {
                       slope.idxEnd = i;
                       i++;
                   }
                   slope.startVal = mDataBuffer[slope.idxStart];
                   slope.endVal   = mDataBuffer[slope.idxEnd];
                   slope.startTime = mTimevals[slope.idxStart];
                   slope.endTime   = mTimevals[slope.idxEnd];
                   slope.idxStart += sampleBase;
                   slope.idxEnd   += sampleBase;
                   if (Math.abs(slope.startVal-slope.endVal) > 1.0 /*|| (slope.endTime-slope.startTime) > 0.1*/ ) {
                       all_slopes.add(slope);
                   }
               }
           }




           // Pass 2. Remove the small bumps out of the list. We should get a nice, smooth set of slopes, but some adjacent 
           // slopes of the same direction that have to be recombined
 
           final double Threshold = /* 0.08; */ 2.8;
           final double MinStepInterval = 0.3; //Certainly no less than 4 steps per second
           final double MaxStepInterval = 1.1; //
           final double MinGSlope = 0.8;
           final double MaxGSlope = 3.2; // Gs per second
           for (idx = 0; idx < all_slopes.size(); idx++) {
               Slope slope = all_slopes.get(idx);


               // ignore the flat ones.
               switch(slope.direction) {
               case 0: break;  // Do nothing

               case UP:
                       if ( (slope.endVal - slope.startVal) > Threshold ) {
                           //pruned_slopes.add(slope);
                       }
                       break;

               case DOWN:
                      if ( (slope.startVal - slope.endVal) > Threshold &&
                            (slope.startTime - mPrevStepTime) > MinStepInterval  &&
                            (slope.startVal-slope.endVal)/(slope.endTime-slope.startTime) > MinGSlope ) {
                           if ((slope.startTime - mPrevStepTime) < MaxStepInterval ) {

                               // This check is expected to reduce bump counting.  If the bump is an isolated impulse it will be rejected. If it is vibration 
                               // or repetitive bumps at the right frequency it looks like a step to us.
 
                               pruned_slopes.add(slope);
                           }
                           else {
                           //    System.err.printf("aardvark: slope.StartTIme = %f, prevsteptime=%f no step dist = %f and should be no more than %f\n",
                           //            slope.startTime, mPrevStepTime, (slope.startTime - mPrevStepTime) ,  MaxStepInterval );
                           }
                           mPrevStepTime = slope.startTime;
                       }
                       break;

               default:
                       break;
               }
           }
           
           all_slopes = null; // hint to the vm to delete all_slopes

           // combine like-slopes
           Slope slope1;
           Slope slope2;
           if (pruned_slopes.size() > 0 ) {
               slope1 = pruned_slopes.get(0);
/*
               for (idx = 0; idx < pruned_slopes.size()-1; ) {
                   slope2 = pruned_slopes.get(idx+1);
    
                   if (slope1.direction != slope2.direction) {
                       slope1 = slope2;
                       idx++;
                   }
                   else if (slope1.direction == slope2.direction) {
    
                        // Combine and add. 
    
                        double start_val = 0.0;
                        double end_val   = 0.0;

                        // 20 is empirical. (~1/2 of running pace samples per step). So is the threshold
                        if (Math.abs(slope2.idxStart - slope1.idxEnd) > 20 && Math.abs(slope2.startVal-slope1.endVal) > 0.03) {
    
                            // A big gap? Add a new node with the opposite slope.
    
                            Slope new_slope = new Slope();
                            new_slope.direction =  (slope1.direction == UP)?DOWN:UP;
                            new_slope.idxStart = slope1.idxEnd;
                            new_slope.idxEnd   = slope2.idxStart;
                            new_slope.startVal = slope1.endVal;
                            new_slope.endVal   = slope2.endVal;
                            pruned_slopes.add(idx+1, new_slope);
                        }
                        else {
                            if (slope1.direction == UP ) {
                                start_val = (slope1.startVal < slope2.startVal)? slope1.startVal : slope2.startVal;
                                end_val   = (slope1.endVal   > slope2.endVal)?   slope1.endVal   : slope2.endVal;
                            }
                            else {
                                start_val = (slope1.startVal > slope2.startVal)? slope1.startVal : slope2.startVal;
                                end_val   = (slope1.endVal   < slope2.endVal)?   slope1.endVal   : slope2.endVal;
                            }
                            slope1.startVal = start_val;
                            slope1.endVal   = end_val;
                            slope1.idxEnd   = slope2.idxEnd;
    
                            pruned_slopes.remove(idx+1);
    
    					}
                   }
              }

              slope1 = pruned_slopes.get(0);
*/
              for (idx = 0; idx < pruned_slopes.size(); idx++ ) {
                   slope1  = pruned_slopes.get(idx);


if (/*(slope1.startVal - slope1.endVal) > 0.25 */true) {
mLog.printf( Logger.LoggingLevel.VERBOSE, "Downslope %d\n", slope1.idxEnd);

                   final_slopes.add(slope1);
}
                  /* if (idx == pruned_slopes.size()-2) {
                       if ((slope2.startVal - slope2.endVal) > 0 ) {
                           mLog.printf( Logger.LoggingLevel.VERBOSE, "Downslope %d\n", slope2.idxEnd);

                           final_slopes.add(slope2);
                       }
                   }
                   slope1 = slope2; */
               }
           }

           pruned_slopes = null;
/*
           if (final_slopes.size() > 0 ) {
               slope1 = final_slopes.get(0);
               for (idx = 1; idx < final_slopes.size()-1;) {
                   slope2 = final_slopes.get(idx+1);
    
                   if (slope1.direction == UP && slope2.direction == DOWN) {

                        Feature f = new Feature(Feature.FeatureType.PeakUpward, slope1.idxEnd, slope1.endVal);
                        mFeatures.add(f);
                        lastfeatureidx = slope2.idxEnd;
                        mLog.print( Logger.LoggingLevel.INFO, "Peak upwards idx:"+slope1.idxEnd+" :val = "+slope1.endVal+"\n")   ;
                        slope1 = slope2;

                        idx++;
                   }
                   else if (slope1.direction == DOWN && slope2.direction == UP) {

                        Feature f = new Feature(Feature.FeatureType.PeakDownward, slope1.idxEnd, slope1.endVal);
                        mFeatures.add(f);
                        lastfeatureidx = slope2.idxEnd;
                        mLog.print( Logger.LoggingLevel.INFO, "Peak downwards idx:"+slope1.idxEnd+" :val = "+slope1.endVal+"\n")   ;
                        slope1 = slope2;

                        idx++;
                   }
                   else {
    
                       // Something went Wrong and we didn't combine everything
    
    //System.err.println("Weird slope arrangement in Extract features");
    idx++;
                        // We get a big gap... thats not good. insert a new node.
                   }
                   
               }
           }
*/
if (final_slopes.size() == 0) {
     needed[0] = mDataBuffer.length;
     return 0;
}
else {
lastfeatureidx = final_slopes.get(final_slopes.size()-1).idxEnd+1;
            needed[0] = mDataBuffer.length-(lastfeatureidx-sampleBase);
return final_slopes.size();
}

/*
           // 
       
           if (i >= mDataBuffer.length-1) {
                 Feature f = new Feature(Feature.FeatureType.EndofBuffer, sampleBase+i, Double.NaN);
                 mFeatures.add(f);
                 lastfeatureidx = mDataBuffer.length-1;
           }
           return lastfeatureidx;
 */
       };


      public double MinVal() { return mMinVal; };
      public double MaxVal() { return mMaxVal; };
      public double AvgVal() { return mAvgVal; };
     
      protected void CalculateWindowParameters() {
       
           mAvgVal = 0.0;
           mMinVal = Double.MAX_VALUE;
           mMaxVal = -Double.MAX_VALUE;
           for (int i = 0; i < mDataBuffer.length; i++ ) {
               mAvgVal +=  mDataBuffer[i];
       
               if (mMinVal > mDataBuffer[i]) {
                   mMinVal = mDataBuffer[i];
               }
               if (mMaxVal < mDataBuffer[i]) {
                   mMaxVal = mDataBuffer[i];
               }
           }
       
           mAvgVal  *= 1.0/this.mSize;
       };


     // Returns the number of steps in the sample.
     // Also returns the number of samples required for the 
     // next round of measurments in a single element array

     public int RecogniseSteps(int [] needed, int sampleBase) {
           int last     = -1;
           int last_idx = sampleBase; // If nothing happens, we are at the end of the buffer
           int residue  = 0;
           int step_count = 0;
           int step_interval = 0;

// mLog.SetLogLevel(Logger.LoggingLevel.VERBOSE);
for(int i = 0;  i < mDataBuffer.length; i++) {
mLog.print( Logger.LoggingLevel.VERBOSE, "in recognise step dx[" + (sampleBase+i) + "] = " + mDataBuffer[i] + "\n");
}
// mLog.SetLogLevel(Logger.LoggingLevel.WARNING);

           CalculateWindowParameters();
           try {
               step_count = ExtractFeatures(needed, sampleBase);
           }
           catch(Exception e) {
               System.err.println("Exception in extract features"+e);
               e.printStackTrace();
           }
 //        mLog.SetLogLevel(Logger.LoggingLevel.WARNING);
return step_count;
         
       /*   
           // Copy the residue to the beginning of the buffer 
           // residue is the samples at the end of the buffer which did not get incorporated into a step measurement
           // They are retained because the end of the buffer could represent the beginning of the next step.



           // Now, recognise steps based on the features.

            // This will be where we recognise steps. In particualr a step occurse where the max is followed by a min 
            // at least <adaptive step threshold> apart, no less than <minsteps> from the last step. We can adapt to noise 
            // by removing the tiny up/downs in amplitude or time.
    
            // first filter: the peak to peak must be > 0.25... (we will eventually make that adaptive)
    
            boolean done = false;
            int likely_step_interval = (int)(RUNNING_PACE_SAMPLES_PER_STEP); // Some room for slow walking
mLog.printf( Logger.LoggingLevel.VERBOSE, "samplebase = %d, feature buffer len= %d, sample_index = %d\n", sampleBase, mFeatures.size(), mFeatures.get(0).Index());
            for (int i = 0; i < (mFeatures.size()-1) && !done;) {
            
                 if (mFeatures.get(i).getType() == Feature.FeatureType.PeakUpward) {
                     int next_max = i+1;
                     int best_max = -1;
                     int best_min = -1;
                     double best_max_value = -Double.MAX_VALUE;
                     double best_min_value = -Double.MAX_VALUE;

                     int min_time_idx = mFeatures.get(i).Index()+likely_step_interval;
    
                     // If we have a previous step rate, lets start with that. The limit  is to be no less than ethe next likely step
    
                     next_max = FindNextFeature(Feature.FeatureType.PeakUpward, i, mFeatures.size(), min_time_idx);

                     if (next_max == -1){
                         // If we didn't find an next_max in the buffer at all, we should refill and continue

                         last_idx = mFeatures.get(i).Index();
    mLog.printf( Logger.LoggingLevel.VERBOSE, "no second max at feature index= %d, last_idx = %d, previous data idx = %d", i, last_idx, mFeatures.get(i-1).Index());
                         done = true;
                         continue;
                     }
                     if (mFeatures.get(next_max).getType() == Feature.FeatureType.EndofBuffer) {
                         last_idx = mFeatures.get(i).Index();
    mLog.printf( Logger.LoggingLevel.VERBOSE, "end of buffer at feature index= %d last time idx = %d\n", i, last_idx);
                         done = true;
                         continue;
                     }

                     if ((mFeatures.get(next_max).Index()-mFeatures.get(i).Index()) > MAX_SAMPLES_PER_STEP) {
                          i++;
    mLog.printf( Logger.LoggingLevel.INFO, "RecogniseStep reject: distance between up-peaks is too large = %d\n", mFeatures.get(next_max).Index()-mFeatures.get(i).Index());
                          continue;
                     }

                     mLog.printf( Logger.LoggingLevel.VERBOSE, "this max = %d. next_max = %d\n", mFeatures.get(i).Index(), mFeatures.get(next_max).Index());

                     // We have a candidate. Look for the best minimum. There will probably be multiple minima in here given noise on the 
                     // signal. We choose the largest. We are not calculating a slope. We just want the lowest and highest.
     
                     best_min = FindBestMinimum(i, next_max); // Find Next automatically skips the first
                     if (best_min == -1) {
                         last_idx = mFeatures.get(i).Index();
mLog.printf( Logger.LoggingLevel.VERBOSE,  "no min at feature index= %d last_idx = %d\n", i, last_idx);
                         done = true;
                         continue;
                     }

                     best_min_value = mFeatures.get(best_min).Value();
    
                     // We should also isolate the best max in the range. The max we have right now may be junk. The max is only 
                     // useful if it is before the min. It could be this one, so we start from i

                     best_max = FindBestMaximum(i, best_min);
                     if (best_max == -1) {
                         // unlikely. We should see at least on max, thats how we got here.
                         last_idx = mFeatures.get(i).Index();
mLog.printf( Logger.LoggingLevel.VERBOSE,  "no best max before best min %d at feature index= %d last_idx = %d\n", best_min, i, last_idx);
                         done = true;
                         continue;
                     }

                     best_max_value = mFeatures.get(best_max).Value();

                     if ((best_max_value-best_min_value) > 0.15) {
                         mLog.printf(Logger.LoggingLevel.INFO, "maybe step at feature idx %d, sample idx = %d maxval = %f, minval = %f\n", best_min, mFeatures.get(best_min).Index(), best_max_value, best_min_value);
if ( mPrevSampleIndex == mFeatures.get(best_min).Index()) { 
// Scream
System.err.printf("!!!DUP!!!!\n");
}
mPrevSampleIndex = mFeatures.get(best_min).Index();

                         //if (mPrevStepIdx > 0) {
                         if (true) {
                            step_interval = (mFeatures.get(best_min).Index()+sampleBase)-mPrevStepIdx; // If we have a regular step rate, we are walking
                            mStepInterval += step_interval;
                            mStepInterval /= 2;   // Cheap fir filter with alpha at .5

                                // we do this checking because ...
                            mLog.printf( Logger.LoggingLevel.INFO,  "could be step at = %d interval = %d overall interval = %d\n", mFeatures.get(best_min).Index(), step_interval,
                                                                                                                mStepInterval);
                            step_count++;
                         }
                         mPrevStepIdx = mFeatures.get(best_min).Index();
                         i = best_min+1;
                         last_idx = mFeatures.get(i).Index();
                         done = true;
                     }
                     else {
                         i++;
                         last_idx = mFeatures.get(i).Index();
                         if (mPrevStepIdx > 0 && (last_idx-mPrevStepIdx)> MAX_SAMPLES_PER_STEP) {
                             mPrevStepIdx = 0;
                         }
                     }
 
mLog.printf( Logger.LoggingLevel.VERBOSE, "i = %d last_idx = %d\n", i, last_idx);
                 }
                 else {
                     i++;
                 }
            }
    
mLog.printf( Logger.LoggingLevel.VERBOSE, "out of buffer, last_idx = %d\n", last_idx);
mLog.SetLogLevel(Logger.LoggingLevel.WARNING);
            mFeatures.clear();
            last_idx -= sampleBase;  // We remove the sample base for calculating the sample buffer index
            needed[0] = mDataBuffer.length-last_idx;

            return step_count;
 */
       };


     protected int FindBestMinimum(int startIdx, int limitIdx) {
                 int next_min = startIdx;
                 int best_min = -1;
                 double best_min_value = Double.MAX_VALUE;

                 while (next_min < limitIdx) {
                     next_min = FindNextFeature(Feature.FeatureType.PeakDownward, next_min, limitIdx, 1);
                     if (next_min == -1 ) {
                          return best_min;
                     }
                     if (mFeatures.get(next_min).getType() == Feature.FeatureType.EndofBuffer) {
                          // Nothinng more to see here.... return what we've got.
                          return best_min;
                     }
                     else if (mFeatures.get(next_min).getType() == Feature.FeatureType.PeakDownward) {
                          if (mFeatures.get(next_min).Value() < best_min_value) {
                              best_min = next_min;
                              best_min_value = mFeatures.get(best_min).Value();
                          }
                     }
                 }
                 return best_min;
            };

     protected int FindBestMaximum(int startIdx, int limitIdx) {
                 int next_max = startIdx;
                 int best_max = -1;
                 double best_max_value = Double.MAX_VALUE;

                 if (mFeatures.get(next_max).getType() == Feature.FeatureType.PeakUpward) {
                     best_max = startIdx;
                 }
                 while (next_max < limitIdx) {
                     next_max = FindNextFeature(Feature.FeatureType.PeakUpward, next_max, limitIdx, 1);
                     if (next_max == -1) {
                         return best_max;
                     }
                     else if (mFeatures.get(next_max).getType() == Feature.FeatureType.EndofBuffer) {
                          // Nothinng more to see here.... return what we've got.
                          return best_max;
                     }
                     else if (mFeatures.get(next_max).getType() == Feature.FeatureType.PeakUpward) {
                          if (mFeatures.get(next_max).Value() < best_max_value) {
                              best_max = next_max;
                              best_max_value = mFeatures.get(best_max).Value();
                          }
                     }
                 }
                 return best_max;
            };

     //
     // fType -- type of feature we want to look for.
     // startIdx - idx into the feature list to start at
     // limitIdx - the limit in the feature list. We will not go to limit, only up to it.
     // minDistance - number of samples minimum distance to return. ie the difference between the time Index() of the feature at startIdx and 
     //               the next selected feature's time Index().
     //
     // return the index into the feature list of the requested item, or -1 if the item isn't in the buffer
     // if we reach end of buffer, we return that, also requested or not.
     // so type needs to be checked.
     //
     protected int FindNextFeature(Feature.FeatureType fType, int startIdx, int limitIdx, int minTimeIdx) {
                 int next_feature;

                 if (limitIdx == -1) {
                     limitIdx = mFeatures.size();
                 }

                 for (next_feature = startIdx+1; next_feature < limitIdx; next_feature++ ) {
                     if (mFeatures.get(next_feature).getType() == fType) {

/* System.err.printf( "candidate  [%d] idx = %d index interval = %d\n",
                                    next_feature,
                                    mFeatures.get(next_feature).Index(),
                                    mFeatures.get(next_feature).Index()-mFeatures.get(startIdx).Index());
 */
                         if (mFeatures.get(next_feature).Index() > minTimeIdx) {
                             return next_feature;
                         }
                     }
                     else if ((mFeatures.get(next_feature).getType() == Feature.FeatureType.EndofBuffer)) {
//System.err.printf( "end of buffer. \n");
                         return next_feature;
                     }
                 }
                          
                 return -1;
         };

     protected Logger   mLog;
     protected int      mSize;
     protected double  mMinVal;
     protected double  mMaxVal;
     protected double  mAvgVal;
     protected ActivityCode  mActivity;

     private  double mPrevStepTime = 0.0;
     private int mPrevStepIdx  = 0; // time index of the last step
     private int mStepInterval = 0;
     
     private int mSampleCount = 0;
     protected  ArrayList<Feature> mFeatures;
     protected  double[]  mDataBuffer;
     protected  double[]  mTimevals;


     // temporarty stuff for debugging. Remove soon

     private int mPrevSampleIndex = 0;
};




