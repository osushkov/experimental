package com.experimental.documentvector;

import com.experimental.documentmodel.BagOfWeightedLemmas;
import com.experimental.documentmodel.Document;
import com.experimental.documentmodel.Sentence;
import com.experimental.documentmodel.Token;
import com.experimental.languagemodel.LemmaIDFWeights;
import com.experimental.languagemodel.StopWords;
import com.experimental.nlp.POSTag;
import com.experimental.nlp.SimplePOSTag;
import com.experimental.utils.Log;
import com.google.common.base.Preconditions;

/**
 * Created by sushkov on 12/01/15.
 */
public class DocumentVectoriser {

  private final Word2VecDB word2VecDb;
  private final LemmaIDFWeights lemmaIdfWeights;

  public DocumentVectoriser(Word2VecDB word2VecDb, LemmaIDFWeights lemmaIdfWeights) {
    this.word2VecDb = Preconditions.checkNotNull(word2VecDb);
    this.lemmaIdfWeights = Preconditions.checkNotNull(lemmaIdfWeights);
  }

  public int getDimensionality() {
    return word2VecDb.dimensionality;
  }

  public ConceptVector computeDocumentVector(Document document) {
    ConceptVector result = new ConceptVectorImpl(getDimensionality());
    result.setToZero();

    double weightSum = 0.0;

    for (BagOfWeightedLemmas.WeightedLemmaEntry entry : document.getBagOfLemmas().getEntries()) {
      if (!includeEntry(entry)) {
        continue;
      }

      ConceptVector entryVector = word2VecDb.getWordVector(entry.lemma.lemma);
      if (entryVector == null) {
        continue;
      }

      double posWeight = getPosWeight(entry);
      double localWeight = getLocalWeight(entry);
      double globalWeight = getGlobalWeight(entry);
      double weight = posWeight * localWeight * globalWeight;
      Preconditions.checkState(weight >= 0.0);

//      Log.out(Double.toString(weight) + " " + entry.lemma.lemma);
      entryVector.scale(weight);
      result.add(entryVector);
      weightSum += weight;

    }

    // Normalise the document or just divide it by the sum of weights?
//    result.normalise();
    result.scale(1.0 / weightSum);

    return result;
  }

  private boolean includeEntry(BagOfWeightedLemmas.WeightedLemmaEntry entry) {
    return !StopWords.instance().isStopWord(entry.lemma.lemma) && entry.lemma.tag != SimplePOSTag.OTHER;
  }

  private double getLocalWeight(BagOfWeightedLemmas.WeightedLemmaEntry entry) {
    return entry.weight;
//    return Math.log(entry.weight + 1.0);
  }

  private double getGlobalWeight(BagOfWeightedLemmas.WeightedLemmaEntry entry) {
    return lemmaIdfWeights.getLemmaWeight(entry.lemma);
  }

  private double getPosWeight(BagOfWeightedLemmas.WeightedLemmaEntry entry) {
    switch (entry.lemma.tag) {
      case ADVERB:    return 0.2;
      case VERB:      return 1.0;
      case ADJECTIVE: return 1.0;
      case NOUN:      return 1.0;
      case OTHER:     return 0.0;
      default:        return 0.0;
    }
  }

}
