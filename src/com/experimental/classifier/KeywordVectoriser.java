package com.experimental.classifier;

import com.experimental.documentmodel.WebsiteDocument;
import com.experimental.keywords.KeywordCandidateGenerator;
import com.google.common.base.Preconditions;

import java.util.List;

/**
 * Created by sushkov on 29/01/15.
 */
public class KeywordVectoriser {


  public List<Double> vectoriseKeywordCandidate(KeywordCandidateGenerator.KeywordCandidate candidate,
                                                WebsiteDocument document) {
    Preconditions.checkNotNull(candidate);
    Preconditions.checkNotNull(document);

    if (candidate.phraseLemmas.size() == 1) {
      return vectoriseKeywordCandidateOneWord(candidate, document);
    } else if (candidate.phraseLemmas.size() == 2) {
      return vectoriseKeywordCandidateTwoWord(candidate, document);
    } else if (candidate.phraseLemmas.size() == 3) {
      return vectoriseKeywordCandidateThreeWord(candidate, document);
    } else {
      return null;
    }
  }

  private List<Double> vectoriseKeywordCandidateOneWord(KeywordCandidateGenerator.KeywordCandidate candidate,
                                                        WebsiteDocument document) {
    Preconditions.checkArgument(candidate.phraseLemmas.size() == 1);

    return null;
  }

  private List<Double> vectoriseKeywordCandidateTwoWord(KeywordCandidateGenerator.KeywordCandidate candidate,
                                                        WebsiteDocument document) {
    Preconditions.checkArgument(candidate.phraseLemmas.size() == 2);

    return null;
  }

  private List<Double> vectoriseKeywordCandidateThreeWord(KeywordCandidateGenerator.KeywordCandidate candidate,
                                                          WebsiteDocument document) {
    Preconditions.checkArgument(candidate.phraseLemmas.size() == 3);

    return null;
  }
}
