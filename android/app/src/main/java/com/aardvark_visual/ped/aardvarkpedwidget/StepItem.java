
package com.aardvark_visual.ped.aardvarkped;


import java.io.IOException;
import java.util.StringTokenizer;

public class StepItem {

    public final static String TAG = "AardvarkPed.StepLog";
    public long timestamp;
    public long delta_time;
    public int  numSteps;
    public int  heartRate;
    public double direction_x;
    public double direction_y;
    public double direction_z;

    public StepItem() {
        this.timestamp   = 0;
        this.numSteps    = 0;
        this.heartRate   = 0;
        this.direction_x = 0.0;
        this.direction_y = 0.0;
        this.direction_y = 0.0;
        this.delta_time  = 0;

    }
    public StepItem(long timeStamp, int  numSteps, int  heartRate, 
                 double directionX, double directionY, double directionZ,
                 double latitude, double longitude, double altitude,
                 long deltaTime) {

          this.timestamp   = timeStamp;
          this.numSteps    = numSteps;
          this.heartRate   = heartRate;
          this.direction_x = directionX;
          this.direction_y = directionY;
          this.direction_y = directionZ;
          this.delta_time  = deltaTime;
    }

    public static final class Reader {
        static StringTokenizer tokenizer;
        static String mLine = null;
    
        /** call this method to initialize reader for InputStream */
        static void init() {
            tokenizer = new StringTokenizer("");
        }
    
        /** get next token */
        static String next(String line) throws IOException {
            mLine = line;
            return next();
        }

        static String next() {
            while ( !tokenizer.hasMoreTokens() ) {
                tokenizer = new StringTokenizer(mLine);
            }
            return tokenizer.nextToken();
        }
    
        static long nextLong() throws IOException {
            return Long.parseLong( next() );
        }
        
        static int nextInt() throws IOException {
            return Integer.parseInt( next() );
        }
        
        static double nextDouble() throws IOException {
            return Double.parseDouble( next() );
        };
    }


    StepItem(String step) {
         // Scanner scanner = new Scanner(step);
         // scanner.next();
        try {
            Reader.next(step);
            numSteps    = Reader.nextInt();   // scanner.nextInt();
            delta_time  = Reader.nextLong();  // scanner.nextLong();
            timestamp   = Reader.nextLong();  // scanner.nextLong();
            direction_x = Reader.nextDouble();  // scanner.nextDouble();
            direction_y = Reader.nextDouble();  // scanner.nextDouble();
            direction_y = Reader.nextDouble();  // scanner.nextDouble();
            heartRate   = Reader.nextInt();     // scanner.nextInt();
        }
        catch(Exception e) {
            System.err.println("cant read stepitem");
        }
    }

    public void Read(String step) {
        try {
            Reader.next(step);
            numSteps    = Reader.nextInt();   // scanner.nextInt();
            delta_time  = Reader.nextLong();  // scanner.nextLong();
            timestamp   = Reader.nextLong();  // scanner.nextLong();
            direction_x = Reader.nextDouble();  // scanner.nextDouble();
            direction_y = Reader.nextDouble();  // scanner.nextDouble();
            direction_y = Reader.nextDouble();  // scanner.nextDouble();
            heartRate   = Reader.nextInt();     // scanner.nextInt();
        }
        catch(Exception e) {
            System.err.println("cant read stepitem");
        }
    }
}

