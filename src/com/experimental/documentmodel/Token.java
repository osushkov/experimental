package com.experimental.documentmodel;

import com.experimental.nlp.POSTag;
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
  public final POSTag partOfSpeech;

  public Token(String raw, String lemma, POSTag partOfSpeech) {
    this.raw = Preconditions.checkNotNull(raw);
    this.lemma = Preconditions.checkNotNull(lemma);
    this.partOfSpeech = Preconditions.checkNotNull(partOfSpeech);
  }

  public void writeTo(BufferedWriter out) throws IOException {
    Preconditions.checkNotNull(out);

    StringBuffer tokenBuffer = new StringBuffer();
    tokenBuffer.append(raw).append(" ");
    tokenBuffer.append(lemma).append(" ");
    tokenBuffer.append(partOfSpeech.toString()).append("\n");

    out.append(tokenBuffer.toString());
  }

  public static Token readFrom(BufferedReader in) throws IOException {
    Preconditions.checkNotNull(in);

    String line = in.readLine();
    if (line == null) {
      throw new IOException("no line in buffer in Token.readFrom");
    }

    String[] splitStrings = line.split(" ");
    Preconditions.checkState(splitStrings.length == 3);

    return new Token(splitStrings[0], splitStrings[1], POSTag.valueOf(splitStrings[2]));
  }

}
