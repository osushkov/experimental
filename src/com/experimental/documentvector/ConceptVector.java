package com.experimental.documentvector;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Created by sushkov on 12/01/15.
 */
public interface ConceptVector {
  int dimensions();
  double getValue(int dim);
  void setValue(int dim, double value);

  void normalise();
  double length();

  double dotProduct(ConceptVector other);

  void overwriteWith(ConceptVector other);
  void setToZero();

  void add(ConceptVector other);
  void scale(double factor);

  ConceptVector getCopy();

  double distanceTo(ConceptVector other);

  void writeTo(BufferedWriter out) throws IOException;

  int getNumElements();
}
