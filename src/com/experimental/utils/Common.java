package com.experimental.utils;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by sushkov on 12/01/15.
 */
public class Common {
  public static final Random rnd = new Random();

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

  public static double computeKLDivergence(List<Double> p1, List<Double> p2) {
    Preconditions.checkNotNull(p1);
    Preconditions.checkNotNull(p2);
    Preconditions.checkArgument(p1.size() == p2.size());

    double result = 0.0;

    for (int i = 0; i < p1.size(); i++) {
      if (p1.get(i) > Double.MIN_VALUE) {
        Preconditions.checkState(p2.get(i) > Double.MIN_VALUE);
        result += p1.get(i) * Math.log(p1.get(i) / p2.get(i));
      }
    }

    return result;
  }

  public static List<Double> combinedProbability(List<Double> p1, List<Double> p2) {
    Preconditions.checkNotNull(p1);
    Preconditions.checkNotNull(p2);
    Preconditions.checkArgument(p1.size() == p2.size());

    List<Double> result = new ArrayList<Double>();

    double sum = 0.0;
    for (int i = 0; i < p1.size(); i++) {
      double x = p1.get(i) * p2.get(i);
      sum += x;
      result.add(x);
    }

    // normalise the resulting distribution.
    for (int i = 0; i < result.size(); i++) {
      result.set(i, result.get(i) / sum);
    }

    return result;
  }

  public static List<Double> uniformProbabilityDistribution(int dim) {
    Preconditions.checkArgument(dim > 0);

    List<Double> result = new ArrayList<Double>();
    for (int i = 0; i < dim; i++) {
      result.add(1.0 / (double) dim);
    }
    return result;
  }

  public static List<Double> getClampedProbabilityDistribution(List<Double> raw) {
    List<Double> result = new ArrayList<Double>();
    double floor = 0.01 / raw.size();

    double sum = 0.0;
    for (int i = 0; i < raw.size(); i++) {
      double val = raw.get(i) + floor;
      sum += val;
      result.add(val);
    }

    for (int i = 0; i < result.size(); i++) {
      result.set(i, result.get(i) / sum);
    }

    return result;
  }
}
