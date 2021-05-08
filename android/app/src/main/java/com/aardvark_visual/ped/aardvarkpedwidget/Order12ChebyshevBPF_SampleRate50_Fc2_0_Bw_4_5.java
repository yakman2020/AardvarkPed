
/*
 *  Copyright (c) 2014-2015 Aardvark Visual Software Inc,
 *
 *  All Rights Reserved.
 *
 */

package com.aardvark_visual.filters;


public class Order12ChebyshevBPF_SampleRate50_Fc2_0_Bw_4_5 extends FilterCoefficients {

    public String name() { return "12 pole Chebyshev band pass filter. Sample rate = 50 samples per second. Bandpass 0.9-4.5 hz"; };
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
                                                -1.732687803453194950,
                                                -1.796123716712072720,
                                                -1.679086652141180020,
                                                -1.853995991566663950,
                                                -1.645441341052614530,
                                                -1.898791624054450900,
                                                -1.635043627538141480,
                                                -1.931842094704047330,
                                                -1.649772793570739580,
                                                -1.956840180418351950,
                                                -1.691394117551490160,
                                                -1.977132943395911500,
                                         };
                   return coeff;
              };

        @Override
        public final double[] a2() {
                final double[] coeff = {
                                                0.804449857088574971,
                                                0.841793329884366504,
                                                0.788229325181853957,
                                                0.884243063752063740,
                                                0.799377505161049329,
                                                0.920560341760469858,
                                                0.835158373664025011,
                                                0.948982342549589242,
                                                0.890665570845445953,
                                                0.971545171992344136,
                                                0.960950000348418265,
                                                0.990826223967455566,
                                         };
                   return coeff;
              };

        @Override
        public final double[] b0() {
                final double[] coeff = {
                                                -0.090006480010065615,
                                                -0.094184682676252116,
                                                -0.104822905432609123,
                                                -0.117591320304834754,
                                                -0.129022809000355759,
                                                -0.148582216013614543,
                                                -0.155547142919309128,
                                                -0.176746706635841244,
                                                -0.180226819088055462,
                                                -0.196592864572422282,
                                                -0.199782320155387522,
                                                -0.205993612386958708,
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
                                                0.090006480010065615,
                                                0.094184682676252116,
                                                0.104822905432609123,
                                                0.117591320304834754,
                                                0.129022809000355759,
                                                0.148582216013614543,
                                                0.155547142919309128,
                                                0.176746706635841244,
                                                0.180226819088055462,
                                                0.196592864572422282,
                                                0.199782320155387522,
                                                0.205993612386958708,
                                         };
                   return coeff;
              };

    };

   static class ChebyNthOrderCoefficients implements NthOrderCoefficients {

        @Override
        public final double[] Numerator() {
              final double [] b = { 
                                                8.03027066426e-11,
                                                0.0,
                                                -9.63632479712e-10,
                                                0.0,
                                                5.29997863841e-09,
                                                0.0,
                                                -1.76665954614e-08,
                                                0.0,
                                                3.97498397881e-08,
                                                0.0,
                                                -6.3599743661e-08,
                                                0.0,
                                                7.41997009378e-08,
                                                0.0,
                                                -6.3599743661e-08,
                                                0.0,
                                                3.97498397881e-08,
                                                0.0,
                                                -1.76665954614e-08,
                                                0.0,
                                                5.29997863841e-09,
                                                0.0,
                                                -9.63632479712e-10,
                                                0.0,
                                                8.03027066426e-11,
                                         };
                   return b;
              };


        @Override
        public final double[] Denominator() {
              final double [] a = { 
                                                1.0,
                                                -21.4481528862,
                                                221.386406504,
                                                -1463.53921964,
                                                6956.77974773,
                                                -25305.1507343,
                                                73189.9262313,
                                                -172650.649718,
                                                338045.342386,
                                                -556171.204101,
                                                775473.587476,
                                                -921475.498139,
                                                936148.282768,
                                                -813957.890808,
                                                605076.425423,
                                                -383345.857446,
                                                205834.201499,
                                                -92875.2484547,
                                                34786.3809635,
                                                -10627.6857358,
                                                2582.05173687,
                                                -480.123865899,
                                                64.2049770466,
                                                -5.50003421058,
                                                0.226796219667,
                                         };
                   return a;
              };

    };

   @Override
   public SectionCoefficients SectionCoeff() { return ChebyshevSectionCoefficients.newInstance(); };

   @Override
   public NthOrderCoefficients NthOrderCoeff() { return new ChebyNthOrderCoefficients(); };
};
