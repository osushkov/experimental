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
}
