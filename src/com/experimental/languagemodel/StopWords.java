package com.experimental.languagemodel;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by sushkov on 15/01/15.
 */
public class StopWords {
  private final Set<String> stopWords = new HashSet<String>();

  private StopWords() {
    String preset = "the be and of a in to have it i that for you he with on do this they at " +
        "but we his from that not n't by she or as what go their can who get if would her all my " +
        "will as there so some into than then its also";

    List<String> words = Lists.newArrayList(preset.split(" "));
    for (String word : words) {
      stopWords.add(word.toLowerCase());
    }
  }

  private static StopWords myInstance = null;
  public static StopWords instance() {
    if (myInstance == null) {
      myInstance = new StopWords();
    }
    return myInstance;
  }

  public boolean isStopWord(String lemma) {
    return stopWords.contains(Preconditions.checkNotNull(lemma).toLowerCase());
  }
}
