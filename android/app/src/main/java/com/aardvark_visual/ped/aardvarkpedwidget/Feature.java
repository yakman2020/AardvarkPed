
/*
 *  Copyright (c) 2014-2015 Aardvark Visual Software Inc,
 *
 *  All Rights Reserved.
 *
 */

package com.aardvark_visual.ped.aardvarkped;

public class Feature {
    public enum FeatureType {
            Invalid,
            PeakUpward,
            PeakDownward,
            ThresholdCrossing,
            EndofBuffer;
        };

    public Feature(FeatureType t, double time, int Index, double Value) {
            mType  = t;
            mIdx   = Index;
            mTimestamp  = time;
            mValue = Value;
        };

     public FeatureType getType() { return mType; };
     public double Timeval()              { return mTimestamp; };
     public int Index()                { return mIdx; };
     public double Value()             { return mValue; };

     // We need this to reuse features after a window buffer refill

     private FeatureType mType;
     private double mTimestamp;
     private int    mIdx;
     private double mValue;
};


