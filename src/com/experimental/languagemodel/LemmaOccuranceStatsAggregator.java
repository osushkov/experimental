package com.experimental.languagemodel;

import com.experimental.Constants;
import com.experimental.documentmodel.BagOfWeightedLemmas;
import com.experimental.documentmodel.Document;
import com.experimental.languagemodel.LemmaDB.LemmaId;
import com.experimental.nlp.SimplePOSTag;
import com.experimental.utils.Log;
import com.google.common.base.Preconditions;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by sushkov on 11/01/15.
 */
public class LemmaOccuranceStatsAggregator {
  private static final String VARIANCES_DATA_FILENAME = "global_lemma_occurance_statistics.txt";

  public static class LemmaStats {
    final LemmaId lemmaId;

    public double weightStandardDeviation = 0.0;
    public double averageWeightPerDocument = 0.0;
    public double fractionOfDocumentOccured = 0.0;

    public int totalDocsOccuredIn = 0;
    public double sumOfSquares = 0.0;
    public double sum = 0.0;

    LemmaStats(LemmaId lemmaId) {
      this.lemmaId = Preconditions.checkNotNull(lemmaId);
    }

    synchronized void addEntry(BagOfWeightedLemmas.WeightedLemmaEntry entry, double totalBagWeight) {
      double frequency = (double) entry.weight / (double) totalBagWeight;

      sum += frequency;
      sumOfSquares += frequency * frequency;
      totalDocsOccuredIn++;
    }
  }

  private static final Comparator<LemmaStats> VARIANCE_ORDER =
      new Comparator<LemmaStats>() {
        public int compare(LemmaStats e1, LemmaStats e2) {
          return Double.compare(e2.weightStandardDeviation, e1.weightStandardDeviation);
        }
      };

  private final LemmaDB lemmaDB = LemmaDB.instance;
  private final LemmaMorphologies lemmaMorphologies = LemmaMorphologies.instance;
  private final Map<LemmaId, LemmaStats> lemmaVarianceMap = new ConcurrentHashMap<LemmaId, LemmaStats>();

  private final AtomicInteger totalDocuments = new AtomicInteger(0);
  private boolean haveVariance = false;


  public void addDocument(Document document) {
    Preconditions.checkNotNull(document);

    BagOfWeightedLemmas documentBag = document.getBagOfLemmas();
    double totalWeight = documentBag.getSumWeight();
    if (totalWeight < 1.0) {
      return;
    }

    try {
      if (!lemmaMorphologies.tryLoad()) {
        Log.out("Could not load lemma morphologies, exiting");
        return;
      }
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    for (BagOfWeightedLemmas.WeightedLemmaEntry entry : documentBag.getEntries()) {
      if (entry.lemma.tag == SimplePOSTag.OTHER ||
          entry.lemma.lemma.length() < 2 ||
          StopWords.instance().isStopWord(entry.lemma.lemma) ||
          lemmaMorphologies.numLemmaOccurances(entry.lemma) < 10) {
        continue;
      }

      LemmaId lemmaId = lemmaDB.addLemma(entry.lemma);
      Preconditions.checkNotNull(lemmaId);

      lemmaVarianceMap.putIfAbsent(lemmaId, new LemmaStats(lemmaId));
      LemmaStats varianceEntry = lemmaVarianceMap.get(lemmaId);
      varianceEntry.addEntry(entry, totalWeight);
    }

    totalDocuments.incrementAndGet();
    haveVariance = false;
  }

  public LemmaStats getLemmaStats(Lemma lemma) {
    Preconditions.checkNotNull(lemma);
    LemmaId lemmaId = lemmaDB.addLemma(lemma);
    return lemmaVarianceMap.get(lemmaId);
  }

  public void computeStats() {
    for (LemmaStats entry : lemmaVarianceMap.values()) {
      double expectedSumOfSquares = entry.sumOfSquares / totalDocuments.get();
      double expectedSum = entry.sum / totalDocuments.get();

      entry.weightStandardDeviation = Math.sqrt(expectedSumOfSquares - expectedSum*expectedSum);
      entry.averageWeightPerDocument = entry.sum / totalDocuments.get();
      entry.fractionOfDocumentOccured = (double) entry.totalDocsOccuredIn / (double) totalDocuments.get();
    }

    Log.out("total documentS: " + totalDocuments.get());
    haveVariance = true;
  }

  public boolean tryLoadFromDisk() throws IOException {
    File aggregateDataFile = new File(Constants.AGGREGATE_DATA_PATH);
    String varianceFilePath = aggregateDataFile.toPath().resolve(VARIANCES_DATA_FILENAME).toString();

    File varianceFile = new File(varianceFilePath);
    if (!varianceFile.exists()) {
      return false;
    }

    lemmaVarianceMap.clear();
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(varianceFile.getAbsolutePath()));

      int numEntries = Integer.parseInt(Preconditions.checkNotNull(br.readLine()));
      for (int i = 0; i < numEntries; i++) {
        LemmaStats newInfo = loadLemmaVarianceInfo(br);
        lemmaVarianceMap.put(newInfo.lemmaId, newInfo);
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return false;
    } finally {
      if (br != null) {
        br.close();
      }
    }

    haveVariance = true;
    return true;
  }

  public void save() throws IOException {
    if (!haveVariance) {
      computeStats();
    }

    File aggregateDataFile = new File(Constants.AGGREGATE_DATA_PATH);
    String varianceFilePath = aggregateDataFile.toPath().resolve(VARIANCES_DATA_FILENAME).toString();

    BufferedWriter bw = null;
    try {
      try {
        FileWriter fw = new FileWriter(varianceFilePath);
        bw = new BufferedWriter(fw);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
        return;
      }

      bw.write(Integer.toString(lemmaVarianceMap.values().size()) + "\n");
      List<LemmaStats> values = new ArrayList(lemmaVarianceMap.values());
      Collections.sort(values, VARIANCE_ORDER);

      for (LemmaStats info : values) {
        saveLemmaVarianceInfo(info, bw);
      }
    } finally {
      if (bw != null) {
        bw.close();
      }
    }
  }

  private void saveLemmaVarianceInfo(LemmaStats info, BufferedWriter bw) throws IOException {
    bw.write(Double.toString(info.weightStandardDeviation) + " ");
    bw.write(Double.toString(info.averageWeightPerDocument) + " ");
    bw.write(Double.toString(info.fractionOfDocumentOccured) + " ");

    bw.write(Integer.toString(info.totalDocsOccuredIn) + " ");
    bw.write(Double.toString(info.sumOfSquares) + " ");
    bw.write(Double.toString(info.sum) + "\n");

    lemmaDB.getLemma(info.lemmaId).writeTo(bw);
  }

  private LemmaStats loadLemmaVarianceInfo(BufferedReader br) throws IOException {
    String line = Preconditions.checkNotNull(br.readLine());
    String[] lineTokens = line.split(" ");
    Preconditions.checkState(lineTokens.length == 6);


    double weightStandardDeviation = Double.parseDouble(lineTokens[0]);
    double averageWeightPerDocument = Double.parseDouble(lineTokens[1]);
    double fractionOfDocumentOccured = Double.parseDouble(lineTokens[2]);

    int totalDocsOccuredIn = Integer.parseInt(lineTokens[3]);
    double sumOfSquares = Double.parseDouble(lineTokens[4]);
    double sum = Double.parseDouble(lineTokens[5]);

    Preconditions.checkState(weightStandardDeviation >= 0.0);
    Preconditions.checkState(averageWeightPerDocument >= 0.0);
    Preconditions.checkState(fractionOfDocumentOccured >= 0.0 && fractionOfDocumentOccured <= 1.0);
    Preconditions.checkState(totalDocsOccuredIn >= 0);
    Preconditions.checkState(sumOfSquares >= 0.0);
    Preconditions.checkState(sum >= 0.0);

    Lemma lemma = Lemma.readFrom(br);
    LemmaId lemmaId = lemmaDB.addLemma(lemma);

    LemmaStats result = new LemmaStats(lemmaId);
    result.weightStandardDeviation = weightStandardDeviation;
    result.averageWeightPerDocument = averageWeightPerDocument;
    result.fractionOfDocumentOccured = fractionOfDocumentOccured;

    result.totalDocsOccuredIn = totalDocsOccuredIn;
    result.sumOfSquares = sumOfSquares;
    result.sum = sum;

    return result;
  }

}
