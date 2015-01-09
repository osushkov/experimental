package com.experimental.documentmodel;

import com.experimental.nlp.NounPhrase;
import com.google.common.base.Preconditions;
import edu.stanford.nlp.trees.Tree;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

/**
 * Created by sushkov on 4/01/15.
 */
public class Sentence {
  private static final double DEFAULT_EMPHASIS = 1.0;

  public final double emphasis;
  public final List<Token> tokens;
  public final List<NounPhrase> nounPhrases;

  public Sentence(List<Token> tokens, List<NounPhrase> nounPhrases) {
    this(tokens, nounPhrases, DEFAULT_EMPHASIS);
  }

  public Sentence(List<Token> tokens, List<NounPhrase> nounPhrases, double emphasis) {
    this.tokens = Preconditions.checkNotNull(tokens);
    this.nounPhrases = Preconditions.checkNotNull(nounPhrases);
    this.emphasis = emphasis;

    Preconditions.checkArgument(emphasis > 0.0);
  }

  public static Sentence readFrom(BufferedReader in) throws IOException {
    Preconditions.checkNotNull(in);
    return null;
  }

  public void writeTo(BufferedWriter bw) throws IOException {
    Preconditions.checkNotNull(bw);

    bw.write(Double.toString(emphasis)); bw.write("\n");

    bw.write(Integer.toString(tokens.size())); bw.write("\n");
    for (Token token : tokens) {
      token.writeTo(bw);
    }

    bw.write(Integer.toString(nounPhrases.size())); bw.write("\n");
    for (NounPhrase nounPhrase : nounPhrases) {
      nounPhrase.writeTo(bw);
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
}
