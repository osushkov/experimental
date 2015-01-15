package com.experimental.documentvector;

import com.google.common.base.Preconditions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sushkov on 12/01/15.
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

    @Override
    public String toString() {
      return Integer.toString(index) + ":" + Double.toString(value);
    }

    public void writeTo(BufferedWriter out) throws IOException {
      out.write(Integer.toString(index) + " " + Double.toString(value) + "\n");
    }

    public void readFromInPlace(BufferedReader in) throws IOException {
      String line = in.readLine();
      String[] splitLine = line.split(" ");
      this.index = Integer.parseInt(splitLine[0]);
      this.value = Double.parseDouble(splitLine[1]);
    }

    public static SparseVectorEntry readFrom(BufferedReader in) throws IOException {
      String line = Preconditions.checkNotNull(in.readLine());

      String[] splitLine = line.split(" ");
      Preconditions.checkState(splitLine.length == 2);

      return new SparseVectorEntry(Integer.parseInt(splitLine[0]), Double.parseDouble(splitLine[1]));
    }
  }

  public final List<SparseVectorEntry> entries = new ArrayList<SparseVectorEntry>();
  public int numElements;
  private final int dimensions;

  public SparseConceptVectorImpl(int dim) {
    Preconditions.checkArgument(dim > 0);
    this.dimensions = dim;
    this.numElements = 0;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < numElements; i++) {
      builder.append(entries.get(i).toString() + " , ");
    }
    return builder.toString();
  }

  @Override
  public int dimensions() {
    return dimensions;
  }

  @Override
  public double getValue(int dim) {
    for (int i = 0; i < numElements; i++) {
      if (entries.get(i).index == dim) {
        return entries.get(i).value;
      }
    }
    return 0.0;
  }

  @Override
  public void setValue(int dim, double value) {
    for (int i = 0; i < numElements; i++) {
      if (entries.get(i).index == dim) {
        if (value == 0.0) {
          entries.remove(i);
        } else {
          entries.get(i).value = value;
        }
        return;
      }
    }

    if (value == 0.0) {
      return;
    }

    SparseVectorEntry newEntry = new SparseVectorEntry(dim, value);
    if (numElements == 0 || newEntry.index > entries.get(numElements-1).index) {
      entries.add(newEntry);
      numElements++;
    } else {
      for (int i = 0; i < numElements; i++) {
        if (entries.get(i).index > newEntry.index) {
          entries.add(i, newEntry);
          numElements++;
          break;
        }
      }
    }
  }

  @Override
  public void normalise() {
    double lengthi = 1.0 / length();
    for (int i = 0; i < numElements; i++) {
      SparseVectorEntry entry = entries.get(i);
      entry.value *= lengthi;
    }
  }

  @Override
  public double length() {
    double squaredSum = 0.0;
    for (int i = 0; i < numElements; i++) {
      SparseVectorEntry entry = entries.get(i);
      squaredSum += entry.value*entry.value;
    }
    return Math.sqrt(squaredSum);
  }

  @Override
  public double dotProduct(ConceptVector other) {
    Preconditions.checkNotNull(other);

    if (other instanceof SparseConceptVectorImpl) {
      return sparseDotProduct((SparseConceptVectorImpl) other);
    } else {
      double sum = 0.0;
      for (int i = 0; i < numElements; i++) {
        SparseVectorEntry entry = entries.get(i);
        sum += other.getValue(entry.index) * entry.value;
      }
      return sum;
    }
  }

  @Override
  public void overwriteWith(ConceptVector other) {
    Preconditions.checkNotNull(other);
    Preconditions.checkArgument(dimensions == other.dimensions());

    numElements = 0;
    for (int i = 0; i < dimensions; i++) {
      double value = other.getValue(i);
      if (value > 0.0) {
        setValue(i, value);
      }
    }
  }

  @Override
  public void setToZero() {
    numElements = 0;
  }

  @Override
  public void add(ConceptVector other) {
    Preconditions.checkNotNull(other);
    Preconditions.checkArgument(dimensions == other.dimensions());

    for (int i = 0; i < dimensions; i++) {
      double curValue = getValue(i);
      double newValue = curValue + other.getValue(i);
      setValue(i, newValue);
    }
  }

  @Override
  public void scale(double factor) {
    for (int i = 0; i < numElements; i++) {
      entries.get(i).value *= factor;
    }
  }

  private double sparseDotProduct(SparseConceptVectorImpl other) {
    Preconditions.checkNotNull(other);

    double sum = 0.0;

    int thisIndex = 0;
    int otherIndex = 0;

    while (thisIndex < this.numElements && otherIndex < other.numElements) {
      if (this.entries.get(thisIndex).index == other.entries.get(otherIndex).index) {
        sum += this.entries.get(thisIndex).value * other.entries.get(otherIndex).value;

        thisIndex++;
        otherIndex++;
      } else if (this.entries.get(thisIndex).index < other.entries.get(otherIndex).index) {
        thisIndex++;
      } else {
        otherIndex++;
      }
    }

    return sum;
  }


  public void writeTo(BufferedWriter out) throws IOException {
    out.write(Integer.toString(numElements) + "\n");
    for (int i = 0; i < numElements; i++) {
      entries.get(i).writeTo(out);
    }
  }

  public SparseConceptVectorImpl readFromInPlace(BufferedReader in) throws IOException {
    String line = in.readLine();
    if (line == null) {
      return null;
    }

    numElements = 0;
    int numFileEntries = Integer.parseInt(line);
    for (int i = 0; i < numFileEntries; i++) {
      if (i < entries.size()) {
        entries.get(i).readFromInPlace(in);
      } else {
        entries.add(SparseVectorEntry.readFrom(in));
      }
      numElements++;
    }

    return this;
  }

  public static SparseConceptVectorImpl readFrom(BufferedReader in) throws IOException {
    SparseConceptVectorImpl result = new SparseConceptVectorImpl(800);

    String line = in.readLine();
    if (line == null) {
      return null;
    }

    int numEntries = Integer.parseInt(line);
    for (int i = 0; i < numEntries; i++) {
      SparseVectorEntry entry = SparseVectorEntry.readFrom(in);
      if (entry != null) {
        result.setValue(entry.index, entry.value);
      } else {
        System.out.println("error reading SparseVectorEntry is null");
      }
    }

    return result;
  }
}
