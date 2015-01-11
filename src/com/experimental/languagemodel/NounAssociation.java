package com.experimental.languagemodel;

import com.google.common.base.Preconditions;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by sushkov on 11/01/15.
 */
public class NounAssociation {

  public static class Association {
    public final LemmaId associatedLemma;
    public int weight;

    Association(LemmaId associatedLemma) {
      this.associatedLemma = Preconditions.checkNotNull(associatedLemma);
      this.weight = 0;
    }
  }

  private final Map<LemmaId, Association> verbAssociations = new HashMap<LemmaId, Association>();
  private final Map<LemmaId, Association> adjectiveAssociations = new HashMap<LemmaId, Association>();

  public void associateVerb(LemmaId verbId) {
    Preconditions.checkNotNull(verbId);
    associate(verbAssociations, verbId);
  }

  public void associateAdjective(LemmaId adjectiveId) {
    Preconditions.checkNotNull(adjectiveId);
    associate(adjectiveAssociations, adjectiveId);
  }

  public Collection<Association> getVerbAssociations() {
    return verbAssociations.values();
  }

  public Collection<Association> getAdjectiveAssociations() {
    return adjectiveAssociations.values();
  }

  private static void associate(Map<LemmaId, Association> associationMap, LemmaId id) {
    Association association = associationMap.get(id);
    if (association == null) {
      association = new Association(id);
      associationMap.put(id, association);
    }

    association.weight++;
  }
}
