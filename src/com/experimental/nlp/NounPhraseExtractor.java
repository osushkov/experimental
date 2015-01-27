package com.experimental.nlp;

import com.experimental.documentmodel.Sentence;
import com.experimental.documentmodel.Token;
import com.experimental.languagemodel.Lemma;
import com.experimental.languagemodel.LemmaDB;
import com.google.common.base.Preconditions;
import edu.stanford.nlp.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sushkov on 9/01/15.
 */
public class NounPhraseExtractor {
  private final LemmaDB lemmaDb;

  public NounPhraseExtractor(LemmaDB lemmaDb) {
    this.lemmaDb = Preconditions.checkNotNull(lemmaDb);
  }

  public List<NounPhrase> extractNounPhrases(Sentence sentence) {
    List<NounPhrase> result = new ArrayList<NounPhrase>();

    List<Token> curNounPhrase = new ArrayList<Token>();
    boolean haveNoun = false;

    for (int i = 0; i < sentence.tokens.size(); i++) {
      Token token = sentence.tokens.get(i);

      if (token.partOfSpeech.isNoun() || token.partOfSpeech.isAdjective()) {
        if (token.partOfSpeech.isNoun()) {
          haveNoun = true;
        }

        curNounPhrase.add(token);
      } else {
        if (haveNoun) {
          Preconditions.checkState(curNounPhrase.size() > 0);
          result.add(new NounPhrase(tokensToLemmas(curNounPhrase), lemmaDb));
        }

        curNounPhrase.clear();
        haveNoun = false;
      }
    }

    if (haveNoun) {
      Preconditions.checkState(curNounPhrase.size() > 0);
      result.add(new NounPhrase(tokensToLemmas(curNounPhrase), lemmaDb));
    }

    return result;
  }

  private List<Lemma> tokensToLemmas(List<Token> tokens) {
    List<Lemma> result = new ArrayList<Lemma>();
    for (Token token : tokens) {
      result.add(Lemma.fromToken(token));
    }
    return result;
  }
}
