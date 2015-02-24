package com.experimental.adgeneration;

import com.experimental.documentmodel.Document;
import com.experimental.documentmodel.WebsiteDocument;
import com.experimental.documentvector.DocumentVectorDB;
import com.experimental.keywords.KeywordCandidateGenerator;
import com.experimental.languagemodel.Lemma;
import com.experimental.languagemodel.LemmaDB;
import com.experimental.languagemodel.NounAssociation;
import com.experimental.languagemodel.NounAssociations;
import com.experimental.nlp.NounPhrase;
import com.experimental.nlp.SimplePOSTag;
import com.experimental.utils.Log;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

  private static class ActionVerb {
    final Lemma verb;
    final double associationWeight;

    ActionVerb(Lemma verb, double associationWeight) {
      this.verb = verb;
      this.associationWeight = associationWeight;
    }
  }

  private static final List<String> actionVerbs = Lists.newArrayList("buy", "hire", "rent");

  private final LemmaDB lemmaDB = LemmaDB.instance;
  private final NounAssociations nounAssociations;
  private final DocumentVectorDB documentVectorDB;
  private final TopicAggregator topicAggregator;

  public AdTextGenerator(NounAssociations nounAssociations, DocumentVectorDB documentVectorDB) {
    this.nounAssociations = Preconditions.checkNotNull(nounAssociations);
    this.documentVectorDB = Preconditions.checkNotNull(documentVectorDB);
    this.topicAggregator = new TopicAggregator(nounAssociations);
  }

  public AdText generateAdText(WebsiteDocument document, KeywordCandidateGenerator.KeywordCandidate keyword) {
    String title = getAdTitle(document, keyword);
    String description = getAdDescription(document, keyword);

    return new AdText(title, description, keyword.phraseLemmas);
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
    ActionVerb keywordActionVerb = getBestActionVerb(keyword.phraseLemmas);
    if (keywordActionVerb.associationWeight > 0.05) {
      Log.out("using keyword action verb");

      return keywordActionVerb.verb.lemma + " " + keyword.toString();
    }

    return getDescriptionFromTopic(document, keyword);
  }

  private String getDescriptionFromTopic(WebsiteDocument document, KeywordCandidateGenerator.KeywordCandidate keyword) {
    List<DocumentVectorDB.DocumentSimilarityPair> nearest = documentVectorDB.getNearestDocuments(document, 30);
    List<Document> nearestDocuments = new ArrayList<Document>();

    for (DocumentVectorDB.DocumentSimilarityPair entry : nearest) {
      nearestDocuments.add(entry.document);
    }

    topicAggregator.aggregateDocuments(nearestDocuments);
    Map<NounPhrase, Double> topicNounPhrases = topicAggregator.getNounPhrases();

    double bestWeight = 0.0;
    NounPhrase bestPhrase = null;

    for (Map.Entry<NounPhrase, Double> entry : topicNounPhrases.entrySet()) {
      if (entry.getValue() > bestWeight) {
        bestWeight = entry.getValue();
        bestPhrase = entry.getKey();
      }
    }

    if (bestPhrase == null) {
      return keyword.toString();
    }

    ActionVerb topicActionVerb = getBestActionVerb(bestPhrase.getPhraseLemmas());
    if (topicActionVerb.associationWeight > 0.05) {
      Log.out("using topic action verb");

      StringBuffer buffer = new StringBuffer();
      buffer.append(topicActionVerb.verb.lemma);

      for (Lemma phraseLemma : bestPhrase.getPhraseLemmas()) {
        buffer.append(" " + phraseLemma.lemma);
      }
      return buffer.toString();
    }

    StringBuffer buffer = new StringBuffer();
    for (Lemma phraseLemma : bestPhrase.getPhraseLemmas()) {
      buffer.append(" " + phraseLemma.lemma);
    }
    return buffer.toString();
  }

  private ActionVerb getBestActionVerb(List<Lemma> phrase) {
    double bestWeight = 0.0;
    Lemma bestVerb = null;

    Lemma targetPhraseLemma = phrase.get(phrase.size()-1);
    for (String verb : actionVerbs) {
      Lemma verbLemma = new Lemma(verb, SimplePOSTag.VERB);
      if (targetPhraseLemma.tag == SimplePOSTag.NOUN) {
        NounAssociation association = nounAssociations.getAssociations(targetPhraseLemma);
        ActionVerb verbAssoc = findAssociationForVerb(association, verbLemma);

        if (verbAssoc.associationWeight > bestWeight) {
          bestWeight = verbAssoc.associationWeight;
          bestVerb = verbLemma;
        }
      }
    }

    if (bestVerb != null) {
      return new ActionVerb(bestVerb, bestWeight);
    } else {
      return null;
    }
  }

  private ActionVerb findAssociationForVerb(NounAssociation nounAssociation, Lemma verb) {
    LemmaDB.LemmaId verbId = lemmaDB.addLemma(verb);
    LemmaDB.LemmaId beId = lemmaDB.addLemma(new Lemma("be", SimplePOSTag.VERB));

    int sum = 0;
    int target = 0;

    for (NounAssociation.Association association : nounAssociation.getVerbAssociations()) {
      if (!association.associatedLemma.equals(beId)) {
        if (association.associatedLemma.equals(verbId)) {
          target = association.weight;
        }

        sum += association.weight;
      }
    }

    if (sum == 0) {
      return new ActionVerb(verb, 0.0);
    } else {
      return new ActionVerb(verb, (double) target / (double) sum);
    }
  }

}
