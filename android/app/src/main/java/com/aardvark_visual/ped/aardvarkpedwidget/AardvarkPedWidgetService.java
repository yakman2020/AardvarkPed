
/*
 *  Copyright (c) 2014-2015 Aardvark Visual Software Inc,
 *
 *  All Rights Reserved.
 *
 */

package com.aardvark_visual.ped.aardvarkpedwidget;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.SensorEventListener;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;


import java.io.*;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.hardware.Sensor;
import android.hardware.SensorManager;

import com.aardvark_visual.ped.BuildConfig;
import com.aardvark_visual.ped.R;
import com.aardvark_visual.ped.aardvarkped.*;
import com.aardvark_visual.ped.aardvarkpedanalysis.AardvarkPedAnalysis;
import com.aardvark_visual.ped.bluetooth.*;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * helper methods.
 */
public class AardvarkPedWidgetService extends AardvarkPedIntentService implements CounterUpdateCallback {

    public  final static String TAG = "AardvarkWidgetService";

    // Intents intended to be from the widget.  The widget does the display, and has the control buttons. 
    public static final String ACTION_GET_COUNT        = "com.aardvark_visual.ped.aardvarkpedservice.action.GET_COUNT";
    public static final String ACTION_START_COUNT      = "com.aardvark_visual.ped.aardvarkpedservice.action.START_COUNT";
    public static final String ACTION_PAUSE_COUNT      = "com.aardvark_visual.ped.aardvarkpedservice.action.PAUSE_COUNT";
    public static final String ACTION_RESET_COUNT      = "com.aardvark_visual.ped.aardvarkpedservice.action.RESET_COUNT";
    public static final String ACTION_MIDNIGHT_CLEANUP = "com.aardvark_visual.ped.aardvarkpedservice.action.MIDNIGHT_CLEANUP";


    // Intents from the activity. These get broadcast, so they go from the activity when there is a change in the 
    // preferences, which is the activities area of concern.  We expect since they are broadcast we need not forward them
    // to the 

    public  static final String INTENT_StrideLength_Updated     = "com.aardvark_visual.ped.stride_length_in_meters_update";
    public  static final String INTENT_ShowMetric_Updated       = "com.aardvark_visual.ped.show_metric_update";
    public  static final String INTENT_UseNativeCounter_Updated = "com.aardvark_visual.ped.use_native_counter_update";
    public  static final String INTENT_HeartRateDevice_Updated  = "com.aardvark_visual.ped.heart_rate_device_update";
    public  static final String INTENT_PurgeBefore_Updated      = "com.aardvark_visual.ped.purgebefore_update";
    public  static final String INTENT_PurgeArchive_Updated     = "com.aardvark_visual.ped.purgearchive_update";

    public static final  String INTENT_StopService  = "com.aardvark_visual.ped.StopService";
    public static final  String INTENT_StartService = "com.aardvark_visual.ped.StartService";

    public static final  String INTENT_ImportStepCsv = "com.aardvark_visual.ped.ImportStepCsv";
    public static final  String INTENT_ExportStepCsv = "com.aardvark_visual.ped.ExportStepCsv";


    // The keys are defined in xml/preferences.xml

    public static final  String PREFKEY_UseNativeCounter  = "pref_aardvark_step";
    public static final  String PREFKEY_StrideLength      = "stride_length";
    public static final  String PREFKEY_MeasurementSystem = "metric";
    public static final  String PREFKEY_ClearAfter        = "step_retention_time";
    public static final  String PREFKEY_Archive           = "archive_old_steps";
    public static final  String PREFKEY_ScanHeartRate     = "pref_scan_heart_rate";
    public static final  String PREFKEY_SelectedHeartRate = "heart_rate_name";

    public static final String PREFKEY_StrideLength_FLOAT       = "com.aardvark_visual.ped.stride_length_in_meters_float";
    public static final String PREFKEY_ShowMetric_BOOLEAN       = "com.aardvark_visual.ped.show_metric_boolean";
    public static final String PREFKEY_UseNativeCounter_BOOLEAN = "com.aardvark_visual.ped.use_native_counter_boolean";
    public static final String PREFKEY_StepCount_INT            = "com.aardvark_visual.ped.step_count_int";
    public static final String PREFKEY_PurgeDays_INT            = "com.aardvark_visual.ped.purgebefore_int";
    public static final String PREFKEY_Archive_BOOLEAN          = "com.aardvark_visual.ped.archive_boolean";
    public static final String PREFKEY_HeartRate_STRING         = "PREFKEY_HeartRate_STRING";
    public static final String PREFKEY_HeartRateName_STRING     = "PREFKEY_HeartRateName_STRING";
    public static final String PREFKEY_AvailableHeartRateDevices_STRINGSET = "PREFKEY_AvailableHeartRateDevices_STRINGSET";
    public static final String PREFKEY_StartTime_LONG           = "com.aardvark_visual.ped.start_time_LONG";

    // public static final String PREFKEY_HEART_RATE_DEVICE_UUID = "com.aardvark_visual.ped.bt.heart_rate_device_UUID";
    // public static final String PREFKEY_HEART_RATE_DEVICE_NAME_STRING = "com.aardvark_visual.ped.bt.heart_rate_device_name_STRING";
    // public static final String PREFKEY_HEART_RATE_SERVICE_UUID = "com.aardvark_visual.ped.bt.heart_rate_service_UUID";

    private static final String EXTRA_SHOWMETRIC         = "com.aardvark_visual.ped.aardvarkpedservice.extra.ShowMetric";
    private static final String EXTRA_STRIDE_LENGTH      = "com.aardvark_visual.ped.aardvarkpedservice.extra.Stride";
    private static final String EXTRA_USE_NATIVE         = "com.aardvark_visual.ped.aardvarkpedservice.extra.UseNative";
    private static final String EXTRA_HEART_RATE_DEV_ID  = "com.aardvark_visual.ped.aardvarkpedservice.extra.hrdevid";
    private static final String EXTRA_HEART_RATE_DEV_NM  = "com.aardvark_visual.ped.aardvarkpedservice.extra.hrdevnm";
    private static final String EXTRA_PURGEBEFORE        = "com.aardvark_visual.ped.aardvarkpedservice.extra.purgedays";
    private static final String EXTRA_PURGEARCHIVE       = "com.aardvark_visual.ped.aardvarkpedservice.extra.purgearchive";

    private static final String EXTRA_STEP_FILE_NAME     = "com.aardvark_visual.ped.aardvarkpedservice.extra.step_file_name";

    static final String STATE_STEP_COUNT = "step_count";
    static final String STATE_DISTANCE   = "distance";
    static final String STATE_LAST_STEP_TIME = "last_step_time";
    private static String mSteplogFilename = null;
    private static String mSteplogFilepath = null;
    private static StepLog  mStepLog       = null;
    private static int      mPurgeNumDays  = -1;
    private static boolean  mPurgeArchive  = false;

    private static NotificationManager mNotificationManager = null;

    private SharedPreferences mSharedPreferences;
    private  static String  mHeartRateDeviceId     = null;   // No hr device
    private  static String  mHeartRateDeviceName   = null;   // No hr device
    private  static double  mStrideLength            = 0.8;  // Stride length in meters. We initialise this from the sharedpreferences.
    private  static boolean mShowMetric              = false; // Use the metric system for display if true (km and cm) Use miles and inches otherwise
    private  static boolean mPreferNativeStepCounter = true;  // Use the StepCounterListener for step detection rather than the linear accel listener.
                                                       // This allows the user to use the counter that works the best for them, if available.

    private  PedStepCounterListener   mStepCounterListener = null;
    private  PedGravityListener       m_GravityListener            = null;
    private  PedLinearAccelListener   m_LinearAccelerationListener = null;
    private  PedAccelerometerListener m_AccelerometerListener      = null;

    private AardvarkPedBluetooth     mBt = null;

    private  SensorManager mSensorManager = null;
    private  Sensor mAccelerometer        = null;
    private  Sensor mStepCounter = null;
    private  Sensor mHeartRateSensor    = null;
    private  Sensor mLinearAcceleration = null;
    private  Sensor mGravity = null;


    private  SensorEventListener[]    m_ActiveListeners = new SensorEventListener[2]; // The one we are listening to.
    private  Sensor[]                 m_ActiveSensors   = new Sensor[2];              // The one we are listening to. 
    private  int[]                    m_ActiveSensorRates = new int[2];

    private  double[] mCurrentGravityVector = new double[3];

    private  int  mHeartRateBps  = -1;
    private  int  mStepCount     = -1;
    private  long mLastStepTime  = 0;
    private  long mLastResetTime = 0;
    private  static boolean mCleanupCalled = false;

    private static AlarmManager alarm_mgr = null;
    private static PendingIntent mAlarmUpdate = null;
    private final static int AardvarkPedID = 90823;

    private PowerManager mPowerMgr = null;
    private final static String AardvarkWakeLockTag = "AardvarkWakeLock";
    private PowerManager.WakeLock mWakeLock = null;

    private static Notification.Builder mBuilder = null;
    public  AardvarkPedWidgetService() {
           super(AardvarkPedWidgetService.class.getSimpleName());
       };

    private static BroadcastReceiver mTickReceiver = null;

/*
    public class StepLogWatcher extends FileObserver {

        public long    mLastModified = 0;
        public boolean mNeedReopen = false;

        StepLogWatcher(String path) {
            super(path, FileObserver.ALL_EVENTS&~(FileObserver.ACCESS));
            mLastModified = System.currentTimeMillis();
        };

        StepLogWatcher(String path, int mask) {
            super(path, mask&~(FileObserver.ACCESS));
            mLastModified = System.currentTimeMillis();
        };

        @Override
        public void onEvent(int event, String path) {

            try {
                System.err.printf("file %s has been touched by %x\n", path, event);
                switch(event) {
                case FileObserver.ACCESS:
                    break;
                case FileObserver.CREATE:
                case FileObserver.OPEN:
                case FileObserver.ATTRIB:
                case FileObserver.CLOSE_NOWRITE:
                    break;

                case FileObserver.MOVED_FROM:

                case FileObserver.MOVED_TO:

                case FileObserver.MOVE_SELF:

                case FileObserver.CLOSE_WRITE:
                case FileObserver.DELETE:
                case FileObserver.DELETE_SELF:
                    mNeedReopen = true;
                    break;
                case FileObserver.MODIFY:
                    mLastModified = System.currentTimeMillis();
                    break;


                default:
                    System.err.printf("don't know event %x\n", path, event);
                    break;
                }
            }
            catch(Exception e) {
                System.err.println("exception in steplog observer"+e);
                e.printStackTrace();
            }
        }
    }

    public static StepLogWatcher mSteplogWatcher = null;
*/

    public static boolean ImportExportInProgress() {
          return mStepLog.ImportExportInProgress();
      }

    @Override
    public void onCreate() {

        
        if (mPowerMgr == null)  mPowerMgr = (PowerManager) getSystemService(POWER_SERVICE);
        if (mWakeLock == null)  mWakeLock = mPowerMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AardvarkWakelockTag");
        mWakeLock.acquire();

        Log.i(TAG, "AardvarkPedWidgetService::onCreate");
        super.onCreate();

       try {
            Context prefsContext = createPackageContext(AardvarkPedAnalysis.AppContextName, Context.CONTEXT_IGNORE_SECURITY);
         //   mSharedPreferences = prefsContext.getSharedPreferences("com.aardvark_visual.ped.aardvarkpedanalysis_preferences",
            mSharedPreferences = prefsContext.getSharedPreferences(AardvarkPedAnalysis.AppSharedPreferencesName, Context.MODE_MULTI_PROCESS);
        }
        catch(Exception e) {
            mSharedPreferences = getSharedPreferences(AardvarkPedAnalysis.AppSharedPreferencesName, Context.MODE_PRIVATE); // This is just to prevent segvs
        }

        Map<String, ?> zoot = mSharedPreferences.getAll();

        boolean val1  =  mSharedPreferences.getBoolean(PREFKEY_UseNativeCounter, true);
        String val2   =  mSharedPreferences.getString(PREFKEY_StrideLength,     "void");
        String val3   =  mSharedPreferences.getString(PREFKEY_MeasurementSystem, "zip");
        String heart_rate_device_addr = mSharedPreferences.getString(PREFKEY_HeartRate_STRING, "");
        String heart_rate_device_name = mSharedPreferences.getString(PREFKEY_HeartRateName_STRING, "");

        mStrideLength = mSharedPreferences.getFloat(PREFKEY_StrideLength_FLOAT, (float) mStrideLength);
        mShowMetric   = mSharedPreferences.getBoolean(PREFKEY_ShowMetric_BOOLEAN, mShowMetric);
        mPreferNativeStepCounter = mSharedPreferences.getBoolean(PREFKEY_UseNativeCounter, mPreferNativeStepCounter);
        mPurgeArchive = mSharedPreferences.getBoolean(PREFKEY_Archive_BOOLEAN, mPurgeArchive);
        mPurgeNumDays = mSharedPreferences.getInt( PREFKEY_PurgeDays_INT, mPurgeNumDays);

        mStepCount     =  mSharedPreferences.getInt(PREFKEY_StepCount_INT, 0);
        mLastResetTime =  mSharedPreferences.getLong(PREFKEY_StartTime_LONG, 0);

        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        //The intent to launch when the user clicks the expanded notification
        Intent intent = new Intent(this, AardvarkPedAnalysis.class);

        // Create an artificial backtrack path which goes to the home view.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(AardvarkPedAnalysis.class);
        stackBuilder.addNextIntent(intent);

   //     intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        /*
        //This constructor is deprecated. Use Notification.Builder instead
        Notification notice = new Notification(R.drawable.aardvark_ped_icon, "Aardvark Ped", System.currentTimeMillis());

        notice.setLatestEventInfo(this, "AardvarkPed", "StepCount: 0\nDistancei(mi): 0.0", pendIntent);
        notice.flags |= Notification.FLAG_NO_CLEAR;
         */

        double unit_conversion = mShowMetric? 0.001 : (1.0/1609.344); // Show km in metric, miles in english
        String notification_text = String.format("steps: %4d  distance(%s): %6.3f", mStepCount, (mShowMetric)?"km":"mi", (mStepCount*mStrideLength*unit_conversion));

        mBuilder = new Notification.Builder(this)
            .setContentText(notification_text)
            .setContentTitle("AardvarkPed")
            .setSmallIcon(R.drawable.aardvark_ped_notification)
            .setAutoCancel(false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendIntent );

        mNotificationManager.notify(AardvarkPedID, mBuilder.build());

        startForeground(AardvarkPedID, mBuilder.getNotification());
        
        // Set up the listeners
        File file_dir = null;
        boolean mkdirs_success = false;
        try {
            file_dir = new File(this.getExternalFilesDir(null) + "/Documents/com.aardvark_visual.ped.aardvarkped/logs");
        }
        catch (Exception e) {
            System.err.println("no files dir");
        }
        if (file_dir == null || !file_dir.exists()) {
            mkdirs_success = file_dir.mkdirs();
            file_dir = new File(this.getExternalFilesDir(null) + "/Documents/com.aardvark_visual.ped.aardvarkped/logs");
        }

        File stepdb_dir = null;
        mkdirs_success = false;
        try {
            stepdb_dir = new File(this.getExternalFilesDir(null) + "/Documents/com.aardvark_visual.ped.aardvarkped/db");
        }
        catch (Exception e) {
            System.err.println("no files dir");
        }
        if (stepdb_dir == null || !stepdb_dir.exists()) {
            mkdirs_success = stepdb_dir.mkdirs();
            stepdb_dir = new File(this.getExternalFilesDir(null) + "/Documents/com.aardvark_visual.ped.aardvarkped/db");
        }

        mSteplogFilename = file_dir+"/aardvarksteplog.csv";
        mSteplogFilepath = stepdb_dir.toString();
        mStepLog = new StepLog(this, mSteplogFilepath, mSteplogFilename, 'a');

//        String path = mStepLog.getAbsolutePath();
//        mSteplogWatcher = new StepLogWatcher(path);
//        mSteplogWatcher.startWatching();

        m_ActiveListeners[0] = null;
        m_ActiveListeners[1] = null;

        m_ActiveSensors[0]   = null;
        m_ActiveSensors[1]   = null;

        if (mSensorManager == null ) {
            mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        }

        mStepCounter = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        mLinearAcceleration = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        mGravity            = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mTickReceiver=new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0){
                     Calendar t = Calendar.getInstance();

                     if (t.get(Calendar.HOUR_OF_DAY) == 0 ) {
                         if (t.get(Calendar.MINUTE) == 0) {
                             if (!mCleanupCalled) {
                                 mCleanupCalled = true;
                                 startActionMidnightCleanup(context);
                                 Log.i(TAG, "Midnight cleanup started by tick");
                             }
                         }
                         else {
                             mCleanupCalled = false;
                         }
                     }
                }
                else if(intent.getAction().compareTo(Intent.ACTION_SHUTDOWN) == 0){

                    // OnDestroy doesn't get called on power down. We need to service shutdown to persist the count

                    mSharedPreferences.edit().putInt(PREFKEY_StepCount_INT, mStepCount).commit();
                    mSharedPreferences.edit().putLong(PREFKEY_StartTime_LONG, mLastResetTime).commit();

                }
            }
        };

        //Register the broadcast receiver to receive TIME_TICK
        IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_TICK);
                     filter.addAction(Intent.ACTION_SHUTDOWN);
                     filter.addAction("android.intent.action.PACKAGE_REMOVED");

        registerReceiver(mTickReceiver, filter);

        if (BuildConfig.HasHeartRate == true) {
            try {
                mBt = AardvarkPedBluetooth.NewInstance(this, mSharedPreferences);
                if (!heart_rate_device_addr.equals("") && mBt != null) {
                    AardvarkPedBluetooth.setDeviceReconnectCallback(new AardvarkPedBluetooth.DeviceReconnectCallback() {
                             @Override
                             public void onDeviceReconnectComplete(final boolean success, final BluetoothDevice dev, final Object data) {
                                     System.err.println("Reconnect complete callback");
                                     AardvarkPedWidgetService.startActionSetHeartRateDevice(AardvarkPedWidgetService.this, dev.getAddress(), dev.getName());
                                     AardvarkPedAnalysis.SettingsFragment.HeartRateConnectStateChanged();
                                 };
                        },  null
                        );
    
                    AardvarkPedBluetooth.ReconnectDevice(heart_rate_device_addr, heart_rate_device_name);
                }
            }
            catch(Exception e) {
                mBt = null;
                // Only a device with no bluetooth classes is the amazon phone 
            }
        }
    }


    public void onDestroy() {
        Log.i(TAG, "Service has been destroyed");
        super.onDestroy();
        try {
            Log.d(TAG, "close steplog");
            mStepLog.close();
        }
        catch(Exception e) {
            Log.d(TAG, "steplog not closed");
        }
        unregisterReceiver(mTickReceiver);
        if (mBt != null) {
            AardvarkPedBluetooth.unregisterHeartRateDevice();
        }

        Log.i(TAG, "Persist stepcount = "+Integer.toString(mStepCount));
        mSharedPreferences.edit().putInt(PREFKEY_StepCount_INT, mStepCount).commit();
        mSharedPreferences.edit().putLong(PREFKEY_StartTime_LONG, mLastResetTime).commit();

        if (mWakeLock != null)  {
            try {
                mWakeLock.release();
                mWakeLock = null;
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }

    }

    public static double StrideLength() {
         return mStrideLength;
    }

    @Override
    public int onStartCommand(Intent intent, int flags,  int startId) {
        return super.onStartCommand(intent, flags, startId);
    }
 

   // We override handle message because, well, it keeps stopping us. We don't want to stop.

//  @Override
//    public void handleMessage(Message msg) {
//        onHandleIntent((Intent) msg.obj);
//        stopSelf(/*msg.arg1*/0);
//    }


    public static void startActionSetPreferNative(Context context, boolean preferNative) {
        Intent intent = new Intent(context, AardvarkPedWidgetService.class);
        intent.setAction(INTENT_UseNativeCounter_Updated);
        intent.putExtra(EXTRA_USE_NATIVE, preferNative);

        context.startService(intent);
    }

    public static void startActionSetPurgeBefore(Context context, int purgeNumDays) {
        Intent intent = new Intent(context, AardvarkPedWidgetService.class);
        intent.setAction(INTENT_PurgeBefore_Updated);
        intent.putExtra(EXTRA_PURGEBEFORE, purgeNumDays);

        context.startService(intent);
    }

    public static void startActionSetArchive(Context context, boolean archive) {
        Intent intent = new Intent(context, AardvarkPedWidgetService.class);
        intent.setAction(INTENT_PurgeArchive_Updated);
        intent.putExtra(EXTRA_PURGEARCHIVE, archive);

        context.startService(intent);
    }

    public static void startActionSetShowMetric(Context context, boolean showMetric){
        Intent intent = new Intent(context, AardvarkPedWidgetService.class);
        intent.setAction(INTENT_ShowMetric_Updated);
        intent.putExtra(EXTRA_SHOWMETRIC, showMetric);

        context.startService(intent);
    }

    public static void startActionSetHeartRateDevice(Context context, String btDeviceId, String btDeviceName){
        Intent intent = new Intent(context, AardvarkPedWidgetService.class);
        intent.setAction(INTENT_HeartRateDevice_Updated);
        intent.putExtra(EXTRA_HEART_RATE_DEV_ID, btDeviceId);
        intent.putExtra(EXTRA_HEART_RATE_DEV_NM, btDeviceName);

        context.startService(intent);
    }


    // We the service don't get preferences.  We are informed of them by the activity immediately on start, and 
    // as they change.

    public static void startActionSetStrideLength(Context context, float strideLen) {
        Intent intent = new Intent(context, AardvarkPedWidgetService.class);
        intent.setAction(INTENT_StrideLength_Updated);
        intent.putExtra(EXTRA_STRIDE_LENGTH, strideLen);

        context.startService(intent);
    }

    public static void startActionGetCount(Context context) {
        Intent intent = new Intent(context, AardvarkPedWidgetService.class);
        intent.setAction(ACTION_START_COUNT);

        context.startService(intent);
    }

    /**
     * Starts this service to perform action GetCount with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionStartCount(Context context) {
        try {
            Intent intent = new Intent(context, AardvarkPedWidgetService.class);
            intent.setAction(ACTION_START_COUNT);

            context.startService(intent);

            // And then we tickle ourselves every three seconds until stop

            final GregorianCalendar     thyme     = (GregorianCalendar)Calendar.getInstance();
            alarm_mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

            thyme.set(GregorianCalendar.MINUTE, 0);
            thyme.add(GregorianCalendar.SECOND, 3);

            Intent updateIntent = new Intent(context, AardvarkPedWidgetService.class);
            updateIntent.setAction(AardvarkPedWidgetService.ACTION_GET_COUNT);
            mAlarmUpdate = PendingIntent.getService(context, 0, updateIntent, 0);
        }
        catch(Exception e) {
            e.printStackTrace();
        }

    }


    public static void startActionPauseCount(Context context) {
        try {
            Intent intent = new Intent(context, AardvarkPedWidgetService.class);
            intent.setAction(ACTION_PAUSE_COUNT);

            context.startService(intent);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Starts this service to perform action ResetCount with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionResetCount(Context context) {
        Intent intent = new Intent(context, AardvarkPedWidgetService.class);
        intent.setAction(ACTION_RESET_COUNT);
        context.startService(intent);
    }

    public static void startActionMidnightCleanup(Context context) {
        Intent intent = new Intent(context, AardvarkPedWidgetService.class);
        intent.setAction(ACTION_MIDNIGHT_CLEANUP);
        context.startService(intent);
    }

    public static void startActionStopService(Context context) {
        Intent intent = new Intent(context, AardvarkPedWidgetService.class);
        intent.setAction(INTENT_StopService);
        context.startService(intent);
    }

    public static void startActionStartService(Context context) {
        Intent intent = new Intent(context, AardvarkPedWidgetService.class);
        intent.setAction( INTENT_StartService );
        context.startService(intent);
    }

    public static void startActionImportStepCsv(Context context, String filename) {
        Intent intent = new Intent(context, AardvarkPedWidgetService.class);
        intent.setAction( INTENT_ImportStepCsv);
        intent.putExtra(EXTRA_STEP_FILE_NAME, filename);
        context.startService(intent);
    }

   
    // We don't take a file name or date range here. When you export 
    // via the option, it takes everything and puts it in the standard place.

    public static void startActionExportStepCsv(Context context, String filename) {
        Intent intent = new Intent(context, AardvarkPedWidgetService.class);
        intent.setAction( INTENT_ExportStepCsv);
        intent.putExtra(EXTRA_STEP_FILE_NAME, filename);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            try {
                final String action = intent.getAction();
                if (ACTION_GET_COUNT.equals(action)) {
                    handleActionGetCount();
                } 
                else if (ACTION_START_COUNT.equals(action)) {
                    handleActionStartCount();
                } 
                else if (ACTION_PAUSE_COUNT.equals(action)) {
                    handleActionPauseCount();
                } 
                else if (ACTION_RESET_COUNT.equals(action)) {
                    handleActionResetCount();
                } 
                else if ( INTENT_StrideLength_Updated.equals(action)) {
                    final float stride_length = intent.getFloatExtra(EXTRA_STRIDE_LENGTH, (float)mStrideLength);
                    handleActionStrideLength(stride_length);
                }
                else if ( INTENT_ShowMetric_Updated.equals(action)) {
                    final boolean show_metric = intent.getBooleanExtra(EXTRA_SHOWMETRIC, mShowMetric);
                    handleActionShowMetric(show_metric);
                }
                else if ( INTENT_UseNativeCounter_Updated.equals(action)) {
                    final boolean use_native = intent.getBooleanExtra(EXTRA_USE_NATIVE, mPreferNativeStepCounter);
                    handleActionPreferNative(use_native);
                }
                else if ( INTENT_HeartRateDevice_Updated.equals(action)) {
                    final String device_id = intent.getStringExtra(EXTRA_HEART_RATE_DEV_ID);
                    final String device_nm = intent.getStringExtra(EXTRA_HEART_RATE_DEV_NM);
                    handleActionHeartRateDevice(device_id, device_nm);
                }
                else if ( INTENT_PurgeBefore_Updated.equals(action)) {
                    final int num_days = intent.getIntExtra(EXTRA_PURGEBEFORE, -1);
                    handleActionPurgeBefore(num_days);
                }
                else if ( INTENT_PurgeArchive_Updated.equals(action)) {
                    final boolean archive = intent.getBooleanExtra(EXTRA_PURGEARCHIVE, mPurgeArchive);
                    handleActionPurgeArchive(archive);
    //startActionMidnightCleanup(this);
                }
                else if (ACTION_MIDNIGHT_CLEANUP.equals(action)) {
                    handleActionMidnightCleanup();
                }
                else if (INTENT_StartService.equals(action)) {
                    handleActionStartService();
                }
                else if (INTENT_StopService.equals(action)) {
                    handleActionStopService();
                }
                else if (INTENT_ImportStepCsv.equals(action)) {
                    final String filename = intent.getStringExtra(EXTRA_STEP_FILE_NAME);
                    handleActionImportStepCsv(filename);
                }
                else if (INTENT_ExportStepCsv.equals(action)) {
                    final String filename = intent.getStringExtra(EXTRA_STEP_FILE_NAME);
                    handleActionExportStepCsv(filename);
                }
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Handle action GetCount in the provided background thread with the provided
     * parameters.
     */
    private void handleActionGetCount() {
        double unit_conversion = mShowMetric? 0.001 : (1.0/1609.344); // Show km in metric, miles in english

            // This asks for an update of the widget display. Normally 
            // these would be produced from the service when the counter updates,
            // but they can also be requested. If it is requested, we just send the 
            // intent with the info.

            Intent intent = new Intent();
            intent.setAction(AardvarkPedWidgetProvider.WIDGET_STEP_COUNT_UPDATE);
            intent.putExtra("step_count",  mStepCount);
            intent.putExtra("distance", (float) (mStepCount*mStrideLength*unit_conversion));
            intent.putExtra("show_metric", mShowMetric);

            sendBroadcast(intent);

            mSharedPreferences.edit().putInt(PREFKEY_StepCount_INT, mStepCount).commit();
            mSharedPreferences.edit().putLong(PREFKEY_StartTime_LONG, mLastResetTime).commit();
        }

    /**
     * Handle action ResetCount in the provided background thread with the provided
     * parameters.
     */
    private void handleActionResetCount() {
        Log.i(TAG, "AardvarkPedWidgetService::onResetCount");
           Calendar now = Calendar.getInstance();
           
            mLastResetTime = now.getTimeInMillis();
            if (m_ActiveListeners[0] != null  ) {
                ((AardvarkPedListener) m_ActiveListeners[0]).ResetCount();
            }
            try {
                mStepLog.flush();
            }
            catch (Exception e) {
                Log.d(TAG, "could not flush steplog");
            }
            onCounterUpdate(0, 0);
            mSharedPreferences.edit().putInt(PREFKEY_StepCount_INT, mStepCount).commit();
            mSharedPreferences.edit().putLong(PREFKEY_StartTime_LONG, mLastResetTime).commit();
        };


    /**
     * Handle action StartCount in the provided background thread with the provided
     * parameters.
     */
    private void handleActionStartCount() {
            SensorEventListener prev_primary_listener   = m_ActiveListeners[0];
            SensorEventListener prev_secondary_listener = m_ActiveListeners[1];

            Log.i(TAG, "AardvarkPedWidgetService::onStartCount");

            if (mPowerMgr == null)  mPowerMgr = (PowerManager) getSystemService(POWER_SERVICE);
            if (mWakeLock == null)  mWakeLock = mPowerMgr.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK  |
                                                                      PowerManager.ACQUIRE_CAUSES_WAKEUP |
                                                                      PowerManager.ON_AFTER_RELEASE, "AardvarkWakelockTag");
            mWakeLock.acquire();

            // Next order of business, figure out what sensors....

            if (mPreferNativeStepCounter && mStepCounter != null) {
                 if (mStepCounterListener == null) {
                      mStepCounterListener = new PedStepCounterListener();
                      mStepCounterListener.setStepCountCallback(this);
                 }
                 m_ActiveListeners[0] = mStepCounterListener;
                 m_ActiveListeners[1] = null;

                 m_ActiveSensors[0]   = mStepCounter;
                 m_ActiveSensors[1]   = null;

                m_ActiveSensorRates[0] = 100000; // 10 hz. We are just getting the count, which is being maintained by the driver
                m_ActiveSensorRates[1] = 0;

            }
            else if (mLinearAcceleration != null && mGravity != null) {

                if (m_LinearAccelerationListener == null ) { 
                    m_GravityListener   = new PedGravityListener(this, 47.61, mCurrentGravityVector);
                    m_LinearAccelerationListener = new PedLinearAccelListener(this, mCurrentGravityVector);
                    m_LinearAccelerationListener.setStepCountCallback(this);
                }

                m_ActiveListeners[0] = m_LinearAccelerationListener;
                m_ActiveListeners[1] = m_GravityListener;
  
                m_ActiveSensorRates[0] = 20000; // 50 hz
                m_ActiveSensorRates[1] = 20000; // 50 hz

                m_ActiveSensors[0]   = mLinearAcceleration;
                m_ActiveSensors[1]   = mGravity;

            }
            else if (mAccelerometer != null) {

                if (m_AccelerometerListener == null ) { 
                    m_AccelerometerListener = new PedAccelerometerListener(this, mCurrentGravityVector);
                    m_AccelerometerListener.setStepCountCallback(this);
                }

                m_ActiveListeners[0] = m_AccelerometerListener;
                m_ActiveListeners[1] = null;
  
                m_ActiveSensorRates[0] = 20000; // 50 hz
                m_ActiveSensorRates[1] = 0;     // 0 hz

                m_ActiveSensors[0]   = mAccelerometer;
                m_ActiveSensors[1]   = null;

            }
            else {
                
            }

            if (prev_primary_listener != null && prev_primary_listener != m_ActiveListeners[0]) {
                mSensorManager.unregisterListener(prev_primary_listener);
                ((AardvarkPedListener)prev_primary_listener).StopCount();
            }

            if (prev_secondary_listener != null && prev_secondary_listener != m_ActiveListeners[1]) {
                mSensorManager.unregisterListener(m_ActiveListeners[1]);
                mSensorManager.registerListener(m_ActiveListeners[1], m_ActiveSensors[1], m_ActiveSensorRates[1]);
            }

            if (prev_primary_listener != m_ActiveListeners[0]) {
                mSensorManager.registerListener(m_ActiveListeners[0], m_ActiveSensors[0], m_ActiveSensorRates[0]);
                ((AardvarkPedListener)m_ActiveListeners[0]).StartCount();
            }

            // Start the count at the current couni, to synchronise with the persisted count on create

            ((AardvarkPedListener)m_ActiveListeners[0]).setStepCount(mStepCount);
            onCounterUpdate(mStepCount, 0);
        };


    private void handleActionPauseCount() {
            Log.i(TAG, "AardvarkPedWidgetService::onPauseCount");

            if (m_ActiveListeners[0] != null) {
                mSensorManager.unregisterListener(m_ActiveListeners[0]);
                ((AardvarkPedListener)m_ActiveListeners[0]).StopCount();
            }
            if (m_ActiveListeners[1] != null) {
                mSensorManager.unregisterListener(m_ActiveListeners[1]);
            }
            mSharedPreferences.edit().putInt(PREFKEY_StepCount_INT, mStepCount).commit();
            mSharedPreferences.edit().putLong(PREFKEY_StartTime_LONG, mLastResetTime).commit();
            if (mWakeLock != null)  {
                mWakeLock.release();
                mWakeLock = null;
            }

            m_ActiveListeners[0] = null; // We quit listening
            m_ActiveListeners[1] = null;
  
            try {
                mStepLog.flush();
            }
            catch (Exception e) {
                Log.d(TAG, "could not flush steplog");
            }
        };

    public void handleActionStrideLength(double strideLength) {
              Log.i(TAG, "AardvarkPedWidgetService::handleActionStrideLength = "+Double.toString(strideLength));
              mStrideLength = strideLength;
        };


    public void handleActionShowMetric(boolean showMetric) {
              Log.i(TAG, "AardvarkPedWidgetService::handleActionStrideLength = "+Boolean.toString(showMetric));
              mShowMetric = showMetric;
        };


    public void handleActionPreferNative(boolean preferNative) {
              Log.i(TAG, "AardvarkPedWidgetService::handleActionPreferNativeUpdate = "+Boolean.toString(preferNative));
              mPreferNativeStepCounter = preferNative;

              // Will switch the listeners now that we changed the preference
              handleActionStartCount();
        };

    public void handleActionPurgeBefore(int numDays) {
              Log.i(TAG, "AardvarkPedWidgetService::handleActionPurgeBefore = "+Integer.toString(numDays));
              mPurgeNumDays = numDays;
        };

    public void handleActionPurgeArchive(boolean archive) {
              Log.i(TAG, "AardvarkPedWidgetService::handleActionPurgeArchive = "+Boolean.toString(archive));
              mPurgeArchive = archive;
        };

    public void handleActionHeartRateDevice(String deviceId, String deviceName) {
              Log.i(TAG, "AardvarkPedWidgetService::handleActionHeartRateDevice = "+deviceId);
              mHeartRateDeviceId   = deviceId;
              mHeartRateDeviceName = deviceName;

              mSharedPreferences.edit().putString(PREFKEY_HeartRate_STRING, deviceId).commit();
              mSharedPreferences.edit().putString(PREFKEY_HeartRateName_STRING, deviceName).commit();
              AardvarkPedAnalysis.SettingsFragment.HeartRateConnectStateChanged();

              if (mBt == null) {
                  return;
              }

              if (deviceId.equals("")) {
                  mHeartRateDeviceId   = null;
                  mHeartRateDeviceName = null;
                  AardvarkPedBluetooth.unregisterHeartRateDevice();
                  mHeartRateBps = -1; // If its -1 we pass that through
    
                  // This turns the widget's heart rate display off.

                  Intent intent = new Intent();
                  intent.setAction(AardvarkPedWidgetProvider.WIDGET_HEART_COUNT_UPDATE);
                  intent.putExtra("hrate", mHeartRateBps);
                  sendBroadcast(intent);
              }
              else {
    
                  // Here is where we set up the listener. The listener will send intents to update the widget

                  CounterUpdateCallback  callback = new CounterUpdateCallback() {
                      @Override
                      public void onCounterUpdate(int heartRate, int deltaHeartRate) {
                          System.err.println("update heart rate rate = " + heartRate);
    
                          // if the heartrate is negative, we dropped the connection
                          // Either way, send the intent to the widget
    
                          mHeartRateBps = heartRate; // If its -1 we pass that through
    
                          Intent intent = new Intent();
                          intent.setAction(AardvarkPedWidgetProvider.WIDGET_HEART_COUNT_UPDATE);
                          intent.putExtra("hrate", mHeartRateBps);
                          sendBroadcast(intent);

                          Calendar current_time = Calendar.getInstance();
                          mStepLog.Reopen();
                          mStepLog.WriteEntry(0, 0, current_time.getTimeInMillis(), 0.0, 0.0, 0.0, mHeartRateBps);
                          mStepLog.Close();
                      };
                  };

                  boolean success = AardvarkPedBluetooth.registerHeartRateDevice(deviceId, callback);
              }
        };

    private void handleActionMidnightCleanup() {
        Log.i(TAG, "AardvarkPedWidgetService::onMidnightCleanup");
        Calendar now = Calendar.getInstance();

        mLastResetTime = now.getTimeInMillis();
        /*
        if (m_ActiveListeners[0] != null  ) {
            ((AardvarkPedListener) m_ActiveListeners[0]).ResetCount();
        }
        */
        if ( mStepCounterListener != null) {
            mStepCounterListener.ResetCount();
        }
        if ( m_LinearAccelerationListener != null ) {
            m_LinearAccelerationListener.ResetCount();
        }
        if ( m_AccelerometerListener != null ) {
            m_AccelerometerListener.ResetCount();
        }

        if (mPurgeNumDays > 0) {
            now.add(Calendar.DAY_OF_YEAR, -mPurgeNumDays);
            now.set(Calendar.HOUR_OF_DAY, 0);
            now.set(Calendar.MINUTE, 0);
            now.set(Calendar.SECOND, 0);
            try {
                mStepLog.PurgeLogFile(now.getTimeInMillis(), mPurgeArchive);
            }
            catch (Exception e) {
                Log.d(TAG, "could not truncate steplog");
                e.printStackTrace();
            }
        }

        onCounterUpdate(0, 0);
    };


    private void handleActionStartService() {
        Log.i(TAG, "AardvarkPedWidgetService::handleActionStartService");
        handleActionStartCount();
    };


    private void handleActionStopService() {
        Log.i(TAG, "AardvarkPedWidgetService::handleActionStopService");
        handleActionPauseCount();
        stopSelf();
    };

    private void handleActionImportStepCsv(String filename) {
        Log.i(TAG, "AardvarkPedWidgetService::handleActionImportStepCsv");

        boolean rslt = mStepLog.ImportArchiveFile(filename);
    };

    private void handleActionExportStepCsv(String filename) {
        long start_time = mStepLog.mStepDB.getFirst();
        long end_time   = mStepLog.mStepDB.getLast();

        Log.i(TAG, "AardvarkPedWidgetService::handleActionExportStepCsv");

        boolean rslt = mStepLog.ExportArchiveFile(start_time, end_time, filename );
    };


    public void onCounterUpdate(int steps, int deltastep) {
        long deltatime = 0;
        long current_time = System.currentTimeMillis();
        double direction_x = 0.0;
        double direction_y = 1.0;
        double direction_z = 0.0;
        int    heart_rate  = 0;
        double unit_conversion = mShowMetric? 0.001 : (1.0/1609.344); // Show km in metric, miles in english
        Calendar reset_clock = Calendar.getInstance();
        int elapsed_minutes = 0; // Since last reset
        int elapsed_hours   = 0;
        long diff_millis = 0;
        long current_millis = reset_clock.getTimeInMillis(); // Because getInstance returns the current time.

            // If we are 0, it means the time was never set, ie we are starting for the first time, or restarting.

            if (mPowerMgr == null)  mPowerMgr = (PowerManager) getSystemService(POWER_SERVICE);
            if (mWakeLock == null)  mWakeLock = mPowerMgr.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK  |
                                                                      PowerManager.ACQUIRE_CAUSES_WAKEUP |
                                                                      PowerManager.ON_AFTER_RELEASE, "AardvarkWakelockTag");
            mWakeLock.acquire();  // 

            if (mLastResetTime != 0) {
                reset_clock.setTimeInMillis(mLastResetTime);
                diff_millis = current_millis - mLastResetTime;
                elapsed_minutes = (int)(diff_millis/ (60*1000) % 60);
                elapsed_hours   = (int)(diff_millis/ (3600*1000) % 60);
            }
            else {
                mLastResetTime = reset_clock.getTimeInMillis();
                elapsed_minutes = 0; // for clarity. then all the possible paths are in this if
                elapsed_hours   = 0;
            }
 
            deltastep     = steps-mStepCount;
            mStepCount    = steps;
            if (mLastStepTime == 0) {
                deltatime = 500*deltastep;  // 1/2 second since the last step is "average".
            }
            else {
                deltatime = current_time - mLastStepTime;
            }
            mLastStepTime = current_time;


            Log.i(TAG, "AardvarkPedWidgetService::onCounterUpdate");
            Intent intent = new Intent();
            intent.setAction(AardvarkPedWidgetProvider.WIDGET_STEP_COUNT_UPDATE);

            intent.putExtra("step_count",  steps);
            intent.putExtra("distance", (float) (mStepCount*mStrideLength*unit_conversion));
            intent.putExtra("show_metric", mShowMetric);
            intent.putExtra("lst_rst_h", reset_clock.get(Calendar.HOUR_OF_DAY));
            intent.putExtra("lst_rst_m", reset_clock.get(Calendar.MINUTE));
            intent.putExtra("elpsed_h", elapsed_hours);
            intent.putExtra("elpsed_m", elapsed_minutes);
            intent.putExtra("hrate", mHeartRateBps);
            sendBroadcast(intent);

            String text = String.format("steps: %4d  distance(%s): %6.3f", mStepCount, (mShowMetric)?"km":"mi", (mStepCount*mStrideLength*unit_conversion));
            mBuilder.setContentText(text);
            mNotificationManager.notify(AardvarkPedID, mBuilder.build());

            mSharedPreferences.edit().putInt(PREFKEY_StepCount_INT, mStepCount).commit();
            mSharedPreferences.edit().putLong(PREFKEY_StartTime_LONG, mLastResetTime).commit();

            if (deltastep >= 0) {
                 
                // Now write out the step count.... deltastep, deltatime, currenttime, direction vector(later), heartrate (later).

                mStepLog.Reopen();
                mStepLog.WriteEntry(deltastep, deltatime, current_time, direction_x, direction_y, direction_z, mHeartRateBps);
                mStepLog.Close();

            }
            else {
                Log.d(TAG, "skipped negative step log entry");
            }
       };
}
