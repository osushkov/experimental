package com.experimental.nlp;

import com.experimental.documentmodel.Token;
import com.experimental.languagemodel.Lemma;
import com.experimental.languagemodel.LemmaDB;
import com.google.common.base.Preconditions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sushkov on 9/01/15.
 */
public class NounPhrase {

  private final List<LemmaDB.LemmaId> phraseLemmas = new ArrayList<LemmaDB.LemmaId>();
  private final LemmaDB lemmaDb;

  public NounPhrase(List<Lemma> phraseLemmas, LemmaDB lemmaDb) {
    for (Lemma lemma : phraseLemmas) {
      LemmaDB.LemmaId lemmaId = lemmaDb.addLemma(lemma);
      Preconditions.checkState(lemmaId != null);

      this.phraseLemmas.add(lemmaId);
    }

    this.lemmaDb = Preconditions.checkNotNull(lemmaDb);
  }

  public static NounPhrase readFrom(BufferedReader in, LemmaDB lemmaDb) throws IOException {
    Preconditions.checkNotNull(in);

    List<Lemma> lemmas = new ArrayList<Lemma>();

    int numPhraseTokens = Integer.parseInt(Preconditions.checkNotNull(in.readLine()));
    for (int i = 0; i < numPhraseTokens; i++) {
      lemmas.add(Lemma.readFrom(in));
    }

    return new NounPhrase(lemmas, lemmaDb);
  }

  public void writeTo(BufferedWriter bw) throws IOException {
    List<Lemma> lemmas = getPhraseLemmas();

    bw.write(Integer.toString(lemmas.size())); bw.write("\n");
    for (Lemma lemma : lemmas) {
      lemma.writeTo(bw);
    }
  }

  public List<Lemma> getPhraseLemmas() {
    List<Lemma> result = new ArrayList<Lemma>();
    for (LemmaDB.LemmaId lemmaId : phraseLemmas) {
      Lemma lemma = lemmaDb.getLemma(lemmaId);
      Preconditions.checkNotNull(lemma);
      result.add(lemma);
    }

    return result;
  }

  public NounPhrase getNounOnlyPhrase() {
    List<Lemma> lemmas = getPhraseLemmas();
    List<Lemma> nounOnly = new ArrayList<Lemma>();
    for (Lemma lemma : lemmas) {
      if (lemma.tag == SimplePOSTag.NOUN) {
        nounOnly.add(lemma);
      }
    }

    return new NounPhrase(nounOnly, lemmaDb);
  }

  public List<LemmaDB.LemmaId> getPhraseLemmaIds() {
    return phraseLemmas;
  }

  @Override
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    for (Lemma lemma : getPhraseLemmas()) {
      buffer.append(lemma.lemma).append("(").append(lemma.tag.toString()).append(") ");
    }
    return buffer.toString();
  }

  public boolean isCompositePhrase() {
    return phraseLemmas.size() > 1;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    NounPhrase that = (NounPhrase) o;
    return phraseLemmas.equals(that.phraseLemmas);
  }

  @Override
  public int hashCode() {
    return phraseLemmas.hashCode();
  }
}
