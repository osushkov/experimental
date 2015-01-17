package com.experimental.languagemodel;

import com.experimental.Constants;
import com.experimental.documentmodel.BagOfWeightedLemmas;
import com.experimental.documentmodel.Document;
import com.google.common.base.Preconditions;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sushkov on 16/01/15.
 */
public class LemmaIDFWeights {

  private static class LemmaWeightInfo {
    final String lemma;
    int totalOccurances = 0;
    List<Double> documentOccurance = new ArrayList<Double>();

    double globalWeight = 0.0;

    LemmaWeightInfo(String lemma) {
      this.lemma = Preconditions.checkNotNull(lemma);
    }
  }


  private static final String LEMMA_IDF_WEIGHTS_FILENAME = "lemma_idf_weights.txt";

  // This is used when we finally have the token idf weights, whether computed or loaded from a file.
  private final Map<String, Double> lemmaIdfWeights = new HashMap<String, Double>();

  // This is used for when we are computing the token idf weights from the documents.
  private final Map<String, LemmaWeightInfo> lemmaWeightInfo = new HashMap<String, LemmaWeightInfo>();
  private int numDocuments = 0;

  public double getLemmaWeight(String lemma) {
    Preconditions.checkNotNull(lemma);

    if (!lemmaIdfWeights.containsKey(lemma)) {
      return 0.0;
    } else {
      return lemmaIdfWeights.get(lemma);
    }
  }

  public void processDocument(Document document) {
    // TODO: finish this function.

    Preconditions.checkNotNull(document);

    for (BagOfWeightedLemmas.WeightedLemmaEntry entry : document.getBagOfLemmas().getEntries()) {
//      if (lemmaWeightInfo.containsKey(entry.lemma.lemma))
    }

    numDocuments++;
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
      for (int i = 0; i < numEntries; i++) {
        String line = Preconditions.checkNotNull(br.readLine());
        String[] lineTokens = line.split(" ");
        Preconditions.checkState(lineTokens.length == 2);

        String lemma = lineTokens[0];
        double lemmaIdfWeight = Double.parseDouble(lineTokens[1]);

        lemmaIdfWeights.put(lemma, lemmaIdfWeight);
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
      for (Map.Entry<String, Double> entry : lemmaIdfWeights.entrySet()) {
        bw.write(entry.getKey() + " " + entry.getValue() + "\n");
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
        entropySum += p * Math.log(p) / Math.log(numDocuments);
      }

      weightInfo.globalWeight = 1.0 - entropySum;
      lemmaIdfWeights.put(weightInfo.lemma, weightInfo.globalWeight);
    }
  }

}