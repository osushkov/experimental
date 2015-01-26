package com.experimental.languagemodel;

import com.experimental.Constants;
import com.experimental.documentmodel.BagOfWeightedLemmas;
import com.experimental.documentmodel.Document;
import com.experimental.nlp.SimplePOSTag;
import com.experimental.utils.Log;
import com.google.common.base.Preconditions;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by sushkov on 25/01/15.
 */
public class LemmaQuality {
  private static final String QUALITY_DATA_FILENAME = "lemma_quality.txt";

  private static class LemmaQualityInfo {
    final LemmaDB.LemmaId lemmaId;

    double quality = 0.0;
    double sumOfSquares = 0.0;
    double sum = 0.0;

    LemmaQualityInfo(LemmaDB.LemmaId lemmaId) {
      this.lemmaId = Preconditions.checkNotNull(lemmaId);
    }

    synchronized void addEntry(BagOfWeightedLemmas.WeightedLemmaEntry entry, double totalBagWeight) {
      double frequency = (double) entry.weight / (double) totalBagWeight;

      sum += frequency;
      sumOfSquares += frequency * frequency;
    }
  }

  private static final Comparator<LemmaQualityInfo> QUALITY_ORDER =
      new Comparator<LemmaQualityInfo>() {
        public int compare(LemmaQualityInfo e1, LemmaQualityInfo e2) {
          return Double.compare(e2.quality, e1.quality);
        }
      };

  private final LemmaDB lemmaDB = LemmaDB.instance;
  private final LemmaMorphologies lemmaMorphologies = LemmaMorphologies.instance;
  private final Map<LemmaDB.LemmaId, LemmaQualityInfo> lemmaQualityMap =
      new ConcurrentHashMap<LemmaDB.LemmaId, LemmaQualityInfo>();
  private final AtomicInteger totalDocuments = new AtomicInteger(0);
  private boolean haveQuality = false;

  public void addDocument(Document document) {
    Preconditions.checkNotNull(document);

    BagOfWeightedLemmas documentBag = document.getBagOfLemmas();
    double totalWeight = documentBag.getSumWeight();
    if (totalWeight < 50.0) {
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
          entry.lemma.lemma.length() < 3 ||
          StopWords.instance().isStopWord(entry.lemma.lemma) ||
          lemmaMorphologies.numLemmaOccurances(entry.lemma) < 1000) {
        continue;
      }

      LemmaDB.LemmaId lemmaId = lemmaDB.addLemma(entry.lemma);
      Preconditions.checkNotNull(lemmaId);

      lemmaQualityMap.putIfAbsent(lemmaId, new LemmaQualityInfo(lemmaId));
      LemmaQualityInfo qualityEntry = lemmaQualityMap.get(lemmaId);
      qualityEntry.addEntry(entry, totalWeight);
    }

    totalDocuments.incrementAndGet();
    haveQuality = false;
  }

  public void computeQuality() {
    for (LemmaQualityInfo entry : lemmaQualityMap.values()) {
      entry.quality = entry.sumOfSquares - entry.sum * entry.sum / (double) totalDocuments.get();
      entry.quality *= getLemmaPosWeight(lemmaDB.getLemma(entry.lemmaId));
    }

    haveQuality = true;
  }

  private double getLemmaPosWeight(Lemma lemma) {
    if (lemma == null) {
      return 0.0;
    }

    switch(lemma.tag) {
      case NOUN:      return 1.0;
      case VERB:      return 0.66;
      case ADJECTIVE: return 0.66;
      case ADVERB:    return 0.2;
      case OTHER:     return 0.0;
      default:        return 0.0;
    }
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
    bw.write(Double.toString(info.quality) + " ");
    bw.write(Double.toString(info.sumOfSquares) + " ");
    bw.write(Double.toString(info.sum) + "\n");

    lemmaDB.getLemma(info.lemmaId).writeTo(bw);
  }

  private LemmaQualityInfo loadLemmaQualityInfo(BufferedReader br) throws IOException {
    String line = Preconditions.checkNotNull(br.readLine());
    String[] lineTokens = line.split(" ");
    Preconditions.checkState(lineTokens.length == 3);

    double quality = Double.parseDouble(lineTokens[0]);
    double sumOfSquares = Double.parseDouble(lineTokens[1]);
    double sum = Double.parseDouble(lineTokens[2]);

    Lemma lemma = Lemma.readFrom(br);
    LemmaDB.LemmaId lemmaId = lemmaDB.addLemma(lemma);

    LemmaQualityInfo result = new LemmaQualityInfo(lemmaId);
    result.quality = quality;
    result.sumOfSquares = sumOfSquares;
    result.sum = sum;

    return result;
  }

}
