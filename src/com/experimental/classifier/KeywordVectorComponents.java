package com.experimental.classifier;

import com.experimental.documentmodel.BagOfWeightedLemmas;
import com.experimental.documentmodel.WebsiteDocument;
import com.experimental.languagemodel.Lemma;
import com.experimental.languagemodel.LemmaOccuranceStatsAggregator;
import com.experimental.languagemodel.LemmaQuality;
import com.google.common.base.Preconditions;

/**
 * Created by sushkov on 30/01/15.
 */
public class KeywordVectorComponents {
  private final Lemma phraseLemma;
  private final WebsiteDocument document;
  private final LemmaQuality lemmaQuality;
  private final LemmaOccuranceStatsAggregator.LemmaStats localStats;
  private final LemmaOccuranceStatsAggregator.LemmaStats globalStats;

  public KeywordVectorComponents(Lemma phraseLemma,
                                 WebsiteDocument document,
                                 LemmaQuality lemmaQuality,
                                 LemmaOccuranceStatsAggregator.LemmaStats localStats,
                                 LemmaOccuranceStatsAggregator.LemmaStats globalStats) {

    this.phraseLemma = Preconditions.checkNotNull(phraseLemma);
    this.document = Preconditions.checkNotNull(document);
    this.lemmaQuality = Preconditions.checkNotNull(lemmaQuality);
    this.localStats = localStats;
    this.globalStats = globalStats;
  }

  public double lemmaWeight() {
    return getLemmaWeight(phraseLemma, document);
  }

  public double lemmaWeightRatio() {
    return getLemmaWeightRatio(phraseLemma, document);
  }

  public double lemmaQuality() {
    return lemmaQuality.getLemmaQuality(phraseLemma);
  }

  public double weightToGobalRatio() {
    double lemmaWeight = getLemmaWeight(phraseLemma, document);
    return globalStats != null ? lemmaWeight / Math.max(Double.MIN_VALUE, globalStats.averageWeightPerDocument) : 0.0;
  }

  public double globalAverageWeightPerDocument() {
    return globalStats != null ? globalStats.averageWeightPerDocument : 0.0;
  }

  public double globalFractionOfDocumentsOccured() {
    return globalStats != null ? globalStats.fractionOfDocumentOccured : 0.0;
  }

  public double globalWeightStandardDeviation() {
    return globalStats != null ? globalStats.weightStandardDeviation : 0.0;
  }

  public double weightToGlobalMeanDistance() {
    double lemmaWeight = getLemmaWeight(phraseLemma, document);
    return globalStats != null ?
        (lemmaWeight - globalStats.averageWeightPerDocument) / globalStats.weightStandardDeviation : 0.0;
  }

  public double weightToLocalRatio() {
    double lemmaWeight = getLemmaWeight(phraseLemma, document);
    return localStats != null ? lemmaWeight / Math.max(Double.MIN_VALUE, localStats.averageWeightPerDocument) : 0.0;
  }

  public double localAverageWeightPerDocument() {
    return localStats != null ? localStats.averageWeightPerDocument : 0.0;
  }

  public double localFractionOfDocumentsOccured() {
    return localStats != null ? localStats.fractionOfDocumentOccured : 0.0;
  }

  public double localWeightStandardDeviation() {
    return localStats != null ? localStats.weightStandardDeviation : 0.0;
  }

  public double weightToLocalMeanDistance() {
    double lemmaWeight = getLemmaWeight(phraseLemma, document);
    return localStats != null ?
        (lemmaWeight - localStats.averageWeightPerDocument) / localStats.weightStandardDeviation : 0.0;
  }

  public double localToGlobalAverageWeightRatio() {
    return localStats != null && globalStats != null ?
        localStats.averageWeightPerDocument / Math.max(Double.MIN_VALUE, globalStats.averageWeightPerDocument) : 0.0;
  }

  public double localToGlobalStandardDeviationRatio() {
    return localStats != null && globalStats != null ?
        localStats.weightStandardDeviation / Math.max(Double.MIN_VALUE, globalStats.weightStandardDeviation) : 0.0;
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