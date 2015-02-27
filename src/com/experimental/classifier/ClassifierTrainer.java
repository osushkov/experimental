package com.experimental.classifier;

import com.experimental.Constants;
import com.experimental.documentmodel.DocumentDB;
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
import org.apache.spark.mllib.classification.*;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.mllib.regression.GeneralizedLinearModel;
import org.apache.spark.mllib.regression.LabeledPoint;

import java.io.*;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

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

  public static class LearnedModel {
    public GeneralizedLinearModel oneKeywordClassifier = null;
    public GeneralizedLinearModel twoKeywordClassifier = null;
    public GeneralizedLinearModel threeOrModeKeywordClassifier = null;
  }

  private final JavaSparkContext sc;
  private final KeywordVectoriser keywordVectoriser;
  private final KeywordCandidateGenerator candidateGenerator;

  public ClassifierTrainer(KeywordCandidateGenerator candidateGenerator, KeywordVectoriser keywordVectoriser) {
    SparkConf conf = new SparkConf()
        .setAppName("myApp")
        .setMaster("local");
    this.sc = new JavaSparkContext(conf);
    Logger.getLogger("org").setLevel(Level.ERROR);
    Logger.getLogger("akka").setLevel(Level.ERROR);

    this.keywordVectoriser = Preconditions.checkNotNull(keywordVectoriser);
    this.candidateGenerator = Preconditions.checkNotNull(candidateGenerator);
  }

  public LearnedModel train() {
    System.gc();
    Log.out("generating training data");
    final TrainingData trainingData = generateTrainingData();
    System.gc();

    final Executor executor = Executors.newFixedThreadPool(4);
    final AtomicInteger numModels = new AtomicInteger(0);
    final Semaphore sem = new Semaphore(0);

    final LearnedModel learnedModel = new LearnedModel();

    Log.out("training: " + trainingData.oneKeyword.size() + " " + trainingData.twoKeywords.size() + " " +
        trainingData.threeOrMoreKeywords.size());
    executor.execute(new Runnable() {
      @Override
      public void run() {
        numModels.incrementAndGet();
        learnedModel.oneKeywordClassifier = trainClassifier(trainingData.oneKeyword, "one_keyword_classifier.txt", 0.5);
        sem.release();
      }
    });

    executor.execute(new Runnable() {
      @Override
      public void run() {
        numModels.incrementAndGet();
        learnedModel.twoKeywordClassifier = trainClassifier(trainingData.twoKeywords, "two_keywords_classifier.txt", 0.5);
        sem.release();
      }
    });

    executor.execute(new Runnable() {
      @Override
      public void run() {
        numModels.incrementAndGet();
        learnedModel.threeOrModeKeywordClassifier =
            trainClassifier(trainingData.threeOrMoreKeywords, "three_keywords_classifier.txt", 0.5);
        sem.release();
      }
    });

    for (int i = 0; i < numModels.get(); i++) {
      try {
        sem.acquire();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    Log.out("finished training");
    System.gc();
    testModel(learnedModel);

    return learnedModel;
  }

  private void testModel(LearnedModel learnedModel) {
    List<DocumentKeywordTrainingBundle> documentKeywords = null;
    try {
      documentKeywords = loadFromDisk();
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    for (DocumentKeywordTrainingBundle bundle : documentKeywords) {
      WebsiteDocument document = DocumentDB.instance.createWebsiteDocument(bundle.documentRootPath);
      Log.out(document.getSitePages().get(0).url);
      Set<KeywordCandidateGenerator.KeywordCandidate> candidates = candidateGenerator.generateCandidates(document);
      List<KeywordVector> vectors = keywordVectoriser.vectoriseKeywordCandidates(candidates, document);

      for (KeywordVector vector : vectors) {
        double[] doubleVec = Common.listOfDoubleToArray(vector.vector);

        boolean isGood = false;
        double certainty = 0.0;
        if (vector.keyword.phraseLemmas.size() == 1) {
          certainty = learnedModel.oneKeywordClassifier.predict(Vectors.dense(doubleVec));
          isGood = certainty >= 0.5;
        } else if (vector.keyword.phraseLemmas.size() == 2) {
          certainty = learnedModel.twoKeywordClassifier.predict(Vectors.dense(doubleVec));
          isGood = certainty >= 0.5;
        } else if (vector.keyword.phraseLemmas.size() >= 3) {
          certainty = learnedModel.threeOrModeKeywordClassifier.predict(Vectors.dense(doubleVec));
          isGood = certainty >= 0.5;
        }

        if (isGood) {
          Log.out("= " + vector.keyword.toString() + " \t" + Double.toString(certainty));
        }
      }
    }
  }

  private GeneralizedLinearModel trainClassifier(List<LabeledPoint> trainingPoints, String outputFileName,
                                              double positiveThreshold) {
    if (trainingPoints.size() == 0) {
      return null;
    }

    JavaRDD< LabeledPoint > trainingData = sc.parallelize(trainingPoints);
    trainingData.cache();

    int numIterations = 5000;
//    final SVMModel model = SVMWithSGD.train(trainingData.rdd(), numIterations);
    final LogisticRegressionModel model = LogisticRegressionWithSGD.train(trainingData.rdd(), numIterations);
//    final NaiveBayesModel model = NaiveBayes.train(trainingData.rdd());
//    model.clearThreshold();

    int numPositive = 0;
    int numNegative = 0;
    int numPositiveCorrect = 0;
    int numNegativeCorrect = 0;

    for (LabeledPoint point : trainingPoints) {
      double mr = model.predict(point.features());

      if (point.label() >= 0.5) {
        numPositive++;
        if (mr >= positiveThreshold) {
          numPositiveCorrect++;
        }
      } else {
        numNegative++;
        if (mr < positiveThreshold) {
          numNegativeCorrect++;
        }
      }
    }

    Log.out(outputFileName + " positive: " + numPositiveCorrect + "/" + numPositive);
    Log.out(outputFileName + " negative: " + numNegativeCorrect + "/" + numNegative);
    Log.out(outputFileName + " training error: " +
        Double.toString(numPositiveCorrect / (double) numPositive) + " " +
        Double.toString(numNegativeCorrect / (double) numNegative));

    return model;
  }

  private TrainingData generateTrainingData() {
    List<DocumentKeywordTrainingBundle> documentKeywords = null;
    try {
      documentKeywords = loadFromDisk();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }

    int numProcessed = 0;

    TrainingData result = new TrainingData();
    for (DocumentKeywordTrainingBundle bundle : documentKeywords) {
      TrainingData bundleResults = trainingDataFromBundle(bundle);
      result.oneKeyword.addAll(bundleResults.oneKeyword);
      result.twoKeywords.addAll(bundleResults.twoKeywords);
      result.threeOrMoreKeywords.addAll(bundleResults.threeOrMoreKeywords);

      numProcessed++;

      if (numProcessed % 10 == 0) {
        Log.out("runnning gc");
        System.gc();
      }
    }

    return result;
  }

  private TrainingData trainingDataFromBundle(DocumentKeywordTrainingBundle bundle) {
    WebsiteDocument document = DocumentDB.instance.createWebsiteDocument(bundle.documentRootPath);
    Set<KeywordCandidateGenerator.KeywordCandidate> candidates = candidateGenerator.generateCandidates(document);
    List<KeywordVector> vectors = keywordVectoriser.vectoriseKeywordCandidates(candidates, document);

    TrainingData result = new TrainingData();
    for (KeywordVector vector : vectors) {
      if (isKeywordPositive(vector.keyword, bundle)) {
        addKeywordVector(1.0, vector, result);
//          Log.out("+ " + vector.toString());
      } else {
        addKeywordVector(0.0, vector, result);
        addKeywordVector(0.0, vector, result);
//          Log.out("- " + vector.toString());
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
