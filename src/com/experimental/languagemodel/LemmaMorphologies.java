package com.experimental.languagemodel;

import com.experimental.Constants;
import com.experimental.documentmodel.Token;
import com.experimental.languagemodel.LemmaDB.LemmaId;
import com.experimental.languagemodel.MorphologyDB.MorphologyId;
import com.experimental.nlp.SimplePOSTag;
import com.google.common.base.Preconditions;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by sushkov on 13/01/15.
 */
public class LemmaMorphologies {
  private static final String LEMMA_MORPHOLOGIES_FILENAME = "lemma_morphologies.txt";

  private final LemmaDB lemmaDb;
  private final MorphologyDB morphologyDb;

  private final Map<LemmaId, Map<MorphologyId, AtomicInteger>> lemmaToMorphologyMap =
      new ConcurrentHashMap<LemmaId, Map<MorphologyId, AtomicInteger>>();

  private final Map<MorphologyId, Map<LemmaId, AtomicInteger>> morphologytoLemmaMap =
      new ConcurrentHashMap<MorphologyId, Map<LemmaId, AtomicInteger>>();

  public LemmaMorphologies(LemmaDB lemmaDb, MorphologyDB morphologyDb) {
    this.lemmaDb = Preconditions.checkNotNull(lemmaDb);
    this.morphologyDb = Preconditions.checkNotNull(morphologyDb);
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
    MorphologyId morphologyId = morphologyDb.addMorphology(morphology);

    lemmaToMorphologyMap.putIfAbsent(lemmaId, new ConcurrentHashMap<MorphologyId, AtomicInteger>());
    Map<MorphologyId, AtomicInteger> lemmaMorphologyEntry = lemmaToMorphologyMap.get(lemmaId);

    lemmaMorphologyEntry.putIfAbsent(morphologyId, new AtomicInteger(0));
    lemmaMorphologyEntry.get(morphologyId).incrementAndGet();
  }

  private void addTokenToMorphologyToLemmaMap(String morphology, Lemma lemma, int occurances) {
    LemmaId lemmaId = lemmaDb.addLemma(lemma);
    MorphologyId morphologyId = morphologyDb.addMorphology(morphology);

    morphologytoLemmaMap.putIfAbsent(morphologyId, new ConcurrentHashMap<LemmaId, AtomicInteger>());
    Map<LemmaId, AtomicInteger> morphologyLemmaEntry = morphologytoLemmaMap.get(morphologyId);

    morphologyLemmaEntry.putIfAbsent(lemmaId, new AtomicInteger(0));
    morphologyLemmaEntry.get(lemmaId).incrementAndGet();
  }

  public Map<String, Integer> getMorphologiesFor(Lemma lemma) {
    Preconditions.checkNotNull(lemma);

    LemmaId lemmaId = lemmaDb.addLemma(lemma);
    Map<String, Integer> result = new HashMap<String, Integer>();

    for (Map.Entry<MorphologyId, AtomicInteger> entry : lemmaToMorphologyMap.get(lemmaId).entrySet()) {
      String word = Preconditions.checkNotNull(morphologyDb.getMorphology(entry.getKey()));
      result.put(word, entry.getValue().get());
    }

    return result;
  }

  public void save() throws IOException {
    trim();

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

      for (Map.Entry<LemmaId, Map<MorphologyId, AtomicInteger>> entry : lemmaToMorphologyMap.entrySet()) {
        Lemma lemma = lemmaDb.getLemma(entry.getKey());
        lemma.writeTo(bw);

        bw.write(Integer.toString(entry.getValue().size()) + "\n");
        for (Map.Entry<MorphologyId, AtomicInteger> morphEntry : entry.getValue().entrySet()) {
          String word = Preconditions.checkNotNull(morphologyDb.getMorphology(morphEntry.getKey()));
          bw.write(Integer.toString(morphEntry.getValue().get()) + " " + word + "\n");
        }
      }
    } finally {
      if (bw != null) {
        bw.close();
      }
    }
  }

  private void trim() {
    List<LemmaId> toRemove = new ArrayList<LemmaId>();
    for (LemmaId lemmaId: lemmaToMorphologyMap.keySet()) {
      Map<MorphologyId, AtomicInteger> morphologies = lemmaToMorphologyMap.get(lemmaId);

      int totalOccurances = 0;
      for (AtomicInteger occurances : morphologies.values()) {
        totalOccurances += occurances.get();
      }

      if (totalOccurances < 5) {
        toRemove.add(lemmaId);
      }
    }

    for (LemmaId lemmaId : toRemove) {
      lemmaToMorphologyMap.remove(lemmaId);
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
