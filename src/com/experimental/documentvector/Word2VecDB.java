package com.experimental.documentvector;

import com.experimental.Constants;
import com.experimental.languagemodel.Lemma;
import com.experimental.languagemodel.LemmaDB;
import com.experimental.utils.Log;
import com.google.common.base.Preconditions;

import java.io.*;
import java.util.*;

/**
 * Created by sushkov on 14/01/15.
 */
public class Word2VecDB {
  public static final String WORD2VEC_FILENAME = "word2vec.txt";

  public final int dimensionality;
  private Map<String, ConceptVector> wordMap = new HashMap<String, ConceptVector>();

  public static class WordSimilarityScore {
    public final String word;
    public final double similarity;

    public WordSimilarityScore(String word, double similarity) {
      this.word = Preconditions.checkNotNull(word);
      this.similarity = similarity;
    }
  }

  private static final Comparator<WordSimilarityScore> SIMILARITY_ORDER =
      new Comparator<WordSimilarityScore>() {
        public int compare(WordSimilarityScore e1, WordSimilarityScore e2) {
          return Double.compare(e2.similarity, e1.similarity);
        }
      };

  public Word2VecDB(int dimensionality) {
    Preconditions.checkArgument(dimensionality > 0);
    this.dimensionality = dimensionality;
  }


  public static Word2VecDB tryLoad() {
    File aggregateDataFile = new File(Constants.AGGREGATE_DATA_PATH);
    String wordVecFilePath = aggregateDataFile.toPath().resolve(WORD2VEC_FILENAME).toString();

    File wordVecFile = new File(wordVecFilePath);
    if (!wordVecFile.exists()) {
      return null;
    }

    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(wordVecFile.getAbsolutePath()));
      return readFrom(br);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return null;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private static Word2VecDB readFrom(BufferedReader in) throws IOException {
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
      if (lineTokens.length != (dimensions + 1)) {
        Log.out(line);
        Log.out(lineTokens[0]);
        Log.out(Integer.toString(lineTokens.length) + " " + dimensions);
      }
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

  public List<WordSimilarityScore> getClosestWordsTo(ConceptVector wordVec, int num) {
    Preconditions.checkNotNull(wordVec);

    ConceptVector normalisedWordVec = wordVec.getCopy();
    normalisedWordVec.normalise();

    List<WordSimilarityScore> result = new ArrayList<WordSimilarityScore>();
    for (Map.Entry<String, ConceptVector> storedEntry : wordMap.entrySet()) {
      ConceptVector normalisedStored = storedEntry.getValue().getCopy();
      normalisedStored.normalise();

      double dp = normalisedStored.dotProduct(normalisedWordVec);
      result.add(new WordSimilarityScore(storedEntry.getKey(), dp));
    }

    Collections.sort(result, SIMILARITY_ORDER);
    return result;
  }
}
