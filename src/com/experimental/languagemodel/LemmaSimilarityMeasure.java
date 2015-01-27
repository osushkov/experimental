package com.experimental.languagemodel;

import com.experimental.WordNet;
import com.experimental.documentvector.ConceptVector;
import com.experimental.documentvector.Word2VecDB;
import com.google.common.base.Preconditions;

/**
 * Created by sushkov on 26/01/15.
 */
public class LemmaSimilarityMeasure {

  private final WordNet wordnet;
  private final Word2VecDB word2Vec;

  public LemmaSimilarityMeasure(WordNet wordnet, Word2VecDB word2Vec) {
    this.wordnet = Preconditions.checkNotNull(wordnet);
    this.word2Vec = null; //Preconditions.checkNotNull(word2Vec);
  }

  public double getLemmaSimilarity(Lemma lemma0, Lemma lemma1) {
    double wordnetSimilarity = getWordNetSimilarity(lemma0, lemma1);
    //double word2vecSimilarity = getWord2VecSimilairty(lemma0, lemma1);

    //return word2vecSimilarity;
    return wordnetSimilarity;
//    return (wordnetSimilarity + word2vecSimilarity) / 2.0;
  }

  private double getWordNetSimilarity(Lemma lemma0, Lemma lemma1) {
    try {
      return wordnet.getLemmaSimilarity(lemma0, lemma1);
    } catch (Throwable e) {
      return 0.0;
    }
  }

  private double getWord2VecSimilairty(Lemma lemma0, Lemma lemma1) {
    ConceptVector vec0 = word2Vec.getWordVector(lemma0.lemma);
    ConceptVector vec1 = word2Vec.getWordVector(lemma1.lemma);

    if (vec0 == null || vec1 == null) {
      return 0.0;
    }

    vec0.normalise();
    vec1.normalise();

    return vec0.dotProduct(vec1);
  }
}
