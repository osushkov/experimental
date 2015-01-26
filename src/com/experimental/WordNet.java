package com.experimental;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.experimental.utils.Log;
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

  public void loadWordNet() throws IOException {
    URL url = new URL("file" , null , WORDNET_DICT_PATH) ;

    dict = new Dictionary(url);
//    dict = new RAMDictionary(url, ILoadPolicy.IMMEDIATE_LOAD);
    dict.open();

    stemmer = new WordnetStemmer(dict);
    totalSize = getTotalNumWords();

    List<ISynsetID> s0 = getSynsetsFor("walk", POS.VERB);
    List<ISynsetID> s1 = getSynsetsFor("run", POS.VERB);

    double maxSimilarity = 0.0;
    for (ISynsetID s0Id : s0) {
      for (ISynsetID s1Id : s1) {
        double similarity = getSynsetSimilarity(s0Id, s1Id);
        maxSimilarity = Math.max(maxSimilarity, similarity);
      }
    }

    Log.out(Double.toString(maxSimilarity));

//    String inputWord = "dog";
//    POS pos = POS.NOUN;
//
//    String stemmedWord = stemWord(inputWord, pos);
//    System.out.println("stemmed: " + stemmedWord);
//    if (stemmedWord == null) {
//      System.out.println("Word cannot be stemmed");
//      return;
//    }
//
//    IIndexWord idxWord = dict.getIndexWord(stemmedWord, pos) ;
//    IWordID wordID = idxWord.getWordIDs().get(0);
//    IWord word = dict.getWord(wordID);
//    System.out.println("Lemma = " + word.getLemma());
//
//    ISynset synonyms = word.getSynset();
//
//    outputHypernymHeirarchy(synonyms);

    System.out.println("finished");

    //printAll();
  }

  private String stemWord(String word, POS pos) {
    return stemmer.findStems(word, pos).get(0);
  }



  private void outputHypernymHeirarchy(ISynset synset) {
    List<ISynsetID> hypernyms = synset.getRelatedSynsets(Pointer.HYPERNYM);
    for (ISynsetID hypernymId : hypernyms) {
      ISynset hypernym = dict.getSynset(hypernymId);
      System.out.print("{ ");
      for (IWord word : hypernym.getWords()) {
        System.out.print(word.getLemma() + ",");
      }
      System.out.println(" }");

    }

    for (ISynsetID hypernymId : hypernyms) {
      ISynset hypernym = dict.getSynset(hypernymId);
      outputHypernymHeirarchy(hypernym);
    }
  }

  private List<ISynsetID> getSynsetsFor(String word, POS pos) {
    String stemmedWord = stemWord(word, pos);
    if (stemmedWord == null) {
      Log.out("no synset for " + word);
      return null;
    }

    List<ISynsetID> result = new ArrayList<ISynsetID>();
    IIndexWord idxWord = dict.getIndexWord(stemmedWord, pos);
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
    List<ISynsetID> path0 = getSynsetPathToRoot(s0, new ArrayList<ISynsetID>());
    List<ISynsetID> path1 = getSynsetPathToRoot(s1, new ArrayList<ISynsetID>());

    ISynsetID result = null;
    int index = 0;
    while(index < path0.size() && index < path1.size() && path0.get(index).equals(path1.get(index))) {
      result = path0.get(index);
      index++;
    }

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
    ISynset synset = dict.getSynset(synsetId);
    int size = getSynsetDirectSize(synset);

    List<ISynsetID> hyponyms = synset.getRelatedSynsets(Pointer.HYPONYM);
    for (ISynsetID hyponymId : hyponyms) {
      size += getSynsetSyze(hyponymId);
    }

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

  private void printAll() {
    Iterator<ISynset> iter = dict.getSynsetIterator(POS.VERB);

    int num = 0;
    while(iter.hasNext()) {
      ISynset synset = iter.next();
      System.out.print("{ ");
      for (IWord word : synset.getWords()) {
        System.out.print(word.getLemma() + ",");
        num++;
      }
      System.out.println(" }");
    }

    System.out.println("num : " + num);
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
