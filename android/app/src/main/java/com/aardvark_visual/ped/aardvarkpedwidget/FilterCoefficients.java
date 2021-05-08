
/*
 *  Copyright (c) 2014-2015 Aardvark Visual Software Inc,
 *
 *  All Rights Reserved.
 *
 */

package com.aardvark_visual.filters;


interface SectionCoefficients {
    public double[] a0();
    public double[] a1();
    public double[] a2();

    public double[] b0();
    public double[] b1();
    public double[] b2();
};

interface NthOrderCoefficients {

    public double[] Numerator();
    public double[] Denominator();
};


public class FilterCoefficients {

    int NumSections() { return -1; };
    String name() { return null; };

   public SectionCoefficients SectionCoeff() { return null; };

   public NthOrderCoefficients NthOrderCoeff() { return null; };

};


