package com.experimental.languagemodel;

import com.experimental.Constants;
import com.experimental.languagemodel.LemmaDB.LemmaId;
import com.google.common.base.Preconditions;

import java.io.*;
import java.util.*;

/**
 * Created by sushkov on 11/01/15.
 */
public class NounAssociation {
  // TODO: add function to prune associations if they are extremely unlikely.

  public static class Association {
    public final LemmaId associatedLemma;
    public int weight;

    Association(LemmaId associatedLemma) {
      this.associatedLemma = Preconditions.checkNotNull(associatedLemma);
      this.weight = 0;
    }
  }

  private static final Comparator<Association> WEIGHT_ORDER =
      new Comparator<Association>() {
        public int compare(Association e1, Association e2) {
          return Integer.compare(e2.weight, e1.weight);
        }
      };

  public final LemmaId targetNoun;
  private final LemmaDB lemmaDB;

  private final Map<LemmaId, Association> verbAssociations = new HashMap<LemmaId, Association>();
  private final Map<LemmaId, Association> adjectiveAssociations = new HashMap<LemmaId, Association>();

  public NounAssociation(LemmaId targetNoun, LemmaDB lemmaDB) {
    this.targetNoun = Preconditions.checkNotNull(targetNoun);
    this.lemmaDB = Preconditions.checkNotNull(lemmaDB);
  }

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

  public static NounAssociation readFrom(BufferedReader in, LemmaDB lemmaDB) throws IOException {
    Lemma targetNoun = Lemma.readFrom(in);
    NounAssociation result = new NounAssociation(lemmaDB.addLemma(targetNoun), lemmaDB);

    int numVerbAssociations = Integer.parseInt(Preconditions.checkNotNull(in.readLine()));
    for (int i = 0; i < numVerbAssociations; i++) {
      Lemma verbLemma = Lemma.readFrom(in);
      LemmaId verbLemmaId = lemmaDB.addLemma(verbLemma);

      Association newAssociations = new Association(verbLemmaId);
      newAssociations.weight = Integer.parseInt(Preconditions.checkNotNull(in.readLine()));
      Preconditions.checkState(newAssociations.weight > 0);

      result.verbAssociations.put(verbLemmaId, newAssociations);
    }

    int numAdjectiveAssociations = Integer.parseInt(Preconditions.checkNotNull(in.readLine()));
    for (int i = 0; i < numAdjectiveAssociations; i++) {
      Lemma adjectiveLemma = Lemma.readFrom(in);
      LemmaId adjectiveLemmaId = lemmaDB.addLemma(adjectiveLemma);

      Association newAssociations = new Association(adjectiveLemmaId);
      newAssociations.weight = Integer.parseInt(Preconditions.checkNotNull(in.readLine()));
      Preconditions.checkState(newAssociations.weight > 0);

      result.adjectiveAssociations.put(adjectiveLemmaId, newAssociations);
    }

    return result;
  }

  public void writeTo(BufferedWriter bw) throws IOException {
    Lemma targetLemma = lemmaDB.getLemma(targetNoun);
    targetLemma.writeTo(bw);

    bw.write(Integer.toString(verbAssociations.size()) + "\n");

    List<Association> sortedVerbAssociations = new ArrayList<Association>(verbAssociations.values());
    Collections.sort(sortedVerbAssociations, WEIGHT_ORDER);

    for (Association verbAssociation : sortedVerbAssociations) {
      Lemma verbLemma = lemmaDB.getLemma(verbAssociation.associatedLemma);
      verbLemma.writeTo(bw);
      bw.write(Integer.toString(verbAssociation.weight) + "\n");
    }


    bw.write(Integer.toString(adjectiveAssociations.size()) + "\n");

    List<Association> sortedAdjectiveAssociations = new ArrayList<Association>(adjectiveAssociations.values());
    Collections.sort(sortedAdjectiveAssociations, WEIGHT_ORDER);

    for (Association adjectiveAssociation : sortedAdjectiveAssociations) {
      Lemma verbLemma = lemmaDB.getLemma(adjectiveAssociation.associatedLemma);
      verbLemma.writeTo(bw);
      bw.write(Integer.toString(adjectiveAssociation.weight) + "\n");
    }
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
