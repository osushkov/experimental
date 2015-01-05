package com.experimental.documentmodel;

import com.google.common.base.Preconditions;

import java.util.List;

/**
 * Created by sushkov on 4/01/15.
 */
public class Sentence {
  public final List<WordToken> tokens;

  public Sentence(List<WordToken> tokens) {
    this.tokens = Preconditions.checkNotNull(tokens);
  }

  public String unrolledText() {
    StringBuffer buf = new StringBuffer();
    for (WordToken token : tokens) {
      buf.append(token.raw);
      buf.append(" ");
    }
    buf.append(". ");

    return buf.toString();
  }
}
