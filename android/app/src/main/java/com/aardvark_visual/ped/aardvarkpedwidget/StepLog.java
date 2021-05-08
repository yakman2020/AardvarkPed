
package com.aardvark_visual.ped.aardvarkped;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.os.Environment;
import android.util.Log;

import com.aardvark_visual.ped.aardvarkpedanalysis.AardvarkPedAnalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;
import java.util.Scanner;
import java.util.StringTokenizer;


public class StepLog {

    
    public  final static String TAG = "AardvarkPed.StepLog";
 //   private ArrayList<StepItem> mSteps = new ArrayList<StepItem>();
    private long        mLogOpenTime = 0;
    private static boolean     mImportExportInProgress = false;

    // We Keep these as attributes because the relationship for writing is a continuing one. 
    // We write entries over time.
    // For reading, we just ingest the file and then close it.
    private Context     mContext       = null;
    // private File        mWriteFile     = null;
    // private FileWriter  mStepLogWriter = null;
    // private FileOutputStream fOut;
    // private OutputStreamWriter out;
    // private String      mAbsolutePath  = null;
    private String      mStepLogFileName = null;

    public com.aardvark_visual.ped.aardvarkped.StepDataSource mStepDB = null;

    public static boolean ImportExportInProgress() {
          return mImportExportInProgress;
      }

    public String getAbsolutePath() {
          return null; //mAbsolutePath;
      }

    public void flush() throws IOException {
      }

    public void close() throws IOException {
      }

    private void openStepLogFile(String steplogname){
            try {
                mStepDB.openRead();
            }
            catch(Exception e) {

                // If the database is locked, we just get the data later
               // mSteps = new ArrayList<StepItem>();
            }

            mLogOpenTime     = System.currentTimeMillis();
            mStepLogFileName = steplogname;

        }

    private void clobberStepLogFile(String steplogname){
        }

    private void appendStepLogFile(String steplogname){
        try {
           mStepDB.openAppend();
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        mStepLogFileName = steplogname;



    }

    public void Reopen() {

    }
   

    public void Close() {
    }


    public StepLog(Context context) {
        // Don't open a step log. The mSteps arraylist is empty. Generally say no.
         mContext = context;
    }

    public StepLog(Context context, String steplogpath,  String steplogname, char mode) {

         mContext = context;
         if (mStepDB == null) {
             mStepDB = new StepDataSource(context, steplogpath);
         }

         switch(mode) {
         case 'r': // For read
             openStepLogFile(steplogname);

             break;
         case 'c': // clobber (create new file without even looking
             clobberStepLogFile(steplogname);
             break;
         case 'w': // For write 
         case 'a': // append. 
             appendStepLogFile(steplogname);
             break;
         default:
             
             break;
         }
    }


    public void WriteEntry(int numSteps, 
                           long deltaTime, long timeStamp,
                           double directionX, double directionY, double directionZ, 
                           int heartRate ) {
        try {
             mStepDB.newStep(numSteps, deltaTime, timeStamp,
                    directionX, directionY, directionZ,
                    0.0, 0.0, 0.0, heartRate);

        }
        catch(Exception e) {
             Log.i(TAG, "caught exception in step log write. Try reopen");
        }
    }


    public void PurgeLogFile(long deleteBefore, boolean archive){

         
        mStepDB.openRead();
        if (archive) {
            ExportArchiveFile(0, deleteBefore, mStepLogFileName);
        }
        mStepDB.purgeBefore(deleteBefore);
    }


    // archive_file_name must be a fully qualified name.

    public boolean ImportArchiveFile(String archive_file_name) {
        File archive_file = null;
        FileReader fIn = null;
        BufferedReader archive = null;

        if (mImportExportInProgress) {
            AardvarkPedAnalysis.ShowProgress("Import or export in progress. Try later", 0, 100);
            return false;
        }

        // Brings the log into mSteps

        try {
            archive_file = new File(archive_file_name);
        }
        catch(Exception e2) {
            Log.w(TAG, "Could not open archive file: "+archive_file_name);
            return false;
        }
        try {
            fIn = new FileReader(archive_file);
        }
        catch(Exception e2) {
            Log.w(TAG, "Could not open archive file reader: "+archive_file_name);
            return false;
        }

        try {
            archive = new BufferedReader(fIn);
        }
        catch(Exception e2) {
             Log.d(TAG, "could not open archive buffered reader");
             return false;
        }

        StepItem.Reader.init();
        try {
            String line = null;
            long filelength = archive_file.length();
            long progress   = 0;
            long next_report = (long)(filelength/20);

            

            mImportExportInProgress = true;
            mStepDB.mDbAppend.beginTransaction();
            ContentValues values = new ContentValues();
            AardvarkPedAnalysis.ShowProgress("Import file", 0, 100);

            do {
                line = archive.readLine();

                if (progress >= next_report) {
                    final long p = progress;
                    final long l = filelength;
                    AardvarkPedAnalysis.ShowProgress("Import file", p, l);
                    next_report += (long)(filelength/50);
                }

                StepItem step = new StepItem();
                if (line != null) {
                    step.Read(line);

                    values.put(StepDBHelper.COLUMN_TIMESTAMP,   step.timestamp);
                    values.put(StepDBHelper.COLUMN_DELTASTEPS,  step.numSteps);
                    values.put(StepDBHelper.COLUMN_HEART_RATE,  step.heartRate);
                    values.put(StepDBHelper.COLUMN_DIRECTION_X, step.direction_x);
                    values.put(StepDBHelper.COLUMN_DIRECTION_Y, step.direction_y);
                    values.put(StepDBHelper.COLUMN_DIRECTION_Z, step.direction_z);
                    values.put(StepDBHelper.COLUMN_LATITUDE,    0.0);
                    values.put(StepDBHelper.COLUMN_LONGITUDE,   0.0);
                    values.put(StepDBHelper.COLUMN_ALTITUDE,    0.0);
                    values.put(StepDBHelper.COLUMN_DELTATIME,   step.delta_time);
            
                    long insert_id = mStepDB.mDbAppend.insert/*OrThrow*/(StepDBHelper.STEP_LOG_TABLE, null, values);
            
                    progress += line.length()+1;
                }
            } while (line != null);
            mStepDB.mDbAppend.setTransactionSuccessful();
            mStepDB.mDbAppend.endTransaction();
        } 
        catch (Exception e) {
           // Happens at end of fil
            System.err.println("eof on import "+e);
        }
        AardvarkPedAnalysis.ShowProgress("Import file", 101, 100);

        try {
            archive.close();
        }
        catch (Exception e) {
        }

        mImportExportInProgress = false;
        return true;
    };


    // Generally we will be called on a new file.  But if we are called on an existing file, we destroy the file in favour of our data

    public boolean ExportArchiveFile(long beginTime, long endTime, String archive_file_name) {
        File        f = null;
        FileWriter  archive_file = null;
        FileOutputStream    fOut = null;

        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int day    = cal.get(Calendar.DAY_OF_MONTH);
        int month  = cal.get(Calendar.MONTH)+1;
        int year   = cal.get(Calendar.YEAR);
        String date_string = String.format("%02d%02d%02d%02d%4d", hour, minute, day, month, year);

        if (mImportExportInProgress) {
            AardvarkPedAnalysis.ShowProgress("Import or export in progress. Try later", 0, 100);
            return false;
        }

        char version = 'a';
        for (version = 'a'; version < 'z'; version++) {
            try {
                f = new File(mStepLogFileName+date_string+Character.toString(version));
                if (!f.exists()){
                    break;
                }
            }
            catch(Exception e2) {
                // Expected. We want to find a filename that doesn't exist. In practice we will not be seeing multiple versions in a day
                break;
            }
        }

        try {
            f.createNewFile();
        }
        catch(Exception e2) {
            Log.d(TAG, "could not rename archive");
            return false;
        }

        try {
            fOut = new FileOutputStream(f, false);
        }
        catch (Exception e3) {
            System.err.println("could not open steplog fOut");
            return false;
        }

        Log.i(TAG, "archive file = " + archive_file_name);
        try {
            archive_file = new FileWriter(f, true);
        }
        catch(Exception e) {
            Log.d(TAG, "archive writer not opened");
            return false;
        }

        mImportExportInProgress = true;

        // Magic number is to break up the query into reasonable chunks. The query takes so long on a big database the user 
        // starts getting worried and reordering exports, which get queued up and make a mess.

        AardvarkPedAnalysis.ShowProgress("Export step archive", 0, 100);
        long goal     = 100;
        long progress = 0;
        long this_begin_time = beginTime;
        long this_end_time   = beginTime+(endTime-beginTime)/100;
        for (int i = 0; i < 100; i++ ) {
            List<StepItem> step_list = null;
            try {
                step_list = mStepDB.getStepItems(this_begin_time, this_end_time);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            for (StepItem step : step_list) {
    
                String msg = "st "+step.numSteps+" "+step.delta_time+" "+step.timestamp+" "+
                                     step.direction_x+" "+step.direction_y+" "+step.direction_z+" "+step.heartRate+"\n";
                try {
                    archive_file.append(msg);
                }
                catch(Exception e) {
                }
            }
            AardvarkPedAnalysis.ShowProgress("Export step archive", progress, goal);
            progress++;
            this_begin_time = this_end_time+1;
            this_end_time   = this_begin_time+(endTime-beginTime)/100;
        }

        AardvarkPedAnalysis.ShowProgress("Export step archive", 101, 100);

        try {
            archive_file.close();
        }
        catch (Exception e) {
        }

        mImportExportInProgress = false;
        return true;
    };

}

