package com.experimental.classifier;

import com.experimental.documentmodel.BagOfWeightedLemmas;
import com.experimental.documentmodel.WebsiteDocument;
import com.experimental.keywords.KeyAssociations;
import com.experimental.languagemodel.Lemma;
import com.experimental.languagemodel.LemmaIDFWeights;
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
  private final LemmaIDFWeights lemmaIdfWeights;
  private final KeyAssociations keyAssociations;
  private final LemmaOccuranceStatsAggregator.LemmaStats localStats;
  private final LemmaOccuranceStatsAggregator.LemmaStats globalStats;

  public KeywordVectorComponents(Lemma phraseLemma,
                                 WebsiteDocument document,
                                 LemmaQuality lemmaQuality,
                                 LemmaIDFWeights lemmaIdfWeights,
                                 KeyAssociations keyAssociations,
                                 LemmaOccuranceStatsAggregator.LemmaStats localStats,
                                 LemmaOccuranceStatsAggregator.LemmaStats globalStats) {

    this.phraseLemma = Preconditions.checkNotNull(phraseLemma);
    this.document = Preconditions.checkNotNull(document);
    this.lemmaQuality = Preconditions.checkNotNull(lemmaQuality);
    this.lemmaIdfWeights = Preconditions.checkNotNull(lemmaIdfWeights);
    this.keyAssociations = Preconditions.checkNotNull(keyAssociations);
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

  public double lemmaIdfWeight() {
    return lemmaIdfWeights.getLemmaWeight(phraseLemma);
  }

  public double weightToGobalRatio() {
    double lemmaWeight = getLemmaWeightRatio(phraseLemma, document);
    return globalStats != null ? lemmaWeight / Math.max(Double.MIN_VALUE, globalStats.averageWeightPerDocument) : 0.0;
  }

  public double globalAverageWeightPerDocument() {
    return globalStats != null ? globalStats.averageWeightPerDocument : 0.0;
  }

  public double globalAverageWeightPerOccuredDocument() {
    return globalStats != null ? (globalStats.sum / (double) globalStats.totalDocsOccuredIn) : 0.0;
  }

  public double globalFractionOfDocumentsOccured() {
    return globalStats != null ? globalStats.fractionOfDocumentOccured : 0.0;
  }

  public double globalWeightStandardDeviation() {
    return globalStats != null ? globalStats.weightStandardDeviation : 0.0;
  }

  public double weightToGlobalMeanDistance() {
    double lemmaWeight = getLemmaWeightRatio(phraseLemma, document);
    return globalStats != null ?
        Math.max(0.0, (lemmaWeight - globalStats.averageWeightPerDocument) / globalStats.weightStandardDeviation)
        : 0.0;
  }

  public double weightToLocalRatio() {
    double lemmaWeight = getLemmaWeightRatio(phraseLemma, document);
    return localStats != null ? lemmaWeight / Math.max(Double.MIN_VALUE, localStats.averageWeightPerDocument) : 0.0;
  }

  public double localAverageWeightPerDocument() {
    return localStats != null ? localStats.averageWeightPerDocument : 0.0;
  }

  public double localAverageWeightPerOccuredDocument() {
    return localStats != null ? (localStats.sum / (double) localStats.totalDocsOccuredIn) : 0.0;
  }

  public double localFractionOfDocumentsOccured() {
    return localStats != null ? localStats.fractionOfDocumentOccured : 0.0;
  }

  public double localWeightStandardDeviation() {
    return localStats != null ? localStats.weightStandardDeviation : 0.0;
  }

  public double weightToLocalMeanDistance() {
    double lemmaWeight = getLemmaWeightRatio(phraseLemma, document);
    return localStats != null ?
        Math.max(0.0, (lemmaWeight - localStats.averageWeightPerDocument) / localStats.weightStandardDeviation)
        : 0.0;
  }

  public double localToGlobalAverageWeightRatio() {
    return localStats != null && globalStats != null ?
        localStats.averageWeightPerDocument / Math.max(Double.MIN_VALUE, globalStats.averageWeightPerDocument) : 0.0;
  }

  public double localToGlobalOccuredAverageWeightRatio() {
    return localStats != null && globalStats != null ?
        localAverageWeightPerOccuredDocument() / Math.max(Double.MIN_VALUE, globalAverageWeightPerOccuredDocument()) : 0.0;
  }

  public double localToGlobalStandardDeviationRatio() {
    return localStats != null && globalStats != null ?
        localStats.weightStandardDeviation / Math.max(Double.MIN_VALUE, globalStats.weightStandardDeviation) : 0.0;
  }

  public double localToGlobalDocumentsOccuredRatio() {
    return localStats != null && globalStats != null ?
        localStats.fractionOfDocumentOccured / Math.max(Double.MIN_VALUE, globalStats.fractionOfDocumentOccured) : 0.0;
  }

  public double getKeyAssociationsWeight() {
    return keyAssociations.getKeyAssociationStrength(phraseLemma);
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
