package com.experimental.utils;

import com.google.common.base.Preconditions;

import java.util.List;
import java.util.Random;

/**
 * Created by sushkov on 12/01/15.
 */
public class Common {
  private static final Random rnd = new Random();

  public static final double randInterval(double s, double e){
    return (double) rnd.nextDouble()*(e-s) + s;
  }

  public static double[] listOfDoubleToArray(List<Double> list) {
    Preconditions.checkNotNull(list);

    double[] result = new double[list.size()];
    for (int i = 0; i < list.size(); i++) {
      result[i] = list.get(i);
    }
    return result;
  }
}
