
/*
 *  Copyright (c) 2014-2015 Aardvark Visual Software Inc,
 *
 *  All Rights Reserved.
 *
 */

package com.aardvark_visual.ped.aardvarkped;

import java.io.*;


public final class Logger {

      enum LoggingLevel {
              NONE,
              ERROR,
              WARNING,
              INFO,
              DEBUG,
              TRACE,
              VERBOSE
          };

      public Logger(PrintStream logfile, LoggingLevel lvl) {
               mLog        = logfile;
               mDbgLoggingLevel = lvl;
          };

      public void SetLogLevel(LoggingLevel lvl) { mDbgLoggingLevel = lvl; };

      public void printf(LoggingLevel lvl, String format, Object... args) {
              if (lvl.compareTo(mDbgLoggingLevel) <= 0) {
                  mLog.printf(format, args);
              }
          };

      public void println(LoggingLevel lvl, String msg) {
              if (lvl.compareTo(mDbgLoggingLevel) <= 0) {
                  mLog.println(msg);
              }
          };

      public void print(LoggingLevel lvl, String msg) {
              if (lvl.compareTo(mDbgLoggingLevel) <= 0) {
                  mLog.print(msg);
              }
          };

      public void flush() { mLog.flush(); };

      

      private LoggingLevel mDbgLoggingLevel = LoggingLevel.VERBOSE;
      private PrintStream  mLog;
};


