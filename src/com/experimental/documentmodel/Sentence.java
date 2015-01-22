package com.experimental.documentmodel;

import com.experimental.nlp.NounPhrase;
import com.google.common.base.Preconditions;
import edu.stanford.nlp.trees.Tree;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sushkov on 4/01/15.
 */
public class Sentence {
  private static final double DEFAULT_EMPHASIS = 1.0;

  public final double emphasis;
  public final List<Token> tokens;

  public Sentence(List<Token> tokens) {
    this(tokens, DEFAULT_EMPHASIS);
  }

  public Sentence(List<Token> tokens, double emphasis) {
    this.tokens = Preconditions.checkNotNull(tokens);
    this.emphasis = emphasis;

    Preconditions.checkArgument(emphasis > 0.0);
  }

  public static Sentence readFrom(BufferedReader in) throws IOException {
    Preconditions.checkNotNull(in);

    String line = Preconditions.checkNotNull(in.readLine());
    double emphasis = Double.parseDouble(line);

    line = Preconditions.checkNotNull(in.readLine());
    int numTokens = Integer.parseInt(line);

    List<Token> tokens = new ArrayList<Token>();
    for (int i = 0; i < numTokens; i++) {
      tokens.add(Token.readFrom(in));
    }

    return new Sentence(tokens, emphasis);
  }

  public void writeTo(BufferedWriter bw) throws IOException {
    Preconditions.checkNotNull(bw);

    bw.write(Double.toString(emphasis)); bw.write("\n");

    bw.write(Integer.toString(tokens.size())); bw.write("\n");
    for (Token token : tokens) {
      token.writeTo(bw);
    }
  }

  public String unrolledText() {
    StringBuffer buf = new StringBuffer();
    for (Token token : tokens) {
      buf.append(token.raw);
      buf.append(" ");
    }
    buf.append(". ");

    return buf.toString();
  }

  @Override
  public String toString() {
    StringBuffer buffer = new StringBuffer();

    for (Token token : tokens) {
      buffer.append(token.raw).append("(").append(token.lemma).append(" ").append(token.partOfSpeech).append(") ");
    }

    return buffer.toString();
  }
}
