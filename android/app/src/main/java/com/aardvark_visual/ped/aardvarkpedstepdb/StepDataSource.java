package com.aardvark_visual.ped.aardvarkped;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.aardvark_visual.ped.aardvarkpedwidget.AardvarkPedWidgetService;

import java.util.ArrayList;
import java.util.List;



public class StepDataSource {

     private Context mContext = null;
     public  /*private */ SQLiteDatabase mDbAppend = null;
     private SQLiteDatabase mDbRead   = null; // Same data access object for both analysis (read only) and step log (write only).
     private StepDBHelper   mDbHelper = null;



     public StepDataSource(Context context, final String dbPath) {
         if (mDbHelper == null ) {
             mDbHelper = new StepDBHelper(context, dbPath);
         }
     };

     public void openAppend() throws SQLException {
         mDbAppend = mDbHelper.getWritableDatabase();
     }

     public void openRead()  throws SQLException {
         mDbRead = mDbHelper.getReadableDatabase();
     }

     public void close() {
         mDbHelper.close();
     }
 
     public boolean newStep(int deltaSteps,
                         long deltaTime, long timeStamp,
                         double directionX, double directionY, double directionZ, double latitude, double longitude, double altitude,
                         int heartRate ) {

          // We use content values so that we can insert via a cursor. The cursor allows us to avoid creating 
          // a string value for the insert. Otherwise everybody would have to be converted to strings. C'est ne pas facil.

          ContentValues values = new ContentValues();
          values.put(StepDBHelper.COLUMN_TIMESTAMP,  timeStamp);
          values.put(StepDBHelper.COLUMN_DELTASTEPS, deltaSteps);
          values.put(StepDBHelper.COLUMN_HEART_RATE,  heartRate);
          values.put(StepDBHelper.COLUMN_DIRECTION_X, directionX);
          values.put(StepDBHelper.COLUMN_DIRECTION_Y, directionY);
          values.put(StepDBHelper.COLUMN_DIRECTION_Z, directionZ);
          values.put(StepDBHelper.COLUMN_LATITUDE,    latitude);
          values.put(StepDBHelper.COLUMN_LONGITUDE,   longitude);
          values.put(StepDBHelper.COLUMN_ALTITUDE,    altitude);
          values.put(StepDBHelper.COLUMN_DELTATIME,  deltaTime);

         try {
             mDbAppend.beginTransaction();
             long insert_id = mDbAppend.insertOrThrow(StepDBHelper.STEP_LOG_TABLE, null, values);
             mDbAppend.setTransactionSuccessful();
             mDbAppend.endTransaction();

         }
         catch(SQLiteConstraintException e){

             // We expect this.  In fact, its a feature.
           //  Log.i(AardvarkPedWidgetService.TAG, e.toString());
         }
         catch(Exception e) {

             e.printStackTrace();
         }


          return true;
     };

     public List<StepItem> getStepItems(long beginTime, long endTime ) {
          List<StepItem> step_items = new ArrayList<StepItem>();
          SQLiteDatabase db = (mDbRead != null)? mDbRead : mDbAppend; // If we are querying data, it probably is a read db, but not certainly.
                                                                      // The service will use the write database to query before doing a 
                                                                      // midnight data export/purge.
         Cursor cursor = null;
          String query_sql = "SELECT * FROM "+StepDBHelper.STEP_LOG_TABLE+" WHERE (_idTimeStamp >= "+beginTime+" ) AND (_idTimeStamp <= "+endTime+" )";
          try {
               cursor = db.rawQuery(query_sql, null);
          }
          catch(Exception e) {
              e.printStackTrace();
          }
          boolean valid = cursor.moveToFirst();
          if (valid) {
              while(!cursor.isAfterLast()) {
                   StepItem newstep =  new StepItem(
                          cursor.getLong(StepDBHelper.COLUMN_ID_TIMESTAMP_IDX),
                          cursor.getInt(StepDBHelper.COLUMN_NUMSTEPS_IDX),
                          cursor.getInt(StepDBHelper.COLUMN_HEARTRATE_IDX),
                          cursor.getDouble(StepDBHelper.COLUMN_DIRECTIONX_IDX),
                          cursor.getDouble(StepDBHelper.COLUMN_DIRECTIONY_IDX),
                          cursor.getDouble(StepDBHelper.COLUMN_DIRECTIONZ_IDX),
                          cursor.getDouble(StepDBHelper.COLUMN_LATITUDE_IDX),
                          cursor.getDouble(StepDBHelper.COLUMN_LONGITUDE_IDX),
                          cursor.getDouble(StepDBHelper.COLUMN_ALTITUDE_IDX),
                          cursor.getLong(StepDBHelper.COLUMN_DELTATIME_IDX) );
    
                   step_items.add(newstep);    
                   cursor.moveToNext();
              }
          }
          cursor.close();

          return step_items;
     };

     public int[] getStepCountPerMinute(long beginTime, long endTime ) {
          ArrayList<Integer> step_counts = new ArrayList<Integer>();
          int count = 0;
          SQLiteDatabase db = (mDbRead != null)? mDbRead : mDbAppend; // If we are querying data, it probably is a read db, but not certainly.
                                                                      // The service will use the write database to query before doing a 
                                                                      // midnight data export/purge.
         Cursor cursor = null;
          String query_sql = "SELECT SUM(numSteps) FROM "+StepDBHelper.STEP_LOG_TABLE+" WHERE (_idTimeStamp >= "+beginTime+" ) AND (_idTimeStamp <= "+endTime+")"+
                                                        "GROUP BY strftime('%Y-%j-%H-%M', _idTimeStamp/1000, 'unixepoch')";
          try {
               cursor = db.rawQuery(query_sql, null);
          }
          catch(Exception e) {
              e.printStackTrace();
          }
          boolean valid = cursor.moveToFirst();
          if (valid) {
              while(!cursor.isAfterLast()) {
                    count = cursor.getInt(0);
    
                   step_counts.add(count);
                   cursor.moveToNext();
              }
          }
          cursor.close();
          int[] ints = new int[step_counts.size()];
          int i = 0;
          for (Integer n : step_counts) {
              ints[i++] = n;
          }
          return ints;
     };

     public long getStepCount(long beginTime, long endTime ) {
          long count = 0;
          SQLiteDatabase db = (mDbRead != null)? mDbRead : mDbAppend; // If we are querying data, it probably is a read db, but not certainly.
                                                                      // The service will use the write database to query before doing a 
                                                                      // midnight data export/purge.
         Cursor cursor = null;
          String query_sql = "SELECT SUM(numSteps) FROM StepTable WHERE _idTimeStamp >=?  AND _idTimeStamp <=? ";
          try {
               cursor = db.rawQuery(query_sql, new String[] { Long.toString(beginTime), Long.toString(endTime) } );
          }
          catch(Exception e) {
              e.printStackTrace();
          }
          boolean valid = cursor.moveToFirst();
          if (valid) {
              count = cursor.getLong(0);
          }

          cursor.close();
          return count;
     };

     public long getHeartRate(long beginTime, long endTime ) {
          long count = 0;
          SQLiteDatabase db = (mDbRead != null)? mDbRead : mDbAppend; // If we are querying data, it probably is a read db, but not certainly.
                                                                      // The service will use the write database to query before doing a 
                                                                      // midnight data export/purge.
         Cursor cursor = null;
          String query_sql = "SELECT AVG(heartRate) FROM "+StepDBHelper.STEP_LOG_TABLE+" WHERE (_idTimeStamp >= "+beginTime+" ) AND (_idTimeStamp <= "+endTime+" ) AND (heartRate > 0)";
          try {
               cursor = db.rawQuery(query_sql, null);
          }
          catch(Exception e) {
              e.printStackTrace();
          }
          boolean valid = cursor.moveToFirst();
          if (valid) {
              count = cursor.getLong(0);
          }

          cursor.close();
          return count;
     };


     public long getHeartRate(long beginTime, long endTime, long[] minHr, long[] maxHr ) {
          long heart_rate = 0;
          SQLiteDatabase db = (mDbRead != null)? mDbRead : mDbAppend; // If we are querying data, it probably is a read db, but not certainly.
                                                                      // The service will use the write database to query before doing a 
                                                                      // midnight data export/purge.
          if (minHr != null) {
              minHr[0] = 0;
          }
          if (maxHr != null) {
              maxHr[0] = 0;
          }

          Cursor cursor = null;
          String query_sql = 
                           "SELECT avg(heartRate), min(heartRate), max(heartRate) FROM "+
                           " StepTable WHERE (_idTimeStamp >= ? ) "+
                                        "AND (_idTimeStamp <= ? ) "+
                                        "AND (heartRate > 40) AND (heartRate < 200)";
          try {
               cursor = db.rawQuery(query_sql, new String[] { Long.toString(beginTime), Long.toString(endTime) } );

              boolean valid = cursor.moveToFirst();
              if (valid) {
                  heart_rate = cursor.getLong(0);
                  if (minHr != null) {
                      minHr[0] = cursor.getLong(1);
                  }
                  if (maxHr != null) {
                      maxHr[0] = cursor.getLong(2);
                  }
              }
          }
          catch(Exception e) {
              e.printStackTrace();
          }

          cursor.close();
          return heart_rate;
     };


     public long getHeartRateMin(long beginTime, long endTime ) {
          long count = 0;
          SQLiteDatabase db = (mDbRead != null)? mDbRead : mDbAppend; // If we are querying data, it probably is a read db, but not certainly.
                                                                      // The service will use the write database to query before doing a 
                                                                      // midnight data export/purge.
         Cursor cursor = null;
          String query_sql = "SELECT min(heartRate) FROM "+StepDBHelper.STEP_LOG_TABLE+" WHERE (_idTimeStamp >= "+beginTime+" ) AND (_idTimeStamp <= "+endTime+" ) AND (heartRate > 40)";
          try {
               cursor = db.rawQuery(query_sql, null);
          }
          catch(Exception e) {
              e.printStackTrace();
          }
          boolean valid = cursor.moveToFirst();
          if (valid) {
              count = cursor.getLong(0);
          }

          cursor.close();
          return count;
     };


   public long getFirst() {
          long timestamp = 0;
          SQLiteDatabase db = (mDbRead != null)? mDbRead : mDbAppend; // If we are querying data, it probably is a read db, but not certainly.
                                                                      // The service will use the write database to query before doing a 
                                                                      // midnight data export/purge.
          Cursor cursor = null;
          boolean valid = false;
          String query_sql = "SELECT _idTimeStamp FROM "+StepDBHelper.STEP_LOG_TABLE+" WHERE _idTimeStamp > 0 ORDER BY _idTimeStamp ASC LIMIT 1";
          try {
               cursor = db.rawQuery(query_sql, null);
               valid = cursor.moveToFirst();
          }
          catch(Exception e) {
              e.printStackTrace();
          }

          if (valid) {
              timestamp = cursor.getLong(0);
              cursor.close();
          }

          return timestamp;
     };

   public long getLast() {
          long timestamp = 0;
          SQLiteDatabase db = (mDbRead != null)? mDbRead : mDbAppend; // If we are querying data, it probably is a read db, but not certainly.
                                                                      // The service will use the write database to query before doing a 
                                                                      // midnight data export/purge.
          Cursor cursor = null;
          String query_sql = "SELECT _idTimeStamp FROM "+StepDBHelper.STEP_LOG_TABLE+" WHERE _idTimeStamp > 0 ORDER BY _idTimeStamp DESC LIMIT 1";
          try {
               cursor = db.rawQuery(query_sql, null);
          }
          catch(Exception e) {
              e.printStackTrace();
          }
          boolean valid = cursor.moveToFirst();
          if (valid) {
              timestamp = cursor.getLong(0);
          }
          cursor.close();
          return timestamp;
     };

     public boolean purgeBefore(long time ) {
          boolean rslt = true;

          try {
               mDbAppend.execSQL("DELETE FROM StepTable WHERE _idTimeStamp < "+time); 
          }
          catch(Exception e) {
              e.printStackTrace();
              rslt = false;
          }

          return rslt;
     };

}
