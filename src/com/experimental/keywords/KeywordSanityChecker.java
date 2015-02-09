package com.experimental.keywords;

import com.experimental.WordNet;
import com.experimental.languagemodel.Lemma;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by sushkov on 9/02/15.
 */
public class KeywordSanityChecker {

  private static final Set<String> STOP_WORDS = new HashSet<String>(buildStopWords());
  private final WordNet wordnet;

  public KeywordSanityChecker(WordNet wordnet) {
    this.wordnet = Preconditions.checkNotNull(wordnet);
  }

  public boolean isSane(KeywordCandidateGenerator.KeywordCandidate candidate) {
    if (candidate.phraseLemmas.size() == 1) {
      Lemma lemma = candidate.phraseLemmas.get(0);
      if (STOP_WORDS.contains(lemma.lemma)) {
        return false;
      }

    }

    return true;
  }

  private static List<String> buildStopWords() {
    List<String> result = new ArrayList<String>();
    result.addAll(getLoremIpsumWords());
    result.add("pty");
    result.add("pty.");
    result.add("ltd");
    result.add("ltd.");
    return result;
  }

  private static List<String> getLoremIpsumWords() {
    String passage = "lorem ipsum dolor sit amet consectetur adipiscing elit sed eiusmod tempor incididunt ut " +
        "labore et dolore magna aliqua ut enim minim veniam quis nostrud exercitation ullamco laboris nisi ut " +
        "aliquip ex ea commodo consequat duis aute irure dolor reprehenderit voluptate velit esse cillum dolore " +
        "eu fugiat nulla pariatur excepteur sint occaecat cupidatat proident sunt culpa qui officia deserunt " +
        "mollit anim id est laborum";
    return Lists.newArrayList(passage.split(" "));
  }
}
