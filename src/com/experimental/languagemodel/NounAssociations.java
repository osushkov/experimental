package com.experimental.languagemodel;

import com.experimental.Constants;
import com.experimental.documentmodel.Document;
import com.experimental.documentmodel.Sentence;
import com.experimental.documentmodel.Token;
import com.experimental.languagemodel.LemmaDB.LemmaId;
import com.experimental.utils.Log;
import com.google.common.base.Preconditions;

import java.io.*;
import java.util.*;

/**
 * Created by sushkov on 13/01/15.
 */
public class NounAssociations {

  private static final String NOUN_ASSOCIATIONS_FILENAME = "noun_associations.txt";

  private final Map<LemmaId, NounAssociation> nounAssociations = new HashMap<LemmaId, NounAssociation>();
  private final Map<LemmaId, Integer> associsationOccurances = null;
  private final LemmaDB lemmaDB;

  public NounAssociations(LemmaDB lemmaDB) {
    this.lemmaDB = Preconditions.checkNotNull(lemmaDB);
  }

  public void addDocument(Document document) {
    Preconditions.checkNotNull(document);

    for (Sentence sentence : document.getSentences()) {
      processSentence(sentence);
    }
  }

  private void processSentence(Sentence sentence) {
    for (int i = 0; i < sentence.tokens.size(); i++) {
      Token cur = sentence.tokens.get(i);

      if (cur.partOfSpeech.isAdjective() || cur.partOfSpeech.isVerb()) {
        int nounIndex = findNextNoun(sentence, i+1);
        if (nounIndex != -1 && (nounIndex - i) < 4) {
          while (nounIndex < sentence.tokens.size() && sentence.tokens.get(nounIndex).partOfSpeech.isNoun()) {
            associate(sentence.tokens.get(nounIndex), cur);
            nounIndex++;
          }
        }
      }
    }
  }

  private int findNextNoun(Sentence sentence, int startIndex) {
    for (int i = startIndex; i < sentence.tokens.size(); i++) {
      Token cur = sentence.tokens.get(i);
      if (cur.partOfSpeech.isNoun()) {
        return i;
      }
    }
    return -1;
  }

  public void associate(Token noun, Token word) {
    Preconditions.checkNotNull(noun);
    Preconditions.checkNotNull(word);

    if (!noun.partOfSpeech.isNoun() ||
        !(word.partOfSpeech.isAdjective() || word.partOfSpeech.isVerb())) {
      return;
    }

    LemmaId nounId = lemmaDB.addLemma(Lemma.fromToken(noun));
    LemmaId wordId = lemmaDB.addLemma(Lemma.fromToken(word));

    NounAssociation curEntry = nounAssociations.get(nounId);
    if (curEntry == null) {
      curEntry = new NounAssociation(nounId, lemmaDB);
      nounAssociations.put(nounId, curEntry);
    }

    if (word.partOfSpeech.isAdjective()) {
      curEntry.associateAdjective(wordId);
    } else if (word.partOfSpeech.isVerb()) {
      curEntry.associateVerb(wordId);
    } else {
      assert(false);
    }
  }

  public NounAssociation getAssociations(Token noun) {
    Preconditions.checkNotNull(noun);
    if (!noun.partOfSpeech.isNoun()) {
      return null;
    }

    LemmaId nounLemmaId = lemmaDB.addLemma(Lemma.fromToken(noun));
    return nounAssociations.get(nounLemmaId);
  }

  public NounAssociation getAssociations(LemmaId lemmaId) {
    Preconditions.checkNotNull(lemmaId);
    return nounAssociations.get(lemmaId);
  }

  public boolean tryLoad() throws IOException {
    File aggregateDataFile = new File(Constants.AGGREGATE_DATA_PATH);
    String associationsFilePath = aggregateDataFile.toPath().resolve(NOUN_ASSOCIATIONS_FILENAME).toString();

    File associationsFile = new File(associationsFilePath);
    if (!associationsFile.exists()) {
      return false;
    }

    nounAssociations.clear();
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(associationsFile.getAbsolutePath()));

      int numEntries = Integer.parseInt(Preconditions.checkNotNull(br.readLine()));
      for (int i = 0; i < numEntries; i++) {
        NounAssociation newAssociations = NounAssociation.readFrom(br, lemmaDB);
        nounAssociations.put(newAssociations.targetNoun, newAssociations);
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return false;
    } finally {
      if (br != null) {
        br.close();
      }
    }

    return true;
  }

  public void save() throws IOException {
    File aggregateDataFile = new File(Constants.AGGREGATE_DATA_PATH);
    String associationsFilePath = aggregateDataFile.toPath().resolve(NOUN_ASSOCIATIONS_FILENAME).toString();

    BufferedWriter bw = null;
    try {
      try {
        FileWriter fw = new FileWriter(associationsFilePath);
        bw = new BufferedWriter(fw);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
        return;
      }

      bw.write(Integer.toString(nounAssociations.values().size()) + "\n");
      for (NounAssociation info : nounAssociations.values()) {
        info.writeTo(bw);
      }
    } finally {
      if (bw != null) {
        bw.close();
      }
    }
  }
}
