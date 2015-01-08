package com.experimental.documentmodel;

import com.google.common.base.Preconditions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Created by sushkov on 6/01/15.
 */
public class Token {
  public final String raw;
  public final String lemma;
  public final String partOfSpeech;
  public final String namedEntityTag;

  public Token(String raw, String lemma, String partOfSpeech, String namedEntityTag) {
    this.raw = Preconditions.checkNotNull(raw);
    this.lemma = Preconditions.checkNotNull(lemma);
    this.partOfSpeech = Preconditions.checkNotNull(partOfSpeech);
    this.namedEntityTag = Preconditions.checkNotNull(namedEntityTag);
  }

  public void writeTo(BufferedWriter out) throws IOException {

  }

  public static Document readFrom(BufferedReader in) throws IOException {
    return null;
  }

}
