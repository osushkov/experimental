package com.experimental.languagemodel;

import com.experimental.documentmodel.BagOfWeightedLemmas;
import com.experimental.documentmodel.Document;
import com.experimental.documentmodel.Sentence;
import com.experimental.documentmodel.Token;
import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sushkov on 11/01/15.
 */
public class LemmaQualityAggregator {

  private static class LemmaQualityInfo {
    LemmaId lemmaid;
    int numDocumentsOccured = 0;

    double quality = 0.0;
    double sumOfSquares = 0.0;
    double sum = 0.0;

    LemmaQualityInfo(LemmaId lemmaId) {
      this.lemmaid = Preconditions.checkNotNull(lemmaid);
    }
  }

  private final LemmaDB lemmaDB;
  private final Map<LemmaId, LemmaQualityInfo> lemmaQualityMap = new HashMap<LemmaId, LemmaQualityInfo>();

  public LemmaQualityAggregator(LemmaDB lemmaDB) {
    this.lemmaDB = Preconditions.checkNotNull(lemmaDB);
  }

  public void addDocument(Document document) {
    Preconditions.checkNotNull(document);

    BagOfWeightedLemmas documentBag = document.getBagOfLemmas();
    for (BagOfWeightedLemmas.WeightedLemmaEntry entry : documentBag.getEntries()) {
      LemmaId lemmaId = lemmaDB.addLemma(entry.lemma);
      Preconditions.checkNotNull(lemmaId);

      LemmaQualityInfo qualityEntry = lemmaQualityMap.get(lemmaId);
      if (qualityEntry == null) {
        qualityEntry = new LemmaQualityInfo((lemmaId));
        lemmaQualityMap.put(lemmaId, qualityEntry);
      }

      qualityEntry.numDocumentsOccured++;
      qualityEntry.sum += entry.weight;
      qualityEntry.sumOfSquares += entry.weight * entry.weight;
    }

  }

  public void computeQuality() {
    for (LemmaQualityInfo entry : lemmaQualityMap.values()) {
      entry.quality = entry.sumOfSquares - entry.sum * entry.sum / entry.numDocumentsOccured;
    }
  }

  

}
