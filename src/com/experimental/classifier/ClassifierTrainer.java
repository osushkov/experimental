package com.experimental.classifier;

import com.experimental.Constants;
import com.experimental.documentmodel.WebsiteDocument;
import com.experimental.keywords.KeywordCandidateGenerator;
import com.experimental.languagemodel.Lemma;
import com.experimental.languagemodel.LemmaOccuranceStatsAggregator;
import com.experimental.languagemodel.LemmaQuality;
import com.experimental.languagemodel.NounPhrasesDB;
import com.experimental.utils.Common;
import com.experimental.utils.Log;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.mllib.classification.LogisticRegressionModel;
import org.apache.spark.mllib.classification.LogisticRegressionWithSGD;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.mllib.regression.LabeledPoint;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

/**
 * Created by sushkov on 31/01/15.
 */
public class ClassifierTrainer {

  private final String TRAINING_DATA_FILENAME = "classifier_training_data.txt";

  private static class DocumentKeywordTrainingBundle {
    String documentRootPath = null;
    List<List<String>> documentKeywords = new ArrayList<List<String>>();
  }

  private static class TrainingData {
    public final List<LabeledPoint> oneKeyword = new ArrayList<LabeledPoint>();
    public final List<LabeledPoint> twoKeywords = new ArrayList<LabeledPoint>();
    public final List<LabeledPoint> threeOrMoreKeywords = new ArrayList<LabeledPoint>();
  }

  private final JavaSparkContext sc;
  private final KeywordVectoriser keywordVectoriser;
  private final KeywordCandidateGenerator candidateGenerator;

  public ClassifierTrainer(NounPhrasesDB nounPhraseDb, KeywordVectoriser keywordVectoriser,
                           LemmaOccuranceStatsAggregator lemmaStats) {
    Preconditions.checkNotNull(nounPhraseDb);
    Preconditions.checkNotNull(lemmaStats);

    SparkConf conf = new SparkConf().setAppName("myApp").setMaster("local");
    this.sc = new JavaSparkContext(conf);
    Logger.getLogger("org").setLevel(Level.WARN);
    Logger.getLogger("akka").setLevel(Level.WARN);

    this.keywordVectoriser = Preconditions.checkNotNull(keywordVectoriser);
    this.candidateGenerator = new KeywordCandidateGenerator(nounPhraseDb, lemmaStats);
  }

  public void train() {
    TrainingData trainingData = generateTrainingData();

    trainClassifier(trainingData.oneKeyword, "one_keyword_classifier.txt");
    trainClassifier(trainingData.twoKeywords, "two_keywords_classifier.txt");
    trainClassifier(trainingData.threeOrMoreKeywords, "three_keywords_classifier.txt");
  }

  private void trainClassifier(List<LabeledPoint> trainingPoints, String outputFileName) {
    if (trainingPoints.size() == 0) {
      return;
    }

    JavaRDD< LabeledPoint > trainingData = sc.parallelize(trainingPoints);
    trainingData.cache();

    int numIterations = 500;
    final LogisticRegressionModel model = LogisticRegressionWithSGD.train(trainingData.rdd(), numIterations);
    model.clearThreshold();

    int numPositive = 0;
    int numNegative = 0;
    int numPositiveCorrect = 0;
    int numNegativeCorrect = 0;

    for (LabeledPoint point : trainingPoints) {
      double mr = model.predict(point.features());

      if (point.label() >= 0.5) {
        numPositive++;
        if (mr >= 0.5) {
          numPositiveCorrect++;
        }
      } else {
        numNegative++;
        if (mr < 0.5) {
          numNegativeCorrect++;
        }
      }
    }

    Log.out("training error: " +
        Double.toString(numPositiveCorrect / (double) numPositive) + " " +
        Double.toString(numNegativeCorrect / (double) numNegative));
  }

  private TrainingData generateTrainingData() {
    List<DocumentKeywordTrainingBundle> documentKeywords = null;
    try {
      documentKeywords = loadFromDisk();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }

    TrainingData result = new TrainingData();
    for (DocumentKeywordTrainingBundle bundle : documentKeywords) {
      TrainingData bundleResults = trainingDataFromBundle(bundle);
      result.oneKeyword.addAll(bundleResults.oneKeyword);
      result.twoKeywords.addAll(bundleResults.twoKeywords);
      result.threeOrMoreKeywords.addAll(bundleResults.threeOrMoreKeywords);
    }

    return result;
  }

  private TrainingData trainingDataFromBundle(DocumentKeywordTrainingBundle bundle) {
    WebsiteDocument document = new WebsiteDocument(bundle.documentRootPath);
    List<KeywordCandidateGenerator.KeywordCandidate> candidates = candidateGenerator.generateCandidates(document);
    List<KeywordVector> vectors = keywordVectoriser.vectoriseKeywordCandidates(candidates, document);

    TrainingData result = new TrainingData();
    for (KeywordVector vector : vectors) {
      if (isKeywordPositive(vector.keyword, bundle)) {
        addKeywordVector(1.0, vector, result);
      } else {
        addKeywordVector(0.0, vector, result);
      }
    }

    return result;
  }

  private boolean isKeywordPositive(KeywordCandidateGenerator.KeywordCandidate candidate,
                                    DocumentKeywordTrainingBundle bundle) {
    List<String> candidateStrings = new ArrayList<String>();
    for (Lemma lemma : candidate.phraseLemmas) {
      candidateStrings.add(lemma.lemma);
    }

    for (List<String> positiveSamples : bundle.documentKeywords) {
      if (positiveSamples.equals(candidateStrings)) {
        return true;
      }
    }
    return false;
  }

  private void addKeywordVector(double oneOrZero, KeywordVector vector, TrainingData outData) {
    double[] doubleVec = Common.listOfDoubleToArray(vector.vector);
    LabeledPoint label = new LabeledPoint(oneOrZero, Vectors.dense(doubleVec));

    switch(vector.keyword.phraseLemmas.size()) {
      case 0:
        Log.out("ZERO LEMMAS!");
        break;
      case 1:
        outData.oneKeyword.add(label);
        break;
      case 2:
        outData.twoKeywords.add(label);
        break;
      default:
        outData.threeOrMoreKeywords.add(label);
        break;
    }
  }

  private List<DocumentKeywordTrainingBundle> loadFromDisk() throws IOException {
    File aggregateDataFile = new File(Constants.AGGREGATE_DATA_PATH);
    String trainingDataFilePath = aggregateDataFile.toPath().resolve(TRAINING_DATA_FILENAME).toString();

    File trainingDataFile = new File(trainingDataFilePath);
    if (!trainingDataFile.exists()) {
      return null;
    }

    List<DocumentKeywordTrainingBundle> result = new ArrayList<DocumentKeywordTrainingBundle>();
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(trainingDataFile.getAbsolutePath()));

      DocumentKeywordTrainingBundle current = new DocumentKeywordTrainingBundle();

      String line = br.readLine();
      while(line != null) {
        if (current.documentRootPath == null) {
          current.documentRootPath = line;
          line = br.readLine(); // skip the next line
        } else if (line.length() == 0) {
          result.add(current);
          current= new DocumentKeywordTrainingBundle();
        } else {
          String[] phraseEntries = line.split(" ");
          current.documentKeywords.add(Lists.newArrayList(phraseEntries));
        }

        line = br.readLine();
      }

      result.add(current);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return null;
    } finally {
      if (br != null) {
        br.close();
      }
    }

    return result;
  }

}