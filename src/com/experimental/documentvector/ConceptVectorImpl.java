package com.experimental.documentvector;

import com.experimental.utils.Common;
import com.google.common.base.Preconditions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Created by sushkov on 12/01/15.
 */
public class ConceptVectorImpl implements ConceptVector {

  private final double[] values;
  private final int dimensions;

  public static ConceptVector createRandomUnitVector(int dim) {
    ConceptVectorImpl result = new ConceptVectorImpl(dim);
    for (int i = 0; i < dim; i++) {
      result.setValue(i, Common.randInterval(0.0, 1.0));
    }
    result.normalise();
    return result;
  }

  public ConceptVectorImpl(int dim) {
    Preconditions.checkArgument(dim > 0);

    values = new double[dim];
    for (int i = 0; i < dim; i++) {
      values[i] = 0.0;
    }

    this.dimensions = dim;
  }

  @Override
  public ConceptVector getCopy() {
    ConceptVectorImpl result = new ConceptVectorImpl(dimensions);

    for (int i = 0; i < dimensions; i++) {
      result.setValue(i, values[i]);
    }

    return result;
  }

  @Override
  public int dimensions() {
    return dimensions;
  }

  @Override
  public double getValue(int dim) {
    Preconditions.checkArgument(dim < dimensions);
    return values[dim];
  }

  @Override
  public void setValue(int dim, double value) {
    Preconditions.checkArgument(dim < dimensions);
    values[dim] = value;
  }

  @Override
  public void normalise() {
    double lengthi = 1.0 / length();
    for (int i = 0; i < dimensions; i++) {
      values[i] *= lengthi;
    }
  }

  @Override
  public double length() {
    double squaredSum = 0.0;
    for (int i = 0; i < dimensions; i++) {
      squaredSum += values[i]*values[i];
    }
    return Math.sqrt(squaredSum);
  }

  @Override
  public double dotProduct(ConceptVector other) {
    Preconditions.checkArgument(dimensions == other.dimensions());
    double sum = 0.0;
    for (int i = 0; i < dimensions; i++) {
      sum += values[i] * other.getValue(i);
    }
    return sum;
  }

  @Override
  public void overwriteWith(ConceptVector other) {
    Preconditions.checkNotNull(other);
    Preconditions.checkArgument(dimensions == other.dimensions());

    for (int i = 0; i < dimensions; i++) {
      values[i] = other.getValue(i);
    }
  }

  @Override
  public void setToZero() {
    for (int i = 0; i < dimensions; i++) {
      values[i] = 0.0;
    }
  }

  @Override
  public void add(ConceptVector other) {
    Preconditions.checkNotNull(other);
    Preconditions.checkArgument(dimensions == other.dimensions());

    if (other instanceof SparseConceptVectorImpl) {
      SparseConceptVectorImpl otherSparse = (SparseConceptVectorImpl) other;
      for (int i = 0; i < otherSparse.numElements; i++) {
        values[otherSparse.entries.get(i).index] += otherSparse.entries.get(i).value;
      }
    } else {
      for (int i = 0; i < dimensions; i++) {
        values[i] += other.getValue(i);
      }
    }
  }

  @Override
  public void scale(double factor) {
    for (int i = 0; i < dimensions; i++) {
      values[i] *= factor;
    }
  }

  public void writeTo(BufferedWriter out) throws IOException {
    out.write(Integer.toString(dimensions) + "\n");
    for (int i = 0; i < dimensions; i++) {
      out.write(Double.toString(values[i]) + "\n");
    }
  }

  public static ConceptVectorImpl readFrom(BufferedReader in) throws IOException {
    int dim = Integer.parseInt(in.readLine());
    ConceptVectorImpl result = new ConceptVectorImpl(dim);
    for (int i = 0; i < dim; i++) {
      result.setValue(i, Double.parseDouble(in.readLine()));
    }
    return result;
  }
}
