package com.experimental.languagemodel;

import com.experimental.Constants;
import com.experimental.documentmodel.BagOfWeightedLemmas;
import com.experimental.documentmodel.Document;
import com.experimental.documentmodel.Sentence;
import com.experimental.documentmodel.Token;
import com.experimental.languagemodel.LemmaDB.LemmaId;
import com.experimental.nlp.SimplePOSTag;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.io.*;
import java.util.*;

/**
 * Created by sushkov on 11/01/15.
 */
public class LemmaQualityAggregator {
  private static final String QUALITY_DATA_FILENAME = "lemma_quality_data.txt";

  private static class LemmaQualityInfo {
    final LemmaId lemmaId;
    int numDocumentsOccured = 0;

    double quality = 0.0;
    double sumOfSquares = 0.0;
    double sum = 0.0;

    LemmaQualityInfo(LemmaId lemmaId) {
      this.lemmaId = Preconditions.checkNotNull(lemmaId);
    }
  }

  private static final Comparator<LemmaQualityInfo> QUALITY_ORDER =
      new Comparator<LemmaQualityInfo>() {
        public int compare(LemmaQualityInfo e1, LemmaQualityInfo e2) {
          return Double.compare(e2.quality, e1.quality);
        }
      };

  private final LemmaDB lemmaDB = LemmaDB.instance;
  private final Map<LemmaId, LemmaQualityInfo> lemmaQualityMap = new HashMap<LemmaId, LemmaQualityInfo>();
  private boolean haveQuality = false;
  private int totalDocuments = 0;

  public void addDocument(Document document) {
    Preconditions.checkNotNull(document);

    BagOfWeightedLemmas documentBag = document.getBagOfLemmas();
    double totalWeight = documentBag.getSumWeight();
    if (totalWeight < 1.0) {
      return;
    }

    for (BagOfWeightedLemmas.WeightedLemmaEntry entry : documentBag.getEntries()) {
      if (entry.lemma.tag == SimplePOSTag.OTHER || entry.lemma.lemma.length() < 3) {
        continue;
      }

      LemmaId lemmaId = lemmaDB.addLemma(entry.lemma);
      Preconditions.checkNotNull(lemmaId);

      LemmaQualityInfo qualityEntry = lemmaQualityMap.get(lemmaId);
      if (qualityEntry == null) {
        qualityEntry = new LemmaQualityInfo((lemmaId));
        lemmaQualityMap.put(lemmaId, qualityEntry);
      }

      qualityEntry.numDocumentsOccured++;
      qualityEntry.sum += entry.weight / totalWeight;
      qualityEntry.sumOfSquares += (entry.weight / totalWeight) * (entry.weight / totalWeight);
    }

    totalDocuments++;
    haveQuality = false;
  }

  public void computeQuality() {
    for (LemmaQualityInfo entry : lemmaQualityMap.values()) {
      entry.quality = entry.sumOfSquares - entry.sum * entry.sum / totalDocuments;
    }

    haveQuality = true;
  }

  public boolean tryLoadFromDisk() throws IOException {
    File aggregateDataFile = new File(Constants.AGGREGATE_DATA_PATH);
    String qualityFilePath = aggregateDataFile.toPath().resolve(QUALITY_DATA_FILENAME).toString();

    File qualityFile = new File(qualityFilePath);
    if (!qualityFile.exists()) {
      return false;
    }

    lemmaQualityMap.clear();
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(qualityFile.getAbsolutePath()));

      int numEntries = Integer.parseInt(Preconditions.checkNotNull(br.readLine()));
      for (int i = 0; i < numEntries; i++) {
        LemmaQualityInfo newInfo = loadLemmaQualityInfo(br);
        lemmaQualityMap.put(newInfo.lemmaId, newInfo);
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return false;
    } finally {
      if (br != null) {
        br.close();
      }
    }

    haveQuality = true;
    return true;
  }

  public void save() throws IOException {
    if (!haveQuality) {
      computeQuality();
    }

    File aggregateDataFile = new File(Constants.AGGREGATE_DATA_PATH);
    String qualityFilePath = aggregateDataFile.toPath().resolve(QUALITY_DATA_FILENAME).toString();

    BufferedWriter bw = null;
    try {
      try {
        FileWriter fw = new FileWriter(qualityFilePath);
        bw = new BufferedWriter(fw);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
        return;
      }

      bw.write(Integer.toString(lemmaQualityMap.values().size()) + "\n");
      List<LemmaQualityInfo> values = new ArrayList(lemmaQualityMap.values());
      Collections.sort(values, QUALITY_ORDER);

      for (LemmaQualityInfo info : values) {
        saveLemmaQualityInfo(info, bw);
      }
    } finally {
      if (bw != null) {
        bw.close();
      }
    }
  }

  private void saveLemmaQualityInfo(LemmaQualityInfo info, BufferedWriter bw) throws IOException {
    bw.write(Integer.toString(info.numDocumentsOccured) + " ");
    bw.write(Double.toString(info.quality) + " ");
    bw.write(Double.toString(info.sumOfSquares) + " ");
    bw.write(Double.toString(info.sum) + "\n");

    lemmaDB.getLemma(info.lemmaId).writeTo(bw);
  }

  private LemmaQualityInfo loadLemmaQualityInfo(BufferedReader br) throws IOException {
    String line = Preconditions.checkNotNull(br.readLine());
    String[] lineTokens = line.split(" ");
    Preconditions.checkState(lineTokens.length == 4);

    int numDocumentsOccured = Integer.parseInt(lineTokens[0]);
    double quality = Double.parseDouble(lineTokens[1]);
    double sumOfSquares = Double.parseDouble(lineTokens[2]);
    double sum = Double.parseDouble(lineTokens[3]);

    Lemma lemma = Lemma.readFrom(br);
    LemmaId lemmaId = lemmaDB.addLemma(lemma);

    LemmaQualityInfo result = new LemmaQualityInfo(lemmaId);
    result.numDocumentsOccured = numDocumentsOccured;
    result.quality = quality;
    result.sumOfSquares = sumOfSquares;
    result.sum = sum;

    return result;
  }

}
