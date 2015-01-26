package com.experimental.languagemodel;

import com.experimental.Constants;
import com.experimental.documentmodel.BagOfWeightedLemmas;
import com.experimental.utils.Log;
import com.google.common.base.Preconditions;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sushkov on 26/01/15.
 */
public class BasisVectorGenerator {
  private static final String LEMMA_QUALITY_FILENAME = "lemma_quality.txt";

  private final BagOfWeightedLemmas corpusLemmaBag;
  private final LemmaMorphologies lemmaMorphologies;
  private final LemmaSimilarityMeasure similarityMeasure;
  private final LemmaIDFWeights lemmaIdfWeights;
  private final LemmaDB lemmaDb;

  private List<LemmaQuality.LemmaQualityInfo> lemmaQualityList = new ArrayList<LemmaQuality.LemmaQualityInfo>();

  public BasisVectorGenerator(BagOfWeightedLemmas corpusLemmaBag,
                              LemmaMorphologies lemmaMorphologies,
                              LemmaSimilarityMeasure similarityMeasure,
                              LemmaIDFWeights lemmaIdfWeights,
                              LemmaDB lemmaDb) {
    this.corpusLemmaBag = Preconditions.checkNotNull(corpusLemmaBag);
    this.lemmaMorphologies = Preconditions.checkNotNull(lemmaMorphologies);
    this.similarityMeasure = Preconditions.checkNotNull(similarityMeasure);
    this.lemmaIdfWeights = Preconditions.checkNotNull(lemmaIdfWeights);
    this.lemmaDb = Preconditions.checkNotNull(lemmaDb);
  }

  public BasisVector buildBasisVector(int size) {
    try {
      if (!tryLoadingLemmaQuality()) {
        Log.out("Could not load lemma quality file");
        return null;
      }
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }

    List<BasisVector.BasisElement> basisElements = new ArrayList<BasisVector.BasisElement>();

    for (LemmaQuality.LemmaQualityInfo lemmaQuality : lemmaQualityList) {
      if (basisElements.size() == size) {
        break;
      }

      Lemma lemma = lemmaDb.getLemma(lemmaQuality.lemmaId);
      if (isLemmaSuitable(lemmaQuality.lemmaId, basisElements)) {
        double weight = lemmaIdfWeights.getLemmaWeight(lemma);
        basisElements.add(new BasisVector.BasisElement(lemma, weight));
      }
    }

    Preconditions.checkState(basisElements.size() == size);
    return new BasisVector(basisElements);
  }

  private boolean isLemmaSuitable(LemmaDB.LemmaId lemmaId, List<BasisVector.BasisElement> basisElementsSoFar) {
    Lemma lemma = lemmaDb.getLemma(lemmaId);

    if (lemmaMorphologies.numLemmaOccurances(lemma) < 10000) {
      Log.out("too few morphology occurances: " + lemma.lemma);
      return false;
    }

    if (corpusLemmaBag.getBag().get(lemma).weight < 100.0) {
      Log.out("too few corpus occurances: " + lemma.lemma);
      return false;
    }

    for (BasisVector.BasisElement existingElement : basisElementsSoFar) {
      double similarity = similarityMeasure.getLemmaSimilarity(lemma, existingElement.lemma);
      if (similarity > 0.8) {
        Log.out("too similar: " + lemma.lemma + "," + existingElement.lemma.lemma + " " + similarity);
        return false;
      }
    }

    return true;
  }

  private boolean tryLoadingLemmaQuality() throws IOException {
    File aggregateDataFile = new File(Constants.AGGREGATE_DATA_PATH);
    String lemmaQualityFilePath = aggregateDataFile.toPath().resolve(LEMMA_QUALITY_FILENAME).toString();

    File lemmaQualityFile = new File(lemmaQualityFilePath);
    if (!lemmaQualityFile.exists()) {
      return false;
    }

    lemmaQualityList.clear();

    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(lemmaQualityFile.getAbsolutePath()));

      int numEntries = Integer.parseInt(Preconditions.checkNotNull(br.readLine()));
      Preconditions.checkState(numEntries > 0);

      for (int i = 0; i < numEntries; i++) {
        lemmaQualityList.add(loadLemmaQualityInfo(br));
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

  private LemmaQuality.LemmaQualityInfo loadLemmaQualityInfo(BufferedReader br) throws IOException {
    String line = Preconditions.checkNotNull(br.readLine());
    String[] lineTokens = line.split(" ");
    Preconditions.checkState(lineTokens.length == 3);

    double quality = Double.parseDouble(lineTokens[0]);
    double sumOfSquares = Double.parseDouble(lineTokens[1]);
    double sum = Double.parseDouble(lineTokens[2]);

    Lemma lemma = Lemma.readFrom(br);
    LemmaDB.LemmaId lemmaId = lemmaDb.addLemma(lemma);

    LemmaQuality.LemmaQualityInfo result = new LemmaQuality.LemmaQualityInfo(lemmaId);
    result.quality = quality;
    result.sumOfSquares = sumOfSquares;
    result.sum = sum;

    return result;
  }

}
