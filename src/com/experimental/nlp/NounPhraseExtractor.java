package com.experimental.nlp;

import com.experimental.documentmodel.Sentence;
import com.experimental.documentmodel.Token;
import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sushkov on 9/01/15.
 */
public class NounPhraseExtractor {


  public List<NounPhrase> extractNounPhrases(List<Token> tokens) {
    List<NounPhrase> result = new ArrayList<NounPhrase>();
    List<Token> curNounPhrase = new ArrayList<Token>();
    boolean haveNoun = false;

    for (Token token : tokens) {
      if (token.partOfSpeech.isNoun() || token.partOfSpeech.isAdjective() ||
          token.partOfSpeech == POSTag.DT || token.partOfSpeech == POSTag.CD) {
        if (token.partOfSpeech.isNoun()) {
          haveNoun = true;
        }

        curNounPhrase.add(token);
      } else {
        if (haveNoun) {
          Preconditions.checkState(curNounPhrase.size() > 0);
          result.add(new NounPhrase(curNounPhrase));
        }

        curNounPhrase.clear();
        haveNoun = false;
      }
    }

    if (haveNoun) {
      Preconditions.checkState(curNounPhrase.size() > 0);
      result.add(new NounPhrase(curNounPhrase));
    }

    return result;
  }
}
