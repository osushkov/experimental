package com.experimental.languagemodel;

import com.experimental.Constants;
import com.experimental.documentmodel.BagOfWeightedLemmas;
import com.experimental.documentmodel.Document;
import com.experimental.languagemodel.LemmaDB.LemmaId;
import com.experimental.utils.Log;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AtomicDouble;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by sushkov on 16/01/15.
 */
public class LemmaIDFWeights {

  private static class LemmaWeightInfo {
    final LemmaId lemmaId;
    double totalOccurances = 0.0;
    List<Double> documentOccurance = new ArrayList<Double>();

    double globalWeight = 0.0;

    LemmaWeightInfo(LemmaId lemmaId) {
      this.lemmaId = Preconditions.checkNotNull(lemmaId);
    }

    synchronized void update(BagOfWeightedLemmas.WeightedLemmaEntry entry, double documentWeight) {
      totalOccurances += entry.weight * documentWeight;
      documentOccurance.add(entry.weight * documentWeight);
    }
  }

  private static final String LEMMA_IDF_WEIGHTS_FILENAME = "lemma_idf_weights.txt";

  // This is used when we finally have the token idf weights, whether computed or loaded from a file.
  private final Map<LemmaId, Double> lemmaIdfWeights = new HashMap<LemmaId, Double>();

  // This is used for when we are computing the token idf weights from the documents.
  private final Map<LemmaId, LemmaWeightInfo> lemmaWeightInfo = new ConcurrentHashMap<LemmaId, LemmaWeightInfo>();
  private final LemmaDB lemmaDb;
  private AtomicDouble numDocuments = new AtomicDouble(0.0);

  public LemmaIDFWeights(LemmaDB lemmaDb) {
    this.lemmaDb = Preconditions.checkNotNull(lemmaDb);
  }

  public double getLemmaWeight(Lemma lemma) {
    Preconditions.checkNotNull(lemma);
    LemmaId lemmaId = lemmaDb.addLemma(lemma);

    if (!lemmaIdfWeights.containsKey(lemma)) {
      return 0.0;
    } else {
      return lemmaIdfWeights.get(lemma);
    }
  }

  public void processDocument(Document document, double documentWeight) {
    Preconditions.checkNotNull(document);

    for (BagOfWeightedLemmas.WeightedLemmaEntry entry : document.getBagOfLemmas().getEntries()) {
      LemmaId lemmaId = lemmaDb.addLemma(entry.lemma);
      lemmaWeightInfo.putIfAbsent(lemmaId, new LemmaWeightInfo(lemmaId));

      LemmaWeightInfo info = lemmaWeightInfo.get(lemmaId);
      info.update(entry, documentWeight);
    }

    numDocuments.getAndAdd(documentWeight);
  }

  public boolean isDocumentValid(Document document) {
    return document.getSentences().size() > 10;
  }

  public boolean tryLoad() throws IOException {
    File aggregateDataFile = new File(Constants.AGGREGATE_DATA_PATH);
    String lemmaIdfWeightsFilePath = aggregateDataFile.toPath().resolve(LEMMA_IDF_WEIGHTS_FILENAME).toString();

    File lemmaIdfWeightsFile = new File(lemmaIdfWeightsFilePath);
    if (!lemmaIdfWeightsFile.exists()) {
      return false;
    }

    lemmaIdfWeights.clear();
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(lemmaIdfWeightsFile.getAbsolutePath()));

      int numEntries = Integer.parseInt(Preconditions.checkNotNull(br.readLine()));
      Preconditions.checkState(numEntries >= 0);

      for (int i = 0; i < numEntries; i++) {
        Lemma lemma = Lemma.readFrom(br);
        LemmaId lemmaId = lemmaDb.addLemma(lemma);

        double lemmaIdfWeight = Double.parseDouble(Preconditions.checkNotNull(br.readLine()));
        Preconditions.checkState(lemmaIdfWeight >= 0.0);

        lemmaIdfWeights.put(lemmaId, lemmaIdfWeight);
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return false;
    } finally {
      if (br != null) {
        br.close();
      }
    }

    return true;
  }

  public void save() throws IOException {
    processWeightInfo();

    File aggregateDataFile = new File(Constants.AGGREGATE_DATA_PATH);
    String lemmaIdfWeightsFilePath = aggregateDataFile.toPath().resolve(LEMMA_IDF_WEIGHTS_FILENAME).toString();

    BufferedWriter bw = null;
    try {
      try {
        FileWriter fw = new FileWriter(lemmaIdfWeightsFilePath);
        bw = new BufferedWriter(fw);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
        return;
      }

      bw.write(Integer.toString(lemmaIdfWeights.values().size()) + "\n");
      for (Map.Entry<LemmaId, Double> entry : lemmaIdfWeights.entrySet()) {
        Lemma lemma = lemmaDb.getLemma(entry.getKey());
        lemma.writeTo(bw);
        bw.write(entry.getValue() + "\n");
      }
    } finally {
      if (bw != null) {
        bw.close();
      }
    }
  }

  private void processWeightInfo() {
    for (LemmaWeightInfo weightInfo : lemmaWeightInfo.values()) {
      double entropySum = 0.0;
      for (double occurances : weightInfo.documentOccurance) {
        double p = (double) occurances / (double) weightInfo.totalOccurances;

        entropySum += p * Math.log(p) / Math.log(numDocuments.get());
        Log.out("p: " + p + " " + entropySum);
      }

      weightInfo.globalWeight = 1.0 + entropySum;
      Log.out("weight: " + weightInfo.globalWeight);
      lemmaIdfWeights.put(weightInfo.lemmaId, weightInfo.globalWeight);
    }
  }

}
