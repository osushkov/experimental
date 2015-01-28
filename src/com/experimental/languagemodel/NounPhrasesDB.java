package com.experimental.languagemodel;

import com.experimental.Constants;
import com.experimental.documentmodel.Sentence;
import com.experimental.nlp.NounPhrase;
import com.experimental.nlp.NounPhraseExtractor;
import com.experimental.utils.Log;
import com.google.common.base.Preconditions;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by sushkov on 27/01/15.
 */
public class NounPhrasesDB {
  private static final String NOUN_PHRASE_FILENAME = "noun_phrases.txt";

  private static class NounPhraseEntry {
    final NounPhrase phrase;
    final AtomicInteger numOccurances;

    NounPhraseEntry(NounPhrase phrase) {
      this.phrase = Preconditions.checkNotNull(phrase);
      this.numOccurances = new AtomicInteger(0);
    }

    NounPhraseEntry(NounPhrase phrase, int numOccurances) {
      Preconditions.checkArgument(numOccurances >= 0);

      this.phrase = Preconditions.checkNotNull(phrase);
      this.numOccurances = new AtomicInteger(numOccurances);
    }
  }

  private final Map<NounPhrase, NounPhraseEntry> phraseEntries =
      new ConcurrentHashMap<NounPhrase, NounPhraseEntry>();

  private final Map<LemmaDB.LemmaId, Set<NounPhraseEntry>> lemmaPhrases =
      new ConcurrentHashMap<LemmaDB.LemmaId, Set<NounPhraseEntry>>();

  private final LemmaDB lemmaDb;
  private final LemmaMorphologies lemmaMorphologies;
  private final NounPhraseExtractor phraseExtractor;

  public NounPhrasesDB(LemmaDB lemmaDb, LemmaMorphologies lemmaMorphologies) {
    this.lemmaDb = Preconditions.checkNotNull(lemmaDb);
    this.lemmaMorphologies = Preconditions.checkNotNull(lemmaMorphologies);
    this.phraseExtractor = new NounPhraseExtractor(lemmaDb);
  }

  public void addSentence(Sentence sentence) {
    Preconditions.checkNotNull(sentence);
    List<NounPhrase> nounPhrases = phraseExtractor.extractNounPhrases(sentence);
    for (NounPhrase phrase : nounPhrases) {
      if (phrase.isCompositePhrase() && isCommonPhrase(phrase)) {
//        Log.out(phrase.toString());
        if (!phraseEntries.containsKey(phrase)) {
          phraseEntries.put(phrase, new NounPhraseEntry(phrase));
        }

        NounPhraseEntry entry = phraseEntries.get(phrase);
        entry.numOccurances.incrementAndGet();
      }
    }
  }

  private boolean isCommonPhrase(NounPhrase phrase) {
    for (Lemma lemma : phrase.getPhraseLemmas()) {
      if (lemmaMorphologies.numLemmaOccurances(lemma) < 1000) {
        return false;
      }
    }
    return true;
  }

  private void insertLookup(NounPhraseEntry phraseEntry) {
    for (LemmaDB.LemmaId lemmaId: phraseEntry.phrase.getPhraseLemmaIds()) {
      lemmaPhrases.putIfAbsent(lemmaId, new HashSet<NounPhraseEntry>());
      lemmaPhrases.get(lemmaId).add(phraseEntry);
    }
  }

  public void save() throws IOException {
    File aggregateDataFile = new File(Constants.AGGREGATE_DATA_PATH);
    String nounPhraseFilePath = aggregateDataFile.toPath().resolve(NOUN_PHRASE_FILENAME).toString();

    BufferedWriter bw = null;
    try {
      try {
        FileWriter fw = new FileWriter(nounPhraseFilePath);
        bw = new BufferedWriter(fw);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
        return;
      }

      bw.write(Integer.toString(phraseEntries.values().size()) + "\n");
      for (NounPhraseEntry entry : phraseEntries.values()) {
        bw.write(Integer.toString(entry.numOccurances.get()) + "\n");
        entry.phrase.writeTo(bw);
      }
    } finally {
      if (bw != null) {
        bw.close();
      }
    }
  }

  public boolean tryLoad() throws IOException {
    File aggregateDataFile = new File(Constants.AGGREGATE_DATA_PATH);
    String nounPhraseFilePath = aggregateDataFile.toPath().resolve(NOUN_PHRASE_FILENAME).toString();

    File nounPhraseFile = new File(nounPhraseFilePath);
    if (!nounPhraseFile.exists()) {
      return false;
    }

    phraseEntries.clear();
    lemmaPhrases.clear();
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(nounPhraseFile.getAbsolutePath()));

      int numEntries = Integer.parseInt(Preconditions.checkNotNull(br.readLine()));
      Preconditions.checkState(numEntries >= 0);

      for (int i = 0; i < numEntries; i++) {
        int numOccurances = Integer.parseInt(Preconditions.checkNotNull(br.readLine()));
        Preconditions.checkState(numOccurances > 0);
        NounPhrase phrase = NounPhrase.readFrom(br, lemmaDb);

        if (numOccurances > 10) {
          NounPhraseEntry entry = new NounPhraseEntry(phrase, numOccurances);
          phraseEntries.put(phrase, entry);
          insertLookup(entry);
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
