
/*
 *  Copyright (c) 2014-2015 Aardvark Visual Software Inc,
 *
 *  All Rights Reserved.
 *
 */

package com.aardvark_visual.filters;


public class Order8ChebyshevBPF_SampleRate50_Fc1_75_Bw5_0 extends FilterCoefficients {

    public String name() { return "8 pole Chebyshev band pass filter. Sample rate = 50 samples per second. Bandpass 0.9-4.5 hz"; };

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
                                                -1.543284096724302670,
                                                -1.774865206594103300,
                                                -1.453688653668390170,
                                                -1.887682031419573470,
                                                -1.459431559971728460,
                                                -1.943245402941082390,
                                                -1.548775829849976530,
                                                -1.979010286045805640,
                                         };
                   return coeff;
              };

        @Override
        public final double[] a2() {
                final double[] coeff = {
                                                0.623522178418829065,
                                                0.795800672954280253,
                                                0.623685168103009446,
                                                0.897862342431991145,
                                                0.723261152303718791,
                                                0.950222910479563976,
                                                0.892580122871456827,
                                                0.984904624781100191,
                                         };
                   return coeff;
              };

        @Override
        public final double[] b0() {
                final double[] coeff = {
                                                -0.137247729048205702,
                                                -0.175169126164816058,
                                                -0.153768028462195921,
                                                -0.221365729517257886,
                                                -0.195381725625624503,
                                                -0.256693161781400336,
                                                -0.244689164876151055,
                                                -0.269998719381131458,
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
                                                0.137247729048205702,
                                                0.175169126164816058,
                                                0.153768028462195921,
                                                0.221365729517257886,
                                                0.195381725625624503,
                                                0.256693161781400336,
                                                0.244689164876151055,
                                                0.269998719381131458,
                                         };
                   return coeff;
              };

    };

   static class ChebyNthOrderCoefficients implements NthOrderCoefficients {

        @Override
        public final double[] Numerator() {
              final double [] b = { 
                                                2.71152646175e-06,
                                                0.0,
                                                -2.1692211694e-05,
                                                0.0,
                                                7.59227409291e-05,
                                                0.0,
                                                -0.000151845481858,
                                                0.0,
                                                0.000189806852323,
                                                0.0,
                                                -0.000151845481858,
                                                0.0,
                                                7.59227409291e-05,
                                                0.0,
                                                -2.1692211694e-05,
                                                0.0,
                                                2.71152646175e-06,
                                         };
                   return b;
              };


        @Override
        public final double[] Denominator() {
              final double [] a = { 
                                                1.0,
                                                -13.5899830672,
                                                87.1207800979,
                                                -349.785311818,
                                                984.610885154,
                                                -2060.74758281,
                                                3317.68775174,
                                                -4191.48798541,
                                                4199.96426854,
                                                -3349.17275596,
                                                2118.45629199,
                                                -1051.76021327,
                                                401.800784058,
                                                -114.184906779,
                                                22.7649608992,
                                                -2.84486121206,
                                                0.167877852583,
                                         };
                   return a;
              };

    };

   @Override
   public SectionCoefficients SectionCoeff() { return ChebyshevSectionCoefficients.newInstance(); };

   @Override
   public NthOrderCoefficients NthOrderCoeff() { return new ChebyNthOrderCoefficients(); };
};
