package com.experimental.keywords;

import com.experimental.languagemodel.Lemma;
import com.experimental.languagemodel.LemmaDB;
import com.experimental.languagemodel.NounAssociation;
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
      "hire", "employ", "buy", "sell", "rent", "lease", "purchase");

  private final NounAssociations nounAssociations;
  private final LemmaDB lemmaDb = LemmaDB.instance;

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

  public double getKeyAssociationStrength(Lemma noun) {
    NounAssociation associations = nounAssociations.getAssociations(noun);
    if (associations == null) {
      return 0.0;
    }

//    for (NounAssociation.Association association : associations.getVerbAssociations()) {
//      if (association.)
//    }

    return 0.0;
  }

  private boolean isAssociationCounted(NounAssociation.Association association) {
    Lemma lemma = lemmaDb.getLemma(association.associatedLemma);
    if (lemma == null) {
      return false;
    } else {
      return !lemma.lemma.equalsIgnoreCase("be");
    }
  }



}
