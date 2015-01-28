package com.experimental.keywords;

import com.experimental.languagemodel.Lemma;
import com.experimental.languagemodel.NounAssociations;
import com.experimental.utils.Log;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.List;

/**
 * Created by sushkov on 28/01/15.
 */
public class KeyAssociations {

  private static final List<String> ASSOCIATIVE_WORDS = Lists.newArrayList(
      "hire", "buy", "sell", "rent", "lease", "purchase");

  private final NounAssociations nounAssociations;

  public KeyAssociations() {
    this.nounAssociations = new NounAssociations();
  }

  public void init() {
    try {
      if (!nounAssociations.tryLoad()) {
        Log.out("Could not load NounAssociations");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public double getAssociationStrength(Lemma noun) {
    return 0.0;
  }



}
