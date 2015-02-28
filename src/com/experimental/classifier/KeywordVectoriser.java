package com.experimental.classifier;

import com.experimental.WordNet;
import com.experimental.documentclustering.DocumentClusters;
import com.experimental.documentmodel.Sentence;
import com.experimental.documentmodel.Token;
import com.experimental.documentmodel.WebsiteDocument;
import com.experimental.documentvector.DocumentVectorDB;
import com.experimental.keywords.KeyAssociations;
import com.experimental.keywords.KeywordCandidateGenerator;
import com.experimental.languagemodel.*;
import com.experimental.nlp.NounPhrase;
import com.experimental.utils.Common;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Created by sushkov on 29/01/15.
 */
public class KeywordVectoriser {

  private final LemmaOccuranceStatsAggregator globalLemmaStats;
  private final LemmaQuality lemmaQuality;
  private final DocumentVectorDB documentVectorDb;
  private final LemmaIDFWeights lemmaIdfWeights;
  private final WordNet wordnet;
  private final DocumentClusters documentClusters;
//  private final NounPhrasesDB nounPhrasesDB;
//  private final KeyAssociations keyAssociations;

  public KeywordVectoriser(LemmaOccuranceStatsAggregator globalLemmaStats,
                           LemmaQuality lemmaQuality,
                           DocumentVectorDB documentVectorDb,
                           LemmaIDFWeights lemmaIdfWeights,
                           DocumentClusters documentClusters,
                           WordNet wordnet) {
    this.globalLemmaStats = Preconditions.checkNotNull(globalLemmaStats);
    this.lemmaQuality = Preconditions.checkNotNull(lemmaQuality);
    this.documentVectorDb = Preconditions.checkNotNull(documentVectorDb);
    this.lemmaIdfWeights = Preconditions.checkNotNull(lemmaIdfWeights);
    this.wordnet = Preconditions.checkNotNull(wordnet);
    this.documentClusters = Preconditions.checkNotNull(documentClusters);
//    this.nounPhrasesDB = Preconditions.checkNotNull(nounPhrasesDB);
  }

  public List<KeywordVector> vectoriseKeywordCandidates(Collection<KeywordCandidateGenerator.KeywordCandidate> candidates,
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

//    KeywordVectorComponentsNoPOS components = new KeywordVectorComponentsNoPOS(
//        phraseLemma.lemma, lemmaMorphologies, document, lemmaQuality, lemmaIdfWeights,
//        localOccuranceStats, globalLemmaStats, wordnet);
    KeywordVectorComponents components = new KeywordVectorComponents(
        phraseLemma, document, lemmaQuality, lemmaIdfWeights, localStats, globalStats, wordnet);

    List<Double> resultVector = new ArrayList<Double>();
    resultVector.add(components.lemmaWeight());
    resultVector.add(components.lemmaTopWeights());
    resultVector.add(components.lemmaWeightRatio());
    resultVector.add(components.lemmaQuality());
    resultVector.add(components.lemmaEntropyWeight());
    resultVector.add(components.headerTitleWeight());
    resultVector.add(components.headerDescriptionWeight());
    resultVector.add(components.headerKeywordWeight());
    resultVector.add(components.linksWeight());
//    resultVector.add(components.lemmaTopicDiscrimination());
//    resultVector.add(components.lemmaTopicDiscriminationSd());
    resultVector.add(components.lemmaDictionaryWord());

    resultVector.add(components.weightToGobalRatio()); // 3
    resultVector.add(components.globalAverageWeightPerDocument());
    resultVector.add(components.globalAverageWeightPerOccuredDocument());
    resultVector.add(components.globalFractionOfDocumentsOccured());
    resultVector.add(components.globalWeightStandardDeviation());
    resultVector.add(components.weightToGlobalMeanDistance()); // 7

    resultVector.add(components.weightToLocalRatio());
    resultVector.add(components.localAverageWeightPerDocument());
    resultVector.add(components.localAverageWeightPerOccuredDocument());
    resultVector.add(components.localFractionOfDocumentsOccured());
    resultVector.add(components.localWeightStandardDeviation());
    resultVector.add(components.weightToLocalMeanDistance()); // 12

    resultVector.add(components.localToGlobalAverageWeightRatio());
    resultVector.add(components.localToGlobalOccuredAverageWeightRatio());
    resultVector.add(components.localToGlobalStandardDeviationRatio());
    resultVector.add(components.localToGlobalDocumentsOccuredRatio());
    resultVector.add(components.globalIdfWeight());

    resultVector.add(getKLDivergence(candidate.phraseLemmas));

    resultVector.add(documentClusters.getLemmaStandardDeviation(candidate.phraseLemmas.get(0)));

    resultVector.add(documentClusters.getLemmaNumClusters(candidate.phraseLemmas.get(0)));

    resultVector = generateSquaredVector(resultVector);

    return new KeywordVector(candidate, resultVector);
  }

  private KeywordVector vectoriseKeywordCandidateTwoWord(KeywordCandidateGenerator.KeywordCandidate candidate,
                                                        WebsiteDocument document,
                                                        LemmaOccuranceStatsAggregator localOccuranceStats) {
    Preconditions.checkArgument(candidate.phraseLemmas.size() == 2);

//    KeywordVectorComponentsNoPOS c0 = new KeywordVectorComponentsNoPOS(
//        candidate.phraseLemmas.get(0).lemma, lemmaMorphologies, document, lemmaQuality, lemmaIdfWeights,
//        localOccuranceStats, globalLemmaStats, wordnet);
//
//    KeywordVectorComponentsNoPOS c1 = new KeywordVectorComponentsNoPOS(
//        candidate.phraseLemmas.get(1).lemma, lemmaMorphologies, document, lemmaQuality, lemmaIdfWeights,
//        localOccuranceStats, globalLemmaStats, wordnet);

    KeywordVectorComponents c0 = new KeywordVectorComponents(
        candidate.phraseLemmas.get(0), document, lemmaQuality, lemmaIdfWeights,
        localOccuranceStats.getLemmaStats(candidate.phraseLemmas.get(0)),
        globalLemmaStats.getLemmaStats(candidate.phraseLemmas.get(0)),
        wordnet);

    KeywordVectorComponents c1 = new KeywordVectorComponents(
        candidate.phraseLemmas.get(1), document, lemmaQuality, lemmaIdfWeights,
        localOccuranceStats.getLemmaStats(candidate.phraseLemmas.get(1)),
        globalLemmaStats.getLemmaStats(candidate.phraseLemmas.get(1)),
        wordnet);

    List<Double> resultVector = new ArrayList<Double>();
    resultVector.add(c0.lemmaWeight());
    resultVector.add(c1.lemmaWeight());

    resultVector.add(c0.lemmaTopWeights());
    resultVector.add(c1.lemmaTopWeights());

    resultVector.add(c0.lemmaWeightRatio());
    resultVector.add(c1.lemmaWeightRatio());

    resultVector.add(c0.lemmaQuality());
    resultVector.add(c1.lemmaQuality());

    resultVector.add(c0.lemmaEntropyWeight());
    resultVector.add(c1.lemmaEntropyWeight());

    resultVector.add(c0.headerTitleWeight());
    resultVector.add(c1.headerTitleWeight());

    resultVector.add(c0.headerDescriptionWeight());
    resultVector.add(c1.headerDescriptionWeight());

    resultVector.add(c0.headerKeywordWeight());
    resultVector.add(c1.headerKeywordWeight());

    resultVector.add(c0.linksWeight());
    resultVector.add(c1.linksWeight());

//    resultVector.add(c0.lemmaTopicDiscrimination());
//    resultVector.add(c1.lemmaTopicDiscrimination());
//
//    resultVector.add(c0.lemmaTopicDiscriminationSd());
//    resultVector.add(c1.lemmaTopicDiscriminationSd());

//    resultVector.add(c0.lemmaDictionaryWord());
//    resultVector.add(c1.lemmaDictionaryWord());

    resultVector.add(c0.weightToGobalRatio());
    resultVector.add(c1.weightToGobalRatio());

    resultVector.add(c0.globalAverageWeightPerDocument());
    resultVector.add(c1.globalAverageWeightPerDocument());

    resultVector.add(c0.globalAverageWeightPerOccuredDocument());
    resultVector.add(c1.globalAverageWeightPerOccuredDocument());

    resultVector.add(c0.globalFractionOfDocumentsOccured());
    resultVector.add(c1.globalFractionOfDocumentsOccured());

    resultVector.add(c0.globalWeightStandardDeviation());
    resultVector.add(c1.globalWeightStandardDeviation());

    resultVector.add(c0.weightToGlobalMeanDistance());
    resultVector.add(c1.weightToGlobalMeanDistance());

    resultVector.add(c0.weightToLocalRatio());
    resultVector.add(c1.weightToLocalRatio());

    resultVector.add(c0.localAverageWeightPerDocument());
    resultVector.add(c1.localAverageWeightPerDocument());

    resultVector.add(c0.globalAverageWeightPerOccuredDocument());
    resultVector.add(c1.globalAverageWeightPerOccuredDocument());

    resultVector.add(c0.localFractionOfDocumentsOccured());
    resultVector.add(c1.localFractionOfDocumentsOccured());

    resultVector.add(c0.localWeightStandardDeviation());
    resultVector.add(c1.localWeightStandardDeviation());

    resultVector.add(c0.weightToLocalMeanDistance());
    resultVector.add(c1.weightToLocalMeanDistance());

    resultVector.add(c0.localToGlobalAverageWeightRatio());
    resultVector.add(c1.localToGlobalAverageWeightRatio());

    resultVector.add(c0.localToGlobalOccuredAverageWeightRatio());
    resultVector.add(c1.localToGlobalOccuredAverageWeightRatio());

    resultVector.add(c0.localToGlobalStandardDeviationRatio());
    resultVector.add(c1.localToGlobalStandardDeviationRatio());

    resultVector.add(c0.localToGlobalDocumentsOccuredRatio());
    resultVector.add(c1.localToGlobalDocumentsOccuredRatio());

    resultVector.add(c0.globalIdfWeight());
    resultVector.add(c1.globalIdfWeight());

    resultVector.add(getKLDivergence(candidate.phraseLemmas));

    resultVector.add(getKLDivergence(candidate.phraseLemmas.get(0)));
    resultVector.add(getKLDivergence(candidate.phraseLemmas.get(1)));

    resultVector.add(documentClusters.getLemmaStandardDeviation(candidate.phraseLemmas.get(0)));
    resultVector.add(documentClusters.getLemmaStandardDeviation(candidate.phraseLemmas.get(1)));

    resultVector.add(documentClusters.getLemmaNumClusters(candidate.phraseLemmas.get(0)));
    resultVector.add(documentClusters.getLemmaNumClusters(candidate.phraseLemmas.get(1)));

//    resultVector.add(getCandidateSimilarCorpusWeight(candidate.phraseLemmas, document));
//    resultVector.add(getPhraseAffinity(candidate.phraseLemmas));



//    resultVector.add(logSum(Lists.newArrayList(c0.lemmaWeight(), c1.lemmaWeight())));
//    resultVector.add(logSum(Lists.newArrayList(c0.lemmaWeightRatio(), c1.lemmaWeightRatio())));
//    resultVector.add(logSum(Lists.newArrayList(c0.lemmaQuality(), c1.lemmaQuality())));
//    resultVector.add(logSum(Lists.newArrayList(c0.lemmaEntropyWeight(), c1.lemmaEntropyWeight())));
//
//    resultVector.add(logSum(Lists.newArrayList(c0.weightToGobalRatio(), c1.weightToGobalRatio())));
//    resultVector.add(logSum(Lists.newArrayList(c0.globalAverageWeightPerDocument(), c1.globalAverageWeightPerDocument())));
//    resultVector.add(logSum(Lists.newArrayList(c0.globalAverageWeightPerOccuredDocument(), c1.globalAverageWeightPerOccuredDocument())));
//    resultVector.add(logSum(Lists.newArrayList(c0.globalFractionOfDocumentsOccured(), c1.globalFractionOfDocumentsOccured())));
//    resultVector.add(logSum(Lists.newArrayList(c0.globalWeightStandardDeviation(), c1.globalWeightStandardDeviation())));
//    resultVector.add(logSum(Lists.newArrayList(c0.weightToGlobalMeanDistance(), c1.weightToGlobalMeanDistance())));
//
//    resultVector.add(logSum(Lists.newArrayList(c0.weightToLocalRatio(), c1.weightToLocalRatio())));
//    resultVector.add(logSum(Lists.newArrayList(c0.localAverageWeightPerDocument(), c1.localAverageWeightPerDocument())));
//    resultVector.add(logSum(Lists.newArrayList(c0.localAverageWeightPerOccuredDocument(), c1.localAverageWeightPerOccuredDocument())));
//    resultVector.add(logSum(Lists.newArrayList(c0.localFractionOfDocumentsOccured(), c1.localFractionOfDocumentsOccured())));
//    resultVector.add(logSum(Lists.newArrayList(c0.localWeightStandardDeviation(), c1.localWeightStandardDeviation())));
//    resultVector.add(logSum(Lists.newArrayList(c0.weightToLocalMeanDistance(), c1.weightToLocalMeanDistance())));
//
//    resultVector.add(logSum(Lists.newArrayList(c0.localToGlobalAverageWeightRatio(), c1.localToGlobalAverageWeightRatio())));
//    resultVector.add(logSum(Lists.newArrayList(c0.localToGlobalOccuredAverageWeightRatio(), c1.localToGlobalOccuredAverageWeightRatio())));
//    resultVector.add(logSum(Lists.newArrayList(c0.localToGlobalStandardDeviationRatio(), c1.localToGlobalStandardDeviationRatio())));
//    resultVector.add(logSum(Lists.newArrayList(c0.localToGlobalDocumentsOccuredRatio(), c1.localToGlobalDocumentsOccuredRatio())));
//    resultVector.add(logSum(Lists.newArrayList(c0.getKeyAssociationsWeight(), c1.getKeyAssociationsWeight())));
//    resultVector.add(logSum(Lists.newArrayList(c0.getGlobalIdfWeight(), c1.getGlobalIdfWeight())));

    resultVector = generateSquaredVector(resultVector);

    return new KeywordVector(candidate, resultVector);
  }

  private KeywordVector vectoriseKeywordCandidateThreeWord(KeywordCandidateGenerator.KeywordCandidate candidate,
                                                          WebsiteDocument document,
                                                          LemmaOccuranceStatsAggregator localOccuranceStats) {
    Preconditions.checkArgument(candidate.phraseLemmas.size() == 3);

//    KeywordVectorComponentsNoPOS c0 = new KeywordVectorComponentsNoPOS(
//        candidate.phraseLemmas.get(0).lemma, lemmaMorphologies, document, lemmaQuality, lemmaIdfWeights,
//        localOccuranceStats, globalLemmaStats, wordnet);
//
//    KeywordVectorComponentsNoPOS c1 = new KeywordVectorComponentsNoPOS(
//        candidate.phraseLemmas.get(1).lemma, lemmaMorphologies, document, lemmaQuality, lemmaIdfWeights,
//        localOccuranceStats, globalLemmaStats, wordnet);
//
//    KeywordVectorComponentsNoPOS c2 = new KeywordVectorComponentsNoPOS(
//        candidate.phraseLemmas.get(2).lemma, lemmaMorphologies, document, lemmaQuality, lemmaIdfWeights,
//        localOccuranceStats, globalLemmaStats, wordnet);

    KeywordVectorComponents c0 = new KeywordVectorComponents(
        candidate.phraseLemmas.get(0), document, lemmaQuality, lemmaIdfWeights,
        localOccuranceStats.getLemmaStats(candidate.phraseLemmas.get(0)),
        globalLemmaStats.getLemmaStats(candidate.phraseLemmas.get(0)),
        wordnet);

    KeywordVectorComponents c1 = new KeywordVectorComponents(
        candidate.phraseLemmas.get(1), document, lemmaQuality, lemmaIdfWeights,
        localOccuranceStats.getLemmaStats(candidate.phraseLemmas.get(1)),
        globalLemmaStats.getLemmaStats(candidate.phraseLemmas.get(1)),
        wordnet);

    KeywordVectorComponents c2 = new KeywordVectorComponents(
        candidate.phraseLemmas.get(2), document, lemmaQuality, lemmaIdfWeights,
        localOccuranceStats.getLemmaStats(candidate.phraseLemmas.get(2)),
        globalLemmaStats.getLemmaStats(candidate.phraseLemmas.get(2)),
        wordnet);

    List<Double> resultVector = new ArrayList<Double>();

    resultVector.add(c0.lemmaWeight());
    resultVector.add(c1.lemmaWeight());
    resultVector.add(c2.lemmaWeight());

    resultVector.add(c0.lemmaTopWeights());
    resultVector.add(c1.lemmaTopWeights());
    resultVector.add(c2.lemmaTopWeights());

    resultVector.add(c0.lemmaWeightRatio());
    resultVector.add(c1.lemmaWeightRatio());
    resultVector.add(c2.lemmaWeightRatio());

    resultVector.add(c0.lemmaQuality());
    resultVector.add(c1.lemmaQuality());
    resultVector.add(c2.lemmaQuality());

    resultVector.add(c0.lemmaEntropyWeight());
    resultVector.add(c1.lemmaEntropyWeight());
    resultVector.add(c2.lemmaEntropyWeight());

    resultVector.add(c0.headerTitleWeight());
    resultVector.add(c1.headerTitleWeight());
    resultVector.add(c2.headerTitleWeight());

    resultVector.add(c0.headerDescriptionWeight());
    resultVector.add(c1.headerDescriptionWeight());
    resultVector.add(c2.headerDescriptionWeight());

    resultVector.add(c0.headerKeywordWeight());
    resultVector.add(c1.headerKeywordWeight());
    resultVector.add(c2.headerKeywordWeight());

    resultVector.add(c0.linksWeight());
    resultVector.add(c1.linksWeight());
    resultVector.add(c2.linksWeight());

//    resultVector.add(c0.lemmaTopicDiscrimination());
//    resultVector.add(c1.lemmaTopicDiscrimination());
//    resultVector.add(c2.lemmaTopicDiscrimination());
//
//    resultVector.add(c0.lemmaTopicDiscriminationSd());
//    resultVector.add(c1.lemmaTopicDiscriminationSd());
//    resultVector.add(c2.lemmaTopicDiscriminationSd());

//    resultVector.add(c0.lemmaDictionaryWord());
//    resultVector.add(c1.lemmaDictionaryWord());
//    resultVector.add(c2.lemmaDictionaryWord());

    resultVector.add(c0.weightToGobalRatio());
    resultVector.add(c1.weightToGobalRatio());
    resultVector.add(c2.weightToGobalRatio());

    resultVector.add(c0.globalAverageWeightPerDocument());
    resultVector.add(c1.globalAverageWeightPerDocument());
    resultVector.add(c2.globalAverageWeightPerDocument());

    resultVector.add(c0.globalAverageWeightPerOccuredDocument());
    resultVector.add(c1.globalAverageWeightPerOccuredDocument());
    resultVector.add(c2.globalAverageWeightPerOccuredDocument());

    resultVector.add(c0.globalFractionOfDocumentsOccured());
    resultVector.add(c1.globalFractionOfDocumentsOccured());
    resultVector.add(c2.globalFractionOfDocumentsOccured());

    resultVector.add(c0.globalWeightStandardDeviation());
    resultVector.add(c1.globalWeightStandardDeviation());
    resultVector.add(c2.globalWeightStandardDeviation());

    resultVector.add(c0.weightToGlobalMeanDistance());
    resultVector.add(c1.weightToGlobalMeanDistance());
    resultVector.add(c2.weightToGlobalMeanDistance());

    resultVector.add(c0.weightToLocalRatio());
    resultVector.add(c1.weightToLocalRatio());
    resultVector.add(c2.weightToLocalRatio());

    resultVector.add(c0.localAverageWeightPerDocument());
    resultVector.add(c1.localAverageWeightPerDocument());
    resultVector.add(c2.localAverageWeightPerDocument());

    resultVector.add(c0.localAverageWeightPerOccuredDocument());
    resultVector.add(c1.localAverageWeightPerOccuredDocument());
    resultVector.add(c2.localAverageWeightPerOccuredDocument());

    resultVector.add(c0.localFractionOfDocumentsOccured());
    resultVector.add(c1.localFractionOfDocumentsOccured());
    resultVector.add(c2.localFractionOfDocumentsOccured());

    resultVector.add(c0.localWeightStandardDeviation());
    resultVector.add(c1.localWeightStandardDeviation());
    resultVector.add(c2.localWeightStandardDeviation());

    resultVector.add(c0.weightToLocalMeanDistance());
    resultVector.add(c1.weightToLocalMeanDistance());
    resultVector.add(c2.weightToLocalMeanDistance());

    resultVector.add(c0.localToGlobalAverageWeightRatio());
    resultVector.add(c1.localToGlobalAverageWeightRatio());
    resultVector.add(c2.localToGlobalAverageWeightRatio());

    resultVector.add(c0.localToGlobalOccuredAverageWeightRatio());
    resultVector.add(c1.localToGlobalOccuredAverageWeightRatio());
    resultVector.add(c2.localToGlobalOccuredAverageWeightRatio());

    resultVector.add(c0.localToGlobalStandardDeviationRatio());
    resultVector.add(c1.localToGlobalStandardDeviationRatio());
    resultVector.add(c2.localToGlobalStandardDeviationRatio());

    resultVector.add(c0.localToGlobalDocumentsOccuredRatio());
    resultVector.add(c1.localToGlobalDocumentsOccuredRatio());
    resultVector.add(c2.localToGlobalDocumentsOccuredRatio());

    resultVector.add(c0.globalIdfWeight());
    resultVector.add(c1.globalIdfWeight());
    resultVector.add(c2.globalIdfWeight());

    resultVector.add(getKLDivergence(candidate.phraseLemmas));

    resultVector.add(getKLDivergence(candidate.phraseLemmas.get(0)));
    resultVector.add(getKLDivergence(candidate.phraseLemmas.get(1)));
    resultVector.add(getKLDivergence(candidate.phraseLemmas.get(2)));

    resultVector.add(documentClusters.getLemmaStandardDeviation(candidate.phraseLemmas.get(0)));
    resultVector.add(documentClusters.getLemmaStandardDeviation(candidate.phraseLemmas.get(1)));
    resultVector.add(documentClusters.getLemmaStandardDeviation(candidate.phraseLemmas.get(2)));

    resultVector.add(documentClusters.getLemmaNumClusters(candidate.phraseLemmas.get(0)));
    resultVector.add(documentClusters.getLemmaNumClusters(candidate.phraseLemmas.get(1)));
    resultVector.add(documentClusters.getLemmaNumClusters(candidate.phraseLemmas.get(2)));

//    int size = candidate.phraseLemmas.size();
//    List<Lemma> pairLemmas = Lists.newArrayList(candidate.phraseLemmas.get(size-2), candidate.phraseLemmas.get(size-1));
//    resultVector.add(getCandidateSimilarCorpusWeight(pairLemmas, document));
//
//    pairLemmas = Lists.newArrayList(candidate.phraseLemmas.get(size-3), candidate.phraseLemmas.get(size-2));
//    resultVector.add(getCandidateSimilarCorpusWeight(pairLemmas, document));
//
//    resultVector.add(getPhraseAffinity(candidate.phraseLemmas));




//    resultVector.add(logSum(Lists.newArrayList(c0.lemmaWeight(), c1.lemmaWeight(), c2.lemmaWeight())));
//    resultVector.add(logSum(Lists.newArrayList(c0.lemmaTopWeights(), c1.lemmaTopWeights(), c2.lemmaTopWeights())));
//    resultVector.add(logSum(Lists.newArrayList(c0.lemmaWeightRatio(), c1.lemmaWeightRatio(), c2.lemmaWeightRatio())));
//    resultVector.add(logSum(Lists.newArrayList(c0.lemmaQuality(), c1.lemmaQuality(), c2.lemmaQuality())));
//    resultVector.add(logSum(Lists.newArrayList(c0.lemmaEntropyWeight(), c1.lemmaEntropyWeight(), c2.lemmaEntropyWeight())));
//
//    resultVector.add(logSum(Lists.newArrayList(
//        c0.headerTitleWeight(), c1.headerTitleWeight(), c2.headerTitleWeight())));
//    resultVector.add(logSum(Lists.newArrayList(
//        c0.headerDescriptionWeight(), c1.headerDescriptionWeight(), c2.headerDescriptionWeight())));
//    resultVector.add(logSum(Lists.newArrayList(
//        c0.headerKeywordWeight(), c1.headerKeywordWeight(), c2.headerKeywordWeight())));
//    resultVector.add(logSum(Lists.newArrayList(
//        c0.linksWeight(), c1.linksWeight(), c2.linksWeight())));
//    resultVector.add(logSum(Lists.newArrayList(
//        c0.lemmaTopicDiscrimination(), c1.lemmaTopicDiscrimination(), c2.lemmaTopicDiscrimination())));
//    resultVector.add(logSum(Lists.newArrayList(
//        c0.lemmaDictionaryWord(), c1.lemmaDictionaryWord(), c2.lemmaDictionaryWord())));
//
//    resultVector.add(logSum(Lists.newArrayList(
//        c0.weightToGobalRatio(), c1.weightToGobalRatio(), c2.weightToGobalRatio())));
//    resultVector.add(logSum(Lists.newArrayList(
//        c0.globalAverageWeightPerDocument(), c1.globalAverageWeightPerDocument(), c2.globalAverageWeightPerDocument())));
//    resultVector.add(logSum(Lists.newArrayList(
//        c0.globalAverageWeightPerOccuredDocument(), c1.globalAverageWeightPerOccuredDocument(), c2.globalAverageWeightPerOccuredDocument())));
//    resultVector.add(logSum(Lists.newArrayList(
//        c0.globalFractionOfDocumentsOccured(), c1.globalFractionOfDocumentsOccured(), c2.globalFractionOfDocumentsOccured())));
//    resultVector.add(logSum(Lists.newArrayList(
//        c0.globalWeightStandardDeviation(), c1.globalWeightStandardDeviation(), c2.globalWeightStandardDeviation())));
//    resultVector.add(logSum(Lists.newArrayList(
//        c0.weightToGlobalMeanDistance(), c1.weightToGlobalMeanDistance(), c2.weightToGlobalMeanDistance())));
//
//    resultVector.add(logSum(Lists.newArrayList(
//        c0.weightToLocalRatio(), c1.weightToLocalRatio(), c2.weightToLocalRatio())));
//    resultVector.add(logSum(Lists.newArrayList(
//        c0.localAverageWeightPerDocument(), c1.localAverageWeightPerDocument(), c2.localAverageWeightPerDocument())));
//    resultVector.add(logSum(Lists.newArrayList(
//        c0.localAverageWeightPerOccuredDocument(), c1.localAverageWeightPerOccuredDocument(), c2.localAverageWeightPerOccuredDocument())));
//    resultVector.add(logSum(Lists.newArrayList(
//        c0.localFractionOfDocumentsOccured(), c1.localFractionOfDocumentsOccured(), c2.localFractionOfDocumentsOccured())));
//    resultVector.add(logSum(Lists.newArrayList(
//        c0.localWeightStandardDeviation(), c1.localWeightStandardDeviation(), c2.localWeightStandardDeviation())));
//    resultVector.add(logSum(Lists.newArrayList(
//        c0.weightToLocalMeanDistance(), c1.weightToLocalMeanDistance(), c2.weightToLocalMeanDistance())));
//
//    resultVector.add(logSum(Lists.newArrayList(
//        c0.localToGlobalAverageWeightRatio(), c1.localToGlobalAverageWeightRatio(), c2.localToGlobalAverageWeightRatio())));
//    resultVector.add(logSum(Lists.newArrayList(
//        c0.localToGlobalOccuredAverageWeightRatio(), c1.localToGlobalOccuredAverageWeightRatio(), c2.localToGlobalOccuredAverageWeightRatio())));
//    resultVector.add(logSum(Lists.newArrayList(
//        c0.localToGlobalStandardDeviationRatio(), c1.localToGlobalStandardDeviationRatio(), c2.localToGlobalStandardDeviationRatio())));
//    resultVector.add(logSum(Lists.newArrayList(
//        c0.localToGlobalDocumentsOccuredRatio(), c1.localToGlobalDocumentsOccuredRatio(), c2.localToGlobalDocumentsOccuredRatio())));
////    resultVector.add(logSum(Lists.newArrayList(
////        c0.getKeyAssociationsWeight(), c1.getKeyAssociationsWeight(), c2.getKeyAssociationsWeight())));
//    resultVector.add(logSum(Lists.newArrayList(
//        c0.globalIdfWeight(), c1.globalIdfWeight(), c2.globalIdfWeight())));



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

  private double getKLDivergence(List<Lemma> phraseLemmas) {
    List<Double> uniformDistribution = Common.uniformProbabilityDistribution(documentClusters.getNumClusters());
    List<Double> combinedDistribution = Common.uniformProbabilityDistribution(documentClusters.getNumClusters());

    for (Lemma lemma : phraseLemmas) {
      List<Double> lemmaDistribution = documentClusters.getRawTermProbabilities(lemma);
      combinedDistribution = Common.combinedProbability(lemmaDistribution, combinedDistribution);
    }

    combinedDistribution = Common.getClampedProbabilityDistribution(combinedDistribution);
    double result = Common.computeKLDivergence(combinedDistribution, uniformDistribution);
    Preconditions.checkState(Double.isFinite(result));
    return result;
  }

  private double getKLDivergence(Lemma lemma) {
    List<Double> uniformDistribution = Common.uniformProbabilityDistribution(documentClusters.getNumClusters());
    List<Double> lemmaDistribution = documentClusters.getRawTermProbabilities(lemma);
    lemmaDistribution = Common.getClampedProbabilityDistribution(lemmaDistribution);

    double result = Common.computeKLDivergence(lemmaDistribution, uniformDistribution);
    Preconditions.checkState(Double.isFinite(result));
    return result;
  }

//  private double getCandidateSimilarCorpusWeight(
//      List<Lemma> candidate, WebsiteDocument document) {
//
//    double sum = 0.0;
//    List<DocumentVectorDB.DocumentSimilarityPair> similarDocs = documentVectorDb.getNearestDocuments(document, 30);
//    for (DocumentVectorDB.DocumentSimilarityPair similarityPair : similarDocs) {
//      for (Sentence sentence : similarityPair.document.getSentences()) {
//        int occurances = countOccurancesOf(candidate, sentence);
//        sum += occurances * sentence.emphasis * similarityPair.similarity;
//      }
//    }
//
//    return Math.log(1.0 + sum);
//  }

//  private int countOccurancesOf(List<Lemma> candidate, Sentence sentence) {
//    int result = 0;
//    int curIndex = 0;
//
//    for (Token token : sentence.tokens) {
//      Lemma lemma = Lemma.fromToken(token);
//      if (lemma.equals(candidate.get(curIndex))) {
//        curIndex++;
//      } else {
//        curIndex = 0;
//      }
//
//      if (curIndex == candidate.size()) {
//        result++;
//        curIndex = 0;
//      }
//    }
//    return result;
//  }

//  private double getPhraseAffinity(List<Lemma> phraseLemmas) {
//    int total = 0;
//    int cooccurances = 0;
//
//    for (Lemma lemma : phraseLemmas) {
//      List<Lemma> otherLemmas = new ArrayList<Lemma>(phraseLemmas);
//      otherLemmas.remove(lemma);
//
//      Set<NounPhrasesDB.NounPhraseEntry> entries = nounPhrasesDB.getLemmaPhrases(lemma);
//      if (entries == null) {
//        continue;
//      }
//
//      for (NounPhrasesDB.NounPhraseEntry entry : entries) {
//        total += entry.numOccurances.get();
//        if (phraseContainsAnyOf(entry.phrase, otherLemmas)) {
//          cooccurances += entry.numOccurances.get();
//        }
//      }
//    }
//
//    if (total == 0) {
//      return 0.0;
//    } else {
//      return Math.log(1.0 + cooccurances) / Math.log(1.0 + total);
//    }
//  }
//
//  private boolean phraseContainsAnyOf(NounPhrase phrase, List<Lemma> lemmas) {
//    for (Lemma lemma : lemmas) {
//      if (phrase.getPhraseLemmas().contains(lemma)) {
//        return true;
//      }
//    }
//    return false;
//  }

}
