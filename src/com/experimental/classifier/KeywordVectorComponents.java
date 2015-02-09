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
    return Math.log(1.0 + getLemmaWeight(phraseLemma, document));
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
    if (globalStats == null) {
      return 0.0;
    } else {
      double lemmaWeight = getLemmaWeightRatio(phraseLemma, document);
      double ratio = lemmaWeight / Math.max(Double.MIN_VALUE, globalStats.averageWeightPerDocument);
      return Math.log(1.0 + ratio);
    }
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
    if (globalStats == null) {
      return 0.0;
    } else {
      double lemmaWeight = getLemmaWeightRatio(phraseLemma, document);
      double dist =
          Math.max(0.0, (lemmaWeight - globalStats.averageWeightPerDocument) / globalStats.weightStandardDeviation);
      return Math.log(1.0 + dist);
    }
  }

  public double weightToLocalRatio() {
    if (localStats == null) {
      return 0.0;
    } else {
      double lemmaWeight = getLemmaWeightRatio(phraseLemma, document);
      double ratio = lemmaWeight / Math.max(Double.MIN_VALUE, localStats.averageWeightPerDocument);
      return Math.log(1.0 + ratio);
    }
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
    if (localStats == null) {
      return 0.0;
    } else {
      double lemmaWeight = getLemmaWeightRatio(phraseLemma, document);
      double dist =
          Math.max(0.0, (lemmaWeight - localStats.averageWeightPerDocument) / localStats.weightStandardDeviation);
      return Math.log(1.0 + dist);
    }
  }

  public double localToGlobalAverageWeightRatio() {
    if (localStats == null || globalStats == null) {
      return 0.0;
    } else {
      double ratio =
          localStats.averageWeightPerDocument / Math.max(Double.MIN_VALUE, globalStats.averageWeightPerDocument);
      return Math.log(1.0 + ratio);
    }
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
    if (localStats == null || globalStats == null) {
      return 0.0;
    } else {
      double ratio =
          localStats.fractionOfDocumentOccured / Math.max(Double.MIN_VALUE, globalStats.fractionOfDocumentOccured);
      return Math.log(1.0 + ratio);
    }
  }

  public double getKeyAssociationsWeight() {
    return keyAssociations.getKeyAssociationStrength(phraseLemma);
  }


  public double getGlobalIdfWeight() {
    if (globalStats == null) {
      return 0.0;
    } else {
      double totalDocs = globalStats.sum / globalStats.averageWeightPerDocument;
      Preconditions.checkState(totalDocs >= globalStats.totalDocsOccuredIn);
      return Math.log(totalDocs / globalStats.totalDocsOccuredIn);
    }
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
