
/*
 *  Copyright (c) 2014-2015 Aardvark Visual Software Inc,
 *
 *  All Rights Reserved.
 *
 *
 */
package com.aardvark_visual.ped.aardvarkpedanalysis;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v13.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart;
import org.achartengine.chart.CombinedXYChart;
import org.achartengine.chart.LineChart;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.util.MathHelper;

import com.aardvark_visual.ped.*;
import com.aardvark_visual.ped.bluetooth.*;
import com.aardvark_visual.ped.bluetooth.AardvarkPedBluetooth;
import com.aardvark_visual.ped.BuildInfo;
import com.aardvark_visual.ped.aardvarkpedwidget.AardvarkPedWidgetService;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.common.GooglePlayServicesUtil;

import com.amazon.device.ads.*;


public class AardvarkPedAnalysis extends Activity implements ActionBar.OnNavigationListener, com.amazon.device.ads.AdListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    // SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    // ViewPager mViewPager;

    public  final static String TAG = "AardvarkPedAnalysis";
    public  final static String AppContextName = "com.aardvark_visual.ped.aardvarkped_free";
    public  final static String AppSharedPreferencesName = "com.aardvark_visual.ped.aardvarkped_free_preferences";
    private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";
    private static final String INTENT_StrideLength_Updated     = "com.aardvark_visual.ped.stride_length_in_meters_update";
    private static final String INTENT_ShowMetric_Updated       = "com.aardvark_visual.ped.show_metric_update";
    private static final String INTENT_UseNativeCounter_Updated = "com.aardvark_visual.ped.use_native_counter_update";
    private static final String INTENT_HeartRateDevice_Updated  = "com.aardvark_visual.ped.heart_rate_device_update";

    private static final String INTENT_StepDB_ImportProgress  = "com.aardvark_visual.ped.import_progress";
    private static final String INTENT_StepDB_ExportProgress  = "com.aardvark_visual.ped.export_progress";

    private static final long TEN_SECONDS    = 10000; // In millis
    private static final long TWENTY_SECONDS = 20000; // In millis
    private static final long THRITY_SECONDS = 30000; // In millis
    private static final long SIXTY_SECONDS  = 60000; // In millis

    private static BroadcastReceiver mBtScanStatusReceiver = null;
    private static AlarmManager mAlarmManager      = null;
    private static BroadcastReceiver mAdRefreshReceiver = null;
    private static PendingIntent mAdRefreshPendingIntent = null;
    private static IntentFilter mAdIntentFilter = null;

    // public static View rootView = null;
    public static int screen_width  = 0;
    public static int screen_height = 0;

    // private static String mSteplogFilename =  null;
    private static AardvarkPedAnalyzeStepsByMinute mAardvarkPedAnalyzeStepsByMinute;
    private static String mStepLogFilename;

    public  static String mStepLogFilePath = null;
    public static Activity mActivity = null;

    private static RelativeLayout mCurrentTab = null; // This is the parent of the ad view
    private static com.amazon.device.ads.AdLayout mAmazonAdView = null;
    private static com.google.android.gms.ads.AdView mGoogleAdView = null;
    private static View mAdView = null;

    private static final int MAX_AD_RETRIES = 5; // We will switch to admob if we keep getting errors.
    private static int mAdRetries = 0;
    private static Fragment mFrag = null;
    private static ActionBar mActionBar = null;

    private static double CalcStepSize(double range, double targetSteps) {
            // calculate an initial guess at step size
            double tempStep = range/targetSteps;
    
            // get the magnitude of the step size
            double mag = (double)Math.floor(Math.log10(tempStep));
            double magPow = (double)Math.pow(10, mag);
    
            // calculate most significant digit of the new step size
            double magMsd = (int)(tempStep/magPow + 0.5);
    
            // promote the MSD to either 1, 2, or 5
            if (magMsd > 5.0)
                magMsd = 10.0f;
            else if (magMsd > 2.0)
                magMsd = 5.0f;
            else if (magMsd > 1.0)
                magMsd = 2.0f;
    
            return magMsd*magPow;
        }
    

    public class AboutPreference extends EditTextPreference {

            public AboutPreference(Context context, AttributeSet attrs, int defStyle) {
                super(context, attrs, defStyle);
            }

            public AboutPreference(Context context, AttributeSet attrs) {
                super(context, attrs);
            }

            public AboutPreference(Context context) {
                super(context);
            }
             @Override
             protected void onBindDialogView(View view) {
                      super.onBindDialogView(view); 
                      // Don't bind the dialog view. We don't want to edit the about
             }
        };


    public void FallbackToAdMob() {
         if (mCurrentTab == null) {
            return;
        }

        System.err.printf("FallbackToAdMob\n");
        if (!(mAdView instanceof com.google.android.gms.ads.AdView) ) {
            // We need to swap out the amazon view and swap in the google view
            try {
                mAdView = (View) mGoogleAdView;
                ViewGroup parent = ((ViewGroup)mGoogleAdView.getParent());
                if (parent != null) parent.removeView(mGoogleAdView);
                mCurrentTab.removeView(mAmazonAdView);
                mCurrentTab.addView(mGoogleAdView);
                mCurrentTab.bringChildToFront(mGoogleAdView);

            }
            catch( Exception e) {
                e.printStackTrace();
            }
        }

        AdRequest.Builder adRequestBuilder = new AdRequest.Builder();

       //     adRequestBuilder.addTestDevice("1BC335027D551F7A91CF85A5898911A3");

        mGoogleAdView.loadAd(adRequestBuilder.build());
    }

    public void refreshAd() {
        if (mCurrentTab == null) {
            return;
        }
        if (!(mAdView instanceof com.amazon.device.ads.AdLayout)) {
            // If the google is the current view, we need to swap it out and the 
            // amazon in.
            try {
                mAdView = (View)mAmazonAdView;
                ViewGroup parent = ((ViewGroup)mAmazonAdView.getParent());
                if (parent != null) parent.removeView(mAmazonAdView);
                mCurrentTab.removeView(mGoogleAdView);
                mCurrentTab.addView(mAmazonAdView);
                mCurrentTab.bringChildToFront(mAmazonAdView);

            }
            catch( Exception e) {
                e.printStackTrace();
            }
        }

        try {
            mAmazonAdView.loadAd(new com.amazon.device.ads.AdTargetingOptions());
            mAmazonAdView.setListener(this);
            mAmazonAdView.setTimeout(20000);
        }
        catch( Exception e) {
            e.printStackTrace();
        }
    }

    @Override 
    public void onAdLoaded(com.amazon.device.ads.Ad ad, AdProperties adProperties)
    {
         mAlarmManager.set( AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + SIXTY_SECONDS, mAdRefreshPendingIntent );
         mAdRetries = 0;
    }
  
    @Override 
    public void onAdFailedToLoad(com.amazon.device.ads.Ad ad, AdError error)
    {  boolean goToAdMob = false;

        if (mAdRetries++ > MAX_AD_RETRIES) {
            goToAdMob = true;
        }
        else {
            System.err.printf("amazon ad failed to load. Msg = %s\n", error.getMessage());
            switch(error.getCode()){
            case NO_FILL:         // The ad request succeeded but no ads were available. Don’t retry immediately.
                mAlarmManager.set( AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + TWENTY_SECONDS, mAdRefreshPendingIntent );
           //     goToAdMob = false;
                goToAdMob = true;
                break;

            case NETWORK_ERROR: // The ad request failed due to problems with network connectivity. Try again when a connection becomes available.
            case NETWORK_TIMEOUT: // The connection to the ad server timed out before it could retrieve all the required data. Try again.
            case INTERNAL_ERROR:  // The ad request failed due to a server-side error. Try again but limit the number of retry attempts.
            case REQUEST_ERROR:   // There is a problem with the ad request parameters. Do not retry. Review the error message and update your app accordingly.

            // Switch to admob
                mAlarmManager.set( AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + SIXTY_SECONDS, mAdRefreshPendingIntent );
                goToAdMob = true;
                break;

            default:
                goToAdMob = true;
                break;
            }
        }

        if (goToAdMob) {
           FallbackToAdMob();
        }

    }

    @Override
    public void onAdDismissed(Ad ad) {
        mAlarmManager.set( AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + TEN_SECONDS, mAdRefreshPendingIntent );
    }

    @Override
    public void onAdExpanded(Ad ad) {
    }



    public void onAdResized(android.graphics.Rect positionOnScreen) {
    }


    @Override
    public void onAdCollapsed(Ad ad) {
        mAlarmManager.set( AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + SIXTY_SECONDS, mAdRefreshPendingIntent );
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String step_log_filename = null;

        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_aardvark_ped_analysis);
        Display display;
        display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screen_width = (int)(size.x*0.75);
        screen_height = (int) (size.y*0.75);
        mActivity = this;

        // Set up the action bar to show a dropdown list.
        mActionBar = getActionBar();
        mActionBar.setDisplayShowTitleEnabled(false);
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                        mActionBar.getThemedContext(),
                        android.R.layout.simple_list_item_1,
                        android.R.id.text1,
                        new String[]{
                                getString(R.string.title_section1),
                                getString(R.string.title_section2),
                                getString(R.string.title_section3),
                                getString(R.string.title_section4),
                                getString(R.string.title_section5),
                                getString(R.string.title_section6)
                        }) {
                          @Override
                          public View getDropDownView(int position, View convertView, ViewGroup parent) {
                             TextView filterName = (TextView) super.getDropDownView(position, convertView, parent);
                             switch (position) {
                             case 2:
                             case 3:
                             case 4:
                                 filterName.setTextColor(0xff888888);
                                 break;

                             default:
                                 filterName.setTextColor(0xffffffff);
                                 break;
                             }
                             return filterName;
                           }
               };

        // Set up the dropdown list navigation in the action bar.
        mActionBar.setListNavigationCallbacks( adapter, this );
                // Specify a SpinnerAdapter to populate the dropdown list.

        File file_dir = null;
        boolean made_dirs = false;
        if (file_dir == null) {
            try {
                file_dir = new File(this.getExternalFilesDir(null)+"/Documents/com.aardvark_visual.ped.aardvarkped/logs");
            }
            catch(Exception e) {

                // Not present before KitKat.
                String dir_name = this.getExternalFilesDir(null).toString();
                file_dir = new File(dir_name + "/Documents/com.aardvark_visual.ped.aardvarkped/logs");
            }
            made_dirs = file_dir.mkdirs();
        }

        File stepdb_dir = null;
        made_dirs       = false;
        if (stepdb_dir == null) {
            try {
                stepdb_dir = new File(this.getExternalFilesDir(null)+"/Documents/com.aardvark_visual.ped.aardvarkped/db");
            }
            catch(Exception e) {

                // Not present before KitKat.
                String dir_name = this.getExternalFilesDir(null).toString();
                stepdb_dir = new File(dir_name + "/Documents/com.aardvark_visual.ped.aardvarkped/db");
            }
            made_dirs = stepdb_dir.mkdirs();
        }
        mStepLogFilename = file_dir+"/aardvarksteplog.csv";
        mStepLogFilePath = stepdb_dir.toString();

        try {
            int vers = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
            com.amazon.device.ads.AdRegistration.setAppKey("c4b58c9006f2403c8304c03c416b1510");
            com.amazon.device.ads.AdRegistration.enableLogging(false);
            com.amazon.device.ads.AdRegistration.enableTesting(false);
           
            mAdRefreshReceiver = new BroadcastReceiver() {
                   @Override
                   public void onReceive(Context c, Intent i) {

                              if (i.getAction().compareTo("com.aardvark_visual.ped.adrefresh") == 0){
                                  refreshAd();
                              }
                          }
                   };

            mAdIntentFilter = new IntentFilter("com.aardvark_visual.ped.adrefresh");
            registerReceiver(mAdRefreshReceiver, mAdIntentFilter);
            mAdRefreshPendingIntent = PendingIntent.getBroadcast( this, 0, new Intent("com.aardvark_visual.ped.adrefresh"), 0 );
            mAlarmManager = (AlarmManager)(this.getSystemService( Context.ALARM_SERVICE ));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Restore the previously serialized current dropdown position.
        if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
            getActionBar().setSelectedNavigationItem(
                    savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Serialize the current dropdown position.
        outState.putInt(STATE_SELECTED_NAVIGATION_ITEM,
                getActionBar().getSelectedNavigationIndex());

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final String log_dir_name = this.getExternalFilesDir(null).toString()+"/Documents/com.aardvark_visual.ped.aardvarkped/logs";
        MenuItem mi;
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_aardvark_ped_analysis, menu);
        //addPreferencesFromResource(R.xml.preferences);
        return true;
    }

    public static void ShowBluetoothProgress(final String progressName, final String progress, final boolean finished ) {
        };


    private static AlertDialog mProgressDialog = null;
    private static AlertDialog.Builder mProgressDialogBuilder = null;
    public static void ShowProgress(final String progressName, final long progress, final long goal) {

            if (AardvarkPedAnalysis.mActivity != null) {
                AardvarkPedAnalysis.mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AardvarkPedAnalysis.ImportProgressBar(AardvarkPedAnalysis.mActivity, progress, goal, progressName);
                    }
                });
            }
       };

    public static boolean ImportProgressBar(Context context, final long progress, final long goal, final String progressName) {
        double percentdone = (goal > 0)?((double)progress/(double)goal)*100.0: 0.0;
        try {
            if (mProgressDialogBuilder == null) {
                mProgressDialogBuilder = new AlertDialog.Builder(context);
                mProgressDialogBuilder.setIcon(R.drawable.aardvark_ped_icon_small);
                mProgressDialogBuilder.setTitle(progressName+": "+Integer.toString((int)percentdone)+" per cent");
                mProgressDialog =  mProgressDialogBuilder.show();
                mProgressDialog.setCanceledOnTouchOutside(false) ;
  
                final ActionBar action_bar = AardvarkPedAnalysis.mActivity.getActionBar();
                action_bar.hide();
                
            }
  
            if (progress < goal) {
                mProgressDialog.setTitle(progressName+" "+Integer.toString((int)percentdone)+" per cent");
                mProgressDialog.show();
            }
            else {
                final ActionBar action_bar = AardvarkPedAnalysis.mActivity.getActionBar();
                mProgressDialog.dismiss();

                mProgressDialogBuilder.setIcon(R.drawable.aardvark_ped_icon_small);
                mProgressDialogBuilder.setTitle(progressName+": done");
                mProgressDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                                           action_bar.show();
                                           mProgressDialog.dismiss();
                                           mProgressDialog        = null;
                                           mProgressDialogBuilder = null;
                                      }
                             });
                mProgressDialog =  mProgressDialogBuilder.show();
                mProgressDialog.setCanceledOnTouchOutside(false) ;
            }
        }
        catch( Exception e) {
            e.printStackTrace();
        }

        return true;
    };


    private static AlertDialog mImportExportConfirmDialog = null;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final String log_dir_name = this.getExternalFilesDir(null).toString()+"/Documents/com.aardvark_visual.ped.aardvarkped/logs";

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        MenuItem.OnMenuItemClickListener import_listener =  new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick (MenuItem item){
                            final String filename = item.getTitle().toString();

                            System.err.println("import file " + log_dir_name+"/"+filename);
                            AlertDialog.Builder builder = new AlertDialog.Builder(AardvarkPedAnalysis.this);
                                                builder.setIcon(R.drawable.aardvark_ped_icon_small);
                                                builder.setTitle( "Import File [" + filename + "]");
                                                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                     public void onClick(DialogInterface dialog, int which) {
                                                                 System.err.println("Import file "+filename);
                                                                 AardvarkPedWidgetService.startActionImportStepCsv(AardvarkPedAnalysis.this, log_dir_name+"/"+filename);
                                                            }
                                                        });
                                                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                     public void onClick(DialogInterface dialog, int which) {
                                                                 System.err.println("cancel");
                                                                 mImportExportConfirmDialog.dismiss();
                                                            }
                                                        });
                                                mImportExportConfirmDialog = builder.show();
                                                mImportExportConfirmDialog.setCanceledOnTouchOutside(false) ;
                            return true;
                        }
                    };

        MenuItem.OnMenuItemClickListener export_listener =  new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick (MenuItem item){
                             final String filename = "aardvarkped.csv";
                             Menu m = (Menu)item.getSubMenu();

                             if (AardvarkPedWidgetService.ImportExportInProgress()) {
                                 AlertDialog.Builder builder = new AlertDialog.Builder(AardvarkPedAnalysis.this);
                                 builder.setIcon(R.drawable.aardvark_ped_icon_small);
                                 builder.setTitle( "Export File in progress.\n Please try again later");
                                 builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                     public void onClick(DialogInterface dialog, int which) {
                                         System.err.println("cancel");
                                         mImportExportConfirmDialog.dismiss();
                                         mImportExportConfirmDialog = null;

                                     }
                                 });

                                 mImportExportConfirmDialog = builder.show();
                                 mImportExportConfirmDialog.setCanceledOnTouchOutside(false) ;

                                 return true;
                             }

            // else
                             System.err.println("export file " + log_dir_name + "/" + filename);
                             AlertDialog.Builder builder = new AlertDialog.Builder(AardvarkPedAnalysis.this);
                                                 builder.setIcon(R.drawable.aardvark_ped_icon_small);
                                                 builder.setTitle("Export File [" + filename + "]");
                                                 builder.setCancelable(true);

                                                 builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                     public void onClick(DialogInterface dialog, int which) {
                                                         System.err.println("Export file " + filename);
                                                         AardvarkPedWidgetService.startActionExportStepCsv(AardvarkPedAnalysis.this, log_dir_name + "/" + filename);
                                                     }
                                                 });
                                                 builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                      public void onClick(DialogInterface dialog, int which) {
                                                                  System.err.println("cancel");
                                                                  mImportExportConfirmDialog.dismiss();
                                                                  mImportExportConfirmDialog = null;

                                                             }
                                                         });

                             mImportExportConfirmDialog = builder.show();
                             mImportExportConfirmDialog.setCanceledOnTouchOutside(false) ;
                            /*
                             WindowManager.LayoutParams WMLP = mImportExportConfirmDialog.getWindow().getAttributes();
                 
                             WMLP.x = 0;   // x position
                             WMLP.y = 0;   // y position
                             WMLP.height = 300;
                             WMLP.width  = 400;

                             mImportExportConfirmDialog.getWindow().setAttributes(WMLP);
                             */

                             return true;
                         }
                    };


        //noinspection SimplifiableIfStatement
        if (id == R.id.action_startservice) {
            AardvarkPedWidgetService.startActionStartService(this);
            return true;
        }
        else if (id == R.id.action_stopservice) {

            AardvarkPedWidgetService.startActionStopService(this);
            return true;
        }
        else if (id == R.id.action_importstepfile) {
            Menu m = (Menu)item.getSubMenu();
            m.clear();
            ((SubMenu)m).setHeaderTitle(R.string.action_selectimportfile);

            MenuItem mi = null;

            ArrayList<String> fileitem = new ArrayList<String>();
            ArrayList<String> filepath = new ArrayList<String>();
            File f = new File(log_dir_name);
            File[] files = f.listFiles();

            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                filepath.add(file.getPath());
                if (file.isDirectory()) {
                    mi = m.add(file.getName() + "/");
                    mi.setOnMenuItemClickListener(import_listener);
                }
                else {
                    mi = m.add(file.getName());
                    mi.setOnMenuItemClickListener(import_listener);
                }
            }
            return true;
        }
        else if (id == R.id.action_exportstepfile) {
            Menu m = (Menu)item.getSubMenu();
            m.clear();
            ((SubMenu)m).setHeaderTitle(R.string.action_selectexportfile);

            MenuItem mi = null;

                    mi = m.add("aardvarkped.csv");
                    mi.setOnMenuItemClickListener(export_listener);
            return true;
        }      

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(int position, long id) {
        // When the given dropdown item is selected, show its contents in the
        // container view.

        switch(position) {
            case 0:
                mFrag =  StepsPerDayChartFragment.newInstance(this, 1);
                break;

            case 1:
                mFrag = StepsPerWeekFragment.newInstance(this, 2);
                break;

            case 5:
                mFrag = (Fragment)SettingsFragment.newInstance(this, 6);
                break;

            default:
                mFrag = (Fragment)FeatureUnavailableFragment.newInstance(this, position+1);
                break;
        }

        getFragmentManager().beginTransaction().replace(R.id.container, mFrag).commit();
        return true;
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {


        if ( mProgressDialog != null ) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }

        if ( mImportExportConfirmDialog != null ) {
            mImportExportConfirmDialog.dismiss();
            mImportExportConfirmDialog = null;
        }

    };


    @Override
    public void onDestroy() {


        if ( mProgressDialog != null ) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }

        if ( mImportExportConfirmDialog != null ) {
            mImportExportConfirmDialog.dismiss();
            mImportExportConfirmDialog = null;
        }

        mProgressDialogBuilder = null;

        super.onDestroy();

    };



    public static class StepsPerDayChartFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private static View rootView = null;
        private static AardvarkPedAnalysis mActivity = null;

        private static GraphicalView               mChart = null;
        private static XYMultipleSeriesRenderer mRenderer = null;

     

        private GraphicalView BuildChartStepsByHour(Context context) {
                
                // We update the count this way

                AardvarkPedAnalysis.mAardvarkPedAnalyzeStepsByMinute = new AardvarkPedAnalyzeStepsByMinute(context, AardvarkPedAnalysis.mStepLogFilePath, AardvarkPedAnalysis.mStepLogFilename);

                XYMultipleSeriesDataset  dataset = new XYMultipleSeriesDataset();
                XYSeries             step_series = null;


                final long DAY_LENGTH_IN_MILLIS = (3600*24*1000); // seconds per hour times hours per day, times millisecond in second
                long daystartinmillis = 0;
                long dayendinmillis   = 0;
                int  num_days = 0;

                Calendar cal = Calendar.getInstance(); // today in millis
                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
                cal.set(Calendar.MILLISECOND, 0);
                dayendinmillis   = cal.getTimeInMillis();
                daystartinmillis = AardvarkPedAnalysis.mAardvarkPedAnalyzeStepsByMinute.FirstDay();

                if (daystartinmillis < 1){
                    daystartinmillis = dayendinmillis; // There is no valid data
                }
                cal.setTimeInMillis(daystartinmillis);


                int max_steps = -1;
                num_days = (int)((dayendinmillis-daystartinmillis)/DAY_LENGTH_IN_MILLIS)+1;

                int[] steps_per_hour = AardvarkPedAnalysis.mAardvarkPedAnalyzeStepsByMinute.StepsForDay(daystartinmillis, num_days, null, null, null);

                

                mRenderer = new XYMultipleSeriesRenderer(1);
                step_series   = new XYSeries("StepsPerDay",  0);
                dataset.addSeries(0, step_series);


                for (int i = 1; i < steps_per_hour.length; i++ ) {
                    if (steps_per_hour[i] > 0.0) {
                        step_series.add((i-1)+0.2, steps_per_hour[i]);
                    }
                    else {
                        step_series.add((i-1)+0.2, MathHelper.NULL_VALUE);
                    }
                    if (max_steps < steps_per_hour[i]) {
                        max_steps = steps_per_hour[i];
                    }
                }

                int[] margins = new int[4];
                margins[0] = (int)(10* getResources().getDisplayMetrics().density);
                margins[1] = (int)(60* getResources().getDisplayMetrics().density);
                margins[2] = (int)(30* getResources().getDisplayMetrics().density);
                margins[3] = (int)(10* getResources().getDisplayMetrics().density);

                // mRenderer.setChartTitle("step speed " +((mShowMetric)? "km/hr" :"mi/hr") );
                mRenderer.setXTitle("");
                mRenderer.setAxesColor(Color.WHITE);
                mRenderer.setLabelsColor(Color.WHITE);
                mRenderer.setApplyBackgroundColor(false);
                mRenderer.setMarginsColor(Color.BLACK);
                mRenderer.setZoomEnabled(false);
                mRenderer.setZoomEnabled(false, false);
                mRenderer.setPanEnabled(true, false);
                mRenderer.setPanLimits(new double[] { 0.0, steps_per_hour.length, 0.0, 1000.0});
                mRenderer.setZoomButtonsVisible(false);
                mRenderer.setBarSpacing(2.0);
                mRenderer.setMargins(margins);
                mRenderer.setAxisTitleTextSize(11* getResources().getDisplayMetrics().density);
                mRenderer.setChartTitleTextSize(20* getResources().getDisplayMetrics().density); 
                mRenderer.setLabelsTextSize(11* getResources().getDisplayMetrics().density);

                mRenderer.setXLabelsAngle(90);

                for (int i = 0; i < num_days; i++ ) {
                    double hour = (i*24)+0.2;
                    int day = cal.get(Calendar.DAY_OF_MONTH);
                    int mon = cal.get(Calendar.MONTH);
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                    String date_string = Integer.toString(mon+1)+"/"+Integer.toString(day);
                    mRenderer.addXTextLabel(hour++, "mid "+date_string);
                    mRenderer.addXTextLabel(hour++, " 1:00 am");
                    mRenderer.addXTextLabel(hour++, " 2:00 am");
                    mRenderer.addXTextLabel(hour++, " 3:00 am");
                    mRenderer.addXTextLabel(hour++, " 4:00 am");
                    mRenderer.addXTextLabel(hour++, " 5:00 am");
                    mRenderer.addXTextLabel(hour++, " 6:00 am");
                    mRenderer.addXTextLabel(hour++, " 7:00 am");
                    mRenderer.addXTextLabel(hour++, " 8:00 am");
                    mRenderer.addXTextLabel(hour++, " 9:00 am");
                    mRenderer.addXTextLabel(hour++, "10:00 am");
                    mRenderer.addXTextLabel(hour++, "11:00 am");
                    mRenderer.addXTextLabel(hour++, "noon "+date_string);
                    mRenderer.addXTextLabel(hour++, " 1:00 pm");
                    mRenderer.addXTextLabel(hour++, " 2:00 pm");
                    mRenderer.addXTextLabel(hour++, " 3:00 pm");
                    mRenderer.addXTextLabel(hour++, " 4:00 pm");
                    mRenderer.addXTextLabel(hour++, " 5:00 pm");
                    mRenderer.addXTextLabel(hour++, " 6:00 pm");
                    mRenderer.addXTextLabel(hour++, " 7:00 pm");
                    mRenderer.addXTextLabel(hour++, " 8:00 pm");
                    mRenderer.addXTextLabel(hour++, " 9:00 pm");
                    mRenderer.addXTextLabel(hour++, "10:00 pm");
                    mRenderer.addXTextLabel(hour++, "11:00 pm");
                }

              
                XYSeriesRenderer step_renderer = new XYSeriesRenderer();
                step_renderer.setColor(Color.parseColor("#11AAff"));
                step_renderer.setDisplayChartValues(true);
                step_renderer.setShowLegendItem(false);
                step_renderer.setChartValuesTextAlign(Paint.Align.RIGHT);
                step_renderer.setChartValuesTextSize(11* getResources().getDisplayMetrics().density);
                step_renderer.setChartValuesSpacing(6.0f);
                mRenderer.addSeriesRenderer(step_renderer);

                mRenderer.setBarWidth(10.0f);
                mRenderer.setXAxisMin((steps_per_hour.length-12));
                mRenderer.setXAxisMax((steps_per_hour.length));
                mRenderer.setXLabelsAlign(Paint.Align.LEFT);
                mRenderer.setXLabelsColor(Color.WHITE);

                double tick_size = AardvarkPedAnalysis.CalcStepSize(max_steps, 6);
                double num_ticks = Math.ceil(max_steps/tick_size);
                double display_range = tick_size*num_ticks;

                mRenderer.setYLabels(0);
                mRenderer.setXLabels(0);
               
                mRenderer.setYAxisMin(0, 0);
                mRenderer.setYAxisMax(display_range, 0);
                mRenderer.setYLabelsAlign(Paint.Align.RIGHT, 0);
                mRenderer.setYLabelsColor(0, Color.parseColor("#11AAff"));
                double label_val = 0.0;
                for (int i = 0; i < (int)num_ticks; i++) {
                    mRenderer.addYTextLabel(label_val, String.format("%4d\t\t", (int)label_val), 0);
                    label_val += tick_size;
                }

                return ChartFactory.getBarChartView(context, dataset, mRenderer, BarChart.Type.DEFAULT);
        };

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
       public static StepsPerDayChartFragment newInstance(AardvarkPedAnalysis activity, int sectionNumber) {
            StepsPerDayChartFragment fragment = new StepsPerDayChartFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            mActivity = activity;
            return fragment;

        }

        public StepsPerDayChartFragment() {

        }


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            try {
                rootView = inflater.inflate(R.layout.fragment_aardvark_ped_analysis, container, false);

                LinearLayout amazonRootView = (LinearLayout) inflater.inflate(R.layout.amazon_ad, container, false);
                mAmazonAdView = (com.amazon.device.ads.AdLayout) amazonRootView.findViewById(R.id.amazon_ad1);
                mAdView = (View)mAmazonAdView;

                amazonRootView.removeAllViews();

                mChart = BuildChartStepsByHour(mActivity);

                RelativeLayout layout = (RelativeLayout) rootView.findViewById(R.id.tab1);
                layout.addView(mAmazonAdView);
                layout.bringChildToFront(mAmazonAdView);
                mCurrentTab = layout;

            // mAmazonAdView = (com.amazon.device.ads.AdLayout) rootView.findViewById(R.id.advertising1);
                AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
                LinearLayout googleRootView = (LinearLayout)inflater.inflate(R.layout.google_ad, container, false);
                mGoogleAdView = (com.google.android.gms.ads.AdView) googleRootView.findViewById(R.id.google_ad1);

                googleRootView.removeAllViews();

      //      adRequestBuilder.addTestDevice("1BC335027D551F7A91CF85A5898911A3");
                AdTargetingOptions adOptions = new AdTargetingOptions();
                mAmazonAdView.loadAd(adOptions);
                mAmazonAdView.setListener(mActivity);
                mAmazonAdView.setTimeout(20000);
            }
            catch(Exception e) {
                e.printStackTrace();
            }

            return rootView;
        }

        @Override
        public void onConfigurationChanged(Configuration newConfig) {

            LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rootView = inflater.inflate(R.layout.fragment_aardvark_ped_analysis, null);

            try {
                LinearLayout layout = (LinearLayout) mActivity.findViewById(R.id.chart1);
                layout.removeAllViews();
                layout.addView(mChart);
                mChart.repaint();
                AdTargetingOptions adOptions = new AdTargetingOptions();
                mAmazonAdView.loadAd(adOptions);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            // AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
       //     adRequestBuilder.addTestDevice("1BC335027D551F7A91CF85A5898911A3");
            //mAdView.loadAd(adRequestBuilder.build());
        }

        @Override
        public void onPause() {
            super.onPause();
            mActivity.unregisterReceiver(mAdRefreshReceiver);

            if (mChart != null) {
                LinearLayout layout = (LinearLayout) mActivity.findViewById(R.id.chart1);
                layout.removeAllViews();
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            mActivity.registerReceiver(mAdRefreshReceiver, mAdIntentFilter);

            LinearLayout layout = (LinearLayout) mActivity.findViewById(R.id.chart1);
            
            layout.removeAllViews();
            layout.addView(mChart);
             mChart.repaint();
        }

        @Override
        public  void onViewStateRestored(Bundle savedInstanceState) {
            super.onViewStateRestored(savedInstanceState);

            int height = (int)(screen_height * 0.50);
            int width  = (int)(screen_width * 0.50);

        }
    }



    public static class StepsPerWeekFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private static View rootView = null;
        private static AardvarkPedAnalysis mActivity = null;

        private static GraphicalView mChart = null;
        private static XYMultipleSeriesDataset  mDataset  = new XYMultipleSeriesDataset();
        private static XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
        private static XYSeries mDataSeries[] = null;
        private static XYSeriesRenderer mCurrentRenderer = null;

        private GraphicalView BuildChartStepsByDay(Context context) {
                
                final long DAY_LENGTH_IN_MILLIS = (3600*24*1000); // seconds per hour times hours per day, times millisecond in second

                AardvarkPedAnalysis.mAardvarkPedAnalyzeStepsByMinute = new AardvarkPedAnalyzeStepsByMinute(context, AardvarkPedAnalysis.mStepLogFilePath, AardvarkPedAnalysis.mStepLogFilename);

                XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
                XYSeries step_series = new XYSeries("StepsPerDay");
                long weekstartinmillis = 0;
                long weekendinmillis = 0;
                int num_days = -1;
                Calendar cal = Calendar.getInstance();

                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
                cal.set(Calendar.MILLISECOND, 0);
                weekendinmillis   = cal.getTimeInMillis();

                // Some version of the OS give different results with cal.

                if (weekendinmillis < Calendar.getInstance().getTimeInMillis()) {
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                    weekendinmillis = cal.getTimeInMillis();
                }

                weekstartinmillis = AardvarkPedAnalysis.mAardvarkPedAnalyzeStepsByMinute.FirstDay();

                // the divide and mod will be preformed in the same operation by the VM, so there is no extra cycles for 
                // the comparison. The result is an integer ceil fn.

                long delta_millis = (weekendinmillis-weekstartinmillis);
                num_days = (int)(delta_millis/DAY_LENGTH_IN_MILLIS);
                if (delta_millis%DAY_LENGTH_IN_MILLIS != 0) {
                   num_days++;
                }

                int[] steps_per_day = mAardvarkPedAnalyzeStepsByMinute.StepsForWeek(weekstartinmillis, num_days);
                int max_steps = -1;

                for (int i = 1; i < steps_per_day.length; i++ ) {
                    if (steps_per_day[i] > 0.0) {
                        step_series.add((i-1), steps_per_day[i]);
                    }
                    else {
                        step_series.add((i-1), MathHelper.NULL_VALUE);
                    }
                    if (max_steps < steps_per_day[i]) {
                        max_steps = steps_per_day[i];
                    }
                }

                dataset.addSeries(step_series);

                int[] margins = new int[4];
                margins[0] = (int)(10* getResources().getDisplayMetrics().density);
                margins[1] = (int)(40* getResources().getDisplayMetrics().density);
                margins[2] = (int)(30* getResources().getDisplayMetrics().density);
                margins[3] = (int)(10* getResources().getDisplayMetrics().density);

                XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
                // mRenderer.setChartTitle("Steps per Day");
                mRenderer.setXTitle("");
                mRenderer.setYTitle("");
                mRenderer.setAxesColor(Color.WHITE);
                mRenderer.setLabelsColor(Color.WHITE);
                mRenderer.setApplyBackgroundColor(false);
                mRenderer.setMarginsColor(Color.TRANSPARENT);
                mRenderer.setZoomEnabled(false);
                mRenderer.setZoomEnabled(false, false);
                mRenderer.setPanEnabled(true, false);
                mRenderer.setPanLimits(new double[]{0.0, num_days, 0.0, 1000.0});
                mRenderer.setZoomButtonsVisible(false);
                mRenderer.setBarSpacing(1.0);
                mRenderer.setMargins(margins);
                mRenderer.setAxisTitleTextSize(11 * getResources().getDisplayMetrics().density);
                mRenderer.setChartTitleTextSize(20 * getResources().getDisplayMetrics().density);
                mRenderer.setLabelsTextSize(11 * getResources().getDisplayMetrics().density);

                mRenderer.setXLabelsAngle(90);
                int day = 0;
                String[] DAY_OF_THE_WEEK = { " ",
                                             "Sun ", "Mon ", "Tue ", "Wed ", "Thu ", "Fri ", "Sat "
                                           };

                cal.setTimeInMillis(weekstartinmillis);
                String date = (cal.get(Calendar.MONTH)+1)+"/"+cal.get(Calendar.DAY_OF_MONTH);
                for (int i = 1; i < steps_per_day.length; i++) {
                    mRenderer.addXTextLabel( day++, DAY_OF_THE_WEEK[cal.get(Calendar.DAY_OF_WEEK)]+date);
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                    date = (cal.get(Calendar.MONTH)+1)+"/"+cal.get(Calendar.DAY_OF_MONTH);
                }

                mRenderer.setBarWidth(30);
                mRenderer.setXAxisMin(day - 7.5);
                mRenderer.setXAxisMax(day - 0.5);
                mRenderer.setXLabelsAlign(Paint.Align.LEFT);
                mRenderer.setXLabelsColor(Color.WHITE);
                mRenderer.setYLabelsColor(0, Color.WHITE);
                mRenderer.setYLabelsAlign(Paint.Align.RIGHT) ;
                mRenderer.setXLabels(0);
                mRenderer.setYAxisMin(0);
                mRenderer.setYAxisMax( (max_steps * 1.2));
        
                XYSeriesRenderer renderer = new XYSeriesRenderer();
                renderer.setChartValuesFormat(new DecimalFormat("#####"));
                renderer.setColor(Color.parseColor("#11AAff"));
                renderer.setDisplayChartValues(true);
                renderer.setChartValuesTextSize(14* getResources().getDisplayMetrics().density);
                renderer.setShowLegendItem(false);

                mRenderer.addSeriesRenderer(renderer);
        
                return ChartFactory.getBarChartView(context, dataset, mRenderer, BarChart.Type.DEFAULT);
        };

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
       public static StepsPerWeekFragment newInstance(AardvarkPedAnalysis activity, int sectionNumber) {
            StepsPerWeekFragment fragment = new StepsPerWeekFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            mActivity = activity;
            return fragment;

        }

        public StepsPerWeekFragment() {

        }


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            try {
                rootView = inflater.inflate(R.layout.fragment_aardvark_ped_analysis, container, false);

                LinearLayout amazonRootView = (LinearLayout) inflater.inflate(R.layout.amazon_ad, container, false);
                mAmazonAdView = (com.amazon.device.ads.AdLayout) amazonRootView.findViewById(R.id.amazon_ad1);
                amazonRootView.removeAllViews();

                mChart = BuildChartStepsByDay(mActivity);

                RelativeLayout layout = (RelativeLayout) rootView.findViewById(R.id.tab2);
                layout.addView(mAmazonAdView);
                layout.bringChildToFront(mAmazonAdView);
                mCurrentTab = layout;

            // mAmazonAdView = (com.amazon.device.ads.AdLayout) rootView.findViewById(R.id.advertising1);
                AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
                LinearLayout googleRootView = (LinearLayout)inflater.inflate(R.layout.google_ad, container, false);

                mGoogleAdView = (com.google.android.gms.ads.AdView) googleRootView.findViewById(R.id.google_ad1);
                googleRootView.removeAllViews();

          //  AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
        //    adRequestBuilder.addTestDevice("1BC335027D551F7A91CF85A5898911A3");
                AdTargetingOptions adOptions = new AdTargetingOptions();
                mAmazonAdView.loadAd(adOptions);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
           
            return rootView;
        }

        @Override
        public void onConfigurationChanged(Configuration newConfig) {

            try {
                LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                rootView = inflater.inflate(R.layout.fragment_aardvark_ped_analysis, null);

                LinearLayout layout = (LinearLayout) mActivity.findViewById(R.id.chart2);
                layout.removeAllViews();
                layout.addView(mChart);
                mChart.repaint();

                AdTargetingOptions adOptions = new AdTargetingOptions();
                mAmazonAdView.loadAd(adOptions);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            //AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
         //   adRequestBuilder.addTestDevice("1BC335027D551F7A91CF85A5898911A3");
            //mAdView.loadAd(adRequestBuilder.build());
        }

        @Override
        public void onPause() {
            super.onPause();
            if (mChart != null) {
                LinearLayout layout = (LinearLayout) mActivity.findViewById(R.id.chart2);
                layout.removeAllViews();
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            LinearLayout layout = (LinearLayout) mActivity.findViewById(R.id.chart2);
            
            layout.removeAllViews();
            layout.addView(mChart);
            mChart.repaint();
        }

        @Override
        public  void onViewStateRestored(Bundle savedInstanceState) {
            super.onViewStateRestored(savedInstanceState);

            int height = (int)(screen_height * 0.50);
            int width  = (int)(screen_width * 0.50);

        }
    }



    public static class DistancePerDayChartFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private static View rootView = null;
        private static AardvarkPedAnalysis mActivity = null;

        private static GraphicalView mChart = null;
        private static XYMultipleSeriesDataset  mDataset  = new XYMultipleSeriesDataset();
        private static XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
        private static XYSeries mDataSeries[] = null;
        private static XYSeriesRenderer mCurrentRenderer = null;

     
        private static boolean mShowMetric          = false;

        private GraphicalView BuildChartStepsByHour(Context context) {
                
                // We update the count this way

                AardvarkPedAnalysis.mAardvarkPedAnalyzeStepsByMinute = new AardvarkPedAnalyzeStepsByMinute(context, AardvarkPedAnalysis.mStepLogFilePath, AardvarkPedAnalysis.mStepLogFilename);

                XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
                XYSeries step_series = new XYSeries("DistancePerDay");

                SharedPreferences  prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
                String str_stride_length = prefs.getString("stride_length", "");
                String str_show_metric   = prefs.getString("metric", "");
                mShowMetric = (str_show_metric.equals("metric"))? true : false;

                final long DAY_LENGTH_IN_MILLIS = (3600*24*1000); // seconds per hour times hours per day, times millisecond in second
                long daystartinmillis = 0;
                long dayendinmillis   = 0;
                int  num_days = 0;
                int  max_steps = -1;
                Calendar cal = Calendar.getInstance(); // today in millis
     
                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                dayendinmillis   = cal.getTimeInMillis();
                daystartinmillis = AardvarkPedAnalysis.mAardvarkPedAnalyzeStepsByMinute.FirstDay();
                cal.setTimeInMillis(daystartinmillis);
                if (daystartinmillis < 1){
                    daystartinmillis = dayendinmillis; // There is no valid data
                }

                num_days = (int)((dayendinmillis-daystartinmillis)/DAY_LENGTH_IN_MILLIS)+1;

                int[] steps_per_hour = AardvarkPedAnalysis.mAardvarkPedAnalyzeStepsByMinute.StepsForDay(daystartinmillis, num_days, null, null, null);

                double stride_len = AardvarkPedWidgetService.StrideLength();
                double step_distance = (mShowMetric)?(stride_len/1000.0): // km
                                                     (stride_len/1609.1); // miles

                for (int i = 1; i < steps_per_hour.length; i++ ) {
                    if (steps_per_hour[i] > 0.0) {
                        step_series.add(i-1, steps_per_hour[i]*step_distance);
                    }
                    else {
                        step_series.add((i-1), MathHelper.NULL_VALUE);
                    }
                    if (max_steps < steps_per_hour[i]) {
                        max_steps = steps_per_hour[i];
                    }
                }

                dataset.addSeries(step_series);

                int[] margins = new int[4];
                margins[0] = (int)(10* getResources().getDisplayMetrics().density);
                margins[1] = (int)(40* getResources().getDisplayMetrics().density);
                margins[2] = (int)(30* getResources().getDisplayMetrics().density);
                margins[3] = (int)(10* getResources().getDisplayMetrics().density);

                XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
                // mRenderer.setChartTitle((mShowMetric? "Distance per Hour (Km)" : "Distance per Hour (Mi)"));
                mRenderer.setXTitle("");
                if (mShowMetric) {
                    mRenderer.setYTitle(context.getText(R.string.kilometers).toString());
                }
                else {
                    mRenderer.setYTitle(context.getText(R.string.miles).toString());
                }
                mRenderer.setAxesColor(Color.WHITE);
                mRenderer.setLabelsColor(Color.WHITE);
                mRenderer.setApplyBackgroundColor(false);
                mRenderer.setMarginsColor(Color.TRANSPARENT);
                mRenderer.setZoomEnabled(false);
                mRenderer.setZoomEnabled(false, false);
                mRenderer.setPanEnabled(true, false);
                mRenderer.setPanLimits(new double[]{0, steps_per_hour.length, 0.0, 1000.0});
                mRenderer.setZoomButtonsVisible(false);
                mRenderer.setBarSpacing(1.0);
                mRenderer.setMargins(margins);
                mRenderer.setAxisTitleTextSize(11 * getResources().getDisplayMetrics().density);
                mRenderer.setChartTitleTextSize(20 * getResources().getDisplayMetrics().density);
                mRenderer.setLabelsTextSize(11 * getResources().getDisplayMetrics().density);

                mRenderer.setXLabelsAngle(90);
                for (int i = 0; i < num_days; i++ ) {
                    int hour = (i*24);
                    int day = cal.get(Calendar.DAY_OF_MONTH);
                    int mon = cal.get(Calendar.MONTH);
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                    String date_string = Integer.toString(mon+1)+"/"+Integer.toString(day);
                    mRenderer.addXTextLabel(hour++, "mid "+date_string);
                    mRenderer.addXTextLabel(hour++, " 1:00 am");
                    mRenderer.addXTextLabel(hour++, " 2:00 am");
                    mRenderer.addXTextLabel(hour++, " 3:00 am");
                    mRenderer.addXTextLabel(hour++, " 4:00 am");
                    mRenderer.addXTextLabel(hour++, " 5:00 am");
                    mRenderer.addXTextLabel(hour++, " 6:00 am");
                    mRenderer.addXTextLabel(hour++, " 7:00 am");
                    mRenderer.addXTextLabel(hour++, " 8:00 am");
                    mRenderer.addXTextLabel(hour++, " 9:00 am");
                    mRenderer.addXTextLabel(hour++, "10:00 am");
                    mRenderer.addXTextLabel(hour++, "11:00 am");
                    mRenderer.addXTextLabel(hour++, "noon "+date_string);
                    mRenderer.addXTextLabel(hour++, " 1:00 pm");
                    mRenderer.addXTextLabel(hour++, " 2:00 pm");
                    mRenderer.addXTextLabel(hour++, " 3:00 pm");
                    mRenderer.addXTextLabel(hour++, " 4:00 pm");
                    mRenderer.addXTextLabel(hour++, " 5:00 pm");
                    mRenderer.addXTextLabel(hour++, " 6:00 pm");
                    mRenderer.addXTextLabel(hour++, " 7:00 pm");
                    mRenderer.addXTextLabel(hour++, " 8:00 pm");
                    mRenderer.addXTextLabel(hour++, " 9:00 pm");
                    mRenderer.addXTextLabel(hour++, "10:00 pm");
                    mRenderer.addXTextLabel(hour++, "11:00 pm");
                }

                int num_hours = 24*num_days;
                mRenderer.setBarWidth(10);
                mRenderer.setXAxisMin(num_hours - 14);
                mRenderer.setXAxisMax(num_hours);
                mRenderer.setYAxisMin(0);
                mRenderer.setYAxisMax( (max_steps * 1.2 * step_distance));
                mRenderer.setYLabelsAlign(Paint.Align.RIGHT);
                mRenderer.setXLabelsAlign(Paint.Align.LEFT);
                mRenderer.setXLabelsColor(Color.WHITE);
                mRenderer.setYLabelsColor(0, Color.WHITE);
                mRenderer.setXLabels(0);
        
                XYSeriesRenderer renderer = new XYSeriesRenderer();
                renderer.setChartValuesFormat(new DecimalFormat("##.###"));
                renderer.setColor(Color.parseColor("#11AAff"));
                renderer.setDisplayChartValues(true);
                renderer.setChartValuesSpacing(6.0f);
                renderer.setChartValuesTextSize(11* getResources().getDisplayMetrics().density);
                renderer.setShowLegendItem(false);

                mRenderer.addSeriesRenderer(renderer);
        
                return ChartFactory.getBarChartView(context, dataset, mRenderer, BarChart.Type.DEFAULT);
        };

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
       public static DistancePerDayChartFragment newInstance(AardvarkPedAnalysis activity, int sectionNumber) {
            DistancePerDayChartFragment fragment = new DistancePerDayChartFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            mActivity = activity;
            return fragment;

        }

        public DistancePerDayChartFragment() {

        }


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            rootView = inflater.inflate(R.layout.fragment_aardvark_ped_analysis, container, false);

            LinearLayout layout = (LinearLayout) mActivity.findViewById(R.id.chart1);
           
            mChart = BuildChartStepsByHour(mActivity);
            return rootView;
        }

        @Override
        public void onConfigurationChanged(Configuration newConfig) {

            LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rootView = inflater.inflate(R.layout.fragment_aardvark_ped_analysis, null);

            LinearLayout layout = (LinearLayout) mActivity.findViewById(R.id.chart1);
            layout.removeAllViews();
            layout.addView(mChart);
            mChart.repaint();
        }

        @Override
        public void onPause() {
            super.onPause();
            if (mChart != null) {
                LinearLayout layout = (LinearLayout) mActivity.findViewById(R.id.chart1);
                layout.removeAllViews();
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            LinearLayout layout = (LinearLayout) mActivity.findViewById(R.id.chart1);
            
            layout.removeAllViews();
            layout.addView(mChart);
             mChart.repaint();
        }

        @Override
        public  void onViewStateRestored(Bundle savedInstanceState) {
            super.onViewStateRestored(savedInstanceState);

            int height = (int)(screen_height * 0.50);
            int width  = (int)(screen_width * 0.50);

        }
    }



    public static class DistancePerWeekChartFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private static View rootView = null;
        private static AardvarkPedAnalysis mActivity = null;

        private static GraphicalView mChart = null;
        private static XYMultipleSeriesDataset  mDataset  = new XYMultipleSeriesDataset();
        private static XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
        private static XYSeries mDataSeries[] = null;
        private static XYSeriesRenderer mCurrentRenderer = null;

        private static boolean mShowMetric          = false;

        private GraphicalView BuildChartStepsByDay(Context context) {
                
                SharedPreferences  prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
                String str_stride_length = prefs.getString(AardvarkPedWidgetService.PREFKEY_StrideLength, "");
                String str_show_metric   = prefs.getString(AardvarkPedWidgetService.PREFKEY_MeasurementSystem, "");

                mShowMetric = (str_show_metric.equals("metric"))? true : false;

                final long DAY_LENGTH_IN_MILLIS = (3600*24*1000); // seconds per hour times hours per day, times millisecond in second

                AardvarkPedAnalysis.mAardvarkPedAnalyzeStepsByMinute = new AardvarkPedAnalyzeStepsByMinute(context, AardvarkPedAnalysis.mStepLogFilePath, AardvarkPedAnalysis.mStepLogFilename);

                XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
                XYSeries step_series = new XYSeries("DistancePerDay");
                long weekstartinmillis = 0;
                long weekendinmillis = 0;
                int num_days = -1;

                Calendar cal = Calendar.getInstance();

                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
                cal.set(Calendar.MILLISECOND, 0);
                weekendinmillis   = cal.getTimeInMillis();

                // Some version of the OS give different results with cal.

                if (weekendinmillis < Calendar.getInstance().getTimeInMillis()) {
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                    weekendinmillis = cal.getTimeInMillis();
                }

                weekstartinmillis = AardvarkPedAnalysis.mAardvarkPedAnalyzeStepsByMinute.FirstDay();

                // the divide and mod will be preformed in the same operation by the VM, so there is no extra cycles for 
                // the comparison. The result is an integer ceil fn.

                long delta_millis = (weekendinmillis-weekstartinmillis);
                num_days = (int)(delta_millis/DAY_LENGTH_IN_MILLIS);
                if (delta_millis%DAY_LENGTH_IN_MILLIS != 0) {
                   num_days++;
                }

                int[] steps_per_day = mAardvarkPedAnalyzeStepsByMinute.StepsForWeek(weekstartinmillis, num_days);
                int max_steps = -1;

                double stride_len = AardvarkPedWidgetService.StrideLength();
                double step_distance = (mShowMetric)?(stride_len/1000.0): // km
                                                     (stride_len/1609.1); // miles

                for (int i = 1; i < steps_per_day.length; i++ ) {
                    if (steps_per_day[i] > 0.0) {
                        step_series.add(i-1, steps_per_day[i]*step_distance);
                    }
                    else {
                        step_series.add(i-1, MathHelper.NULL_VALUE);
                    }
                    if (max_steps < steps_per_day[i]) {
                        max_steps = steps_per_day[i];
                    }
                }

                dataset.addSeries(step_series);

                int[] margins = new int[4];
                margins[0] = (int)(10* getResources().getDisplayMetrics().density);
                margins[1] = (int)(40* getResources().getDisplayMetrics().density);
                margins[2] = (int)(30* getResources().getDisplayMetrics().density);
                margins[3] = (int)(10* getResources().getDisplayMetrics().density);

                XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
                // mRenderer.setChartTitle((mShowMetric? "Distance per Day (Km)" : "Distance per Day (Mi)"));
                mRenderer.setXTitle("");
                if (mShowMetric) {
                    mRenderer.setYTitle(context.getText(R.string.kilometers).toString());
                }
                else {
                    mRenderer.setYTitle(context.getText(R.string.miles).toString());
                }
                mRenderer.setAxesColor(Color.WHITE);
                mRenderer.setLabelsColor(Color.WHITE);
                mRenderer.setApplyBackgroundColor(false);
                mRenderer.setMarginsColor(Color.TRANSPARENT);
                mRenderer.setZoomEnabled(false);
                mRenderer.setZoomEnabled(false, false);
                mRenderer.setPanEnabled(true, false);
                mRenderer.setPanLimits(new double[]{0.0, num_days, 0.0, 1000.0});
                mRenderer.setZoomButtonsVisible(false);
                mRenderer.setBarSpacing(1.0);
                mRenderer.setMargins(margins);
                mRenderer.setAxisTitleTextSize(11 * getResources().getDisplayMetrics().density);
                mRenderer.setChartTitleTextSize(20 * getResources().getDisplayMetrics().density);
                mRenderer.setLabelsTextSize(11 * getResources().getDisplayMetrics().density);

                mRenderer.setXLabelsAngle(90);
                int day = 0;
                String[] DAY_OF_THE_WEEK = { " ",
                                             "Sun ", "Mon ", "Tue ", "Wed ", "Thu ", "Fri ", "Sat "
                                           };

                cal.setTimeInMillis(weekstartinmillis);
                String date = (cal.get(Calendar.MONTH)+1)+"/"+cal.get(Calendar.DAY_OF_MONTH);
                for (int i = 1; i < steps_per_day.length; i++) {
                    mRenderer.addXTextLabel( day++, DAY_OF_THE_WEEK[cal.get(Calendar.DAY_OF_WEEK)]+date);
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                    date = (cal.get(Calendar.MONTH)+1)+"/"+cal.get(Calendar.DAY_OF_MONTH);
                }

                mRenderer.setBarWidth(20);
                mRenderer.setXAxisMin(day - 7.5);
                mRenderer.setXAxisMax(day - 0.5);
                mRenderer.setYAxisMin(0);
                mRenderer.setYAxisMax( (max_steps * 1.2 * step_distance));
                mRenderer.setYLabelsAlign(Paint.Align.RIGHT);
                mRenderer.setXLabelsAlign(Paint.Align.LEFT);
                mRenderer.setXLabelsColor(Color.WHITE);
                mRenderer.setYLabelsColor(0, Color.WHITE);
                mRenderer.setXLabels(0);
        
                XYSeriesRenderer renderer = new XYSeriesRenderer();
                renderer.setChartValuesFormat(new DecimalFormat("##.###"));
                renderer.setColor(Color.parseColor("#11AAff"));
                renderer.setDisplayChartValues(true);
                renderer.setChartValuesTextSize(14* getResources().getDisplayMetrics().density);
                renderer.setShowLegendItem(false);

                mRenderer.addSeriesRenderer(renderer);
        
                return ChartFactory.getBarChartView(context, dataset, mRenderer, BarChart.Type.DEFAULT);
        };

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
       public static DistancePerWeekChartFragment newInstance(AardvarkPedAnalysis activity, int sectionNumber) {
            DistancePerWeekChartFragment fragment = new DistancePerWeekChartFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            mActivity = activity;
            return fragment;

        }

        public DistancePerWeekChartFragment() {

        }


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            rootView = inflater.inflate(R.layout.fragment_aardvark_ped_analysis, container, false);

            LinearLayout layout = (LinearLayout) mActivity.findViewById(R.id.chart2);
           
            mChart = BuildChartStepsByDay(mActivity);
            return rootView;
        }

        @Override
        public void onConfigurationChanged(Configuration newConfig) {

            LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rootView = inflater.inflate(R.layout.fragment_aardvark_ped_analysis, null);

            LinearLayout layout = (LinearLayout) mActivity.findViewById(R.id.chart2);
            layout.removeAllViews();
            layout.addView(mChart);
            mChart.repaint();
        }

        @Override
        public void onPause() {
            super.onPause();
            if (mChart != null) {
                LinearLayout layout = (LinearLayout) mActivity.findViewById(R.id.chart2);
                layout.removeAllViews();
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            LinearLayout layout = (LinearLayout) mActivity.findViewById(R.id.chart2);
            
            layout.removeAllViews();
            layout.addView(mChart);
             mChart.repaint();
        }

        @Override
        public  void onViewStateRestored(Bundle savedInstanceState) {
            super.onViewStateRestored(savedInstanceState);

            int height = (int)(screen_height * 0.50);
            int width  = (int)(screen_width * 0.50);

        }
    }

    public static class CadenceChartFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private static ViewGroup rootView = null;
        private static ImageButton mHeartRateVisible = null;
        private static AardvarkPedAnalysis mActivity = null;

        private static GraphicalView mChart = null;
        private static XYMultipleSeriesDataset  mDataset  = new XYMultipleSeriesDataset();
        private static XYMultipleSeriesRenderer mRenderer = null;
        private static XYSeries mDataSeries[] = null;
        private static XYSeriesRenderer mCurrentRenderer = null;

        private static boolean mShowMetric          = false;
        private static boolean mShowHeart           = true;
        private static boolean mHasHeartData        = true;

        private GraphicalView BuildChartStepCadenceAndHeartRate(Context context) {

                AardvarkPedAnalysis.mAardvarkPedAnalyzeStepsByMinute = new AardvarkPedAnalyzeStepsByMinute(context, AardvarkPedAnalysis.mStepLogFilePath, AardvarkPedAnalysis.mStepLogFilename);

                SharedPreferences  prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
                String str_stride_length = prefs.getString(AardvarkPedWidgetService.PREFKEY_StrideLength, "");
                String str_show_metric   = prefs.getString(AardvarkPedWidgetService.PREFKEY_MeasurementSystem, "");
                mShowMetric = (str_show_metric.equals("metric"))? true : false;

                XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
                XYSeries step_series  = new XYSeries("StepsPerDay",0);
                XYSeries heart_series = new XYSeries("HeartRate", 1);
                long daystartinmillis = 0;
                Calendar cal = Calendar.getInstance();

                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.add(Calendar.HOUR, 1);
                cal.set(Calendar.MILLISECOND, 0);
                cal.add(Calendar.SECOND, -(24*3600)*7); // Previous 24 hours starting at the end of this hour
                daystartinmillis = cal.getTimeInMillis();

                ArrayList<Integer> heart_rate = new ArrayList<Integer>();
                int[] raw_steps_per_minute = mAardvarkPedAnalyzeStepsByMinute.CadenceForDay(daystartinmillis, heart_rate);
                int[] steps_per_minute = new int[raw_steps_per_minute.length-1];

                int running_avg = 0;
                for (int i = 1; i < steps_per_minute.length; i++ ) {
                    if (i <= 4) {
                        running_avg += raw_steps_per_minute[i];
                        steps_per_minute[i-1] = running_avg /(i);
                    }
                    else {
                        running_avg -= raw_steps_per_minute[i-4];
                        running_avg += raw_steps_per_minute[i];
                        steps_per_minute[i-1] = running_avg/4; // I could shift here, but I think the compiler is smart enough to do that.
                    }
                }

                int max_steps = -1;
                double stride_len = AardvarkPedWidgetService.StrideLength();
                double step_speed_per_hour = (mShowMetric)?(stride_len/1000.0)*60.0: // km per hour
                                                           (stride_len/1609.1)*60.0;   // miles per hour
                double step_speed = 0.0;
                for (int i = 1; i < steps_per_minute.length; i++ ) {
                    step_speed = ((double)steps_per_minute[i])*step_speed_per_hour;
                    step_series.add(i, step_speed );
                    if (max_steps < steps_per_minute[i]) {
                        max_steps = steps_per_minute[i];
                    }
                }
                dataset.addSeries(step_series);


                // We have to manually scale, apparently.

                double speed_max = (max_steps*step_speed_per_hour);
                int max_heart_rate = 0;
                for (int i = 1; i < heart_rate.size(); i++ ) {
                    if (heart_rate.get(i) > max_heart_rate) {
                        max_heart_rate = heart_rate.get(i);
                    }
                }

                double hrprev = 0.0;
                if (max_heart_rate > 0) {
                    for (int i = 1; i < heart_rate.size(); i++ ) {
                        double hr = /*(heart_rate.get(i)); //> 0)? */ ((double)heart_rate.get(i)) /* /180.0)*speed_max */; //: hrprev;

                         if (hr > 0.0) {
                             heart_series.add(i, hr);
                         }
                         else {
                             heart_series.add(i, MathHelper.NULL_VALUE);
                         }
                         hrprev = hr;
                    }
                    dataset.addSeries(heart_series);
                    mHasHeartData = true;
                }
                else {
                    mHasHeartData = false;
                }

                int[] margins = new int[4];
                margins[0] = (int)(10* getResources().getDisplayMetrics().density);
                margins[1] = (int)(60* getResources().getDisplayMetrics().density);
                margins[2] = (int)(30* getResources().getDisplayMetrics().density);
                margins[3] = (int)(10* getResources().getDisplayMetrics().density);

                mRenderer = new XYMultipleSeriesRenderer(2);
                // mRenderer.setChartTitle("step speed " +((mShowMetric)? "km/hr" :"mi/hr") );
                mRenderer.setXTitle("");
                mRenderer.setAxesColor(Color.WHITE);
                mRenderer.setLabelsColor(Color.WHITE);
                mRenderer.setApplyBackgroundColor(false);
                mRenderer.setMarginsColor(Color.BLACK);
                mRenderer.setZoomEnabled(false);
                mRenderer.setZoomEnabled(false, false);
                mRenderer.setPanEnabled(true, false);
                mRenderer.setPanLimits(new double[] { 0.0, steps_per_minute.length, 0.0, 1000.0});
                mRenderer.setZoomButtonsVisible(false);
                mRenderer.setBarSpacing(1.0);
                mRenderer.setMargins(margins);
                mRenderer.setAxisTitleTextSize(11* getResources().getDisplayMetrics().density);
                mRenderer.setChartTitleTextSize(20* getResources().getDisplayMetrics().density); 
                mRenderer.setLabelsTextSize(11* getResources().getDisplayMetrics().density);

                mRenderer.setXLabelsAngle(90);

                String date = (cal.get(Calendar.MONTH)+1)+"/"+cal.get(Calendar.DAY_OF_MONTH);
                for (int i = 0; i < steps_per_minute.length; i++ ) {
                    String label = null;
                    int hour = cal.get(Calendar.HOUR);
                        if (hour == 0) hour = 12;

                    String hour_label = Integer.toString(hour);
                    String am_pm      = (cal.get(Calendar.AM_PM) == 0)?"a ": "p ";
                    switch (i%60) {
                    case 0:
                        date = (cal.get(Calendar.MONTH)+1)+"/"+cal.get(Calendar.DAY_OF_MONTH);
                        label = hour_label+":00"+am_pm+date;
                        mRenderer.addXTextLabel( i, label);

                        break;
                    case 30:
                        label = hour_label+":30"+am_pm+date;
                        mRenderer.addXTextLabel( i, label);
                        cal.add(Calendar.HOUR, 1);
                        break;
                    default:
                        break;
                    }

                }

                XYSeriesRenderer renderer = new XYSeriesRenderer();
                renderer.setColor(Color.parseColor("#11AAff"));
                renderer.setDisplayChartValues(false);
                renderer.setShowLegendItem(false);
    
                mRenderer.addSeriesRenderer(renderer);
                if (max_heart_rate > 0) {
                    XYSeriesRenderer renderer2 = new XYSeriesRenderer();
                    renderer2.setColor(Color.parseColor("#ff2222"));
                    renderer2.setDisplayChartValues(false);
                    renderer2.setShowLegendItem(false);
                    renderer2.setLineWidth(2.0f);

                    mRenderer.addSeriesRenderer(renderer2);
                    mRenderer.setYAxisMin(0.0, 1);
                    mRenderer.setYAxisMax(210.0, 1);
                    mRenderer.setYLabelsAlign(Paint.Align.LEFT, 1);
                    mRenderer.setYLabelsColor(1, Color.parseColor("#ff2222"));
                    mRenderer.setYTitle("", 1);
                    double label_val = 0.0;
                    for (int i = 0; i < 7; i++) {
                        mRenderer.addYTextLabel(label_val, Integer.toString((int)label_val), 1);
                        label_val += 210.0/7.0;
                    }
                    mRenderer.addYTextLabel(210, "    bps", 1);
                }
    
                double tick_size = AardvarkPedAnalysis.CalcStepSize(speed_max, 6);
                double num_ticks = Math.ceil(speed_max/tick_size);
                double display_range = tick_size*num_ticks;

                mRenderer.setBarWidth(1);
                mRenderer.setXAxisMin(steps_per_minute.length-180);
                mRenderer.setXAxisMax(steps_per_minute.length); // 3 hours
                mRenderer.setYAxisMin(0, 0);
                mRenderer.setYAxisMax(display_range, 0);
                mRenderer.setYLabelsAlign(Paint.Align.RIGHT, 0);
    
                mRenderer.setXLabelsAlign(Paint.Align.LEFT);
                mRenderer.setXLabelsColor(Color.WHITE);
    
                mRenderer.setYLabelsColor(0, Color.parseColor("#11AAff"));
    
                mRenderer.setXLabels(0);
                mRenderer.setYLabels(0);

                double label_val = 0.0;
                for (int i = 0; i < num_ticks; i++) {
                    mRenderer.addYTextLabel(label_val, String.format("%6.3f\t\t", label_val), 0);
                    label_val += tick_size;
                }


                if (mShowMetric) {
                    //mRenderer.setYTitle(context.getText(R.string.kilometers).toString(), 0);
                    mRenderer.addYTextLabel(speed_max, "km    ", 0);
                }
                else {
                   // mRenderer.setYTitle(context.getText(R.string.miles).toString(), 0);
                   mRenderer.addYTextLabel(speed_max, "mi     ", 0);
                }
            
                if (max_heart_rate <= 0) {
                    return ChartFactory.getBarChartView(context, dataset, mRenderer, BarChart.Type.DEFAULT);
                }
                else {
                    CombinedXYChart.XYCombinedChartDef[] chart_types =
                         new CombinedXYChart.XYCombinedChartDef[]{ new CombinedXYChart.XYCombinedChartDef(BarChart.TYPE, 0),
                                                                   new CombinedXYChart.XYCombinedChartDef(LineChart.TYPE, 1) };
                    return ChartFactory.getCombinedXYChartView(context, dataset, mRenderer, (CombinedXYChart.XYCombinedChartDef[]) chart_types);
               }
        };





        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
       public static CadenceChartFragment newInstance(AardvarkPedAnalysis activity, int sectionNumber) {
            CadenceChartFragment fragment = new CadenceChartFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            mActivity = activity;
            mChart = null;
            mRenderer = null;
            return fragment;

        }

        public CadenceChartFragment() {

        }

        private static View.OnTouchListener mHideHeartRateTouchAction = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent ev) {
                 if (MotionEvent.ACTION_UP != ev.getAction()){
                    return false;
                }

                if (CadenceChartFragment.mRenderer == null) {
                    return false;
                }
                int num_renderers = CadenceChartFragment.mRenderer.getSeriesRendererCount();
                if (num_renderers < 2) {
                    return false;  // There is no heart rate info.
                }
                XYSeriesRenderer hr_renderer = (XYSeriesRenderer)CadenceChartFragment.mRenderer.getSeriesRendererAt(1);

                if (mShowHeart) {
                    mShowHeart = false;
                    hr_renderer.setColor(Color.TRANSPARENT);
                    mChart.repaint();

                }
                else {
                    mShowHeart = true;
                    hr_renderer.setColor(Color.parseColor("#ff2222"));
                    mChart.repaint();
                }
                return true;
            }
        };

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            mChart = BuildChartStepCadenceAndHeartRate(mActivity);

            rootView = (ViewGroup)inflater.inflate(R.layout.fragment_aardvark_ped_analysis, container, false);
            RelativeLayout layout = (RelativeLayout) mActivity.findViewById(R.id.chart5);
            mHeartRateVisible = (ImageButton)rootView.findViewById(R.id.heart_rate_visible);

            if (mHeartRateVisible != null && mHasHeartData) {

                // If there is no heart rate data then we aren't interested in the heart button.

                mHeartRateVisible.setVisibility(View.VISIBLE);
                mHeartRateVisible.setOnTouchListener(mHideHeartRateTouchAction);
            }
           
            return rootView;
        }

        @Override
        public void onConfigurationChanged(Configuration newConfig) {

            LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rootView = (ViewGroup)inflater.inflate(R.layout.fragment_aardvark_ped_analysis, null);

            RelativeLayout chart = (RelativeLayout) mActivity.findViewById(R.id.chart5);

            chart.removeAllViews();
            chart.addView(mChart);
            mChart.repaint();

        }

        @Override
        public void onPause() {
            super.onPause();
            if (mChart != null) {
                RelativeLayout chart = (RelativeLayout) mActivity.findViewById(R.id.chart5);
                chart.removeAllViews();
                mHeartRateVisible.setVisibility(View.GONE);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            RelativeLayout chart = (RelativeLayout) mActivity.findViewById(R.id.chart5);
            RelativeLayout layout = (RelativeLayout) mActivity.findViewById(R.id.tab5);
            
            chart.removeAllViews();
            chart.addView(mChart);
            mChart.repaint();
        }


        @Override
        public  void onViewStateRestored(Bundle savedInstanceState) {
            super.onViewStateRestored(savedInstanceState);

            int height = (int)(screen_height * 0.50);
            int width  = (int)(screen_width * 0.50);

        }
    }


    public static class SettingsFragment extends PreferenceFragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private static final String PREFKEY_StrideLength_FLOAT       = "com.aardvark_visual.ped.stride_length_in_meters_float";
        private static final String PREFKEY_ShowMetric_BOOLEAN       = "com.aardvark_visual.ped.show_metric_boolean";
        private static final String PREFKEY_UseNativeCounter_BOOLEAN = "com.aardvark_visual.ped.use_native_counter_boolean";
        private static final String PREFKEY_PurgeDays_INT            = "com.aardvark_visual.ped.purgebefore_int";
        private static final String PREFKEY_Archive_BOOLEAN          = "com.aardvark_visual.ped.archive_boolean";

        private static AardvarkPedAnalysis mActivity = null;
        private static PreferenceManager mPreferenceManager = null;
        private static SharedPreferences mSharedPreferences = null;
        private static Preference mPrefStepCounter  = null;
        private static Preference mStrideLength     = null;
        private static Preference mMeasurementSystem = null;
        private static Preference mHeartRatePref     = null;
        private static Preference mPurgeTimePref     = null;
        private static Preference mPurgeArchivePref  = null;
        private static int     mStepRetentionTime_Days = -1;
        private static boolean mArchiveSteps           = false;

        private static String  mstrStrideLength     = null;
        public  static double  mStrideLengthInMeters = 0.0;
        public  static String  mHeartRateDeviceId    = null;
        public  static boolean mShowMetric = false;   // we track everything in meters. If we
                                                      // get a stride length in inches we convert to
                                                      // meters. In cm, we convert to meters, in feet, we convert to meters.
                                                      // we show (and track in the preferences) miles for distance and whatever
                                                      // unit specified for stride

        


        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static SettingsFragment newInstance(AardvarkPedAnalysis activity, int sectionNumber) {
            SettingsFragment fragment = new SettingsFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            mActivity = activity;
            return fragment;
        }

        public SettingsFragment() {
        }

        public static void HeartRateConnectStateChanged() {
             // Null in the non-plus version
        }

             
        private void EnableHeartRateSelectionPreference(String [] device_names, String[] device_values) {
                BluetoothDevice heart_dev = AardvarkPedBluetooth.getSelectedHeartRateDevice();
                String summary = null;

                mHeartRatePref.setEnabled(true);
                if (heart_dev != null) {
                    if (AardvarkPedBluetooth.getHeartRateDeviceConnected()) {
                        summary = "selected device "+heart_dev.getName()+" connected" ;
                        mHeartRatePref.setSummary(summary);
                    }
                    else {
                        summary = "selected device "+heart_dev.getName()+" not connected" ;
                        mHeartRatePref.setSummary(summary);
                    }
                }

                ((ListPreference)mHeartRatePref).setEntries(device_names);
                ((ListPreference)mHeartRatePref).setEntryValues(device_values);
                mHeartRatePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        String val = (String) newValue;
                        String name = ((ListPreference)preference).getValue();
                    
                        SettingsFragment.mSharedPreferences.edit().putString("PREFKEY_HeartRateDevice_STRING", val).commit();
                        SettingsFragment.mSharedPreferences.edit().putString("PREFKEY_HeartRateDeviceName_STRING", name).commit();

                        AardvarkPedWidgetService.startActionSetHeartRateDevice(mActivity, val, name );

                        return true;
                    }
                });
            }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.aardvark_ped_settings_fragment, container, false);
            try {
                addPreferencesFromResource(R.xml.preferences);
            }
            catch(Exception e) {

                e.printStackTrace();
            }

            mCurrentTab = null;

            // identify whether we have a step counter.

            SensorManager sensor_manager = (SensorManager)mActivity.getSystemService(SENSOR_SERVICE);
            Sensor step_counter = sensor_manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

            mPreferenceManager = getPreferenceManager();
            mSharedPreferences = mPreferenceManager.getDefaultSharedPreferences(mActivity);

            Map<String, ?> zoot = mSharedPreferences.getAll();

            String preferencesName = mPreferenceManager.getSharedPreferencesName();
            mPrefStepCounter   = findPreference("pref_aardvark_step");
            mStrideLength      = findPreference("stride_length");
            mPurgeTimePref     = findPreference("step_retention_time");
            mPurgeArchivePref  = findPreference("archive_old_steps");
            mMeasurementSystem = findPreference("metric");
                mHeartRatePref = findPreference("heart_rate_name");
            mHeartRateDeviceId = mSharedPreferences.getString("PREFKEY_HeartRateDevice_STRING", null);
            

            Preference build_pref = (Preference) findPreference("build");
            if (build_pref != null ) {
                build_pref.setSummary(BuildInfo.BUILD_ID);
            }

            AardvarkNumberPickerPreference step_retention_pref = (AardvarkNumberPickerPreference) findPreference("step_retention_time");
            if (step_retention_pref != null) {
                mStepRetentionTime_Days = (int) step_retention_pref.getValue();
            }

            if (step_counter == null) {
                mPrefStepCounter.setEnabled(false);
                SharedPreferences.Editor editor = SettingsFragment.mSharedPreferences.edit();
                editor.putBoolean("PREFKEY_UseNativeCounter_BOOLEAN", false);
                editor.commit();
                Intent intent = new Intent();
                intent.setAction(AardvarkPedAnalysis.INTENT_UseNativeCounter_Updated);
                intent.putExtra("val", false);
                mActivity.sendBroadcast(intent);
            }
            else {

                CheckBoxPreference use_local_counter = (CheckBoxPreference)mPrefStepCounter;

                use_local_counter.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        boolean val = (Boolean) newValue;
                        SharedPreferences.Editor editor = SettingsFragment.mSharedPreferences.edit();
                        editor.putBoolean("PREFKEY_UseNativeCounter_BOOLEAN", val);
                        editor.putBoolean("pref_aardvark_step", val);
                        editor.commit();

                        Intent intent = new Intent();
                        intent.setAction(AardvarkPedAnalysis.INTENT_UseNativeCounter_Updated);
                        intent.putExtra("val", val);
                        mActivity.sendBroadcast(intent);

                        return true;
                    }
                });
            }


            ListPreference metric = (ListPreference)mMeasurementSystem;
            if (metric != null) {
                metric.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        String val = (String) newValue;
                        int min_val_idx = 0;
                        int max_val_idx = 0;

                        if (val.equals("metric")) {
                            SettingsFragment.mShowMetric = true;
                        } else {
                            SettingsFragment.mShowMetric = false;
                        }
                        SharedPreferences.Editor editor = SettingsFragment.mSharedPreferences.edit();
                        editor.putBoolean(PREFKEY_ShowMetric_BOOLEAN, SettingsFragment.mShowMetric);
                        editor.commit();

                        // The stride length pref needs to be updated, also
                        AardvarkNumberPickerPreference stride_length_pref = (AardvarkNumberPickerPreference) SettingsFragment.mStrideLength;


                        if (val.equals("metric")) {
                            String[] sa = mActivity.getResources().getStringArray(R.array.stride_length_values_metric);
                            stride_length_pref.setValues(4, sa);

                            sa = mActivity.getResources().getStringArray(R.array.stride_length_value_labels_metric);
                            stride_length_pref.setDisplayedValues(sa);

                            min_val_idx = mActivity.getResources().getInteger(R.integer.stride_length_min_value_idx_metric);
                            max_val_idx = mActivity.getResources().getInteger(R.integer.stride_length_max_value_idx_metric);

                            stride_length_pref.setMinValueIdx(min_val_idx);
                            stride_length_pref.setMaxValueIdx(max_val_idx);

                        } else {
                            String[] sa = mActivity.getResources().getStringArray(R.array.stride_length_values_english);
                            stride_length_pref.setValues(4, sa);

                            sa = mActivity.getResources().getStringArray(R.array.stride_length_value_labels_english);
                            stride_length_pref.setDisplayedValues(sa);

                            min_val_idx = mActivity.getResources().getInteger(R.integer.stride_length_min_value_idx_english);
                            max_val_idx = mActivity.getResources().getInteger(R.integer.stride_length_max_value_idx_english);

                            stride_length_pref.setMinValueIdx(min_val_idx);
                            stride_length_pref.setMaxValueIdx(max_val_idx);

                        }

                        Intent intent = new Intent();
                        intent.setAction(AardvarkPedAnalysis.INTENT_ShowMetric_Updated);
                        intent.putExtra("val", SettingsFragment.mShowMetric);
                        mActivity.sendBroadcast(intent);

                        return true;
                    }
                });
            }

            AardvarkNumberPickerPreference etpref =(AardvarkNumberPickerPreference) mStrideLength;
            if (mStrideLength != null ) {
                etpref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        String stride_val = ((String) newValue).trim();
                        try {
                            SettingsFragment.mStrideLengthInMeters = Double.valueOf(stride_val);
                        }
                        catch (Exception e) {

                            // We have to get a new value. Until then, we should do what we can.

                            return false;
                        }

                        SharedPreferences.Editor editor = SettingsFragment.mSharedPreferences.edit();
                        editor.putFloat(PREFKEY_StrideLength_FLOAT, (float) SettingsFragment.mStrideLengthInMeters);
                        editor.commit();

                        Intent intent = new Intent();
                        intent.setAction(AardvarkPedAnalysis.INTENT_StrideLength_Updated);

                        intent.putExtra("val", (float) SettingsFragment.mStrideLengthInMeters);
                        mActivity.sendBroadcast(intent);


                        return true;
                    }
                });

            AardvarkNumberPickerPreference purge_time_pref =(AardvarkNumberPickerPreference) mPurgeTimePref;
            if ( mPurgeTimePref != null ) {
                purge_time_pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        String purge_days = ((String) newValue).trim();
                        try {
                            SettingsFragment.mStepRetentionTime_Days = Integer.valueOf(purge_days);
                        }
                        catch (Exception e) {

                            // We have to get a new value. Until then, we should do what we can.

                            return false;
                        }

                        SharedPreferences.Editor editor = SettingsFragment.mSharedPreferences.edit();
                        editor.putInt(PREFKEY_PurgeDays_INT, SettingsFragment.mStepRetentionTime_Days);
                        editor.commit();
   
                        AardvarkPedWidgetService.startActionSetPurgeBefore(mActivity, SettingsFragment.mStepRetentionTime_Days);

                        return true;
                    }
                });
            }


            ListPreference archive = (ListPreference)mPurgeArchivePref;
            if (archive != null) {
                archive.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        String val = (String) newValue;

                        if (val.equals("archive")) {
                            SettingsFragment.mArchiveSteps = true;
                        } else {
                            SettingsFragment.mArchiveSteps = false;
                        }

                        SharedPreferences.Editor editor = SettingsFragment.mSharedPreferences.edit();
                        editor.putBoolean(PREFKEY_Archive_BOOLEAN, SettingsFragment.mArchiveSteps);
                        editor.commit();
                         
                        AardvarkPedWidgetService.startActionSetArchive(mActivity, SettingsFragment.mArchiveSteps);

                        return true;
                    }
                });
            }


            }


            return rootView;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
        }
    }

    public static class FeatureUnavailableFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private static View rootView = null;
        private static AardvarkPedAnalysis mActivity = null;
        private static int mSectNum;
        private static Bitmap mDistanceByDayImage  = null;
        private static Bitmap mDistanceByHourImage = null;
        private static Bitmap mStepCadenceImage    = null;


        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
       public static FeatureUnavailableFragment newInstance(AardvarkPedAnalysis activity, int sectionNumber) {
            FeatureUnavailableFragment fragment = new FeatureUnavailableFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            mActivity = activity;
            mSectNum = sectionNumber;
            return fragment;

        }

        public FeatureUnavailableFragment() {

        }


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            if (mDistanceByDayImage == null) {
                mDistanceByDayImage = BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.distance_per_day_unavailable, options);
            }
            if (mDistanceByHourImage == null) {
                mDistanceByHourImage = BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.distance_per_hour_unavailable, options);
            }
            if (mStepCadenceImage   == null ) {
                mStepCadenceImage = BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.step_cadence_unavailable, options);
            }

            try {
                rootView = inflater.inflate(R.layout.fragment_aardvark_ped_analysis, container, false);
                RelativeLayout featureRootView = (RelativeLayout)inflater.inflate(R.layout.purchase_message, container, false);
                LinearLayout featureUnavailable = (LinearLayout) featureRootView.findViewById(R.id.feature_unavailable);
                ImageView pageImage = (ImageView) featureRootView.findViewById(R.id.pageMockup);
                featureRootView.removeView(featureUnavailable);
                ((RelativeLayout)rootView).removeView(featureUnavailable);

                switch(mSectNum) {
                case 3:
                    pageImage.setImageBitmap(mDistanceByHourImage);
                    break;

                case 4: 
                    pageImage.setImageBitmap(mDistanceByDayImage);
                    break;

                case 5:
                    pageImage.setImageBitmap(mStepCadenceImage);
                    break;
          
                default:
                    break;
                }

                ((RelativeLayout)rootView).addView((View)featureUnavailable);
               

            }
            catch(Exception e) {
                e.printStackTrace();
            }

            return rootView;
        }

        @Override
        public void onConfigurationChanged(Configuration newConfig) {


            LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rootView = inflater.inflate(R.layout.fragment_aardvark_ped_analysis, null);
            ImageView pageImage = (ImageView) rootView.findViewById(R.id.pageMockup);

            try {

              //   AdTargetingOptions adOptions = new AdTargetingOptions();
              //  mAmazonAdView.loadAd(adOptions);
                switch(mSectNum) {
                case 3:
                    pageImage.setImageBitmap(mDistanceByHourImage);
                    break;

                case 4: 
                    pageImage.setImageBitmap(mDistanceByDayImage);
                    break;

                case 5:
                    pageImage.setImageBitmap(mStepCadenceImage);
                    break;
          
                default:
                    break;
                }


            }
            catch(Exception e) {
                e.printStackTrace();
            }

            super.onConfigurationChanged(newConfig);

            // AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
       //     adRequestBuilder.addTestDevice("1BC335027D551F7A91CF85A5898911A3");
            //mAdView.loadAd(adRequestBuilder.build());
        }

        @Override
        public void onPause() {
            super.onPause();
        }

        @Override
        public void onResume() {
            super.onResume();
            LinearLayout layout = (LinearLayout) mActivity.findViewById(R.id.feature_unavailable);
            
                //layout.removeAllViews();
                // AdTargetingOptions adOptions = new AdTargetingOptions();
                //mAmazonAdView.loadAd(adOptions);
        }

        @Override
        public  void onViewStateRestored(Bundle savedInstanceState) {
            super.onViewStateRestored(savedInstanceState);

            int height = (int)(screen_height * 0.50);
            int width  = (int)(screen_width * 0.50);

        }
    }
}
