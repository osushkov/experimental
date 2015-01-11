package com.experimental.documentmodel;

import com.experimental.languagemodel.Lemma;
import com.experimental.nlp.POSTag;
import com.google.common.base.Preconditions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

/**
 * Created by sushkov on 11/01/15.
 */
public class BagOfWeightedLemmas {

  public static class WeightedLemmaEntry {
    public final Lemma lemma;
    public double weight = 0.0;

    public WeightedLemmaEntry(Lemma lemma) {
      this.lemma = Preconditions.checkNotNull(lemma);
    }

    public WeightedLemmaEntry(Lemma lemma, double weight) {
      Preconditions.checkArgument(weight >= 0.0);

      this.lemma = Preconditions.checkNotNull(lemma);
      this.weight = weight;
    }
  }

  private final Map<Lemma, WeightedLemmaEntry> bag = new HashMap<Lemma, WeightedLemmaEntry>();

  public BagOfWeightedLemmas(List<Sentence> sentences) {
    Preconditions.checkNotNull(sentences);
    for (Sentence sentence : sentences) {
      for (Token token : sentence.tokens) {
        Lemma tokenLemma = Lemma.fromToken(token);

        WeightedLemmaEntry curEntry = bag.get(tokenLemma);
        if (curEntry == null) {
          curEntry = new WeightedLemmaEntry(tokenLemma);
          bag.put(tokenLemma, curEntry);
        }

        curEntry.weight += sentence.emphasis;
      }
    }
  }

  public Collection<WeightedLemmaEntry> getEntries() {
    return bag.values();
  }

}
