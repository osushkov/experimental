package com.experimental.languagemodel;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sushkov on 11/01/15.
 */
public class LemmaDB {

  public static class LemmaId {
    public final int id;

    public LemmaId(int id) {
      this.id = id;
    }
  }

  private int curId = 0;
  private final BiMap<Lemma, LemmaId> lemmaMap = HashBiMap.create();

  public synchronized LemmaId addLemma(Lemma lemma) {
    Preconditions.checkNotNull(lemma);

    LemmaId lemmaId = lemmaMap.get(lemma);
    if (lemmaId == null) {
      LemmaId newId = new LemmaId(curId++);
      lemmaMap.put(lemma, newId);
      return newId;
    } else {
      return lemmaId;
    }
  }

  public synchronized LemmaId getLemmaId(Lemma lemma) {
    Preconditions.checkNotNull(lemma);
    return lemmaMap.get(lemma);
  }

  public synchronized Lemma getLemma(LemmaId id) {
    Preconditions.checkNotNull(id);
    Preconditions.checkArgument(id.id >= 0 && id.id < curId);

    return lemmaMap.inverse().get(id);
  }
}
