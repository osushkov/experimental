package com.experimental.languagemodel;

import com.experimental.Constants;
import com.experimental.documentmodel.Token;
import com.experimental.nlp.SimplePOSTag;
import com.google.common.base.Preconditions;

import java.io.*;
import java.util.*;

/**
 * Created by sushkov on 13/01/15.
 */
public class LemmaMorphologies {
  private static final String LEMMA_MORPHOLOGIES_FILENAME = "lemma_morphologies.txt";

  private final LemmaDB lemmaDb;
  private final Map<LemmaId, Map<String, Integer>> lemmaToMorphologyMap = new HashMap<LemmaId, Map<String, Integer>>();
  private final Map<String, Map<LemmaId, Integer>> morphologytoLemmaMap = new HashMap<String, Map<LemmaId, Integer>>();

  public LemmaMorphologies(LemmaDB lemmaDb) {
    this.lemmaDb = Preconditions.checkNotNull(lemmaDb);
  }

  public void addToken(Token token) {
    Preconditions.checkNotNull(token);

    Lemma lemma = Lemma.fromToken(token);
    if (lemma.tag == SimplePOSTag.OTHER) {
      return;
    }

    addTokenToLemmaToMorphologyMap(token.raw.toLowerCase(), lemma, 1);
    addTokenToMorphologyToLemmaMap(token.raw.toLowerCase(), lemma, 1);
  }

  private void addTokenToLemmaToMorphologyMap(String morphology, Lemma lemma, int occurances) {
    LemmaId lemmaId = lemmaDb.addLemma(lemma);

    Map<String, Integer> lemmaMorphologyEntry = lemmaToMorphologyMap.get(lemmaId);
    if (lemmaMorphologyEntry == null) {
      lemmaMorphologyEntry = new HashMap<String, Integer>();
      lemmaToMorphologyMap.put(lemmaId, lemmaMorphologyEntry);
    }

    if (!lemmaMorphologyEntry.containsKey(morphology)) {
      lemmaMorphologyEntry.put(morphology, occurances);
    } else {
      lemmaMorphologyEntry.put(morphology, lemmaMorphologyEntry.get(morphology) + occurances);
    }
  }

  private void addTokenToMorphologyToLemmaMap(String morphology, Lemma lemma, int occurances) {
    LemmaId lemmaId = lemmaDb.addLemma(lemma);

    Map<LemmaId, Integer> morphologyLemmaEntry = morphologytoLemmaMap.get(morphology);
    if (morphologyLemmaEntry == null) {
      morphologyLemmaEntry = new HashMap<LemmaId, Integer>();
      morphologytoLemmaMap.put(morphology, morphologyLemmaEntry);
    }


    if (!morphologyLemmaEntry.containsKey(lemmaId)) {
      morphologyLemmaEntry.put(lemmaId, occurances);
    } else {
      morphologyLemmaEntry.put(lemmaId, morphologyLemmaEntry.get(lemmaId) + occurances);
    }
  }

  public Map<String, Integer> getMorphologiesFor(Lemma lemma) {
    Preconditions.checkNotNull(lemma);

    LemmaId lemmaId = lemmaDb.addLemma(lemma);
    return lemmaToMorphologyMap.get(lemmaId);
  }

  public void save() throws IOException {
    File aggregateDataFile = new File(Constants.AGGREGATE_DATA_PATH);
    String morphologiesFilePath = aggregateDataFile.toPath().resolve(LEMMA_MORPHOLOGIES_FILENAME).toString();

    BufferedWriter bw = null;
    try {
      try {
        FileWriter fw = new FileWriter(morphologiesFilePath);
        bw = new BufferedWriter(fw);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
        return;
      }

      bw.write(Integer.toString(lemmaToMorphologyMap.size()) + "\n");

      for (Map.Entry<LemmaId, Map<String, Integer>> entry : lemmaToMorphologyMap.entrySet()) {
        Lemma lemma = lemmaDb.getLemma(entry.getKey());
        lemma.writeTo(bw);

        bw.write(Integer.toString(entry.getValue().size()) + "\n");
        for (Map.Entry<String, Integer> morphEntry : entry.getValue().entrySet()) {
          bw.write(morphEntry.getValue() + " " + morphEntry.getKey() + "\n");
        }
      }
    } finally {
      if (bw != null) {
        bw.close();
      }
    }
  }

  public boolean tryLoad() throws IOException {
    File aggregateDataFile = new File(Constants.AGGREGATE_DATA_PATH);
    String morphologiesFilePath = aggregateDataFile.toPath().resolve(LEMMA_MORPHOLOGIES_FILENAME).toString();

    File morphologiesFile = new File(morphologiesFilePath);
    if (!morphologiesFile.exists()) {
      return false;
    }

    lemmaToMorphologyMap.clear();
    morphologytoLemmaMap.clear();

    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(morphologiesFile.getAbsolutePath()));

      int numEntries = Integer.parseInt(Preconditions.checkNotNull(br.readLine()));
      for (int i = 0; i < numEntries; i++) {
        Lemma lemma = Lemma.readFrom(br);

        int numLemmaEntries = Integer.parseInt(Preconditions.checkNotNull(br.readLine()));
        for (int j = 0; j < numLemmaEntries; j++) {
          String line = Preconditions.checkNotNull(br.readLine());
          String[] lineTokens = line.split(" ");
          Preconditions.checkState(lineTokens.length == 2);

          int occurances = Integer.parseInt(lineTokens[0]);
          String morphology = lineTokens[1];

          addTokenToLemmaToMorphologyMap(morphology, lemma, occurances);
          addTokenToMorphologyToLemmaMap(morphology, lemma, occurances);
        }
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

}
