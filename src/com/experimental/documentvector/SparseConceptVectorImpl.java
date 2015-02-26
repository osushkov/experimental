package com.experimental.documentvector;

import com.google.common.base.Preconditions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sushkov on 26/02/15.
 */
public class SparseConceptVectorImpl implements ConceptVector {
  public static class SparseVectorEntry {
    public int index;
    public double value;

    public SparseVectorEntry(int index, double value) {
      Preconditions.checkArgument(index >= 0);
      this.index = index;
      this.value = value;
    }
  }

  private final List<SparseVectorEntry> entries = new ArrayList<SparseVectorEntry>();
  private final int dim;

  public SparseConceptVectorImpl(int dim) {
    Preconditions.checkArgument(dim > 0);
    this.dim = dim;
  }

  @Override
  public int dimensions() {
    return dim;
  }

  @Override
  public double getValue(int index) {
    for (SparseVectorEntry entry : entries) {
      if (entry.index == index) {
        return entry.value;
      }
    }
    return 0.0;
  }

  @Override
  public void setValue(int index, double value) {
    for (SparseVectorEntry entry : entries) {
      if (entry.index == index) {
        entry.value = value;

        if (value < Double.MIN_VALUE && value > -Double.MIN_VALUE) {
          entries.remove(entry);
        }

        return;
      }
    }

    if (value >= Double.MIN_VALUE || value <= -Double.MIN_VALUE) {
      entries.add(new SparseVectorEntry(index, value));
    }
  }

  @Override
  public void normalise() {
    double length = length();
    for (SparseVectorEntry entry : entries) {
      entry.value /= length;
    }
  }

  @Override
  public double length() {
    double sum2 = 0.0;
    for (SparseVectorEntry entry : entries) {
      sum2 += entry.value * entry.value;
    }
    return Math.sqrt(sum2);
  }

  @Override
  public double dotProduct(ConceptVector other) {
    double result = 0.0;
    for (SparseVectorEntry entry : entries) {
      result += entry.value * other.getValue(entry.index);
    }
    return result;
  }

  @Override
  public void overwriteWith(ConceptVector other) {
    entries.clear();
    for (int i = 0; i < dim; i++) {
      this.setValue(i, other.getValue(i));
    }
  }

  @Override
  public void setToZero() {
    entries.clear();
  }

  @Override
  public void add(ConceptVector other) {
    for (int i = 0; i < dim; i++) {
      double thisValue = this.getValue(i);
      double otherValue = other.getValue(i);

      this.setValue(i, thisValue + otherValue);
    }
  }

  @Override
  public void scale(double factor) {
    for (SparseVectorEntry entry : entries) {
      entry.value *= factor;
    }
  }

  @Override
  public ConceptVector getCopy() {
    SparseConceptVectorImpl result = new SparseConceptVectorImpl(dim);
    for (SparseVectorEntry entry : entries) {
      result.setValue(entry.index, entry.value);
    }
    return result;
  }

  @Override
  public double distanceTo(ConceptVector other) {
    double sum2 = 0.0;
    for (int i = 0; i < dim; i++) {
      double thisValue = this.getValue(i);
      double otherValue = other.getValue(i);

      sum2 += (thisValue - otherValue) * (thisValue - otherValue);
    }

    return Math.sqrt(sum2);
  }

  @Override
  public void writeTo(BufferedWriter out) throws IOException {
    out.write(Integer.toString(dim) + "\n");
    for (int i = 0; i < dim; i++) {
      out.write(Double.toString(getValue(i)) + "\n");
    }
  }

  public static SparseConceptVectorImpl readFrom(BufferedReader in) throws IOException {
    int dim = Integer.parseInt(in.readLine());
    SparseConceptVectorImpl result = new SparseConceptVectorImpl(dim);
    for (int i = 0; i < dim; i++) {
      result.setValue(i, Double.parseDouble(in.readLine()));
    }
    return result;
  }

  @Override
  public boolean haveMinElements(int num) {
    return entries.size() >= num;
  }
}
