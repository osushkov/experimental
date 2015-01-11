package com.experimental.languagemodel;

import com.experimental.nlp.POSTag;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

/**
 * Created by sushkov on 11/01/15.
 */
public class InformativePosTags {

  private static final ImmutableSet<POSTag> includedTags = ImmutableSet.of(
      POSTag.FW,
      POSTag.JJ, POSTag.JJR, POSTag.JJS,
      POSTag.NN, POSTag.NNP, POSTag.NNPS, POSTag.NNS,
      POSTag.RB, POSTag.RBR, POSTag.RBS, POSTag.RP,
      POSTag.SYM,
      POSTag.VB, POSTag.VBD, POSTag.VBG, POSTag.VBN, POSTag.VBP, POSTag.VBZ);

  public static boolean isInformative(POSTag tag) {
    return includedTags.contains(Preconditions.checkNotNull(tag));
  }
}
