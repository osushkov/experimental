package com.experimental;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.experimental.languagemodel.Lemma;
import com.experimental.nlp.SimplePOSTag;
import com.experimental.utils.Log;
import com.google.common.base.Preconditions;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.RAMDictionary;
import edu.mit.jwi.data.ILoadPolicy;
import edu.mit.jwi.item.*;
import edu.mit.jwi.morph.IStemmer;
import edu.mit.jwi.morph.WordnetStemmer;

public class WordNet {
  private static final String WORDNET_DICT_PATH = "/home/sushkov/Programming/Ads/Libraries/Wordnet/dict3.1";

  private IDictionary dict = null;
  private IStemmer stemmer = null;
  private int totalSize = 0;

  private Map<ISynsetID, Integer> synsetSizeCache = new ConcurrentHashMap<ISynsetID, Integer>();
  private Map<ISynsetID, List<ISynsetID>> pathToRootCache = new ConcurrentHashMap<ISynsetID, List<ISynsetID>>();

  public boolean loadWordNet() {
    try {
      URL url = new URL("file" , null , WORDNET_DICT_PATH);
//      dict = new Dictionary(url);
      dict = new RAMDictionary(url, ILoadPolicy.IMMEDIATE_LOAD);
      dict.open();
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }

    stemmer = new WordnetStemmer(dict);
    totalSize = getTotalNumWords();

    return true;
  }

  public double getLemmaSimilarity(Lemma lemma0, Lemma lemma1) {
    Preconditions.checkNotNull(lemma0);
    Preconditions.checkNotNull(lemma1);

    if (lemma0.tag == SimplePOSTag.OTHER || lemma1.tag == SimplePOSTag.OTHER) {
      return 0.0;
    }

    POS pos0 = getPos(lemma0.tag);
    POS pos1 = getPos(lemma1.tag);

    String stemmed0 = stemWord(lemma0.lemma, pos0);
    String stemmed1 = stemWord(lemma1.lemma, pos1);

    if (stemmed0 == null || stemmed1 == null) {
      return 0.0;
    }

    List<ISynsetID> s0 = getSynsetsFor(stemmed0, pos0);
    List<ISynsetID> s1 = getSynsetsFor(stemmed1, pos1);

    if (s0 == null || s1 == null) {
      return 0.0;
    }

    double sum = 0.0;
    int num = 0;
    double maxSimilarity = 0.0;
    for (ISynsetID s0Id : s0) {
      for (ISynsetID s1Id : s1) {
        sum += getSynsetSimilarity(s0Id, s1Id);
        num++;
      }
    }

    return num == 0 ? 0.0 : sum / num;
  }

  private POS getPos(SimplePOSTag posTag) {
    switch(posTag) {
      case ADJECTIVE: return POS.ADJECTIVE;
      case ADVERB: return POS.ADVERB;
      case NOUN: return POS.NOUN;
      case VERB: return POS.VERB;
      default: return null;
    }
  }

  private String stemWord(String word, POS pos) {
    List<String> stems = stemmer.findStems(word, pos);
    if (stems.isEmpty()) {
      return null;
    } else {
      return stems.get(0);
    }
  }

  private List<ISynsetID> getSynsetsFor(String word, POS pos) {
    String stemmedWord = stemWord(word, pos);
    if (stemmedWord == null) {
      return null;
    }

    List<ISynsetID> result = new ArrayList<ISynsetID>();
    IIndexWord idxWord = dict.getIndexWord(stemmedWord, pos);
    if (idxWord == null) {
      return null;
    }
    for (IWordID wordId : idxWord.getWordIDs()) {
      result.add(dict.getWord(wordId).getSynset().getID());
    }
    return result;
  }

  private double getSynsetSimilarity(ISynsetID s0, ISynsetID s1) {
    ISynsetID lcs = findLowestCommonSynset(s0, s1);
    if (lcs == null) {
      return 0.0;
    }

    double pLcs = getSynsetSyze(lcs) / (double) totalSize;
    double pS0 = getSynsetSyze(s0) / (double) totalSize;
    double pS1 = getSynsetSyze(s1) / (double) totalSize;

    return 2.0 * Math.log(pLcs) / (Math.log(pS0) + Math.log(pS1));
  }

  private ISynsetID findLowestCommonSynset(ISynsetID s0, ISynsetID s1) {
    List<ISynsetID> path0 = getSynsetPathToRoot(s0);
    List<ISynsetID> path1 = getSynsetPathToRoot(s1);

    ISynsetID result = null;
    int index = 0;
    while(index < path0.size() && index < path1.size() && path0.get(index).equals(path1.get(index))) {
      result = path0.get(index);
      index++;
    }

    return result;
  }

  private List<ISynsetID> getSynsetPathToRoot(ISynsetID synsetId) {
    if (pathToRootCache.containsKey(synsetId)) {
      return pathToRootCache.get(synsetId);
    }

    List<ISynsetID> result = getSynsetPathToRoot(synsetId, new ArrayList<ISynsetID>());
    pathToRootCache.putIfAbsent(synsetId, result);
    return result;
  }

  private List<ISynsetID> getSynsetPathToRoot(ISynsetID synsetId, List<ISynsetID> pathSoFar) {
    ISynset synset = dict.getSynset(synsetId);
    List<ISynsetID> hypernyms = synset.getRelatedSynsets(Pointer.HYPERNYM);

    pathSoFar.add(0, synsetId);

    if (hypernyms.isEmpty()) {
      return pathSoFar;
    } else{
      return getSynsetPathToRoot(hypernyms.get(0), pathSoFar);
    }
  }


  private int getSynsetSyze(ISynsetID synsetId) {
    if (synsetSizeCache.containsKey(synsetId)) {
      return synsetSizeCache.get(synsetId);
    }

    ISynset synset = dict.getSynset(synsetId);
    int size = getSynsetDirectSize(synset);

    List<ISynsetID> hyponyms = synset.getRelatedSynsets(Pointer.HYPONYM);
    for (ISynsetID hyponymId : hyponyms) {
      size += getSynsetSyze(hyponymId);
    }

    synsetSizeCache.putIfAbsent(synsetId, size);
    return size;
  }

  private int getSynsetDirectSize(ISynset synset) {
    return synset.getWords().size();
  }

  private int getSynsetDirectSize(ISynsetID synsetId) {
    ISynset synset = dict.getSynset(synsetId);
    return synset.getWords().size();
  }

  private boolean isLeafNode(ISynsetID synsetId) {
    return isLeadNode(dict.getSynset(synsetId));
  }

  private boolean isLeadNode(ISynset synset) {
    return synset.getRelatedSynsets(Pointer.HYPONYM).isEmpty();
  }

  private int getTotalNumWords() {
    return getTotalNumWords(POS.ADJECTIVE) +
        getTotalNumWords(POS.ADVERB) +
        getTotalNumWords(POS.VERB) +
        getTotalNumWords(POS.NOUN);
  }

  private int getTotalNumWords(POS pos) {
    Iterator<ISynset> iter = dict.getSynsetIterator(pos);
    int num = 0;
    while(iter.hasNext()) {
      ISynset synset = iter.next();
      num += getSynsetDirectSize(synset);
    }
    return num;
  }

}
