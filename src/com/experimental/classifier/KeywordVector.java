package com.experimental.classifier;

import com.experimental.keywords.KeywordCandidateGenerator;
import com.experimental.languagemodel.Lemma;
import com.google.common.base.Preconditions;

import java.util.List;

/**
 * Created by sushkov on 30/01/15.
 */
public class KeywordVector {
  public final KeywordCandidateGenerator.KeywordCandidate keyword;
  public final List<Double> vector;

  public KeywordVector(KeywordCandidateGenerator.KeywordCandidate keyword, List<Double> vector) {
    this.keyword = Preconditions.checkNotNull(keyword);
    this.vector = Preconditions.checkNotNull(vector);
  }

  @Override
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    for (Lemma lemma : keyword.phraseLemmas) {
      buffer.append(lemma.lemma + " ");
    }
    buffer.append("\n");

    for (int i = 0; i < vector.size(); i++) {
      double val = vector.get(i);
      if (i > 0) {
        buffer.append(",");
      }
      buffer.append(Double.toString(val));
    }

    return buffer.toString();
  }
}
