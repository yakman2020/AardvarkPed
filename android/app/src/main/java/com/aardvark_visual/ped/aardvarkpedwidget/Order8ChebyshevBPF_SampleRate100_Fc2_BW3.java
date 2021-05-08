
/*
 *  Copyright (c) 2014-2015 Aardvark Visual Software Inc,
 *
 *  All Rights Reserved.
 *
 */

package com.aardvark_visual.filters;


public class Order8ChebyshevBPF_SampleRate100_Fc2_BW3 extends FilterCoefficients {

    public String name() { return "8 pole Chebyshev band pass filter. Sample rate = 100 samples per second. Bandpass 0.9-3.5 hz"; };

    public int NumSections() { return  8; }; 

    static  class ChebyshevSectionCoefficients implements SectionCoefficients {


        public static SectionCoefficients newInstance()  {
                ChebyshevSectionCoefficients coeff = new ChebyshevSectionCoefficients();

                return (SectionCoefficients) coeff;
            };

        @Override
        public final double[] a0() {
                final double[] coeff = {
                                                1.000000000000000000,
                                                1.000000000000000000,
                                                1.000000000000000000,
                                                1.000000000000000000,
                                                1.000000000000000000,
                                                1.000000000000000000,
                                                1.000000000000000000,
                                                1.000000000000000000,
                                         };
                   return coeff;
              };

        @Override
        public final double[] a1() {
                final double[] coeff = {
                                                -1.854627823818570500,
                                                -1.909859268535327280,
                                                -1.836765734870681400,
                                                -1.951258156245664570,
                                                -1.860331146148207720,
                                                -1.975156937718123730,
                                                -1.914691151734144730,
                                                -1.991393262169409040,
                                         };
                   return coeff;
              };

        @Override
        public final double[] a2() {
                final double[] coeff = {
                                                0.867877645142258403,
                                                0.915174371349165239,
                                                0.862697252157739181,
                                                0.954013736866317075,
                                                0.899017134026447673,
                                                0.977056486048809902,
                                                0.962340630569641808,
                                                0.992996707408636992,
                                         };
                   return coeff;
              };

        @Override
        public final double[] b0() {
                final double[] coeff = {
                                                -0.055496635940868073,
                                                -0.058521035993330972,
                                                -0.063993934562740593,
                                                -0.070767691094738613,
                                                -0.077044579136281016,
                                                -0.083732448371545337,
                                                -0.088194273820110594,
                                                -0.091003768035675697,
                                         };
                   return coeff;
              };

        @Override
        public final double[] b1() {
                final double[] coeff = {
                                                0.000000000000000000,
                                                0.000000000000000000,
                                                0.000000000000000000,
                                                0.000000000000000000,
                                                0.000000000000000000,
                                                0.000000000000000000,
                                                0.000000000000000000,
                                                0.000000000000000000,
                                         };
                   return coeff;
              };

        @Override
        public final double[] b2() {
                final double[] coeff = {
                                                0.055496635940868073,
                                                0.058521035993330972,
                                                0.063993934562740593,
                                                0.070767691094738613,
                                                0.077044579136281016,
                                                0.083732448371545337,
                                                0.088194273820110594,
                                                0.091003768035675697,
                                         };
                   return coeff;
              };

    };

   static class ChebyNthOrderCoefficients implements NthOrderCoefficients {

        @Override
        public final double[] Numerator() {
              final double [] b = { 
                                                7.61531976645e-10,
                                                0.0,
                                                -6.09225581316e-09,
                                                0.0,
                                                2.13228953461e-08,
                                                0.0,
                                                -4.26457906921e-08,
                                                0.0,
                                                5.33072383652e-08,
                                                0.0,
                                                -4.26457906921e-08,
                                                0.0,
                                                2.13228953461e-08,
                                                0.0,
                                                -6.09225581316e-09,
                                                0.0,
                                                7.61531976645e-10,
                                         };
                   return b;
              };


        @Override
        public final double[] Denominator() {
              final double [] a = { 
                                                1.0,
                                                -15.2940834812,
                                                109.754623839,
                                                -490.573258069,
                                                1528.61843679,
                                                -3520.9605275,
                                                6201.45592084,
                                                -8519.77575817,
                                                9226.90117169,
                                                -7903.55140922,
                                                5336.83876021,
                                                -2810.94443283,
                                                1132.13309199,
                                                -337.068691458,
                                                69.962400509,
                                                -9.04495107253,
                                                0.548705945676,
                                         };
                   return a;
              };

    };

   @Override
   public SectionCoefficients SectionCoeff() { return ChebyshevSectionCoefficients.newInstance(); };

   @Override
   public NthOrderCoefficients NthOrderCoeff() { return new ChebyNthOrderCoefficients(); };
};
