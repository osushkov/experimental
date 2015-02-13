package com.experimental.adgeneration;

import com.experimental.documentmodel.WebsiteDocument;
import com.experimental.documentvector.DocumentVectorDB;
import com.experimental.keywords.KeywordCandidateGenerator;
import com.experimental.languagemodel.Lemma;
import com.experimental.languagemodel.NounAssociations;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Created by sushkov on 11/02/15.
 */
public class AdTextGenerator {

  public static class AdText {
    public final String title;
    public final String description;
    public final List<Lemma> keywords;

    public AdText(String title, String description, List<Lemma> keywords) {
      this.title = Preconditions.checkNotNull(title);
      this.description = Preconditions.checkNotNull(description);
      this.keywords = Lists.newArrayList(Preconditions.checkNotNull(keywords));
    }
  }

  private final NounAssociations nounAssociations;
  private final DocumentVectorDB documentVectorDB;
  private final TopicAggregator topicAggregator;

  public AdTextGenerator(NounAssociations nounAssociations, DocumentVectorDB documentVectorDB) {
    this.nounAssociations = Preconditions.checkNotNull(nounAssociations);
    this.documentVectorDB = Preconditions.checkNotNull(documentVectorDB);
    this.topicAggregator = new TopicAggregator(nounAssociations);
  }

  public AdText generateAdText(WebsiteDocument document, KeywordCandidateGenerator.KeywordCandidate keyword) {


    return null;
  }

  private String getAdTitle(WebsiteDocument document, KeywordCandidateGenerator.KeywordCandidate keyword) {
    if (document.getSitePages().size() == 0) {
      return "NO TITLE";
    }

    if (document.getSitePages().get(0).header.title.size() == 0) {
      return "NO TITLE2";
    }

    return document.getSitePages().get(0).header.title.get(0).unrolledText().trim();
  }

  private String getAdDescription(WebsiteDocument document, KeywordCandidateGenerator.KeywordCandidate keyword) {

    return null;
  }

}
