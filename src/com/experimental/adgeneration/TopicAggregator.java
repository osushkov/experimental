package com.experimental.adgeneration;

import com.experimental.documentmodel.BagOfWeightedLemmas;
import com.experimental.documentmodel.Document;
import com.experimental.documentmodel.Sentence;
import com.experimental.languagemodel.*;
import com.experimental.nlp.NounPhrase;
import com.experimental.nlp.NounPhraseExtractor;
import com.experimental.nlp.SimplePOSTag;
import com.experimental.utils.Log;
import com.google.common.base.Preconditions;

import java.util.*;

/**
 * Created by sushkov on 9/02/15.
 */
public class TopicAggregator {

  private final LemmaDB lemmaDb = LemmaDB.instance;
  private final NounPhraseExtractor nounPhraseExtractor;

  private final Map<NounPhrase, Double> nounWeights = new HashMap<NounPhrase, Double>();
  private final Map<Lemma, Double> verbOccurances = new HashMap<Lemma, Double>();
  private final Map<Lemma, Double> adjectiveOccurances = new HashMap<Lemma, Double>();

  private final NounAssociations nounAssociations;

  private static class WeightedString {
    final String stringEntry;
    final double weight;

    WeightedString(String stringEntry, double weight) {
      this.stringEntry = stringEntry;
      this.weight = weight;
    }
  }

  private static final Comparator<WeightedString> WEIGHT_ORDER =
      new Comparator<WeightedString>() {
        public int compare(WeightedString e1, WeightedString e2) {
          return Double.compare(e2.weight, e1.weight);
        }
      };

  public TopicAggregator(NounAssociations nounAssociations) {
    this.nounPhraseExtractor = new NounPhraseExtractor(lemmaDb);
    this.nounAssociations = Preconditions.checkNotNull(nounAssociations);
  }

  public void aggregateDocuments(List<Document> documents) {
    Preconditions.checkNotNull(documents);
    for (Document document : documents) {
      BagOfWeightedLemmas bagOfLemmas = document.getBagOfLemmas();
      for (BagOfWeightedLemmas.WeightedLemmaEntry entry : bagOfLemmas.getBag().values()) {
        double weight = Math.log(1.0 + entry.weight);
        if (entry.lemma.tag == SimplePOSTag.ADJECTIVE) {
          addLemmaTo(entry.lemma, weight, adjectiveOccurances);
        } else if (entry.lemma.tag == SimplePOSTag.VERB) {
          addLemmaTo(entry.lemma, weight, verbOccurances);
        }
      }

      Map<NounPhrase, Double> documentNounWeights = new HashMap<NounPhrase, Double>();
      for (Sentence sentence : document.getSentences()) {
        List<NounPhrase> phrases = nounPhraseExtractor.extractNounPhrases(sentence);
        for (NounPhrase phrase : phrases) {
          NounPhrase nounOnly = phrase.getNounOnlyPhrase();
          addNounPhraseTo(nounOnly, sentence.emphasis, documentNounWeights);
        }
      }

      for (Map.Entry<NounPhrase, Double> entry : documentNounWeights.entrySet()) {
        addNounPhraseTo(entry.getKey(), Math.log(1.0 + entry.getValue()), nounWeights);
      }
    }

    outputResults();
  }

  private void outputResults() {
    List<Lemma> bestVerbs = outputBestVerb();
    outputBestNounPhrase(bestVerbs);
//    outputBestAdjective();
  }

  private void outputBestNounPhrase(List<Lemma> bestVerbs) {
    Set<Lemma> verbs = new HashSet<Lemma>(bestVerbs);

    List<WeightedString> entries = new ArrayList<WeightedString>();
    for (Map.Entry<NounPhrase, Double> entry : nounWeights.entrySet()) {
      List<Lemma> phraseLemmas = entry.getKey().getPhraseLemmas();
      Lemma correlatedVerb = findMostCorrelatedVerb(verbs, phraseLemmas.get(phraseLemmas.size()-1));

      if (correlatedVerb != null) {
        entries.add(new WeightedString(correlatedVerb + " " + entry.getKey().toString(), entry.getValue()));
      }
    }
    entries.sort(WEIGHT_ORDER);

    for (int i = 0; i < Math.min(10, entries.size()); i++) {
      Log.out("noun: " + entries.get(i).stringEntry + " " +entries.get(i).weight);


    }
  }

  private List<Lemma> outputBestVerb() {
    List<WeightedString> entries = new ArrayList<WeightedString>();
    for (Map.Entry<Lemma, Double> entry : verbOccurances.entrySet()) {
      entries.add(new WeightedString(entry.getKey().lemma, entry.getValue()));
    }
    entries.sort(WEIGHT_ORDER);

    List<Lemma> result = new ArrayList<Lemma>();
    for (int i = 0; i < Math.min(10, entries.size()); i++) {
      result.add(new Lemma(entries.get(i).stringEntry, SimplePOSTag.VERB));
      Log.out("verb: " + entries.get(i).stringEntry + " " +entries.get(i).weight);
    }
    return result;
  }

//  private void outputBestAdjective() {
//    List<WeightedString> entries = new ArrayList<WeightedString>();
//    for (Map.Entry<Lemma, Double> entry : adjectiveOccurances.entrySet()) {
//      entries.add(new WeightedString(entry.getKey().lemma, entry.getValue()));
//    }
//    entries.sort(WEIGHT_ORDER);
//
//    for (int i = 0; i < Math.min(10, entries.size()); i++) {
//      Log.out("adjective: " + entries.get(i).stringEntry + " " +entries.get(i).weight);
//    }
//  }

  private void addLemmaTo(Lemma lemma, double weight, Map<Lemma, Double> occurances) {
    Double existing = occurances.get(lemma);
    if (existing != null) {
      occurances.put(lemma, existing + weight);
    } else {
      occurances.put(lemma, weight);
    }
  }

  private void addNounPhraseTo(NounPhrase phrase, double weight, Map<NounPhrase, Double> occurances) {
    Double existing = occurances.get(phrase);
    if (existing != null) {
      occurances.put(phrase, existing + weight);
    } else {
      occurances.put(phrase, weight);
    }
  }

  private Lemma findMostCorrelatedVerb(Set<Lemma> verbs, Lemma noun) {
    int mostCorrelatedWeight = 0;
    Lemma mostCorrelatedLemma = null;

    NounAssociation associations = nounAssociations.getAssociations(noun);
    if (associations == null) {
      return null;
    }

    for (NounAssociation.Association association : associations.getVerbAssociations()) {
      Lemma lemma = lemmaDb.getLemma(association.associatedLemma);
      if (verbs.contains(lemma) && association.weight > mostCorrelatedWeight && !isStopVerb(lemma)) {
        mostCorrelatedWeight = association.weight;
        mostCorrelatedLemma = lemma;
      }
    }

    return mostCorrelatedLemma;
  }

  private boolean isStopVerb(Lemma verb) {
    return verb.lemma.equals("be") || verb.lemma.equals("have") || verb.lemma.equals("do");
  }
}
