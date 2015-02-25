package com.experimental.documentclustering;

import com.experimental.documentmodel.BagOfWeightedLemmas;
import com.experimental.documentmodel.Document;
import com.experimental.documentvector.ConceptVector;
import com.experimental.documentvector.ConceptVectorImpl;
import com.experimental.utils.Common;
import com.google.common.base.Preconditions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sushkov on 23/02/15.
 */
public class DocumentCluster {
  private final int dim;
  private ConceptVector centroid;
  private BagOfWeightedLemmas aggregateLemmaBag = new BagOfWeightedLemmas();

  private List<ConceptVector> mappedVectors = new ArrayList<ConceptVector>();


  public DocumentCluster(int dim) {
    Preconditions.checkArgument(dim > 0);
    this.dim = dim;

    this.centroid = new ConceptVectorImpl(dim);
    for (int i = 0; i < dim; i++) {
      this.centroid.setValue(i, Common.randInterval(0.0, 1.0));
    }
    this.centroid.normalise();
  }

  public void writeTo(BufferedWriter out) throws IOException {
    Preconditions.checkNotNull(out);
    Preconditions.checkState(centroid != null);
    Preconditions.checkState(aggregateLemmaBag != null);

    out.write(Integer.toString(dim) + "\n");
    centroid.writeTo(out);
    aggregateLemmaBag.writeTo(out);
  }

  public static DocumentCluster readFrom(BufferedReader in) throws IOException {
    Preconditions.checkNotNull(in);

    int dim = Integer.parseInt(Preconditions.checkNotNull(in.readLine()));
    DocumentCluster result = new DocumentCluster(dim);

    result.centroid = ConceptVectorImpl.readFrom(in);
    result.aggregateLemmaBag = BagOfWeightedLemmas.readFrom(in);

    return result;
  }

  public void addDocumentToLemmaBag(Document document) {
    Preconditions.checkNotNull(document);

    BagOfWeightedLemmas documentBag = document.getBagOfLemmas();
    for (BagOfWeightedLemmas.WeightedLemmaEntry entry : documentBag.getBag().values()) {
      aggregateLemmaBag.addLemma(entry.lemma, Math.log(1.0 + entry.weight));
    }
  }

  public synchronized void addMappedVector(ConceptVector vector) {
    mappedVectors.add(Preconditions.checkNotNull(vector));
  }

  public void endOfIteration() {
    if (mappedVectors.size() > 0) {
      centroid = new ConceptVectorImpl(dim);
      for (ConceptVector mappedVector : mappedVectors) {
        centroid.add(mappedVector);
      }
      if (centroid.length() > Double.MIN_VALUE) {
        centroid.normalise();
      }
    }

    mappedVectors.clear();
  }

  public double getSimilarityWith(ConceptVector documentVector) {
    Preconditions.checkNotNull(documentVector);
    return centroid.dotProduct(documentVector);
  }

  public void setCentroid(ConceptVector vec) {
    this.centroid.overwriteWith(Preconditions.checkNotNull(vec));
  }

  public BagOfWeightedLemmas getBagOfLemmas() {
    return aggregateLemmaBag;
  }
}
