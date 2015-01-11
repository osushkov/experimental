package com.experimental.languagemodel;

import com.experimental.documentmodel.Token;
import com.experimental.nlp.POSTag;
import com.google.common.base.Preconditions;

/**
 * Created by sushkov on 11/01/15.
 */
public class Lemma {
  public final String lemma;
  public final POSTag tag;

  public Lemma(String lemma, POSTag tag) {
    this.lemma = Preconditions.checkNotNull(lemma);
    this.tag = Preconditions.checkNotNull(tag);
  }

  public static Lemma fromToken(Token token) {
    Preconditions.checkNotNull(token);
    return new Lemma(token.lemma, token.partOfSpeech);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Lemma lemma1 = (Lemma) o;

    if (!lemma.equals(lemma1.lemma)) return false;
    if (tag != lemma1.tag) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = lemma.hashCode();
    result = 31 * result + tag.hashCode();
    return result;
  }
}
