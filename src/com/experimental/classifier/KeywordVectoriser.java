package com.experimental.classifier;

import com.experimental.documentmodel.BagOfWeightedLemmas;
import com.experimental.documentmodel.Document;
import com.experimental.documentmodel.WebsiteDocument;
import com.experimental.documentvector.DocumentVectorDB;
import com.experimental.keywords.KeyAssociations;
import com.experimental.keywords.KeywordCandidateGenerator;
import com.experimental.languagemodel.Lemma;
import com.experimental.languagemodel.LemmaIDFWeights;
import com.experimental.languagemodel.LemmaOccuranceStatsAggregator;
import com.experimental.languagemodel.LemmaQuality;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sushkov on 29/01/15.
 */
public class KeywordVectoriser {

  private final LemmaOccuranceStatsAggregator globalLemmaStats;
  private final LemmaQuality lemmaQuality;
  private final DocumentVectorDB documentVectorDb;
  private final LemmaIDFWeights lemmaIdfWeights;
  private final KeyAssociations keyAssociations;

  public KeywordVectoriser(LemmaOccuranceStatsAggregator globalLemmaStats,
                           LemmaQuality lemmaQuality,
                           DocumentVectorDB documentVectorDb,
                           LemmaIDFWeights lemmaIdfWeights,
                           KeyAssociations keyAssociations) {
    this.globalLemmaStats = Preconditions.checkNotNull(globalLemmaStats);
    this.lemmaQuality = Preconditions.checkNotNull(lemmaQuality);
    this.documentVectorDb = Preconditions.checkNotNull(documentVectorDb);
    this.lemmaIdfWeights = Preconditions.checkNotNull(lemmaIdfWeights);
    this.keyAssociations = Preconditions.checkNotNull(keyAssociations);
  }

  public List<KeywordVector> vectoriseKeywordCandidates(List<KeywordCandidateGenerator.KeywordCandidate> candidates,
                                                        WebsiteDocument document) {
    Preconditions.checkNotNull(candidates);
    Preconditions.checkNotNull(document);

    LemmaOccuranceStatsAggregator localLemmaStats = getLocalLemmaStats(document);

    List<KeywordVector> result = new ArrayList<KeywordVector>();
    for (KeywordCandidateGenerator.KeywordCandidate candidate : candidates) {
      if (candidate.phraseLemmas.size() == 1) {
        result.add(vectoriseKeywordCandidateOneWord(candidate, document, localLemmaStats));
      } else if (candidate.phraseLemmas.size() == 2) {
        result.add(vectoriseKeywordCandidateTwoWord(candidate, document, localLemmaStats));
      } else if (candidate.phraseLemmas.size() == 3) {
        result.add(vectoriseKeywordCandidateThreeWord(candidate, document, localLemmaStats));
      }
    }
    return result;
  }

  private KeywordVector vectoriseKeywordCandidateOneWord(KeywordCandidateGenerator.KeywordCandidate candidate,
                                                        WebsiteDocument document,
                                                        LemmaOccuranceStatsAggregator localOccuranceStats) {
    Preconditions.checkArgument(candidate.phraseLemmas.size() == 1);

    Lemma phraseLemma = candidate.phraseLemmas.get(0);
    LemmaOccuranceStatsAggregator.LemmaStats localStats = localOccuranceStats.getLemmaStats(phraseLemma);
    LemmaOccuranceStatsAggregator.LemmaStats globalStats = globalLemmaStats.getLemmaStats(phraseLemma);

    KeywordVectorComponents components = new KeywordVectorComponents(
        phraseLemma, document, lemmaQuality, lemmaIdfWeights, keyAssociations, localStats, globalStats);

    List<Double> resultVector = new ArrayList<Double>();
    resultVector.add(components.lemmaWeight());
    resultVector.add(components.lemmaWeightRatio());
    resultVector.add(components.lemmaQuality());
    resultVector.add(components.lemmaIdfWeight());

    resultVector.add(components.weightToGobalRatio()); // 3
    resultVector.add(components.globalAverageWeightPerDocument());
    resultVector.add(components.globalAverageWeightPerOccuredDocument());
    resultVector.add(components.globalFractionOfDocumentsOccured());
    resultVector.add(components.globalWeightStandardDeviation());
//    resultVector.add(components.weightToGlobalMeanDistance()); // 7

    resultVector.add(components.weightToLocalRatio());
    resultVector.add(components.localAverageWeightPerDocument());
    resultVector.add(components.localAverageWeightPerOccuredDocument());
    resultVector.add(components.localFractionOfDocumentsOccured());
    resultVector.add(components.localWeightStandardDeviation());
//    resultVector.add(components.weightToLocalMeanDistance()); // 12

    resultVector.add(components.localToGlobalAverageWeightRatio());
    resultVector.add(components.localToGlobalOccuredAverageWeightRatio());
    resultVector.add(components.localToGlobalStandardDeviationRatio());
    resultVector.add(components.localToGlobalDocumentsOccuredRatio());
    resultVector.add(components.getKeyAssociationsWeight());

    resultVector = generateLoggedVector(resultVector);
    resultVector = generateSquaredVector(resultVector);

    return new KeywordVector(candidate, resultVector);
  }

  private KeywordVector vectoriseKeywordCandidateTwoWord(KeywordCandidateGenerator.KeywordCandidate candidate,
                                                        WebsiteDocument document,
                                                        LemmaOccuranceStatsAggregator localOccuranceStats) {
    Preconditions.checkArgument(candidate.phraseLemmas.size() == 2);

    KeywordVectorComponents c0 = new KeywordVectorComponents(
        candidate.phraseLemmas.get(0), document, lemmaQuality, lemmaIdfWeights, keyAssociations,
        localOccuranceStats.getLemmaStats(candidate.phraseLemmas.get(0)),
        globalLemmaStats.getLemmaStats(candidate.phraseLemmas.get(0)));

    KeywordVectorComponents c1 = new KeywordVectorComponents(
        candidate.phraseLemmas.get(1), document, lemmaQuality, lemmaIdfWeights, keyAssociations,
        localOccuranceStats.getLemmaStats(candidate.phraseLemmas.get(1)),
        globalLemmaStats.getLemmaStats(candidate.phraseLemmas.get(1)));

    List<Double> resultVector = new ArrayList<Double>();
//    resultVector.add(c0.lemmaWeight());       resultVector.add(c1.lemmaWeight());
//    resultVector.add(c0.lemmaWeightRatio());  resultVector.add(c1.lemmaWeightRatio());
//    resultVector.add(c0.lemmaQuality());      resultVector.add(c1.lemmaQuality());
//    resultVector.add(c0.lemmaIdfWeight());    resultVector.add(c1.lemmaIdfWeight());
//
//    resultVector.add(c0.weightToGobalRatio());                resultVector.add(c1.weightToGobalRatio());
//    resultVector.add(c0.globalAverageWeightPerDocument());    resultVector.add(c1.globalAverageWeightPerDocument());
//    resultVector.add(c0.globalFractionOfDocumentsOccured());  resultVector.add(c1.globalFractionOfDocumentsOccured());
//    resultVector.add(c0.globalWeightStandardDeviation());     resultVector.add(c1.globalWeightStandardDeviation());
//    resultVector.add(c0.weightToGlobalMeanDistance());        resultVector.add(c1.weightToGlobalMeanDistance());
//
//    resultVector.add(c0.weightToLocalRatio());              resultVector.add(c1.weightToLocalRatio());
//    resultVector.add(c0.localAverageWeightPerDocument());   resultVector.add(c1.localAverageWeightPerDocument());
//    resultVector.add(c0.localFractionOfDocumentsOccured()); resultVector.add(c1.localFractionOfDocumentsOccured());
//    resultVector.add(c0.localWeightStandardDeviation());    resultVector.add(c1.localWeightStandardDeviation());
//    resultVector.add(c0.weightToLocalMeanDistance());       resultVector.add(c1.weightToLocalMeanDistance());
//
//    resultVector.add(c0.localToGlobalAverageWeightRatio());     resultVector.add(c1.localToGlobalAverageWeightRatio());
//    resultVector.add(c0.localToGlobalStandardDeviationRatio()); resultVector.add(c1.localToGlobalStandardDeviationRatio());

    resultVector.add(logSum(Lists.newArrayList(c0.lemmaWeight(), c1.lemmaWeight())));
    resultVector.add(logSum(Lists.newArrayList(c0.lemmaWeightRatio(), c1.lemmaWeightRatio())));
    resultVector.add(logSum(Lists.newArrayList(c0.lemmaQuality(), c1.lemmaQuality())));
    resultVector.add(logSum(Lists.newArrayList(c0.lemmaIdfWeight(), c1.lemmaIdfWeight())));

    resultVector.add(logSum(Lists.newArrayList(c0.weightToGobalRatio(), c1.weightToGobalRatio())));
    resultVector.add(logSum(Lists.newArrayList(c0.globalAverageWeightPerDocument(), c1.globalAverageWeightPerDocument())));
    resultVector.add(logSum(Lists.newArrayList(c0.globalAverageWeightPerOccuredDocument(), c1.globalAverageWeightPerOccuredDocument())));
    resultVector.add(logSum(Lists.newArrayList(c0.globalFractionOfDocumentsOccured(), c1.globalFractionOfDocumentsOccured())));
    resultVector.add(logSum(Lists.newArrayList(c0.globalWeightStandardDeviation(), c1.globalWeightStandardDeviation())));
//    resultVector.add(logSum(Lists.newArrayList(c0.weightToGlobalMeanDistance(), c1.weightToGlobalMeanDistance())));

    resultVector.add(logSum(Lists.newArrayList(c0.weightToLocalRatio(), c1.weightToLocalRatio())));
    resultVector.add(logSum(Lists.newArrayList(c0.localAverageWeightPerDocument(), c1.localAverageWeightPerDocument())));
    resultVector.add(logSum(Lists.newArrayList(c0.localAverageWeightPerOccuredDocument(), c1.localAverageWeightPerOccuredDocument())));
    resultVector.add(logSum(Lists.newArrayList(c0.localFractionOfDocumentsOccured(), c1.localFractionOfDocumentsOccured())));
    resultVector.add(logSum(Lists.newArrayList(c0.localWeightStandardDeviation(), c1.localWeightStandardDeviation())));
//    resultVector.add(logSum(Lists.newArrayList(c0.weightToLocalMeanDistance(), c1.weightToLocalMeanDistance())));

    resultVector.add(logSum(Lists.newArrayList(c0.localToGlobalAverageWeightRatio(), c1.localToGlobalAverageWeightRatio())));
    resultVector.add(logSum(Lists.newArrayList(c0.localToGlobalOccuredAverageWeightRatio(), c1.localToGlobalOccuredAverageWeightRatio())));
    resultVector.add(logSum(Lists.newArrayList(c0.localToGlobalStandardDeviationRatio(), c1.localToGlobalStandardDeviationRatio())));
    resultVector.add(logSum(Lists.newArrayList(c0.localToGlobalDocumentsOccuredRatio(), c1.localToGlobalDocumentsOccuredRatio())));
    resultVector.add(logSum(Lists.newArrayList(c0.getKeyAssociationsWeight(), c1.getKeyAssociationsWeight())));

    resultVector = generateLoggedVector(resultVector);
    resultVector = generateSquaredVector(resultVector);

    return new KeywordVector(candidate, resultVector);
  }

  private KeywordVector vectoriseKeywordCandidateThreeWord(KeywordCandidateGenerator.KeywordCandidate candidate,
                                                          WebsiteDocument document,
                                                          LemmaOccuranceStatsAggregator localOccuranceStats) {
    Preconditions.checkArgument(candidate.phraseLemmas.size() == 3);

    KeywordVectorComponents c0 = new KeywordVectorComponents(
        candidate.phraseLemmas.get(0), document, lemmaQuality, lemmaIdfWeights, keyAssociations,
        localOccuranceStats.getLemmaStats(candidate.phraseLemmas.get(0)),
        globalLemmaStats.getLemmaStats(candidate.phraseLemmas.get(0)));

    KeywordVectorComponents c1 = new KeywordVectorComponents(
        candidate.phraseLemmas.get(1), document, lemmaQuality, lemmaIdfWeights, keyAssociations,
        localOccuranceStats.getLemmaStats(candidate.phraseLemmas.get(1)),
        globalLemmaStats.getLemmaStats(candidate.phraseLemmas.get(1)));

    KeywordVectorComponents c2 = new KeywordVectorComponents(
        candidate.phraseLemmas.get(2), document, lemmaQuality, lemmaIdfWeights, keyAssociations,
        localOccuranceStats.getLemmaStats(candidate.phraseLemmas.get(2)),
        globalLemmaStats.getLemmaStats(candidate.phraseLemmas.get(2)));

    List<Double> resultVector = new ArrayList<Double>();
    resultVector.add(logSum(Lists.newArrayList(c0.lemmaWeight(), c1.lemmaWeight(), c2.lemmaWeight())));
    resultVector.add(logSum(Lists.newArrayList(c0.lemmaWeightRatio(), c1.lemmaWeightRatio(), c2.lemmaWeightRatio())));
    resultVector.add(logSum(Lists.newArrayList(c0.lemmaQuality(), c1.lemmaQuality(), c2.lemmaQuality())));
    resultVector.add(logSum(Lists.newArrayList(c0.lemmaIdfWeight(), c1.lemmaIdfWeight(), c2.lemmaIdfWeight())));

    resultVector.add(logSum(Lists.newArrayList(
        c0.weightToGobalRatio(), c1.weightToGobalRatio(), c2.weightToGobalRatio())));
    resultVector.add(logSum(Lists.newArrayList(
        c0.globalAverageWeightPerDocument(), c1.globalAverageWeightPerDocument(), c2.globalAverageWeightPerDocument())));
    resultVector.add(logSum(Lists.newArrayList(
        c0.globalAverageWeightPerOccuredDocument(), c1.globalAverageWeightPerOccuredDocument(), c2.globalAverageWeightPerOccuredDocument())));
    resultVector.add(logSum(Lists.newArrayList(
        c0.globalFractionOfDocumentsOccured(), c1.globalFractionOfDocumentsOccured(), c2.globalFractionOfDocumentsOccured())));
    resultVector.add(logSum(Lists.newArrayList(
        c0.globalWeightStandardDeviation(), c1.globalWeightStandardDeviation(), c2.globalWeightStandardDeviation())));
//    resultVector.add(logSum(Lists.newArrayList(
//        c0.weightToGlobalMeanDistance(), c1.weightToGlobalMeanDistance(), c2.weightToGlobalMeanDistance())));

    resultVector.add(logSum(Lists.newArrayList(
        c0.weightToLocalRatio(), c1.weightToLocalRatio(), c2.weightToLocalRatio())));
    resultVector.add(logSum(Lists.newArrayList(
        c0.localAverageWeightPerDocument(), c1.localAverageWeightPerDocument(), c2.localAverageWeightPerDocument())));
    resultVector.add(logSum(Lists.newArrayList(
        c0.localAverageWeightPerOccuredDocument(), c1.localAverageWeightPerOccuredDocument(), c2.localAverageWeightPerOccuredDocument())));
    resultVector.add(logSum(Lists.newArrayList(
        c0.localFractionOfDocumentsOccured(), c1.localFractionOfDocumentsOccured(), c2.localFractionOfDocumentsOccured())));
    resultVector.add(logSum(Lists.newArrayList(
        c0.localWeightStandardDeviation(), c1.localWeightStandardDeviation(), c2.localWeightStandardDeviation())));
//    resultVector.add(logSum(Lists.newArrayList(
//        c0.weightToLocalMeanDistance(), c1.weightToLocalMeanDistance(), c2.weightToLocalMeanDistance())));

    resultVector.add(logSum(Lists.newArrayList(
        c0.localToGlobalAverageWeightRatio(), c1.localToGlobalAverageWeightRatio(), c2.localToGlobalAverageWeightRatio())));
    resultVector.add(logSum(Lists.newArrayList(
        c0.localToGlobalOccuredAverageWeightRatio(), c1.localToGlobalOccuredAverageWeightRatio(), c2.localToGlobalOccuredAverageWeightRatio())));
    resultVector.add(logSum(Lists.newArrayList(
        c0.localToGlobalStandardDeviationRatio(), c1.localToGlobalStandardDeviationRatio(), c2.localToGlobalStandardDeviationRatio())));
    resultVector.add(logSum(Lists.newArrayList(
        c0.localToGlobalDocumentsOccuredRatio(), c1.localToGlobalDocumentsOccuredRatio(), c2.localToGlobalDocumentsOccuredRatio())));
    resultVector.add(logSum(Lists.newArrayList(
        c0.getKeyAssociationsWeight(), c1.getKeyAssociationsWeight(), c2.getKeyAssociationsWeight())));

    resultVector = generateLoggedVector(resultVector);
    resultVector = generateSquaredVector(resultVector);

    return new KeywordVector(candidate, resultVector);
  }

  private LemmaOccuranceStatsAggregator getLocalLemmaStats(WebsiteDocument document) {
    List<DocumentVectorDB.DocumentSimilarityPair> similarityPairs = documentVectorDb.getNearestDocuments(document, 50);

    LemmaOccuranceStatsAggregator result = new LemmaOccuranceStatsAggregator();
    for (DocumentVectorDB.DocumentSimilarityPair pair : similarityPairs) {
      result.addDocument(pair.document);
    }
    result.computeStats();
    return result;
  }

  private double featureAverage(List<Double> values) {
    double sum = 0.0;
    for (double val : values) {
      sum += val;
    }
    return sum / values.size();
  }

  private double logSum(List<Double> values) {
    double sum = 1.0;
    for (double val : values) {
//      sum *= val;
      sum += Math.log(1.0 + val);
    }
    return sum;
  }

  private List<Double> generateLoggedVector(List<Double> vector) {
//    return vector;
    List<Double> result = new ArrayList();
    result.addAll(vector);
    for (double val : vector) {
      result.add(Math.log(1.0 + val));
    }
    return result;
  }

  private List<Double> generateSquaredVector(List<Double> vector) {
//    return vector;
    List<Double> result = new ArrayList(vector);
    for (int i = 0; i < vector.size(); i++) {
      for (int j = i; j < vector.size(); j++) {
        result.add(vector.get(i) * vector.get(j));
      }
    }
    return result;
  }
}
