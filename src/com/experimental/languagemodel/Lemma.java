package com.experimental.languagemodel;

import com.experimental.documentmodel.Token;
import com.experimental.nlp.POSTag;
import com.experimental.nlp.SimplePOSTag;
import com.google.common.base.Preconditions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Created by sushkov on 11/01/15.
 */
public class Lemma {
  public final String lemma;
  public final SimplePOSTag tag;

  public Lemma(String lemma, SimplePOSTag tag) {
    this.lemma = Preconditions.checkNotNull(lemma).toLowerCase();
    this.tag = Preconditions.checkNotNull(tag);
  }

  public static Lemma fromToken(Token token) {
    Preconditions.checkNotNull(token);
    return new Lemma(token.lemma, token.partOfSpeech.getSimplePOSTag());
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

  public static Lemma readFrom(BufferedReader in) throws IOException {
    Preconditions.checkNotNull(in);

    String line = Preconditions.checkNotNull(in.readLine());
    String[] lineTokens = line.split(" ");
    Preconditions.checkState(lineTokens.length == 2);

    return new Lemma(lineTokens[0], SimplePOSTag.valueOf(lineTokens[1]));
  }

  public void writeTo(BufferedWriter bw) throws IOException {
    Preconditions.checkNotNull(bw);

    bw.write(lemma); bw.write(" ");
    bw.write(tag.toString()); bw.write("\n");
  }
}
