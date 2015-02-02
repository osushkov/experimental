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

    public WeightedLemmaEntry(WeightedLemmaEntry other) {
      Preconditions.checkNotNull(other);
      this.lemma = other.lemma;
      this.weight = other.weight;
    }
  }

  private final Map<Lemma, WeightedLemmaEntry> bag = new HashMap<Lemma, WeightedLemmaEntry>();
  private double sumWeight = 0.0;

  public BagOfWeightedLemmas() {}

  public BagOfWeightedLemmas(List<Sentence> sentences) {
    Preconditions.checkNotNull(sentences);
    for (Sentence sentence : sentences) {
      for (Token token : sentence.tokens) {
        Lemma tokenLemma = Lemma.fromToken(token);

        addLemma(tokenLemma, sentence.emphasis);
        WeightedLemmaEntry curEntry = bag.get(tokenLemma);
        if (curEntry == null) {
          curEntry = new WeightedLemmaEntry(tokenLemma);
          bag.put(tokenLemma, curEntry);
        }

        curEntry.weight += sentence.emphasis;
        sumWeight += sentence.emphasis;
      }
    }
  }

  public void addBag(BagOfWeightedLemmas otherBag) {
    for (WeightedLemmaEntry otherEntry : otherBag.bag.values()) {
      addLemma(otherEntry.lemma, otherEntry.weight);
    }
  }

  public Collection<WeightedLemmaEntry> getEntries() {
    return bag.values();
  }

  public Map<Lemma, WeightedLemmaEntry> getBag() {
    return bag;
  }

  public double getSumWeight() {
    return sumWeight;
  }

  private void addLemma(Lemma lemma, double weight) {
    WeightedLemmaEntry curEntry = bag.get(lemma);
    if (curEntry == null) {
      curEntry = new WeightedLemmaEntry(lemma);
      bag.put(lemma, curEntry);
    }

    curEntry.weight += weight;
    sumWeight += weight;
  }
}
