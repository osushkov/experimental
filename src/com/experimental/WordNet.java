package com.experimental;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

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

  public void loadWordNet() throws IOException {
    URL url = new URL("file" , null , WORDNET_DICT_PATH) ;

    dict = new Dictionary(url);
//    dict = new RAMDictionary(url, ILoadPolicy.IMMEDIATE_LOAD);
    dict.open();

    stemmer = new WordnetStemmer(dict);

    String inputWord = "dog";
    POS pos = POS.NOUN;

    String stemmedWord = stemWord(inputWord, pos);
    System.out.println("stemmed: " + stemmedWord);
    if (stemmedWord == null) {
      System.out.println("Word cannot be stemmed");
      return;
    }

    IIndexWord idxWord = dict.getIndexWord(stemmedWord, pos) ;
    IWordID wordID = idxWord.getWordIDs().get(0);
    IWord word = dict.getWord(wordID);
    System.out.println("Lemma = " + word.getLemma());

    ISynset synonyms = word.getSynset();
    outputHypernymHeirarchy(synonyms);

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

}
