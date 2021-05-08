/*
 *  Copyright (c) 2014-2015 Aardvark Visual Software Inc,
 *
 *  All Rights Reserved.
 *
 */

package com.aardvark_visual.ped.aardvarkpedwidget;

import android.app.TaskStackBuilder;
import android.content.SharedPreferences;
import android.util.Log;
import android.app.PendingIntent;
import android.app.AlarmManager;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RemoteViews;

import android.content.BroadcastReceiver;

import com.aardvark_visual.ped.R;
import com.aardvark_visual.ped.aardvarkpedanalysis.AardvarkPedAnalysis;

public class AardvarkPedWidgetProvider extends AppWidgetProvider {

    public  final static String TAG = "AardvarkWidgetProvider";
    public  final static String WIDGET_UPDATE_ACTION ="com.aardvark_visual.ped.aardvarkpedwidget.intent.action.UPDATE_WIDGET";
    public  final static String WIDGET_STEP_COUNT_UPDATE="com.aardvark_visual.ped.aardvarkpedwidget.intent.action.UPDATE_STEP_COUNT";
    public  final static String WIDGET_HEART_COUNT_UPDATE="com.aardvark_visual.ped.aardvarkpedwidget.intent.action.UPDATE_HEART_COUNT";

    public  final static String WIDGET_START_COUNT ="com.aardvark_visual.ped.aardvarkpedwidget.intent.action.START_COUNT";
    public  final static String WIDGET_STOP_COUNT  ="com.aardvark_visual.ped.aardvarkpedwidget.intent.action.STOP_COUNT";
    public  final static String WIDGET_RESET_COUNT ="com.aardvark_visual.ped.aardvarkpedwidget.intent.action.RESET_COUNT";

    public  static final String INTENT_ShowMetric_Updated       = "com.aardvark_visual.ped.show_metric_update";
    public  static final String INTENT_UseNativeCounter_Updated = "com.aardvark_visual.ped.use_native_counter_update";
    public  static final String INTENT_StrideLength_Updated     = "com.aardvark_visual.ped.stride_length_in_meters_update";

//    private static final String PREFKEY_StrideLength_FLOAT       = "com.aardvark_visual.ped.stride_length_in_meters_float";
//    private static final String PREFKEY_ShowMetric_BOOLEAN       = "com.aardvark_visual.ped.show_metric_boolean";
//    private static final String PREFKEY_UseNativeCounter_BOOLEAN = "com.aardvark_visual.ped.use_native_counter_boolean";
//    private static final String PREFKEY_StepCount_INT            = "com.aardvark_visual.ped.step_count_int";

    private SharedPreferences mSharedPreferences = null;
    private  static double  mStrideLength = 0.8;  // Stride length in meters. We initialise this from the sharedpreferences.
    private  static boolean mShowMetric              = false; // Use the metric system for display if true (km and cm) Use miles and inches otherwise
    private  static boolean mPreferNativeStepCounter = true;  // Use the StepDetectorListener for step detection rather than the linear accel listener.

    private static int     mElapsedTime = 0;  // in milliseconds
    private static long    mStartTime   = 0;  // in milliseconds
    private static int     mNumSteps    = 0;
    private static double  mDistance  = 0.0; // in miles or km, whatever the activity decides
    private static boolean mTracking    = false;
    private static int     mHeartRate   = -1;

    private static int   mStartTimeHour   = 0;
    private static int   mStartTimeMinute = 0;
    private static int   mStartTimeSecond = 0;
    
    private static int   mElapsedTimeHour   = 0;
    private static int   mElapsedTimeMinute = 0;
    private static int   mElapsedTimeSecond = 0;

    private static AlarmManager alarm_mgr = null;
    private static PendingIntent mAlarmUpdate = null;
    private static PendingIntent mResetAlarmIntent = null;
    private static AppWidgetManager mAppWidgetManager = null;

    private static AardvarkPedWidgetService mService = null;

    static final String STATE_STEP_COUNT = "step_count";
    static final String STATE_DISTANCE   = "distance";
    static final String STATE_LAST_STEP_TIME = "last_step_time";
    private static String mSteplogFilename = null;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        Log.i(TAG, "AardvarkPedWidgetProvider: onUpdate");
        if (mAppWidgetManager == null) {
            mAppWidgetManager = AppWidgetManager.getInstance(context);
        }

        if (mService == null) {
            Log.i(TAG, "AardvarkPedWidgetProvider: new Service");
            mService = new AardvarkPedWidgetService();

            Log.i(TAG, "AardvarkPedWidgetProvider: send Service StartCount");
            mService.startActionStartCount(context);
        }

        Locale current_locale = context.getResources().getConfiguration().locale;


        // initializing widget layout
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.aardvark_ped_widget);

        // register for button event

        Log.d(TAG, "AardvarkPedWidgetProvider: send remoteviews 1");
        remoteViews.setOnClickPendingIntent(R.id.reset_button,
                buildResetButtonPendingIntent(context));

        remoteViews.setOnClickPendingIntent(R.id.app_button,
                buildAppButtonPendingIntent(context));

        String num_steps_text = String.format(current_locale, "%6d", mNumSteps);
        remoteViews.setTextViewText(R.id.NumberofSteps, num_steps_text);

        String distance_text = String.format(current_locale, "%6.3f", mDistance);
        remoteViews.setTextViewText(R.id.Distance, distance_text);

        Log.d(TAG, "AardvarkPedWidgetProvider: send remoteviews 2");

        if (mShowMetric) {
            remoteViews.setTextViewText(R.id.distance_label, context.getText(R.string.distance_label_metric));
        }
        else {
            remoteViews.setTextViewText(R.id.distance_label, context.getText(R.string.distance_label_english));
        }

        int hour = mStartTimeHour;
        String am_pm = "am";
        if (hour > 12) {
            hour -= 12;
            am_pm = "pm";
        }
        else if (hour == 12) {
            am_pm = "pm";
        }
        else if (hour == 0) {
            am_pm = "am";
            hour += 12;
        }

        String start_time_text = String.format("%2d:%02d:%02d %s", hour, mStartTimeMinute, mStartTimeSecond, am_pm);
        remoteViews.setTextViewText(R.id.time_start, start_time_text);
        
        String elapsed_time_text = String.format("%2d:%02d:%02d", mElapsedTimeHour, mElapsedTimeMinute, mElapsedTimeSecond);
        remoteViews.setTextViewText(R.id.elapsed_time, elapsed_time_text);

        if (mHeartRate < 0) {
            remoteViews.setViewVisibility(R.id.heart, View.INVISIBLE);
            remoteViews.setViewVisibility(R.id.heart_rate, View.INVISIBLE);
        }
        else {
            String heart_rate_text = String.format("%-3d", mHeartRate);

            remoteViews.setViewVisibility(R.id.heart, View.VISIBLE);
            remoteViews.setViewVisibility(R.id.heart_rate, View.VISIBLE);
            
            remoteViews.setTextViewText(R.id.heart_rate, heart_rate_text);

            // 2DO - make the heart blink at the rate
        }

        Log.d(TAG, "AardvarkPedWidgetProvider: send remoteviews 3");
        // updating view with initial data
        remoteViews.setTextViewText(R.id.title, getTitle());

        // request for widget update
        pushWidgetUpdate(context, remoteViews);

        super.onUpdate(context, appWidgetManager, appWidgetIds);
        Log.i(TAG, "onUpdate return");
    }

    public static PendingIntent buildStartButtonPendingIntent(Context context) {

        // initiate widget update request
        Intent intent = new Intent(context, AardvarkPedWidgetService.class);
        intent.setAction(AardvarkPedWidgetService.ACTION_START_COUNT);
        return PendingIntent.getService(context, 0, intent, 0);
    }

    public static PendingIntent buildAppButtonPendingIntent(Context context) {

        // initiate widget update request
        Intent intent = new Intent(context, AardvarkPedAnalysis.class);

        // Create an artificial backtrack path which goes to the home view.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(AardvarkPedAnalysis.class);
        stackBuilder.addNextIntent(intent);
        return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static PendingIntent buildResetButtonPendingIntent(Context context) {

        // initiate widget update request
        Intent intent = new Intent(context, AardvarkPedWidgetService.class);
        intent.setAction(AardvarkPedWidgetService.ACTION_RESET_COUNT);
        return PendingIntent.getService(context, 0, intent, 0);
    }

    private static CharSequence getDesc() {
        return "Step Counter and Distance";
    }

    private static CharSequence getTitle() {
        return "AardvarkPed";
    }

    public static void pushWidgetUpdate(Context context, RemoteViews remoteViews) {
        ComponentName myWidget = new ComponentName(context, AardvarkPedWidgetProvider.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(context);

        Log.i(TAG, "send update app widget\n");
        manager.updateAppWidget(myWidget, remoteViews);
    }


    @Override 
    public void onReceive(Context context, Intent intent){

        Log.i(TAG, "onReceive: intent="+intent.getAction());

        if (intent.getAction().equals("android.appwidget.action.APPWIDGET_UPDATE")  ||
            intent.getAction().equals(WIDGET_UPDATE_ACTION) )        {
            if (mAppWidgetManager == null) {
                mAppWidgetManager = AppWidgetManager.getInstance(context);
            }
            ComponentName thisAppWidget = new ComponentName(context.getPackageName(), AardvarkPedWidgetProvider.class.getName());
            int[] appWidgetIds = mAppWidgetManager.getAppWidgetIds(thisAppWidget);
            onUpdate(context, mAppWidgetManager, appWidgetIds);
            return;
        }
        else if (intent.getAction().equals(WIDGET_STEP_COUNT_UPDATE)){
            mNumSteps = intent.getIntExtra("step_count", -1);
            mDistance = (double)intent.getFloatExtra("distance", -1.0f);
            mShowMetric = intent.getBooleanExtra("show_metric", false);

            mStartTimeHour   = intent.getIntExtra("lst_rst_h", 0);
            mStartTimeMinute = intent.getIntExtra("lst_rst_m", 0);
    
            mElapsedTimeHour   = intent.getIntExtra("elpsed_h", 0);
            mElapsedTimeMinute = intent.getIntExtra("elpsed_m", 0);
            mHeartRate         = intent.getIntExtra("hrate", 0);

            if (mAppWidgetManager == null) {
                mAppWidgetManager = AppWidgetManager.getInstance(context);
            }
            ComponentName thisAppWidget = new ComponentName(context.getPackageName(), AardvarkPedWidgetProvider.class.getName());
            int[] appWidgetIds = mAppWidgetManager.getAppWidgetIds(thisAppWidget);
            onUpdate(context, mAppWidgetManager, appWidgetIds);
            return;
        }
        else if (intent.getAction().equals(WIDGET_HEART_COUNT_UPDATE)){
            mHeartRate         = intent.getIntExtra("hrate", 0);

            if (mAppWidgetManager == null) {
                mAppWidgetManager = AppWidgetManager.getInstance(context);
            }
            ComponentName thisAppWidget = new ComponentName(context.getPackageName(), AardvarkPedWidgetProvider.class.getName());
            int[] appWidgetIds = mAppWidgetManager.getAppWidgetIds(thisAppWidget);
            onUpdate(context, mAppWidgetManager, appWidgetIds);
            return;
        }
        else if (intent.getAction().equals("android.appwidget.action.APPWIDGET_DELETED")) {
             Log.i(TAG, "onReceive: call onDelete" );
        }
        else if (intent.getAction().equals(INTENT_StrideLength_Updated)) {
            double val = (double) intent.getFloatExtra("val", 32.0f);
            onStrideLengthUpdate(val);
            AardvarkPedWidgetService.startActionSetStrideLength(context, (float)val);
        }
        else if (intent.getAction().equals(INTENT_ShowMetric_Updated)) {
            boolean val = intent.getBooleanExtra("val", false);
            onShowMetricUpdate(val);
            AardvarkPedWidgetService.startActionSetShowMetric(context, val);
        }
        else if (intent.getAction().equals(INTENT_UseNativeCounter_Updated)) {
            boolean val = intent.getBooleanExtra("val", false);
            onPreferNativeUpdate(val);
            AardvarkPedWidgetService.startActionSetPreferNative(context, val);
        }
        else if (intent.getAction().equals("android.intent.action.TIME_TICK")) {

        }

        super.onReceive(context, intent);
        Log.i(TAG, "onReceive (rtn): intent="+intent.getAction());
    }


    public void onStrideLengthUpdate(double strideLength) {
              mStrideLength = strideLength;
        };

    public void onShowMetricUpdate(boolean showMetric) {
              mShowMetric = showMetric;

              // we calculate on each widget update, so all we need is this data.

        };

    public void onPreferNativeUpdate(boolean preferNative) {
              mPreferNativeStepCounter = preferNative;

              // tbd. Update the listener to reflect the choice.
        };


    @Override
    public void onEnabled(Context context) {
        Log.i(TAG, "onEnabled");
        super.onEnabled(context);

        GregorianCalendar     thyme     = (GregorianCalendar)Calendar.getInstance();
        alarm_mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
 
        thyme.set(GregorianCalendar.MINUTE, 0);
        thyme.add(GregorianCalendar.SECOND, 3);
      
        Intent updateIntent = new Intent(context, BroadcastReceiver.class);
        updateIntent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
        mAlarmUpdate = PendingIntent.getBroadcast(context, 0, updateIntent, 0);

        // Reset the count at midnight. Its a feature
        thyme  = (GregorianCalendar)Calendar.getInstance();
        thyme.set(GregorianCalendar.MINUTE, 0);
        thyme.set(GregorianCalendar.SECOND, 0);
        thyme.set(GregorianCalendar.HOUR_OF_DAY, 0);

        Intent resetIntent = new Intent(context, AardvarkPedWidgetService.class);
        resetIntent.setAction(AardvarkPedWidgetService.ACTION_RESET_COUNT);
        mResetAlarmIntent = PendingIntent.getBroadcast(context, 0, resetIntent, 0);
        alarm_mgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, thyme.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, mResetAlarmIntent);

        // We need to tickle ourselves. The OS limits updates to once every 30 minutes
        // otherwise.  Thanks a lot.  We tickle the service regularly (every three seconds) just in case the 
        // service has been suspended.

        try {
            Context prefsContext = context.createPackageContext("com.aardvark_visual.ped", Context.CONTEXT_IGNORE_SECURITY);
         //   mSharedPreferences = prefsContext.getSharedPreferences("com.aardvark_visual.ped.aardvarkpedanalysis_preferences",
            mSharedPreferences = prefsContext.getSharedPreferences(AardvarkPedAnalysis.AppSharedPreferencesName, Context.MODE_MULTI_PROCESS);
        }
        catch(Exception e) {
            mSharedPreferences = context.getSharedPreferences(AardvarkPedAnalysis.AppSharedPreferencesName, Context.MODE_PRIVATE); // This is just to prevent segvs
        }

        Map<String, ?> zoot = mSharedPreferences.getAll();

        boolean val1  =  mSharedPreferences.getBoolean(AardvarkPedWidgetService.PREFKEY_UseNativeCounter, true);
        String val2   =  mSharedPreferences.getString(AardvarkPedWidgetService.PREFKEY_StrideLength, "void");
        String val3   =  mSharedPreferences.getString(AardvarkPedWidgetService.PREFKEY_MeasurementSystem, "zip");


        mStrideLength = mSharedPreferences.getFloat(AardvarkPedWidgetService.PREFKEY_StrideLength_FLOAT, (float) mStrideLength);
        mShowMetric   = mSharedPreferences.getBoolean(AardvarkPedWidgetService.PREFKEY_ShowMetric_BOOLEAN, mShowMetric);
        mPreferNativeStepCounter = mSharedPreferences.getBoolean(AardvarkPedWidgetService.PREFKEY_UseNativeCounter_BOOLEAN, mPreferNativeStepCounter);
        mNumSteps     = mSharedPreferences.getInt(AardvarkPedWidgetService.PREFKEY_StepCount_INT, 0);

        try {
            mStartTime = mSharedPreferences.getLong(AardvarkPedWidgetService.PREFKEY_StartTime_LONG, 0);
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        if (mService == null) {
            mService = new AardvarkPedWidgetService();
            mService.startActionStartCount(context);
        }



    }


    @Override
    public void onDisabled(Context context) {
        Log.i(TAG, "onDisabled");
        if (alarm_mgr != null) {
            alarm_mgr.cancel(mAlarmUpdate);
            alarm_mgr.cancel( mResetAlarmIntent );
        }

        super.onDisabled(context);
    }



}
