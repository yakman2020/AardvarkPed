
/*
 *  Copyright (c) 2014-2015 Aardvark Visual Software Inc,
 *
 *  All Rights Reserved.
 *
 */

package com.aardvark_visual.filters;


public class Order6ChebyshevLPF_SampleRate50_Fc_4_5 extends FilterCoefficients {

    public String name() { return "12 pole Chebyshev band pass filter. Sample rate = 50 samples per second. Bandpass 0.9-4.5 hz"; };

    public int NumSections() { return  3; }; 

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
                                         };
                   return coeff;
              };

        @Override
        public final double[] a1() {
                final double[] coeff = {
                                                -1.212247737880507440,
                                                -1.278177389716841140,
                                                -1.458443786614494140,
                                         };
                   return coeff;
              };

        @Override
        public final double[] a2() {
                final double[] coeff = {
                                                0.379239952682160986,
                                                0.510920376531256437,
                                                0.791411174442810394,
                                         };
                   return coeff;
              };

        @Override
        public final double[] b0() {
                final double[] coeff = {
                                                0.041748053700413415,
                                                0.058185746703603804,
                                                0.083241846957079035,
                                         };
                   return coeff;
              };

        @Override
        public final double[] b1() {
                final double[] coeff = {
                                                0.083496107400826830,
                                                0.116371493407207607,
                                                0.166483693914158071,
                                         };
                   return coeff;
              };

        @Override
        public final double[] b2() {
                final double[] coeff = {
                                                0.041748053700413415,
                                                0.058185746703603804,
                                                0.083241846957079035,
                                         };
                   return coeff;
              };

    };

   static class ChebyNthOrderCoefficients implements NthOrderCoefficients {

        @Override
        public final double[] Numerator() {
              final double [] b = { 
                                                0.000202206239796,
                                                0.00121323743877,
                                                0.00303309359693,
                                                0.00404412479591,
                                                0.00303309359693,
                                                0.00121323743877,
                                                0.000202206239796,
                                         };
                   return b;
              };


        @Override
        public final double[] Denominator() {
              final double [] a = { 
                                                1.0,
                                                -3.94886891421,
                                                6.86318420632,
                                                -6.63310854548,
                                                3.73477513609,
                                                -1.15638563588,
                                                0.153344952505,
                                         };
                   return a;
              };

    };


   @Override
   public SectionCoefficients SectionCoeff() { return ChebyshevSectionCoefficients.newInstance(); };

   @Override
   public NthOrderCoefficients NthOrderCoeff() { return new ChebyNthOrderCoefficients(); };
};
