
/*
 *  Copyright (c) 2014-2015 Aardvark Visual Software Inc,
 *
 *  All Rights Reserved.
 *
 */

package com.aardvark_visual.filters;


public class Order8ChebyshevBPF_SampleRate100_Fc1_5_BW5 extends FilterCoefficients {

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
                                                -1.773744358349028660,
                                                -1.903954581311151810,
                                                -1.751054662877636000,
                                                -1.954486769216167820,
                                                -1.785989200439262570,
                                                -1.978025551725772550,
                                                -1.863296179332297740,
                                                -1.992831917414028760,
                                         };
                   return coeff;
              };

        @Override
        public final double[] a2() {
                final double[] coeff = {
                                                0.792736404965893149,
                                                0.907494630882938247,
                                                0.792257710706090967,
                                                0.956150100198432384,
                                                0.849500771429284707,
                                                0.979145881941171803,
                                                0.943567603134856769,
                                                0.993767021610135082,
                                         };
                   return coeff;
              };

        @Override
        public final double[] b0() {
                final double[] coeff = {
                                                -0.074346986678450619,
                                                -0.085109616273926644,
                                                -0.085702317107622342,
                                                -0.103431343087414043,
                                                -0.105166039577446260,
                                                -0.121215775235927026,
                                                -0.123609088633223035,
                                                -0.130185304631982884,
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
                                                0.074346986678450619,
                                                0.085109616273926644,
                                                0.085702317107622342,
                                                0.103431343087414043,
                                                0.105166039577446260,
                                                0.121215775235927026,
                                                0.123609088633223035,
                                                0.130185304631982884,
                                         };
                   return coeff;
              };

    };

   static class ChebyNthOrderCoefficients implements NthOrderCoefficients {

        @Override
        public final double[] Numerator() {
              final double [] b = { 
                                                1.1506248846e-08,
                                                0.0,
                                                -9.20499907677e-08,
                                                0.0,
                                                3.22174967687e-07,
                                                0.0,
                                                -6.44349935374e-07,
                                                0.0,
                                                8.05437419217e-07,
                                                0.0,
                                                -6.44349935374e-07,
                                                0.0,
                                                3.22174967687e-07,
                                                0.0,
                                                -9.20499907677e-08,
                                                0.0,
                                                1.1506248846e-08,
                                         };
                   return b;
              };


        @Override
        public final double[] Denominator() {
              final double [] a = { 
                                                1.0,
                                                -15.0033832207,
                                                105.663865523,
                                                -463.683912006,
                                                1419.09762351,
                                                -3211.83671434,
                                                5560.99330749,
                                                -7513.51917588,
                                                8006.08563159,
                                                -6750.41615319,
                                                4488.82734505,
                                                -2329.37755926,
                                                924.748244419,
                                                -271.508576243,
                                                55.5994550872,
                                                -7.09504283194,
                                                0.425044301114,
                                         };
                   return a;
              };

    };


   @Override
   public SectionCoefficients SectionCoeff() { return ChebyshevSectionCoefficients.newInstance(); };

   @Override
   public NthOrderCoefficients NthOrderCoeff() { return new ChebyNthOrderCoefficients(); };
};
