package com.experimental.documentvector;

import com.experimental.utils.Log;
import com.google.common.base.Preconditions;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by sushkov on 14/01/15.
 */
public class Word2VecDB {
  public static final String WORD2VEC_FILENAME = "word2vec.txt";

  private final int dimensionality;
  private Map<String, ConceptVector> wordMap = new HashMap<String, ConceptVector>();

  public Word2VecDB(int dimensionality) {
    Preconditions.checkArgument(dimensionality > 0);
    this.dimensionality = dimensionality;
  }

  public static Word2VecDB readFrom(BufferedReader in) throws IOException {
    Preconditions.checkNotNull(in);

    String line = Preconditions.checkNotNull(in.readLine());
    String[] lineTokens = line.split(" ");
    Preconditions.checkState(lineTokens.length == 2);

    int numWords = Integer.parseInt(lineTokens[0]);
    int dimensions = Integer.parseInt(lineTokens[1]);

    Word2VecDB result = new Word2VecDB(dimensions);

    for (int i = 0; i < numWords; i++) {
      line = Preconditions.checkNotNull(in.readLine());
      lineTokens = line.split(" ");
      Preconditions.checkState(lineTokens.length == (dimensions + 1));

      String word = lineTokens[0];
      ConceptVector wordVec = new ConceptVectorImpl(dimensions);

      for (int dim = 0; dim < dimensions; dim++) {
        wordVec.setValue(dim, Double.parseDouble(lineTokens[dim+1]));
      }

      result.addWordVector(word, wordVec);
    }

    return result;
  }

  public void addWordVector(String word, ConceptVector vector) {
    Preconditions.checkNotNull(word);
    Preconditions.checkNotNull(vector);
    Preconditions.checkArgument(vector.dimensions() == dimensionality);

    wordMap.put(word.toLowerCase(), vector);
  }

  public ConceptVector getWordVector(String word) {
    Preconditions.checkNotNull(word);

    return wordMap.get(word.toLowerCase());
  }
}
