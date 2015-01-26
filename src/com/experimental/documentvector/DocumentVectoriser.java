package com.experimental.documentvector;

import com.experimental.documentmodel.BagOfWeightedLemmas;
import com.experimental.documentmodel.Document;
import com.experimental.documentmodel.Sentence;
import com.experimental.documentmodel.Token;
import com.experimental.languagemodel.BasisVector;
import com.experimental.languagemodel.LemmaIDFWeights;
import com.experimental.languagemodel.LemmaSimilarityMeasure;
import com.experimental.languagemodel.StopWords;
import com.experimental.nlp.POSTag;
import com.experimental.nlp.SimplePOSTag;
import com.experimental.utils.Log;
import com.google.common.base.Preconditions;

import java.util.List;

/**
 * Created by sushkov on 12/01/15.
 */
public class DocumentVectoriser {

  private final BasisVector basisVector;
  private final LemmaSimilarityMeasure lemmaSimilarityMeasure;

  public DocumentVectoriser(BasisVector basisVector, LemmaSimilarityMeasure lemmaSimilarityMeasure) {
    this.basisVector = Preconditions.checkNotNull(basisVector);
    this.lemmaSimilarityMeasure = Preconditions.checkNotNull(lemmaSimilarityMeasure);
  }

  public int getDimensionality() {
    return basisVector.getBasisElements().size();
  }

  public ConceptVector computeDocumentVector(Document document) {
    ConceptVector result = new ConceptVectorImpl(getDimensionality());
    result.setToZero();

    for (BagOfWeightedLemmas.WeightedLemmaEntry entry : document.getBagOfLemmas().getEntries()) {
      if (!includeEntry(entry)) {
        continue;
      }

      ConceptVector entryVector = getConceptVectorFor(entry);
      if (entryVector == null) {
        continue;
      }

      result.add(entryVector);
    }

    result.normalise();
    return result;
  }

  private ConceptVector getConceptVectorFor(BagOfWeightedLemmas.WeightedLemmaEntry entry) {
    ConceptVector result = new ConceptVectorImpl(getDimensionality());

    double posWeight = getPosWeight(entry);
    double localWeight = getLocalWeight(entry);

    List<BasisVector.BasisElement> basisElements = basisVector.getBasisElements();
    for (int i = 0; i < basisElements.size(); i++) {
      BasisVector.BasisElement element = basisElements.get(i);
      if (entry.lemma.equals(element.lemma)) {
        result.setValue(i, posWeight * localWeight * element.weight);
      } else {
        double similarity = lemmaSimilarityMeasure.getLemmaSimilarity(element.lemma, entry.lemma);
        if (similarity > 0.7) {
          result.setValue(i, similarity * posWeight * localWeight * element.weight);
        }
      }
    }

    return result;
  }

  private boolean includeEntry(BagOfWeightedLemmas.WeightedLemmaEntry entry) {
    return !StopWords.instance().isStopWord(entry.lemma.lemma) && entry.lemma.tag != SimplePOSTag.OTHER;
  }

  private double getLocalWeight(BagOfWeightedLemmas.WeightedLemmaEntry entry) {
//    return entry.weight;
    return Math.log(entry.weight + 1.0);
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
