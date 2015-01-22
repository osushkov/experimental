package com.experimental.nlp;

/**
 * Created by sushkov on 12/01/15.
 */
public enum SimplePOSTag {
  NOUN,
  ADJECTIVE,
  VERB,
  ADVERB,
  OTHER;

  public POSTag toPOSTag() {
    if (this == NOUN) {
      return POSTag.NN;
    } else if (this == ADJECTIVE) {
      return POSTag.JJ;
    } else if (this == VERB) {
      return POSTag.VB;
    } else if (this == ADVERB) {
      return POSTag.RB;
    } else {
      return POSTag.OTHER;
    }
  }
}
