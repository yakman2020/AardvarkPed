
/*
 *  Copyright (c) 2014-2015 Aardvark Visual Software Inc,
 *
 *  All Rights Reserved.
 *
 */

package com.aardvark_visual.filters;

public class InfiniteImpulseResponseFilter {


       private static final int REG_SIZE = 100;
       private int NumSections; // The number of biquad sections. e.g. PoleCount/2 for even PoleCount.
       private double[] RegX1 = new double[REG_SIZE]; // Used in the Form 1 code
       private double[] RegX2 = new double[REG_SIZE];
       private double[] RegY1 = new double[REG_SIZE];
       private double[] RegY2 = new double[REG_SIZE];

       private double[] Reg0  = new double[REG_SIZE];  // Used in the Form 2 code
       private double[] Reg1  = new double[REG_SIZE];
       private double[] Reg2  = new double[REG_SIZE];

       private double[] a2 = null; // The 2nd order IIR coefficients.
       private double[] a1 = null; 
       private double[] a0 = null;
       private double[] b2 = null;
       private double[] b1 = null;
       private double[] b0 = null;




      public void SetCoefficients(FilterCoefficients coeff) {
                NumSections = coeff.NumSections();
                a0 = coeff.SectionCoeff().a0();
                a1 = coeff.SectionCoeff().a1();
                a2 = coeff.SectionCoeff().a2();
             
                b0 = coeff.SectionCoeff().b0();
                b1 = coeff.SectionCoeff().b1();
                b2 = coeff.SectionCoeff().b2();
             
                // We don't use the rest, I think

          };

      public void FilterIIRBiquadForm1(double[] inputData, double[] outputData, int NumSigPts) {
                     double filtered_rslt;

                      // Init the shift registers.
                     for (int j = 0; j < REG_SIZE; j++) {
                         RegX1[j] = 0.0;
                         RegX2[j] = 0.0;
                         RegY1[j] = 0.0;
                         RegY2[j] = 0.0;
                     }

                     for(int j = 0; j < NumSigPts; j++) {

                         filtered_rslt = SectionCalcForm1(0, inputData[j]);
                         for(int filter_stage = 1; filter_stage < NumSections; filter_stage++) {
                              filtered_rslt = SectionCalcForm1(filter_stage, filtered_rslt);
                         }
                         outputData[j] = filtered_rslt;
                     }
                }


      // Form 1 Biquad Section Calc, called by RunIIRBiquadForm1.
      private double SectionCalcForm1(int filterStage, double inData) {
                     double filtered_rslt, CenterTap;

                     CenterTap     = inData * b0[filterStage] + 
                                     b1[filterStage] * RegX1[filterStage] +
                                     b2[filterStage] * RegX2[filterStage];

                     filtered_rslt = a0[filterStage] * CenterTap -
                                     a1[filterStage] * RegY1[filterStage] - 
                                     a2[filterStage] * RegY2[filterStage];

                     RegX2[filterStage] = RegX1[filterStage];
                     RegX1[filterStage] = inData;

                     RegY2[filterStage] = RegY1[filterStage];
                     RegY1[filterStage] = filtered_rslt;

                     return(filtered_rslt);
                }

       // Form 2 Biquad
       // This uses one set of shift registers, Reg0, Reg1, and Reg2 in the center.
       public void FilterIIRBiquadForm2(double[] inputData, double[] outputData, int NumSigPts) {
                    double filtered_rslt;

                    // Init the shift registers.
                    for (int j = 0; j < REG_SIZE; j++) {
                        Reg0[j] = 0.0;
                        Reg1[j] = 0.0;
                        Reg2[j] = 0.0;
                    }

                    for (int j = 0; j < NumSigPts; j++) {
                        filtered_rslt = SectionCalcForm2(0, inputData[j]);
                        for (int section = 1; section < NumSections; section++) {
                            filtered_rslt = SectionCalcForm2(section, filtered_rslt);
                        }
                        outputData[j] = filtered_rslt;
                   }
              }

     // Form 2 Biquad Section Calc, called by FilterIIRBiquadForm2.
     private double SectionCalcForm2(int section, double input) {
                   double rslt;

                   Reg0[section] = input - 
                                   a1[section] * Reg1[section] -
                                   a2[section] * Reg2[section];

                   rslt          = b0[section] * Reg0[section] +
                                   b1[section] * Reg1[section] +
                                   b2[section] * Reg2[section];

                   // Shift the register values
                   Reg2[section] = Reg1[section];
                   Reg1[section] = Reg0[section];

                   return rslt;
             }


         };



