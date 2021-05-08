
/*
 *  Copyright (c) 2014-2015 Aardvark Visual Software Inc,
 *
 *  All Rights Reserved.
 *
 */

package com.aardvark_visual.ped.aardvarkped;

public class CalibrationData {
            double mBiasX = 0.0;  // We use the known magnitude of gravity to judge the bias
            double mBiasY = 0.0;  // and noise coming from the accelerometer. We need a very, very accurate bias number, since
            double mBiasZ = 0.0;  // that accumulates through the velocity and location.

            double mNoiseX = 0.0;     // max-min noise over 1000 samples.
            double mNoiseY = 0.0;
            double mNoiseZ = 0.0;

            double mScaleX = 0.0;     // max-min noise over 1000 samples.
            double mScaleY = 0.0;
            double mScaleZ = 0.0;
         };


