package com.experimental.languagemodel;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * Created by sushkov on 18/01/15.
 */
public class MorphologyDB {

  public static class MorphologyId {
    public final int id;

    public MorphologyId(int id) {
      this.id = id;
    }
  }

  private int curId = 0;
  private final BiMap<String, MorphologyId> morphologyMap = HashBiMap.create();

  public synchronized MorphologyId addMorphology(String word) {
    Preconditions.checkNotNull(word);

    MorphologyId morphologyId = morphologyMap.get(word);
    if (morphologyId == null) {
      MorphologyId newId = new MorphologyId(curId++);
      morphologyMap.put(word, newId);
      return newId;
    } else {
      return morphologyId;
    }
  }

  public synchronized MorphologyId getMorphologyId(String word) {
    Preconditions.checkNotNull(word);
    return morphologyMap.get(word);
  }

  public synchronized String getMorphology(MorphologyId id) {
    Preconditions.checkNotNull(id);
    Preconditions.checkArgument(id.id >= 0 && id.id < curId);

    return morphologyMap.inverse().get(id);
  }

  public static final MorphologyDB instance = new MorphologyDB();
}
