package com.experimental.nlp;

import com.experimental.documentmodel.Sentence;
import com.experimental.documentmodel.Token;
import com.experimental.languagemodel.Lemma;
import com.experimental.languagemodel.LemmaDB;
import com.google.common.base.Preconditions;

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
          result.addAll(generateAllSubPhrases(curNounPhrase));
        }

        curNounPhrase.clear();
        haveNoun = false;
      }
    }

    if (haveNoun) {
      Preconditions.checkState(curNounPhrase.size() > 0);
      result.addAll(generateAllSubPhrases(curNounPhrase));
    }

    return result;
  }

  private List<NounPhrase> generateAllSubPhrases(List<Token> tokens) {
    List<NounPhrase> result = new ArrayList<NounPhrase>();
    if (!tokens.get(tokens.size()-1).partOfSpeech.isNoun()) {
      return result;
    }

    for (int i = 0; i < tokens.size(); i++) {
      int phraseLength = tokens.size() - i;
      if (phraseLength >= 2 && phraseLength <= 3) {
        List<Token> subPhrase = tokens.subList(i, tokens.size());
        if (countAdjectives(subPhrase) <= 1) {
          result.add(new NounPhrase(tokensToLemmas(subPhrase), lemmaDb));
        }
      }
    }

    return result;
  }

  private int countAdjectives(List<Token> tokens) {
    int result = 0;
    for (Token token : tokens) {
      if (token.partOfSpeech.isAdjective()) {
        result++;
      }
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
