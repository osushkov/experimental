package com.experimental.languagemodel;

import com.experimental.Constants;
import com.experimental.utils.Log;
import com.google.common.base.Preconditions;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by sushkov on 26/01/15.
 */
public class BasisVector {
  private static final String BASIS_VECTOR_FILENAME = "basis_vector.txt";

  public static class BasisElement {
    public final Lemma lemma;
    public final double weight;

    public BasisElement(Lemma lemma, double weight) {
      this.lemma = Preconditions.checkNotNull(lemma);
      this.weight = weight;
      Preconditions.checkArgument(weight >= 0.0);
    }

    private void writeTo(BufferedWriter out) throws IOException {
      lemma.writeTo(out);
      out.write(Double.toString(weight) + "\n");
    }

    private static BasisElement readFrom(BufferedReader in) throws IOException {
      Lemma lemma = Lemma.readFrom(in);
      double weight = Double.parseDouble(Preconditions.checkNotNull(in.readLine()));
      Preconditions.checkState(weight >= 0.0);

      return new BasisElement(lemma, weight);
    }
  }

  private final List<BasisElement> basisElements;
  private boolean isLoaded = false;

  public BasisVector() {
    this.basisElements = new ArrayList<BasisElement>();
  }

  public BasisVector(List<BasisElement> basisElements) {
    this.basisElements = Preconditions.checkNotNull(basisElements);
  }

  public List<BasisElement> getBasisElements() {
    return basisElements;
  }

  @Override
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    for (BasisElement element : basisElements) {
      buffer.append(Double.toString(element.weight) + " " + element.lemma.lemma + "\n");
    }
    return buffer.toString();
  }

  public boolean tryLoad() throws IOException {
    if (isLoaded) {
      return true;
    }

    File aggregateDataFile = new File(Constants.AGGREGATE_DATA_PATH);
    String basisVectorFilePath = aggregateDataFile.toPath().resolve(BASIS_VECTOR_FILENAME).toString();

    File basisVectorFile = new File(basisVectorFilePath);
    if (!basisVectorFile.exists()) {
      return false;
    }

    basisElements.clear();

    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(basisVectorFile.getAbsolutePath()));

      int numEntries = Integer.parseInt(Preconditions.checkNotNull(br.readLine()));
      Preconditions.checkState(numEntries > 0);

      for (int i = 0; i < numEntries; i++) {
        BasisElement newElement = BasisElement.readFrom(br);
        basisElements.add(newElement);
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return false;
    } finally {
      if (br != null) {
        br.close();
      }
    }

    isLoaded = true;
    return true;
  }

  public void save() throws IOException {
    File aggregateDataFile = new File(Constants.AGGREGATE_DATA_PATH);
    String basisVectorFilePath = aggregateDataFile.toPath().resolve(BASIS_VECTOR_FILENAME).toString();

    BufferedWriter bw = null;
    try {
      try {
        FileWriter fw = new FileWriter(basisVectorFilePath);
        bw = new BufferedWriter(fw);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
        return;
      }

      bw.write(Integer.toString(basisElements.size()) + "\n");
      for (BasisElement element : basisElements) {
        element.writeTo(bw);
      }
    } finally {
      if (bw != null) {
        bw.close();
      }
    }
  }
}
