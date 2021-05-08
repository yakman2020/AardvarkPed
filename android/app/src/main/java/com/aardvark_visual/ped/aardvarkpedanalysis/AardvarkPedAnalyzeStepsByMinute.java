
/*
 *  Copyright (c) 2014-2015 Aardvark Visual Software Inc,
 *
 *  All Rights Reserved.
 *
 *
 */

package com.aardvark_visual.ped.aardvarkpedanalysis;

import android.content.Context;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;

import com.aardvark_visual.ped.aardvarkped.*;

/**
 * Created by brcamp on 1/30/15.
 */
public class AardvarkPedAnalyzeStepsByMinute {

    private StepLog mSteps = null;
    private com.aardvark_visual.ped.aardvarkped.StepDataSource mStepDB = null;

    AardvarkPedAnalyzeStepsByMinute(Context context, String steplogpath, String steplogname) {
         mSteps  = new StepLog(context, steplogpath, steplogname, 'r');
         mStepDB = mSteps.mStepDB;
    }

    long FirstMinute() {
        Calendar cal = Calendar.getInstance();

        cal.setTimeInMillis(mSteps.mStepDB.getFirst());
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return  cal.getTimeInMillis();
    }

    long LastMinute() {
        Calendar cal = Calendar.getInstance();

        cal.setTimeInMillis(mSteps.mStepDB.getLast());
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 0);
        return  cal.getTimeInMillis();
    }

    long FirstHour() {
        Calendar cal = Calendar.getInstance();
        
        cal.setTimeInMillis(mSteps.mStepDB.getFirst());
        cal.add(Calendar.HOUR_OF_DAY, -1);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 0);
        return  cal.getTimeInMillis();
    }

    long LastHour() {
        Calendar cal = Calendar.getInstance();
        
        cal.setTimeInMillis(mSteps.mStepDB.getLast());
        cal.add(Calendar.HOUR_OF_DAY, -1);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 0);
        return  cal.getTimeInMillis();
    }

    long FirstDay() {
        Calendar cal = Calendar.getInstance();

        long timestamp = mSteps.mStepDB.getFirst();

        if (timestamp == 0) {
            return cal.getTimeInMillis();
        }

        cal.setTimeInMillis(timestamp);
        cal.add(Calendar.DAY_OF_YEAR, -1);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 0);

        cal.add(Calendar.SECOND, 2); // This makes sure we are at the beginning of the current day.
                                     // Just setting it to 0:0:0 is ambiguous.  Not because of the spec but different interpretations
                                     // of different OS versions
        return  cal.getTimeInMillis();
    }

    long LastDay() {
        Calendar cal = Calendar.getInstance();

        long timestamp = mSteps.mStepDB.getLast();
        if (timestamp == 0) {
            return cal.getTimeInMillis();
        }
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 0);
        return  cal.getTimeInMillis();
    }


    // Need the timestamp (in millis) for the beginning of the "day".  Returns steps by hour
    // for dayStart to dayStart+(numDays*24) hours.  We return the total number of steps in int[0], and the steps per hour in int[1]..length.

    int[] StepsForDay(long dayStart, int numDays, 
                       ArrayList<Integer> heartRateMin, 
                       ArrayList<Integer> heartRateMax, 
                       ArrayList<Integer> heartRateAvg) {

        final long hourinmillis = 3600000;
        int[] hourcount = new int[24*numDays+1];
        long hourbegin = dayStart;
        long hourend   = dayStart+hourinmillis;
        long dayend    = dayStart+(hourinmillis*24*(long)numDays);
        long [] max_hr  = new long[1];
        max_hr[0] = 0;
        long [] min_hr  = new long[1];
        min_hr[0] = 0;
        long avg_hr = 0;
        Calendar cal = Calendar.getInstance();
        long current_time = cal.getTimeInMillis();

        if (dayend > current_time) {
            dayend = current_time;
        }

        try {
            for (int i = 1; i < hourcount.length; i++) {
                if (hourbegin >= dayend) {
                    break;
                }
                if (hourend >= dayend) {
                    hourend = dayend;
                }

                hourcount[i] = (int) mSteps.mStepDB.getStepCount(hourbegin, hourend);

                if (heartRateAvg != null || heartRateMin != null || heartRateMax != null ) {
                    min_hr[0] = 0;
                    max_hr[0] = 0;

                    avg_hr = mSteps.mStepDB.getHeartRate(hourbegin, hourend, min_hr, max_hr);
                    if (heartRateAvg != null ) {
                        heartRateAvg.add((int)avg_hr);
                    }
                    if (heartRateMax != null) {
                        heartRateMax.add((int)max_hr[0]);
                    }
                    if (heartRateMin != null) {
                        heartRateMin.add((int)min_hr[0]);
                    }

                }

                hourbegin = hourend;
                hourend += hourinmillis;
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        for (int i = 1; i < hourcount.length; i++) {
            hourcount[0] += hourcount[i];
        }
          
        return hourcount;
    }

    // Need the timestamp (in millis) for the beginning of the "day".  Returns steps by day
    // for day to day+7 

    int[] StepsForWeek(long day, int numDays) {
        final long hourinmillis = 3600000;
        final long dayinmillis  = hourinmillis*24;
        int[] daycount = new int[numDays+1];
        int thisday = 1;
        long daybegin = day;
        long dayend    = day+(hourinmillis*24);
        long weekend   = day+(hourinmillis*24*numDays);

        for (int i = 0; i < daycount.length; i++) {
            daycount[i] = 0;
        }

        for (int i = 1; i < daycount.length; i++) {
            if (daybegin >= weekend) {
                break;
            }

            daycount[thisday] = (int)mSteps.mStepDB.getStepCount(daybegin, dayend);
            thisday++;
            daybegin = dayend;
            dayend  += dayinmillis;
        }

        for (int i = 1; i < daycount.length; i++) {
            daycount[0] += daycount[i];
        }

        return daycount;
    }

    // Returns steps per minute for each minute during "day".
    // Optionally returns a list of heart rate readings
    //
    // Note: the step data is considerable, but sparse. As a result, a naive approach of just doing a select on 
    // each minute results in the selection taking 7-10 seconds, even when the database is empty.
    // The reason is the large number of selects take considerable time in their own right. So... we will count per hour 
    // and only count per minute if there is something in that hour.  This will greatly reduce the number of queries

    int[] CadenceForDay(long day, ArrayList<Integer> heartRate) {
        final long minuteinmillis = 60000;
        final long hourinmillis   = minuteinmillis*60;

        Calendar cal = Calendar.getInstance();
        int thisminute = 0;
        long minutebegin = day;
        long minuteend   = day+minuteinmillis;
        long hourbegin   = day;
        long hourend     = day+hourinmillis;
        long dayend      = mSteps.mStepDB.getLast();
        if (dayend == 0) {
            dayend = cal.getTimeInMillis();
        }
        long num_minutes = (dayend-minutebegin)/minuteinmillis;
        int [] minutecount = new int[(int)num_minutes];

        int steps_per_hour = 0;
        int hr_per_hour    = 0;
        for (int i = 0; i < minutecount.length; ) {
            if (hourend <= dayend) {
                steps_per_hour = (int)mSteps.mStepDB.getStepCount(hourbegin, hourend);
                hr_per_hour    = (int)mSteps.mStepDB.getHeartRate(hourbegin, hourend);
                hourbegin = hourend;
                hourend  += hourinmillis;
                if (steps_per_hour == 0) {
                    // If there were no steps this hour, then we don't need to querey any further, 
                    // and we can zero 60 minutes
                    for (int j = 0; j < 60; j++ ) {
                        minutecount[thisminute] = 0;
                        i++;
                        if (heartRate != null) {
                            if (hr_per_hour == 0) {
                                heartRate.add(0);
                            }
                            else {
                                heartRate.add((int)mSteps.mStepDB.getHeartRate(minutebegin, minuteend, null, null));
                            }
                        }
                        thisminute++;
                        minutebegin = minuteend;
                        minuteend  += minuteinmillis;
                    }
                }
                else {
                    for (int j = 0; j < 60; j++ ) {
                        minutecount[thisminute] = (int)mSteps.mStepDB.getStepCount(minutebegin, minuteend);
                        if (heartRate != null) {
                            if (hr_per_hour == 0) {
                                heartRate.add(0);
                            }
                            else {
                                heartRate.add((int)mSteps.mStepDB.getHeartRate(minutebegin, minuteend, null, null));
                            }
                        }
                        thisminute++;
                        minutebegin = minuteend;
                        minuteend  += minuteinmillis;
                        i++;
                    }
                }
            }
            else {
                if (minuteend <= dayend) {
                    minutecount[thisminute] = (int) mSteps.mStepDB.getStepCount(minutebegin, minuteend);
                    if (heartRate != null) {
                        heartRate.add((int) mSteps.mStepDB.getHeartRate(minutebegin, minuteend, null, null));
                    }
                }
                thisminute++;
                minutebegin = minuteend;
                minuteend  += minuteinmillis;
                i++;
            }
        }

        return minutecount;
    }
}

