package com.experimental.utils;

import java.util.Random;

/**
 * Created by sushkov on 12/01/15.
 */
public class Common {
  private static final Random rnd = new Random();

  public static final double randInterval(double s, double e){
    return (double) rnd.nextDouble()*(e-s) + s;
  }
}
