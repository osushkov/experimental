package com.experimental.documentvector;

import com.experimental.documentmodel.Document;
import com.experimental.documentmodel.Sentence;
import com.experimental.documentmodel.Token;
import com.experimental.languagemodel.StopWords;
import com.experimental.nlp.POSTag;
import com.google.common.base.Preconditions;

/**
 * Created by sushkov on 15/01/15.
 */
public class DocumentVectoriserImpl implements DocumentVectoriser {

  private final Word2VecDB word2VecDb;

  public DocumentVectoriserImpl(Word2VecDB word2VecDb) {
    this.word2VecDb = Preconditions.checkNotNull(word2VecDb);
  }

  @Override
  public int getDimensionality() {
    return word2VecDb.dimensionality;
  }

  @Override
  public ConceptVector vectoriseDocument(Document document) {
    ConceptVector result = new ConceptVectorImpl(getDimensionality());
    result.setToZero();

    double weightSum = 0.0;

    for (Sentence sentence : document.getSentences()) {
      for (Token token : sentence.tokens) {
        if (includeToken(token)) {
          // TODO: query a token lemmatiser here, in case the NLP library wouldnt lemmatise it properly due
          // to lack of context. The lemmatiser should look at all previously seen lemmas for the raw form of the word
          // and use that if it disagrees with the NLP derived lemma.

          ConceptVector tokenVector = word2VecDb.getWordVector(token.lemma);
          tokenVector.scale(sentence.emphasis);

          result.add(tokenVector);
          weightSum += sentence.emphasis;
        }
      }
    }

    // Normalise the document or just divide it by the sum of weights?

    result.normalise();
//    result.scale(1.0 / weightSum);

    return result;
  }

  private boolean includeToken(Token token) {
    if (StopWords.instance().isStopWord(token.lemma)) {
      return false;
    } else {
      return token.partOfSpeech.isNoun() || token.partOfSpeech.isVerb() || token.partOfSpeech.isAdjective() ||
          token.partOfSpeech.isAdverb() || token.partOfSpeech == POSTag.CD;
    }
  }
}
