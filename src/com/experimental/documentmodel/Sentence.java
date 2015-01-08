package com.experimental.documentmodel;

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

  public final List<Token> tokens;
  public final double emphasis;
  public final Tree tree;

  public Sentence(List<Token> tokens, Tree tree) {
    this(tokens, tree, DEFAULT_EMPHASIS);
  }

  public Sentence(List<Token> tokens, Tree tree, double emphasis) {
    this.tokens = Preconditions.checkNotNull(tokens);
    this.tree = Preconditions.checkNotNull(tree);
    this.emphasis = emphasis;

    Preconditions.checkArgument(emphasis > 0.0);
  }

  public static Sentence readFrom(BufferedReader in) throws IOException {
    Preconditions.checkNotNull(in);
    return null;
  }

  public void writeTo(BufferedWriter bw) {
    Preconditions.checkNotNull(bw);
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
