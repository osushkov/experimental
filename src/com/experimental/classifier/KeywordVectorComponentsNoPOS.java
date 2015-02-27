package com.experimental.classifier;

import com.experimental.WordNet;
import com.experimental.documentmodel.BagOfWeightedLemmas;
import com.experimental.documentmodel.Sentence;
import com.experimental.documentmodel.Token;
import com.experimental.documentmodel.WebsiteDocument;
import com.experimental.languagemodel.*;
import com.experimental.nlp.SimplePOSTag;
import com.experimental.sitepage.SitePage;
import com.experimental.utils.Log;
import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by sushkov on 30/01/15.
 */
public class KeywordVectorComponentsNoPOS {
  private final String word;
  private final LemmaMorphologies lemmaMorphologies;
  private final WebsiteDocument document;
  private final LemmaQuality lemmaQuality;
  private final LemmaIDFWeights lemmaIdfWeights;
  private final LemmaOccuranceStatsAggregator localOccuranceStats;
  private final LemmaOccuranceStatsAggregator globalOccuranceStats;
  private final WordNet wordnet;

  private List<Lemma> wordLemmas = new ArrayList<Lemma>();
  private List<Double> wordPosWeights = new ArrayList<Double>();
  private List<LemmaOccuranceStatsAggregator.LemmaStats> posLocalStats =
      new ArrayList<LemmaOccuranceStatsAggregator.LemmaStats>();
  private List<LemmaOccuranceStatsAggregator.LemmaStats> posGlobalStats =
      new ArrayList<LemmaOccuranceStatsAggregator.LemmaStats>();


  public KeywordVectorComponentsNoPOS(String word,
                                      LemmaMorphologies lemmaMorphologies,
                                      WebsiteDocument document,
                                      LemmaQuality lemmaQuality,
                                      LemmaIDFWeights lemmaIdfWeights,
                                      LemmaOccuranceStatsAggregator localOccuranceStats,
                                      LemmaOccuranceStatsAggregator globalOccuranceStats,
                                      WordNet wordnet) {

    this.word = Preconditions.checkNotNull(word);
    this.lemmaMorphologies = Preconditions.checkNotNull(lemmaMorphologies);
    this.document = Preconditions.checkNotNull(document);
    this.lemmaQuality = Preconditions.checkNotNull(lemmaQuality);
    this.lemmaIdfWeights = Preconditions.checkNotNull(lemmaIdfWeights);
    this.localOccuranceStats = localOccuranceStats;
    this.globalOccuranceStats = globalOccuranceStats;
    this.wordnet = Preconditions.checkNotNull(wordnet);


    int sum = 0;
    for (SimplePOSTag tag : SimplePOSTag.values()) {
      Lemma wordLemma = new Lemma(word, tag);
      wordLemmas.add(wordLemma);
      posLocalStats.add(localOccuranceStats.getLemmaStats(wordLemma));
      posGlobalStats.add(globalOccuranceStats.getLemmaStats(wordLemma));

      sum += lemmaMorphologies.numLemmaOccurances(wordLemma);
    }

    for (Lemma wordLemma : wordLemmas) {
      if (sum == 0) {
        wordPosWeights.add(1.0 / (double) wordLemmas.size());
      } else {
        wordPosWeights.add(lemmaMorphologies.numLemmaOccurances(wordLemma) / (double) sum);
      }
    }
  }

  public double lemmaWeight() {
    double result = 0.0;
    for (int i = 0; i < wordLemmas.size(); i++) {
      Lemma wordLemma = wordLemmas.get(i);
      double weight = wordPosWeights.get(i);

      result += weight * Math.log(1.0 + getLemmaWeight(wordLemma, document));
    }
    return result;
  }

  public double lemmaTopWeights() {
    List<Double> weights = new ArrayList<Double>();

    for (Sentence sentence : document.getSentences()) {
      for (Token token : sentence.tokens) {
        Lemma lemma = Lemma.fromToken(token);

        if (wordLemmas.contains(lemma)) {
          weights.add(sentence.emphasis);
        }
      }
    }

    weights.sort(new Comparator<Double>() {
      @Override
      public int compare(Double o1, Double o2) {
        return Double.compare(o2, o1);
      }
    });

    double topSum = 0.0;
    for (int i = 0; i < Math.min(5, weights.size()); i++) {
      topSum += weights.get(i);
    }
    return topSum / (double) Math.min(5, weights.size());
  }

//  public double lemmaTopicDiscrimination() {
//    return documentVectorDB.getTermDiscriminationValue(phraseLemma, document);
//  }
//
//  public double lemmaTopicDiscriminationSd() {
//    return documentVectorDB.getTermDiscriminationValue(phraseLemma, document);
//  }

  public double lemmaWeightRatio() {
    double result = 0.0;
    for (int i = 0; i < wordLemmas.size(); i++) {
      Lemma wordLemma = wordLemmas.get(i);
      double weight = wordPosWeights.get(i);
      result += weight * getLemmaWeightRatio(wordLemma, document);
    }
    return result;
  }

  public double lemmaQuality() {
    double result = 0.0;
    for (int i = 0; i < wordLemmas.size(); i++) {
      Lemma wordLemma = wordLemmas.get(i);
      double weight = wordPosWeights.get(i);
      result += weight * lemmaQuality.getLemmaQuality(wordLemma);
    }
    return result;
  }

  public double lemmaEntropyWeight() {
    double result = 0.0;
    for (int i = 0; i < wordLemmas.size(); i++) {
      Lemma wordLemma = wordLemmas.get(i);
      double weight = wordPosWeights.get(i);
      result += weight * lemmaIdfWeights.getLemmaWeight(wordLemma);
    }
    return result;
  }

  public double lemmaDictionaryWord() {
    for (Lemma lemma : wordLemmas) {
      if (lemma.tag != SimplePOSTag.OTHER && wordnet.isLemmaInDictionary(lemma)) {
        return 1.0;
      }
    }
    return 0.0;
  }

  public double weightToGobalRatio() {
    double result = 0.0;
    for (int i = 0; i < wordLemmas.size(); i++) {
      Lemma wordLemma = wordLemmas.get(i);
      double weight = wordPosWeights.get(i);
      LemmaOccuranceStatsAggregator.LemmaStats globalStats = posGlobalStats.get(i);

      if (globalStats != null) {
        double lemmaWeight = getLemmaWeightRatio(wordLemma, document);
        double ratio = lemmaWeight / Math.max(Double.MIN_VALUE, globalStats.averageWeightPerDocument);
        result += weight * Math.log(1.0 + ratio);
      }
    }
    return result;
  }

  public double globalAverageWeightPerDocument() {
    double result = 0.0;
    for (int i = 0; i < wordLemmas.size(); i++) {
      double weight = wordPosWeights.get(i);
      LemmaOccuranceStatsAggregator.LemmaStats globalStats = posGlobalStats.get(i);
      if (globalStats != null) {
        result += weight * globalStats.averageWeightPerDocument;
      }
    }
    return result;
  }

  public double globalAverageWeightPerOccuredDocument() {
    double result = 0.0;
    for (int i = 0; i < wordLemmas.size(); i++) {
      double weight = wordPosWeights.get(i);
      LemmaOccuranceStatsAggregator.LemmaStats globalStats = posGlobalStats.get(i);
      if (globalStats != null) {
        result += weight * (globalStats.sum / (double) globalStats.totalDocsOccuredIn);
      }
    }
    return result;
  }

  public double globalFractionOfDocumentsOccured() {
    double result = 0.0;
    for (int i = 0; i < wordLemmas.size(); i++) {
      double weight = wordPosWeights.get(i);
      LemmaOccuranceStatsAggregator.LemmaStats globalStats = posGlobalStats.get(i);
      if (globalStats != null) {
        result += weight * globalStats.fractionOfDocumentOccured;
      }
    }
    return result;
  }

  public double globalWeightStandardDeviation() {
    double result = 0.0;
    for (int i = 0; i < wordLemmas.size(); i++) {
      double weight = wordPosWeights.get(i);
      LemmaOccuranceStatsAggregator.LemmaStats globalStats = posGlobalStats.get(i);
      if (globalStats != null) {
        result += weight * globalStats.weightStandardDeviation;
      }
    }
    return result;
  }

  public double weightToGlobalMeanDistance() {
    double result = 0.0;
    for (int i = 0; i < wordLemmas.size(); i++) {
      Lemma wordLemma = wordLemmas.get(i);
      double weight = wordPosWeights.get(i);
      LemmaOccuranceStatsAggregator.LemmaStats globalStats = posGlobalStats.get(i);
      if (globalStats != null) {
        double lemmaWeight = getLemmaWeightRatio(wordLemma, document);
        double dist =
            Math.max(0.0, (lemmaWeight - globalStats.averageWeightPerDocument) / globalStats.weightStandardDeviation);
        result += weight * Math.log(1.0 + dist);
      }
    }
    return result;
  }

  public double weightToLocalRatio() {
    double result = 0.0;
    for (int i = 0; i < wordLemmas.size(); i++) {
      Lemma wordLemma = wordLemmas.get(i);
      double weight = wordPosWeights.get(i);
      LemmaOccuranceStatsAggregator.LemmaStats localStats = posLocalStats.get(i);

      if (localStats != null) {
        double lemmaWeight = getLemmaWeightRatio(wordLemma, document);
        double ratio = lemmaWeight / Math.max(Double.MIN_VALUE, localStats.averageWeightPerDocument);
        result += weight * Math.log(1.0 + ratio);
      }
    }
    return result;
  }

  public double localAverageWeightPerDocument() {
    double result = 0.0;
    for (int i = 0; i < wordLemmas.size(); i++) {
      double weight = wordPosWeights.get(i);
      LemmaOccuranceStatsAggregator.LemmaStats localStats = posLocalStats.get(i);
      if (localStats != null) {
        result += weight * localStats.averageWeightPerDocument;
      }
    }
    return result;
  }

  public double localAverageWeightPerOccuredDocument() {
    double result = 0.0;
    for (int i = 0; i < wordLemmas.size(); i++) {
      double weight = wordPosWeights.get(i);
      LemmaOccuranceStatsAggregator.LemmaStats localStats = posLocalStats.get(i);
      if (localStats != null) {
        result += weight * (localStats.sum / (double) localStats.totalDocsOccuredIn);
      }
    }
    return result;
  }

  public double localFractionOfDocumentsOccured() {
    double result = 0.0;
    for (int i = 0; i < wordLemmas.size(); i++) {
      double weight = wordPosWeights.get(i);
      LemmaOccuranceStatsAggregator.LemmaStats localStats = posLocalStats.get(i);
      if (localStats != null) {
        result += weight * localStats.fractionOfDocumentOccured;
      }
    }
    return result;
  }

  public double localWeightStandardDeviation() {
    double result = 0.0;
    for (int i = 0; i < wordLemmas.size(); i++) {
      double weight = wordPosWeights.get(i);
      LemmaOccuranceStatsAggregator.LemmaStats localStats = posLocalStats.get(i);
      if (localStats != null) {
        result += weight * localStats.weightStandardDeviation;
      }
    }
    return result;
  }

  public double weightToLocalMeanDistance() {
    double result = 0.0;
    for (int i = 0; i < wordLemmas.size(); i++) {
      Lemma wordLemma = wordLemmas.get(i);
      double weight = wordPosWeights.get(i);
      LemmaOccuranceStatsAggregator.LemmaStats localStats = posLocalStats.get(i);
      if (localStats != null) {
        double lemmaWeight = getLemmaWeightRatio(wordLemma, document);
        double dist =
            Math.max(0.0, (lemmaWeight - localStats.averageWeightPerDocument) / localStats.weightStandardDeviation);
        result += weight * Math.log(1.0 + dist);
      }
    }
    return result;
  }

  public double localToGlobalAverageWeightRatio() {
    double result = 0.0;
    for (int i = 0; i < wordLemmas.size(); i++) {
      double weight = wordPosWeights.get(i);
      LemmaOccuranceStatsAggregator.LemmaStats localStats = posLocalStats.get(i);
      LemmaOccuranceStatsAggregator.LemmaStats globalStats = posGlobalStats.get(i);

      if (localStats != null && globalStats != null) {
        double ratio =
            localStats.averageWeightPerDocument / Math.max(Double.MIN_VALUE, globalStats.averageWeightPerDocument);
        result += weight * Math.log(1.0 + ratio);
      }
    }
    return result;
  }

  public double localToGlobalOccuredAverageWeightRatio() {
    double result = 0.0;
    for (int i = 0; i < wordLemmas.size(); i++) {
      double weight = wordPosWeights.get(i);
      LemmaOccuranceStatsAggregator.LemmaStats localStats = posLocalStats.get(i);
      LemmaOccuranceStatsAggregator.LemmaStats globalStats = posGlobalStats.get(i);

      if (localStats != null && globalStats != null) {
        double ratio =
            localAverageWeightPerOccuredDocument() / Math.max(Double.MIN_VALUE, globalAverageWeightPerOccuredDocument());
        result += weight * ratio;
      }
    }
    return result;
  }

  public double localToGlobalStandardDeviationRatio() {
    double result = 0.0;
    for (int i = 0; i < wordLemmas.size(); i++) {
      double weight = wordPosWeights.get(i);
      LemmaOccuranceStatsAggregator.LemmaStats localStats = posLocalStats.get(i);
      LemmaOccuranceStatsAggregator.LemmaStats globalStats = posGlobalStats.get(i);

      if (localStats != null && globalStats != null) {
        double ratio =
            localStats.weightStandardDeviation / Math.max(Double.MIN_VALUE, globalStats.weightStandardDeviation);
        result += weight * ratio;
      }
    }
    return result;
  }

  public double localToGlobalDocumentsOccuredRatio() {
    double result = 0.0;
    for (int i = 0; i < wordLemmas.size(); i++) {
      double weight = wordPosWeights.get(i);
      LemmaOccuranceStatsAggregator.LemmaStats localStats = posLocalStats.get(i);
      LemmaOccuranceStatsAggregator.LemmaStats globalStats = posGlobalStats.get(i);

      if (localStats != null && globalStats != null) {
        double ratio =
            localStats.fractionOfDocumentOccured / Math.max(Double.MIN_VALUE, globalStats.fractionOfDocumentOccured);
        result += weight * Math.log(1.0 + ratio);
      }
    }
    return result;
  }

  public double globalIdfWeight() {
    double result = 0.0;
    for (int i = 0; i < wordLemmas.size(); i++) {
      double weight = wordPosWeights.get(i);
      LemmaOccuranceStatsAggregator.LemmaStats globalStats = posGlobalStats.get(i);
      if (globalStats != null) {
        double totalDocs = globalStats.sum / globalStats.averageWeightPerDocument;
        Preconditions.checkState(totalDocs >= globalStats.totalDocsOccuredIn);
        result += weight *  Math.log(totalDocs / globalStats.totalDocsOccuredIn);
      }
    }
    return result;
  }

  public double headerTitleWeight() {
    BagOfWeightedLemmas titleBag = new BagOfWeightedLemmas();

    for (SitePage page : document.getSitePages()) {
      BagOfWeightedLemmas pageBag = new BagOfWeightedLemmas(page.header.title);
      titleBag.addBag(pageBag);
    }

    double result = 0.0;
    for (Lemma wordLemma : wordLemmas) {
      if (titleBag.getBag().containsKey(wordLemma)) {
        result += titleBag.getBag().get(wordLemma).weight;
      }
    }
    return result;
  }

  public double headerDescriptionWeight() {
    BagOfWeightedLemmas descriptionBag = new BagOfWeightedLemmas();

    for (SitePage page : document.getSitePages()) {
      BagOfWeightedLemmas pageBag = new BagOfWeightedLemmas(page.header.description);
      descriptionBag.addBag(pageBag);
    }

    double result = 0.0;
    for (Lemma wordLemma : wordLemmas) {
      if (descriptionBag.getBag().containsKey(wordLemma)) {
        result += descriptionBag.getBag().get(wordLemma).weight;
      }
    }
    return result;
  }

  public double headerKeywordWeight() {
    BagOfWeightedLemmas keywordsBag = new BagOfWeightedLemmas();

    for (SitePage page : document.getSitePages()) {
      BagOfWeightedLemmas pageBag = new BagOfWeightedLemmas(page.header.keywords);
      keywordsBag.addBag(pageBag);
    }

    double result = 0.0;
    for (Lemma wordLemma : wordLemmas) {
      if (keywordsBag.getBag().containsKey(wordLemma)) {
        result += keywordsBag.getBag().get(wordLemma).weight;
      }
    }
    return result;
  }

  public double linksWeight() {
    BagOfWeightedLemmas linksBag = new BagOfWeightedLemmas();

    for (SitePage page : document.getSitePages()) {
      for (SitePage.Link outgoing : page.outgoingLinks) {
        BagOfWeightedLemmas outgoingBag = new BagOfWeightedLemmas(outgoing.linkText);
        linksBag.addBag(outgoingBag);
      }
    }

    double result = 0.0;
    for (Lemma wordLemma : wordLemmas) {
      if (linksBag.getBag().containsKey(wordLemma)) {
        result += linksBag.getBag().get(wordLemma).weight;
      }
    }
    return result;
  }

  private double getLemmaWeight(Lemma lemma, WebsiteDocument document) {
    BagOfWeightedLemmas bagOfLemmas = document.getBagOfLemmas();
    BagOfWeightedLemmas.WeightedLemmaEntry entry = bagOfLemmas.getBag().get(lemma);
    if (entry == null) {
      return 0.0;
    } else {
      return entry.weight;
    }
  }

  private double getLemmaWeightRatio(Lemma lemma, WebsiteDocument document) {
    BagOfWeightedLemmas bagOfLemmas = document.getBagOfLemmas();
    BagOfWeightedLemmas.WeightedLemmaEntry entry = bagOfLemmas.getBag().get(lemma);
    if (entry == null) {
      return 0.0;
    } else {
      return entry.weight / bagOfLemmas.getSumWeight();
    }
  }
}
