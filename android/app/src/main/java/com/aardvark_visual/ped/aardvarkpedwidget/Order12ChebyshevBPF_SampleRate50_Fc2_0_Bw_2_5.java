
/*
 *  Copyright (c) 2014-2015 Aardvark Visual Software Inc,
 *
 *  All Rights Reserved.
 *
 */

package com.aardvark_visual.filters;


public class Order12ChebyshevBPF_SampleRate50_Fc2_0_Bw_2_5 extends FilterCoefficients {

    public String name() { return "12 pole Chebyshev band pass filter. Sample rate = 50 samples per second. Bandpass 1.1-3.5 hz"; };

    public int NumSections() { return  12; }; 

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
                                                -1.801616879424820850,
                                                -1.835804589335633400,
                                                -1.773725286864399340,
                                                -1.870524513486226500,
                                                -1.757222341318475540,
                                                -1.901776398403490550,
                                                -1.755272811101820760,
                                                -1.928253798947807240,
                                                -1.769396940038134640,
                                                -1.950452519995317320,
                                                -1.799737313261864590,
                                                -1.969584960716447950,
                                         };
                   return coeff;
              };

        @Override
        public final double[] a2() {
                final double[] coeff = {
                                                0.869892862331149597,
                                                0.886789283625432523,
                                                0.864092531661191265,
                                                0.909278511286068625,
                                                0.872648740993257621,
                                                0.932486793898872346,
                                                0.895677120942225291,
                                                0.953965030959696092,
                                                0.931105932406839032,
                                                0.973321563809691703,
                                                0.975621865123646681,
                                                0.991238731121587779,
                                         };
                   return coeff;
              };

        @Override
        public final double[] b0() {
                final double[] coeff = {
                                                -0.062654350056555092,
                                                -0.063871320950692673,
                                                -0.074315763529776410,
                                                -0.078201956794527780,
                                                -0.091840093425668956,
                                                -0.098137624277551835,
                                                -0.109882493581407739,
                                                -0.117033308030738234,
                                                -0.125406633321517247,
                                                -0.131092474237694157,
                                                -0.136291329002495798,
                                                -0.138472956431933858,
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
                                                0.062654350056555092,
                                                0.063871320950692673,
                                                0.074315763529776410,
                                                0.078201956794527780,
                                                0.091840093425668956,
                                                0.098137624277551835,
                                                0.109882493581407739,
                                                0.117033308030738234,
                                                0.125406633321517247,
                                                0.131092474237694157,
                                                0.136291329002495798,
                                                0.138472956431933858,
                                         };
                   return coeff;
              };

    };

   static class ChebyNthOrderCoefficients implements NthOrderCoefficients {

        @Override
        public final double[] Numerator() {
              final double [] b = { 
                                                8.36359504105e-13,
                                                0.0,
                                                -1.00363140493e-11,
                                                0.0,
                                                5.51997272709e-11,
                                                0.0,
                                                -1.83999090903e-10,
                                                0.0,
                                                4.13997954532e-10,
                                                0.0,
                                                -6.62396727251e-10,
                                                0.0,
                                                7.72796181793e-10,
                                                0.0,
                                                -6.62396727251e-10,
                                                0.0,
                                                4.13997954532e-10,
                                                0.0,
                                                -1.83999090903e-10,
                                                0.0,
                                                5.51997272709e-11,
                                                0.0,
                                                -1.00363140493e-11,
                                                0.0,
                                                8.36359504105e-13,
                                         };
                   return b;
              };


        @Override
        public final double[] Denominator() {
              final double [] a = { 
                                                1.0,
                                                -22.1133683529,
                                                235.147626784,
                                                -1600.17503152,
                                                7823.19314107,
                                                -29243.4127972,
                                                86843.4100307,
                                                -210153.52466,
                                                421732.421159,
                                                -710509.245368,
                                                1013511.62011,
                                                -1230958.97129,
                                                1277017.76213,
                                                -1132761.74432,
                                                858264.046115,
                                                -553685.986302,
                                                302439.907227,
                                                -138693.514702,
                                                52745.4797885,
                                                -16346.3008812,
                                                4024.71826218,
                                                -757.702746887,
                                                102.488490269,
                                                -8.87195284555,
                                                0.369338603052,
                                         };
                   return a;
              };

    };

   @Override
   public SectionCoefficients SectionCoeff() { return ChebyshevSectionCoefficients.newInstance(); };

   @Override
   public NthOrderCoefficients NthOrderCoeff() { return new ChebyNthOrderCoefficients(); };
};
