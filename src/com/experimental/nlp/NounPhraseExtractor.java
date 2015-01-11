package com.experimental.nlp;

import com.experimental.documentmodel.Sentence;
import com.experimental.documentmodel.Token;
import com.google.common.base.Preconditions;
import edu.stanford.nlp.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sushkov on 9/01/15.
 */
public class NounPhraseExtractor {

  public static class SentenceTokenIndexRange {
    final int startIndex; // inclusive
    final int endIndex; // inclusive

    public SentenceTokenIndexRange(int startIndex, int endIndex) {
      Preconditions.checkArgument(startIndex >= 0);
      Preconditions.checkArgument(endIndex >= 0);
      Preconditions.checkArgument(startIndex <= endIndex);

      this.startIndex = startIndex;
      this.endIndex = endIndex;
    }
  }


  public List<Pair<NounPhrase, SentenceTokenIndexRange>> extractNounPhrases(List<Token> tokens) {
    List<Pair<NounPhrase, SentenceTokenIndexRange>> result = new ArrayList<Pair<NounPhrase, SentenceTokenIndexRange>>();
    List<Token> curNounPhrase = new ArrayList<Token>();
    boolean haveNoun = false;
    int curStartIndex = -1;

    for (int i = 0; i < tokens.size(); i++) {
      Token token = tokens.get(i);

      if (token.partOfSpeech.isNoun() || token.partOfSpeech.isAdjective() ||
          token.partOfSpeech == POSTag.DT || token.partOfSpeech == POSTag.CD) {
        if (token.partOfSpeech.isNoun()) {
          haveNoun = true;
        }

        if (curStartIndex == -1) {
          curStartIndex = i;
        }
        curNounPhrase.add(token);
      } else {
        if (haveNoun) {
          Preconditions.checkState(curNounPhrase.size() > 0);
          Preconditions.checkState(curStartIndex >= 0 && curStartIndex < tokens.size());

          Pair<NounPhrase, SentenceTokenIndexRange> newPhrase = new Pair<NounPhrase, SentenceTokenIndexRange>(
              new NounPhrase(curNounPhrase),
              new SentenceTokenIndexRange(curStartIndex, i-1)
          );
          result.add(newPhrase);
        }

        curNounPhrase.clear();
        haveNoun = false;
        curStartIndex = -1;
      }
    }

    if (haveNoun) {
      Preconditions.checkState(curNounPhrase.size() > 0);
      Preconditions.checkState(curStartIndex >= 0 && curStartIndex < tokens.size());

      Pair<NounPhrase, SentenceTokenIndexRange> newPhrase = new Pair<NounPhrase, SentenceTokenIndexRange>(
          new NounPhrase(curNounPhrase),
          new SentenceTokenIndexRange(curStartIndex, tokens.size()-1)
      );
      result.add(newPhrase);
    }

    return result;
  }
}
