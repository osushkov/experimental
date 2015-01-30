package com.experimental.classifier;

import com.experimental.keywords.KeywordCandidateGenerator;
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
}
