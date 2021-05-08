package com.aardvark_visual.ped.aardvarkped;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;


class StepDBHelper extends SQLiteOpenHelper {
 
    final static int DB_VERSION = 1;
    final static String DB_NAME = "aardvarkdb.db3";
    private static String DB_PATH = null;

    private Context mContext;
    private SQLiteDatabase mStepDB = null;
    private static final String TAG="aardvarkped.db";

    private final String STEP_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS StepTable (\n"+
            "    _idTimeStamp    INTEGER PRIMARY KEY,\n"+
            "    numSteps     INTEGER NOT NULL,\n"+
            "    heartRate    INTEGER NOT NULL,\n"+
            "    directionX   REAL    NOT NULL,\n"+
            "    directionY   REAL    NOT NULL,\n"+
            "    directionZ   REAL    NOT NULL,\n"+
            "    latitude     REAL    NOT NULL,\n"+
            "    longitude    REAL    NOT NULL,\n"+
            "    altitude     REAL    NOT NULL,\n"+
            "    deltaTime    INTEGER NOT NULL\n"+
            ");";

   public static final String STEP_LOG_TABLE = "StepTable";

   public static final int COLUMN_ID_TIMESTAMP_IDX = 0;
   public static final int COLUMN_NUMSTEPS_IDX     = 1;
   public static final int COLUMN_HEARTRATE_IDX    = 2;
   public static final int COLUMN_DIRECTIONX_IDX   = 3;
   public static final int COLUMN_DIRECTIONY_IDX   = 4;
   public static final int COLUMN_DIRECTIONZ_IDX   = 5;
   public static final int COLUMN_LATITUDE_IDX     = 6;
   public static final int COLUMN_LONGITUDE_IDX    = 7;
   public static final int COLUMN_ALTITUDE_IDX     = 8;
   public static final int COLUMN_DELTATIME_IDX    = 9;

   public static final String COLUMN_TIMESTAMP   = "_idTimeStamp";
   public static final String COLUMN_DELTASTEPS  = "numSteps";
   public static final String COLUMN_HEART_RATE  = "heartRate";
   public static final String COLUMN_DIRECTION_X = "directionX";
   public static final String COLUMN_DIRECTION_Y = "directionY";
   public static final String COLUMN_DIRECTION_Z = "directionZ";
   public static final String COLUMN_LATITUDE    = "latitude";
   public static final String COLUMN_LONGITUDE   = "longitude";
   public static final String COLUMN_ALTITUDE    = "altitude";
   public static final String COLUMN_DELTATIME   = "deltaTime";

   private final String TIME_INDEX_CREATE = "CREATE UNIQUE INDEX IF NOT EXISTS TimeIndex on StepTable(_idTimeStamp);";

     
    public StepDBHelper(Context context, final String path) {


        super(context, path+"/"+DB_NAME, null, DB_VERSION);
        DB_PATH=path;

        // Store the context for later use
        mContext = context;
    };

    // Called when the database is created for the first time.
    @Override
    public void onCreate(SQLiteDatabase database) {
        try {
            database.execSQL(STEP_TABLE_CREATE);
            database.execSQL(TIME_INDEX_CREATE);
            // We should import the step data if present.
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    };
 
    // Close any open database object.

    @Override
    public synchronized void close() {
       super.close();
    };

    // Return the name of the SQLite database being opened, as given to the constructor.
    @Override
    public String getDatabaseName() {
        return super.getDatabaseName();
    };

    // Create and/or open a database.
    @Override
    public SQLiteDatabase getReadableDatabase() {

        SQLiteDatabase db = null;
        try {
            db = super.getReadableDatabase();
        }
        catch(Exception e) {
            db = super.getWritableDatabase();
        }
        return db;
    };


    // Create and/or open a database that will be used for reading and writing.
    @Override
    public SQLiteDatabase getWritableDatabase() {
        SQLiteDatabase db = super.getWritableDatabase();
        try {
            db.execSQL(STEP_TABLE_CREATE);
            db.execSQL(TIME_INDEX_CREATE);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return db;
    };

    // Called when the database connection is being configured, to enable features such as write-ahead logging or foreign key support.
    @Override
    public void onConfigure(SQLiteDatabase db) {

        super.onConfigure(db);

        try {
            db.enableWriteAheadLogging();
            db.execSQL(STEP_TABLE_CREATE);
            db.execSQL(TIME_INDEX_CREATE);
        }
        catch(Exception e) {
            e.printStackTrace();
        }

    };

    // Called when the database needs to be downgraded.
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        super.onDowngrade(db, oldVersion, newVersion);
    };

    // Called when the database has been opened.
    @Override
    public void	onOpen(SQLiteDatabase db) {
        super.onOpen(db);
    };

    // Called when the database needs to be upgraded.
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
         Log.w(TAG, "upgrading aardvarkdatabase from version " + newVersion + " from " + oldVersion);
         // exportStepFile();
         db.execSQL("DROP TABLE IF EXISTS StepTable");
         onCreate(db);
         // importStepFile()
    };

    // Enables or disables the use of write-ahead logging for the database.
    @Override
    public void setWriteAheadLoggingEnabled(boolean enabled) {
        super.setWriteAheadLoggingEnabled(enabled);
    };


    public boolean importStepFile(File f) {
        return false;
    };
    
    // Export to text version of the step log
    public boolean exportStepFile(File f) {
        return false;
    };
    
}

