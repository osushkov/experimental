package com.experimental.documentmodel;

import com.google.common.base.Preconditions;

/**
 * Created by sushkov on 4/01/15.
 */
public class WordToken {
  public final String raw;
  public String stemmed = null;
//  public POS partOfSpeech;

  public WordToken(String rawWord) {
    this.raw = Preconditions.checkNotNull(rawWord);
  }
}
