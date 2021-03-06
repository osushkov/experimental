package com.experimental.keywords;

import com.experimental.languagemodel.Lemma;
import com.experimental.languagemodel.LemmaDB;
import com.experimental.languagemodel.NounAssociation;
import com.experimental.languagemodel.NounAssociations;
import com.experimental.utils.Log;
import com.google.common.base.Preconditions;
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

  public KeyAssociations(NounAssociations nounAssociations) {
    this.nounAssociations = Preconditions.checkNotNull(nounAssociations);
  }

  public double getKeyAssociationStrength(Lemma noun) {
    NounAssociation associations = nounAssociations.getAssociations(noun);
    if (associations == null) {
      return 0.0;
    }

    double totalWeight = 0.0;
    double importantWeight = 0;

    for (NounAssociation.Association association : associations.getVerbAssociations()) {
      if (isAssociationCounted(association)) {
        totalWeight += association.weight;
      }

      if (isImportantAssociation(association)) {
        importantWeight += association.weight;
      }
    }

    return totalWeight > Double.MIN_VALUE ? importantWeight / totalWeight : 0.0;
  }

  private boolean isAssociationCounted(NounAssociation.Association association) {
    Lemma lemma = lemmaDb.getLemma(association.associatedLemma);
    if (lemma == null) {
      return false;
    } else {
      return !lemma.lemma.equals("be") &&
          !lemma.lemma.equals("have") &&
          !lemma.lemma.equals("do");
    }
  }

  private boolean isImportantAssociation(NounAssociation.Association association) {
    Lemma lemma = lemmaDb.getLemma(association.associatedLemma);
    if (lemma == null) {
      return false;
    } else {
      for (String important : ASSOCIATIVE_WORDS) {
        if (important.equals(lemma.lemma)) {
          return true;
        }
      }
      return false;
    }
  }

}
