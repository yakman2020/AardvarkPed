
/*
 *  Copyright (c) 2014-2015 Aardvark Visual Software Inc,
 *
 *  All Rights Reserved.
 *
 */

package com.aardvark_visual.ped.aardvarkped;

// One dimensional kalman filter, filtering accelerometer data . Apply x3 for filtering 3 axis info
// The theory is that the instantaneous accleration info can be filtered per axis independently with a very simple filter. 

public class KalmanState {
  public KalmanState() {
            this.q = 0.0;
            this.r = 0.0;
            this.p = 0.0;
            this.x = 0.0;
       };

  public KalmanState(double q, double r, double p, double intial_value) {
            this.q = q;
            this.r = r;
            this.p = p;
            this.x = intial_value;
       };
  
  public double Update(double measurement) {
            //prediction update
            //omit x = x

            p = p + q;

            //measurement update

            k = p / (p + r);
            x = x + k * (measurement - x);
            p = (1 - k) * p;

            return p;
       };

  public double Value() { return x; };

  private double q; //process noise covariance
  private double r; //measurement noise covariance
  private double x; //value
  private double p; //estimation error covariance
  private double k; //kalman gain
};


