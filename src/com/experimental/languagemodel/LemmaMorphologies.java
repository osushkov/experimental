package com.experimental.languagemodel;

import com.experimental.Constants;
import com.experimental.documentmodel.Token;
import com.google.common.base.Preconditions;

import java.io.*;
import java.util.*;

/**
 * Created by sushkov on 13/01/15.
 */
public class LemmaMorphologies {
  private static final String LEMMA_MORPHOLOGIES_FILENAME = "lemma_morphologies.txt";

  private final Map<String, Map<String, Integer>> morphologiesMap = new HashMap<String, Map<String, Integer>>();

  public void addToken(Token token) {
    Preconditions.checkNotNull(token);
    if (!(token.partOfSpeech.isVerb() || token.partOfSpeech.isAdjective() || token.partOfSpeech.isNoun())) {
      return;
    }

    String lemma = token.lemma.toLowerCase();
    String raw = token.raw.toLowerCase();

    Map<String, Integer> curEntry = morphologiesMap.get(lemma);
    if (curEntry == null) {
      curEntry = new HashMap<String, Integer>();
      morphologiesMap.put(lemma, curEntry);
    }

    Integer curCount = curEntry.get(raw);
    if (curCount == null) {
      curEntry.put(raw, 1);
    } else {
      curEntry.put(raw, curCount+1);
    }
  }

  public Map<String, Integer> getMorphologiesFor(String lemma) {
    Preconditions.checkNotNull(lemma);
    return morphologiesMap.get(lemma.toLowerCase());
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

      bw.write(Integer.toString(morphologiesMap.size()) + "\n");
      for (Map.Entry<String, Map<String, Integer>> entry : morphologiesMap.entrySet()) {
        bw.write(entry.getKey() + " ");
        bw.write(Integer.toString(entry.getValue().size()) + "\n");
        for (Map.Entry<String, Integer> morphologyEntry : entry.getValue().entrySet()) {
          bw.write(Integer.toString(morphologyEntry.getValue()) + " " + morphologyEntry.getKey() + "\n");
        }
      }
    } finally {
      if (bw != null) {
        bw.close();
      }
    }
  }

}
