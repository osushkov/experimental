package com.experimental.classifier;

import com.experimental.Constants;
import com.experimental.utils.Log;
import com.google.common.base.Preconditions;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by sushkov on 6/03/15.
 */
public class VectorNormaliser {

  private int numSamples = 0;
  private List<Double> sum = null;
  private List<Double> sumOfSquares = null;

  private List<Double> means = null;
  private List<Double> standardDeviations = null;

  public void save(String filename) throws IOException {
    Preconditions.checkState(means != null && standardDeviations != null);
    Preconditions.checkState(means.size() == standardDeviations.size());

    File aggregateDataFile = new File(Constants.AGGREGATE_DATA_PATH);
    String fullFilePath = aggregateDataFile.toPath().resolve(filename).toString();

    BufferedWriter bw = null;
    try {
      try {
        FileWriter fw = new FileWriter(fullFilePath);
        bw = new BufferedWriter(fw);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
        return;
      }

      bw.write(Integer.toString(means.size()) + "\n");
      for (int i = 0; i < means.size(); i++) {
        bw.write(Double.toString(means.get(i)) + "\n");
        bw.write(Double.toString(standardDeviations.get(i)) + "\n");
      }
    } finally {
      if (bw != null) {
        bw.close();
      }
    }
  }

  public boolean tryLoad(String filename) throws IOException {
    File aggregateDataFile = new File(Constants.AGGREGATE_DATA_PATH);
    String fullFilePath = aggregateDataFile.toPath().resolve(filename).toString();

    File dataFile = new File(fullFilePath);
    if (!dataFile.exists()) {
      return false;
    }

    means = new ArrayList<Double>();
    standardDeviations = new ArrayList<Double>();

    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(dataFile.getAbsolutePath()));

      int numEntries = Integer.parseInt(Preconditions.checkNotNull(br.readLine()));
      for (int i = 0; i < numEntries; i++) {
        double mean = Double.parseDouble(br.readLine());
        double sd = Double.parseDouble(br.readLine());

        means.add(mean);
        standardDeviations.add(sd);
      }

    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return false;
    } finally {
      if (br != null) {
        br.close();
      }
    }

    return true;
  }

  public void addVector(List<Double> vector) {
    if (sum == null || sumOfSquares == null) {
      initialiseSums(vector.size());
    }

    for (int i = 0; i < vector.size(); i++) {
      if (!Double.isFinite(vector.get(i)) || Double.isNaN(vector.get(i))) {
        Log.out("invalid vector, skipping");
        return;
      }
    }

    for (int i = 0; i < vector.size(); i++) {
      double v = vector.get(i);
      sum.set(i, sum.get(i) + v);
      sumOfSquares.set(i, sumOfSquares.get(i) + v*v);
    }

    numSamples++;
  }

  public void process() {
    means = new ArrayList<Double>();
    standardDeviations = new ArrayList<Double>();

    Log.out("num samples: " + numSamples);

    for (int i = 0; i < sum.size(); i++) {
      double expectedSum = sum.get(i) / numSamples;
      double expectedSumOfSquares = sumOfSquares.get(i) / numSamples;

      if (expectedSumOfSquares - expectedSum*expectedSum < 0.0) {
        Log.out("blah: " + expectedSum + " " + expectedSumOfSquares);
      }

      means.add(expectedSum);
      standardDeviations.add(Math.sqrt(expectedSumOfSquares - expectedSum*expectedSum));
    }
  }

  public List<Double> getStandardised(List<Double> vector) {
    Preconditions.checkState(means.size() == vector.size());
    Preconditions.checkState(standardDeviations.size() == vector.size());

    List<Double> result = new ArrayList<Double>();
    for (int i = 0; i < vector.size(); i++) {
      result.add((vector.get(i) - means.get(i)) / standardDeviations.get(i));
    }
    return result;
  }

  private void initialiseSums(int size) {
    sum = new ArrayList<Double>();
    sumOfSquares = new ArrayList<Double>();

    for (int i = 0; i < size; i++) {
      sum.add(0.0);
      sumOfSquares.add(0.0);
    }
  }
}
