package com.experimental.documentvector;

/**
 * Created by sushkov on 12/01/15.
 */
public interface DocumentVector {
  int dimensions();
  double getValue(int dim);
  void setValue(int dim, double value);

  void normalise();
  double length();

  double dotProduct(DocumentVector other);

  void overwriteWith(DocumentVector other);
  void setToZero();

  void add(DocumentVector other);
  void scale(double factor);
}
