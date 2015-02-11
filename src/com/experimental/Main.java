package com.experimental;

import com.experimental.adgeneration.TopicAggregator;
import com.experimental.classifier.ClassifierTrainer;
import com.experimental.classifier.KeywordVector;
import com.experimental.classifier.KeywordVectoriser;
import com.experimental.classifier.TrainingDataGenerator;
import com.experimental.documentmodel.*;
import com.experimental.documentmodel.Document;
import com.experimental.documentmodel.thirdparty.*;
import com.experimental.documentvector.ConceptVector;
import com.experimental.documentvector.DocumentVectorDB;
import com.experimental.documentvector.DocumentVectoriser;
import com.experimental.documentvector.Word2VecDB;
import com.experimental.keywords.KeyAssociations;
import com.experimental.keywords.KeywordCandidateGenerator;
import com.experimental.keywords.KeywordSanityChecker;
import com.experimental.languagemodel.*;
import com.experimental.nlp.Demo;
import com.experimental.nlp.SimplePOSTag;
import com.experimental.pageparser.PageCrawler;
import com.experimental.pageparser.PageParser;
import com.experimental.sitepage.SitePage;
import com.experimental.utils.Common;
import com.experimental.utils.Log;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.mllib.classification.*;
import org.apache.spark.mllib.linalg.*;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.regression.GeneralizedLinearModel;
import org.apache.spark.mllib.regression.LabeledPoint;

import java.io.*;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
  public static void main(String[] args) {
//    parseWikipediaDocuments();
//    parseWebbaseDocuments();

//    aggregateLemmaQuality();
//    outputConcatenatedLemmatisedDocuments();
//    generateNounAssociations();

//    buildLemmaMorphologiesMap();

//    try {
//      testWord2VecDB();
//    } catch (IOException e) {
//      e.printStackTrace();
//    }

    //stanfordNlpDemo();
//    try {
//      pageCrawler();
//    } catch (IOException e) {
//      e.printStackTrace();
//    }

//    buildLemmaIdfWeights();

//    try {
//      URL main = new URL("http://shit.com/");
//      URI mainUri = main.toURI();
//
//      Log.out(mainUri.resolve("../../fuck").toString());
//    } catch (MalformedURLException e) {
//      e.printStackTrace();
//    } catch (URISyntaxException e) {
//      e.printStackTrace();
//    }

//    aggregateLemmaQuality();

//    generateBasisVector();
//    vectoriseDocuments();
//    findDocumentNearestNeighbours();

//    generateNounPhrases();
//    testKeywordCandidateExtraction();
//    stanfordNlpDemo();

//    try {
//      if (!LemmaMorphologies.instance.tryLoad()) {
//        Log.out("could not load LemmaMorphologies");
//      }
//    } catch (IOException e) {
//      e.printStackTrace();
//      return;
//    }

//    testNounPhraseDB();

    aggregateLemmaVariance();
//    testKeywordVectoriser();

//    try {
//      outputKeywordCandidates();
//    } catch (IOException e) {
//      e.printStackTrace();
//    }

//    trainClassifier();

//    testClassifier();
//    testKeywordCandidateExtraction();

//    testKeywordCandidateExtraction();

    Log.out("FINISHED");
  }

  private static void testTopicAggregator() {
    WebsiteDocument testDocument =
        new WebsiteDocument("/mnt/fastdisk3/documents/website/499/49988AA");

    DocumentVectorDB documentVectorDb = new DocumentVectorDB();
    documentVectorDb.load();

    List<DocumentVectorDB.DocumentSimilarityPair> nearest = documentVectorDb.getNearestDocuments(testDocument, 30);
    List<Document> nearestDocuments = new ArrayList<Document>();

    for (DocumentVectorDB.DocumentSimilarityPair entry : nearest) {
      nearestDocuments.add(entry.document);
    }

    NounAssociations nounAssociations = new NounAssociations();
    try {
      if (!nounAssociations.tryLoad()) {
        Log.out("could not load NounAssociations from disk");
        return;
      }
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    TopicAggregator topicAggregator = new TopicAggregator(nounAssociations);
    topicAggregator.aggregateDocuments(nearestDocuments);
  }

  private static void testClassifier() {
    LogisticRegressionModel model0 = null;
    LogisticRegressionModel model1 = null;
    LogisticRegressionModel model2 = null;

    File aggregateDataFile = new File(Constants.AGGREGATE_DATA_PATH);
    String learnedModelFilePath = aggregateDataFile.toPath().resolve("learned_classifier.txt").toString();

    try {
      FileInputStream fileIn = new FileInputStream(learnedModelFilePath);
      ObjectInputStream in = new ObjectInputStream(fileIn);

      model0 = readModelFrom(in);
      model1 = readModelFrom(in);
      model2 = readModelFrom(in);

      in.close();
      fileIn.close();

    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return;
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }

    LemmaOccuranceStatsAggregator lemmaStatsAggregator = new LemmaOccuranceStatsAggregator();
    try {
      if (!lemmaStatsAggregator.tryLoadFromDisk()) {
        Log.out("could not load LemmaOccuranceStatsAggregator from disk");
        return;
      }
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    LemmaQuality lemmaQuality = new LemmaQuality();
    try {
      if (!lemmaQuality.tryLoadFromDisk()) {
        Log.out("could not load LemmaQuality from disk");
        return;
      }
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    LemmaIDFWeights lemmaIDFWeights = new LemmaIDFWeights(LemmaDB.instance, LemmaMorphologies.instance);
    try {
      if (!lemmaIDFWeights.tryLoad()) {
        Log.out("could not load lemma idf weights");
        return;
      }
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    NounAssociations nounAssociations = new NounAssociations();
    try {
      if (!nounAssociations.tryLoad()) {
        Log.out("could not load NounAssociations from disk");
        return;
      }
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    KeyAssociations keyAssociations = new KeyAssociations(nounAssociations);

    DocumentVectorDB documentVectorDb = new DocumentVectorDB();
    documentVectorDb.load();

    KeywordVectoriser keywordVectoriser =
        new KeywordVectoriser(lemmaStatsAggregator, lemmaQuality, documentVectorDb, lemmaIDFWeights, keyAssociations);

    WebsiteDocument testDocument =
        new WebsiteDocument("/mnt/fastdisk3/documents/website/499/49988AA");

    Set<KeywordCandidateGenerator.KeywordCandidate> candidates = getCandidateKeywords(testDocument);
    List<KeywordVector> vectors = keywordVectoriser.vectoriseKeywordCandidates(candidates, testDocument);

    for (KeywordVector vector : vectors) {
      double[] doubleVec = Common.listOfDoubleToArray(vector.vector);

      if (vector.keyword.phraseLemmas.size() == 1) {
        if (model0.predict(Vectors.dense(doubleVec)) >= 0.5) {
          Log.out("** " + vector.keyword.toString());
        }
      } else if (vector.keyword.phraseLemmas.size() == 2) {
        if (model1.predict(Vectors.dense(doubleVec)) >= 0.5) {
          Log.out("** " + vector.keyword.toString());
        }
      } else if (vector.keyword.phraseLemmas.size() == 3) {
        if (model2.predict(Vectors.dense(doubleVec)) >= 0.85) {
          Log.out("** " + vector.keyword.toString());
        }
      }
    }

  }

  private static LogisticRegressionModel readModelFrom(ObjectInputStream in)
      throws IOException, ClassNotFoundException {
    return (LogisticRegressionModel) in.readObject();
  }


  private static void trainClassifier() {
    final String LEMMA_QUALITY_WIKI_FILENAME = "lemma_quality_wiki.txt";
    final String LEMMA_IDF_WIKI_FILENAME = "lemma_idf_weights_wiki.txt";

    try {
      if (!LemmaMorphologies.instance.tryLoad()) {
        Log.out("could not load LemmaMorphologies");
      }
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    NounPhrasesDB nounPhraseDb = new NounPhrasesDB(LemmaDB.instance, LemmaMorphologies.instance);
    try {
      Log.out("loading NounPhrasesDB...");
      nounPhraseDb.tryLoad();
    } catch (IOException e) {
      e.printStackTrace();
    }
    Log.out("finished loading NounPhrasesDB");

    LemmaOccuranceStatsAggregator lemmaStatsAggregator = new LemmaOccuranceStatsAggregator();
    try {
      if (!lemmaStatsAggregator.tryLoadFromDisk()) {
        Log.out("could not load LemmaOccuranceStatsAggregator from disk");
        return;
      }
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    LemmaQuality lemmaQuality = new LemmaQuality();
    try {
      if (!lemmaQuality.tryLoadFromDisk()) {
        Log.out("could not load LemmaQuality from disk");
        return;
      }
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    LemmaIDFWeights lemmaIDFWeights = new LemmaIDFWeights(LemmaDB.instance, LemmaMorphologies.instance);
    try {
      if (!lemmaIDFWeights.tryLoad()) {
        Log.out("could not load lemma idf weights");
        return;
      }
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    NounAssociations nounAssociations = new NounAssociations();
    try {
      if (!nounAssociations.tryLoad()) {
        Log.out("could not load NounAssociations from disk");
        return;
      }
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    KeyAssociations keyAssociations = new KeyAssociations(nounAssociations);

    DocumentVectorDB documentVectorDb = new DocumentVectorDB();
    documentVectorDb.load();

    KeywordVectoriser keywordVectoriser =
        new KeywordVectoriser(lemmaStatsAggregator, lemmaQuality, documentVectorDb, lemmaIDFWeights, keyAssociations);

    ClassifierTrainer trainer = new ClassifierTrainer(nounPhraseDb, keywordVectoriser, lemmaStatsAggregator);
    final ClassifierTrainer.LearnedModel learnedModel = trainer.train();

    File aggregateDataFile = new File(Constants.AGGREGATE_DATA_PATH);
    String learnedModelFilePath = aggregateDataFile.toPath().resolve("learned_classifier.txt").toString();

    try {
      FileOutputStream fileOut = new FileOutputStream(learnedModelFilePath);
      ObjectOutputStream out = new ObjectOutputStream(fileOut);

      out.writeObject(learnedModel.oneKeywordClassifier);
      out.writeObject(learnedModel.twoKeywordClassifier);
      out.writeObject(learnedModel.threeOrModeKeywordClassifier);

      out.close();
      fileOut.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return;
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
  }

  private static void outputKeywordCandidates() throws IOException {
    final String TRAINING_DATA_FILENAME = "keyword_training_data.txt";

    NounPhrasesDB nounPhraseDb = new NounPhrasesDB(LemmaDB.instance, LemmaMorphologies.instance);

    LemmaOccuranceStatsAggregator lemmaStatsAggregator = new LemmaOccuranceStatsAggregator();
    try {
      if (!lemmaStatsAggregator.tryLoadFromDisk()) {
        Log.out("could not load LemmaOccuranceStatsAggregator from disk");
        return;
      }
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    final TrainingDataGenerator trainingDataGenerator = new TrainingDataGenerator(nounPhraseDb, lemmaStatsAggregator);

    File aggregateDataFile = new File(Constants.AGGREGATE_DATA_PATH);
    String trainingDataFilePath = aggregateDataFile.toPath().resolve(TRAINING_DATA_FILENAME).toString();

    BufferedWriter bw = null;
    try {
      FileWriter fw = new FileWriter(trainingDataFilePath);
      bw = new BufferedWriter(fw);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return;
    }

    final BufferedWriter out = bw;

    List<DocumentNameGenerator.DocumentType> docTypesToProcess =
        Lists.newArrayList(DocumentNameGenerator.DocumentType.WEBSITE);
    DocumentStream documentStream = new DocumentStream(Constants.DOCUMENTS_OUTPUT_PATH);
    documentStream.streamDocuments(docTypesToProcess, new DocumentStream.DocumentStreamOutput() {
      @Override
      public void processDocument(final Document document) {
        if (document instanceof  WebsiteDocument) {
          try {
            trainingDataGenerator.outputTrainingData((WebsiteDocument) document, out);
          } catch (Exception e) {
            return;
          }
        }
      }
    });

    out.close();
  }

  private static void testSpark() {
    SparkConf conf = new SparkConf().setAppName("myApp").setMaster("local");
    JavaSparkContext sc = new JavaSparkContext(conf);

    List<LabeledPoint> trainingList = new ArrayList<LabeledPoint>();

    Random rand = new Random();
    for (int i = 0; i < 100; i++) {
      double x = 2.0 * rand.nextDouble() - 1.0;
      x *= 10.0;
      double y = 2.0 * rand.nextDouble() - 1.0;
      y *= 10.0;

      trainingList.add(new LabeledPoint(y > x ? 1.0 : 0.0, Vectors.dense(x, y)));
    }

    JavaRDD < LabeledPoint > trainingData = sc.parallelize(trainingList);
    trainingData.cache();

    int numIterations = 1000;
    final LogisticRegressionModel model = LogisticRegressionWithSGD.train(trainingData.rdd(), numIterations);
    model.clearThreshold();

    double r = model.predict(Vectors.dense(5.0, 2.0));
    Log.out("predicted: " + r);

    r = model.predict(Vectors.dense(4.8, 5.0));
    Log.out("predicted: " + r);
  }

  private static void testKeywordVectoriser() {
    LemmaOccuranceStatsAggregator lemmaStatsAggregator = new LemmaOccuranceStatsAggregator();
    try {
      if (!lemmaStatsAggregator.tryLoadFromDisk()) {
        Log.out("could not load LemmaOccuranceStatsAggregator from disk");
        return;
      }
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    LemmaQuality lemmaQuality = new LemmaQuality();
    try {
      if (!lemmaQuality.tryLoadFromDisk()) {
        Log.out("could not load LemmaQuality from disk");
        return;
      }
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    LemmaIDFWeights lemmaIDFWeights = new LemmaIDFWeights(LemmaDB.instance, LemmaMorphologies.instance);
    try {
      if (!lemmaIDFWeights.tryLoad()) {
        Log.out("could not load lemma idf weights");
        return;
      }
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    NounAssociations nounAssociations = new NounAssociations();
    try {
      if (!nounAssociations.tryLoad()) {
        Log.out("could not load NounAssociations from disk");
        return;
      }
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    KeyAssociations keyAssociations = new KeyAssociations(nounAssociations);

    DocumentVectorDB documentVectorDb = new DocumentVectorDB();
    documentVectorDb.load();

    KeywordVectoriser keywordVectoriser =
        new KeywordVectoriser(lemmaStatsAggregator, lemmaQuality, documentVectorDb, lemmaIDFWeights, keyAssociations);

    WebsiteDocument testDocument =
        new WebsiteDocument("/home/sushkov/Programming/experimental/experimental/data/documents/website/1E2/1E2810A");

    Set<KeywordCandidateGenerator.KeywordCandidate> candidates = getCandidateKeywords(testDocument);
    List<KeywordVector> vectors = keywordVectoriser.vectoriseKeywordCandidates(candidates, testDocument);

    for (KeywordVector vector : vectors) {
      Log.out(vector.toString());
    }
  }

  private static Set<KeywordCandidateGenerator.KeywordCandidate> getCandidateKeywords(WebsiteDocument document) {
    NounPhrasesDB nounPhraseDb = new NounPhrasesDB(LemmaDB.instance, LemmaMorphologies.instance);
    try {
      Log.out("loading NounPhrasesDB...");
      nounPhraseDb.tryLoad();
    } catch (IOException e) {
      e.printStackTrace();
    }
    Log.out("finished loading NounPhrasesDB");

    LemmaOccuranceStatsAggregator lemmaStatsAggregator = new LemmaOccuranceStatsAggregator();
    try {
      if (!lemmaStatsAggregator.tryLoadFromDisk()) {
        Log.out("could not load LemmaOccuranceStatsAggregator from disk");
        return null;
      }
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }

    WordNet wordnet = new WordNet();
    if (!wordnet.loadWordNet()) {
      Log.out("could not load WordNet");
      return null;
    }

    DocumentVectorDB documentVectorDB = new DocumentVectorDB();
    documentVectorDB.load();

    KeywordSanityChecker sanityChecker = new KeywordSanityChecker(wordnet);

    KeywordCandidateGenerator candidateGenerator =
        new KeywordCandidateGenerator(nounPhraseDb, lemmaStatsAggregator, sanityChecker, documentVectorDB);
    return candidateGenerator.generateCandidates(document);
  }

  private static void testNounPhraseDB() {
    NounPhrasesDB nounPhraseDb = new NounPhrasesDB(LemmaDB.instance, LemmaMorphologies.instance);
    try {
      nounPhraseDb.tryLoad();
    } catch (IOException e) {
      e.printStackTrace();
    }
    Log.out("finished loading");

    List<NounPhrasesDB.NounPhraseEntry> phraseEntries =
        new ArrayList(nounPhraseDb.getLemmaPhrases(new Lemma("arthroscopy", SimplePOSTag.NOUN)));

    Comparator<NounPhrasesDB.NounPhraseEntry> orderFunc =
        new Comparator<NounPhrasesDB.NounPhraseEntry>() {
          public int compare(NounPhrasesDB.NounPhraseEntry e1, NounPhrasesDB.NounPhraseEntry e2) {
            return Double.compare(e2.numOccurances.get(), e1.numOccurances.get());
          }
        };
    phraseEntries.sort(orderFunc);
    Log.out("num entries: " + phraseEntries.size());

    for (NounPhrasesDB.NounPhraseEntry entry : phraseEntries) {
      Log.out(entry.phrase.toString() + " " + entry.numOccurances.get());
    }
  }

  private static void testKeywordCandidateExtraction() {
    NounPhrasesDB nounPhraseDb = new NounPhrasesDB(LemmaDB.instance, LemmaMorphologies.instance);
    try {
      nounPhraseDb.tryLoad();
    } catch (IOException e) {
      e.printStackTrace();
    }
    Log.out("finished loading");

    LemmaOccuranceStatsAggregator lemmaStatsAggregator = new LemmaOccuranceStatsAggregator();
    try {
      if (!lemmaStatsAggregator.tryLoadFromDisk()) {
        Log.out("could not load LemmaOccuranceStatsAggregator from disk");
        return;
      }
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    WordNet wordnet = new WordNet();
    if (!wordnet.loadWordNet()) {
      Log.out("could not load WordNet");
      return;
    }

    DocumentVectorDB documentVectorDB = new DocumentVectorDB();
    documentVectorDB.load();

    KeywordSanityChecker sanityChecker = new KeywordSanityChecker(wordnet);

    KeywordCandidateGenerator candidateGenerator =
        new KeywordCandidateGenerator(nounPhraseDb, lemmaStatsAggregator, sanityChecker, documentVectorDB);

    WebsiteDocument testDocument =
        new WebsiteDocument("/home/sushkov/Programming/experimental/experimental/data/documents/website/1A0/1A02F1F");

    Set<KeywordCandidateGenerator.KeywordCandidate> candidates = candidateGenerator.generateCandidates(testDocument);
    for (KeywordCandidateGenerator.KeywordCandidate candidate : candidates) {
      for (Lemma lemma : candidate.phraseLemmas) {
        System.out.print(lemma.lemma + " ");
      }
      System.out.print("\n");
    }

  }

  private static void generateNounPhrases() {
    Log.out("generateNounPhrases running...");

    try {
      if (!LemmaMorphologies.instance.tryLoad()) {
        Log.out("could not load LemmaMorphologies");
      }
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    final NounPhrasesDB nounPhrasesDb = new NounPhrasesDB(LemmaDB.instance, LemmaMorphologies.instance);
    try {
      if (nounPhrasesDb.tryLoad()) {
        Log.out("loaded NounPhrasesDB from disk");
        return;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    final Executor executor = Executors.newFixedThreadPool(12);
    final AtomicInteger numDocuments = new AtomicInteger(0);
    final Semaphore sem = new Semaphore(0);
    final Random rand = new Random();

    List<DocumentNameGenerator.DocumentType> docTypesToProcess = Lists.newArrayList(
        DocumentNameGenerator.DocumentType.TOPICAL, DocumentNameGenerator.DocumentType.UNRELATED_COLLECTION);
    DocumentStream documentStream = new DocumentStream(Constants.DOCUMENTS_OUTPUT_PATH);
    documentStream.streamDocuments(docTypesToProcess, new DocumentStream.DocumentStreamOutput() {
      @Override
      public void processDocument(final Document document) {
        numDocuments.incrementAndGet();
        executor.execute(new Runnable() {
          @Override
          public void run() {
            try {
              if (rand.nextInt()%2 == 0) {
                for (Sentence sentence : document.getSentences()) {
                  nounPhrasesDb.addSentence(sentence);
                }
              }
            } catch (Throwable e) {
              return;
            } finally {
              sem.release();
            }

            if (rand.nextInt()%5000 == 0) {
              System.gc();
            }
          }
        });
      }
    });

    Log.out("processed all docs");
    for (int i = 0; i < numDocuments.get(); i++) {
      int remaining = numDocuments.get() - i;
      if (remaining%1000 == 0) {
        Log.out("remaining: " + remaining);
      }
      try {
        sem.acquire();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    Log.out("saving");
    try {
      nounPhrasesDb.save();
    } catch (IOException e) {
      e.printStackTrace();
    }

    Log.out("generateNounPhrases finished");

  }

  private static void generateBasisVector() {
    final LemmaIDFWeights lemmaIDFWeights = new LemmaIDFWeights(LemmaDB.instance, LemmaMorphologies.instance);
    try {
      if (!lemmaIDFWeights.tryLoad()) {
        Log.out("could not load lemma idf weights");
        return;
      }
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

//    Word2VecDB word2VecDb = Word2VecDB.tryLoad();
//    if (word2VecDb == null) {
//      Log.out("could not load Word2VecDB");
//      return;
//    }

    WordNet wordnet = new WordNet();
    if (!wordnet.loadWordNet()) {
      Log.out("could not load WordNet");
      return;
    }

    try {
      if (!LemmaMorphologies.instance.tryLoad()) {
        Log.out("could not load LemmaMorphologies");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    LemmaSimilarityMeasure lemmaSimilarityMeasure = new LemmaSimilarityMeasure(wordnet, null);

    final BagOfWeightedLemmas corpusLemmaBag = new BagOfWeightedLemmas();

    List<DocumentNameGenerator.DocumentType> docTypesToProcess =
        Lists.newArrayList(DocumentNameGenerator.DocumentType.WEBSITE);
    DocumentStream documentStream = new DocumentStream(Constants.DOCUMENTS_OUTPUT_PATH);
    documentStream.streamDocuments(docTypesToProcess,
        new DocumentStream.DocumentStreamOutput() {
          @Override
          public void processDocument(final Document document) {
            try {
              corpusLemmaBag.addBag(document.getBagOfLemmas());
            } catch (Throwable e) {
              return;
            }
          }
        });

    BasisVectorGenerator basisVectorGenerator = new BasisVectorGenerator(
        corpusLemmaBag, LemmaMorphologies.instance, lemmaSimilarityMeasure, lemmaIDFWeights, LemmaDB.instance);

    BasisVector basisVector = basisVectorGenerator.buildBasisVector(1200);
    Log.out(basisVector.toString());
    try {
      basisVector.save();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void cssBoxExperiment() {
    PageParser pageParser = new PageParser("http://www.cbdplumbers.com.au/", SentenceProcessor.instance);
    pageParser.parsePage();
  }

  public static void findDocumentNearestNeighbours() {
    final DocumentVectorDB documentVectorDB = new DocumentVectorDB();
    documentVectorDB.load();

    List<DocumentNameGenerator.DocumentType> docTypesToProcess =
        Lists.newArrayList(DocumentNameGenerator.DocumentType.WEBSITE);

    final AtomicInteger numProcessed = new AtomicInteger(0);

    DocumentStream documentStream = new DocumentStream(Constants.DOCUMENTS_OUTPUT_PATH);
    documentStream.streamDocuments(docTypesToProcess,
        new DocumentStream.DocumentStreamOutput() {
          @Override
          public void processDocument(final Document document) {
            try {

              ConceptVector documentVector = document.getConceptVector();
              if (documentVector != null && !Double.isNaN(documentVector.length())) {
                if (numProcessed.get() % 1000 == 0) {
                  List<DocumentVectorDB.DocumentSimilarityPair> similarDocs =
                      documentVectorDB.getNearestDocuments(document, 5);
                  Log.out(document.rootDirectoryPath);
                  for (DocumentVectorDB.DocumentSimilarityPair similarDoc : similarDocs) {
                    Log.out(Double.toString(similarDoc.similarity) + " " + similarDoc.document.rootDirectoryPath);
                  }
                }

                numProcessed.incrementAndGet();
              }
            } catch (Throwable e) {
              e.printStackTrace();
            }
          }
        });
  }

  public static void vectoriseDocuments() {
    Word2VecDB word2VecDb = Word2VecDB.tryLoad();
    if (word2VecDb == null) {
      Log.out("could not load Word2VecDB");
      return;
    }

    WordNet wordnet = new WordNet();
    if (!wordnet.loadWordNet()) {
      Log.out("could not load WordNet");
      return;
    }

    LemmaSimilarityMeasure lemmaSimilarityMeasure = new LemmaSimilarityMeasure(wordnet, word2VecDb);

    BasisVector basisVector = new BasisVector();
    try {
      if (!basisVector.tryLoad()) {
        Log.out("could not load BasisVector");
        return;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    final DocumentVectoriser documentVectoriser = new DocumentVectoriser(basisVector, lemmaSimilarityMeasure);

//    Document doc = new WebsiteDocument("/home/sushkov/Programming/experimental/experimental/data/documents/website/1A0/1A0A28B");
//    ConceptVector vector = documentVectoriser.computeDocumentVector(doc);

    List<DocumentNameGenerator.DocumentType> docTypesToProcess =
        Lists.newArrayList(DocumentNameGenerator.DocumentType.WEBSITE);

    final Executor executor = Executors.newFixedThreadPool(12);
    final AtomicInteger numDocuments = new AtomicInteger(0);
    final Semaphore sem = new Semaphore(0);

    DocumentStream documentStream = new DocumentStream(Constants.DOCUMENTS_OUTPUT_PATH);
    documentStream.streamDocuments(docTypesToProcess,
        new DocumentStream.DocumentStreamOutput() {
          @Override
          public void processDocument(final Document document) {
            numDocuments.incrementAndGet();
            executor.execute(new Runnable() {
              @Override
              public void run() {
                try {
                  ConceptVector vector = documentVectoriser.computeDocumentVector(document);
                  document.setConceptVector(vector);
                  document.save();
                } catch (Throwable e) {
                  e.printStackTrace();
                  return;
                } finally {
                  sem.release();
                }
              }
            });
          }
        });

    for (int i = 0; i < numDocuments.get(); i++) {
      try {
        sem.acquire();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    Log.out("done");
  }

  public static void buildLemmaIdfWeights() {
    final String WIKI_FILENAME = "lemma_idf_weights_wiki.txt";

    List<DocumentNameGenerator.DocumentType> docTypesToProcess =
        Lists.newArrayList(DocumentNameGenerator.DocumentType.TOPICAL);
//        Lists.newArrayList(DocumentNameGenerator.DocumentType.WEBSITE, DocumentNameGenerator.DocumentType.TOPICAL);

    final LemmaIDFWeights lemmaIDFWeights = new LemmaIDFWeights(LemmaDB.instance, LemmaMorphologies.instance);

    final Executor executor = Executors.newFixedThreadPool(12);
    final AtomicInteger numDocuments = new AtomicInteger(0);
    final Semaphore sem = new Semaphore(0);
    final Random rand = new Random();

    DocumentStream documentStream = new DocumentStream(Constants.DOCUMENTS_OUTPUT_PATH);
    documentStream.streamDocuments(docTypesToProcess,
        new DocumentStream.DocumentStreamOutput() {
          @Override
          public void processDocument(final Document document) {
            numDocuments.incrementAndGet();
            executor.execute(new Runnable() {
              @Override
              public void run() {
                try {
                  if (lemmaIDFWeights.isDocumentValid(document)) {
                    lemmaIDFWeights.processDocument(document, 1.0);
//                    if (document instanceof WebsiteDocument) {
//                      lemmaIDFWeights.processDocument(document, 1.0);
//                    } else if (document instanceof TopicalDocument) {
//                      lemmaIDFWeights.processDocument(document, 0.01);
//                    }
                  }
                } catch (Throwable e) {
                  return;
                } finally {
                  sem.release();
                }

                if (rand.nextInt()%5000 == 0) {
                  System.gc();
                }
              }
            });
          }
        });

    Log.out("processed all docs");
    for (int i = 0; i < numDocuments.get(); i++) {
      try {
        sem.acquire();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    try {
      lemmaIDFWeights.save(WIKI_FILENAME);
    } catch (IOException e) {
      e.printStackTrace();
    }

    Log.out("done");
  }

  public static void pageCrawler() throws IOException {
    Log.out("pageCrawler running...");

    File aggregateDataFile = new File(Constants.AGGREGATE_DATA_PATH);
    String siteListPath = aggregateDataFile.toPath().resolve("all_sites.txt").toString();

    List<String> crawlUrls = Lists.newArrayList();

    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(siteListPath));

      String line = br.readLine();
      while (line != null && line.length() > 1) {
        if (!line.startsWith("http://")) {
          line = "http://" + line;
        }
        crawlUrls.add(line);
        line = br.readLine();
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return;
    } finally {
      if (br != null) {
        br.close();
      }
    }

    PageCrawler crawler = new PageCrawler();
    crawler.crawlSites(crawlUrls);

    Log.out("pageCrawler finished");
  }

  public static void pageProcessExperiment() {
    DocumentStream documentStream = new DocumentStream(Constants.DOCUMENTS_OUTPUT_PATH);
    documentStream.streamDocuments(Lists.newArrayList(DocumentNameGenerator.DocumentType.WEBSITE),
        new DocumentStream.DocumentStreamOutput() {
      @Override
      public void processDocument(Document document) {
        WebsiteDocument webDoc = (WebsiteDocument) document;

        Log.out("process document: " + webDoc.getSitePages().size());
        for (SitePage page : webDoc.getSitePages()) {
          for (Sentence sentence : page.header.description) {
            Log.out(sentence.toString());
          }
        }
      }
    });
  }

  public static void stanfordNlpDemo() {
    Demo.runDemo();
  }

  private static void parseWikipediaDocuments() {
    Log.out("parseWikipediaDocuments running...");

    ThirdPartyDocumentParserFactory parserFactory = new ThirdPartyDocumentParserFactory() {
      @Override
      public boolean isValidDocumentFile(String filePath) {
        return filePath.matches(".*wiki_[0-9]*");
      }

      @Override
      public ThirdPartyDocumentParser create(DocumentNameGenerator documentNameGenerator, SentenceProcessor sentenceProcessor) {
        return new WikipediaDocumentParser(documentNameGenerator, sentenceProcessor);
      }
    };

    DocumentNameGenerator documentNameGenerator = new DocumentNameGenerator(Constants.DOCUMENTS_OUTPUT_PATH);
    RecursiveDocumentsParser recursiveParser = new RecursiveDocumentsParser(
        Constants.WIKI_ROOT_PATH, parserFactory, documentNameGenerator, SentenceProcessor.instance);
    recursiveParser.parseDocuments();

    Log.out("parseWikipediaDocuments finished");
  }

  private static void parseWebbaseDocuments() {
    Log.out("parseWebbaseDocuments running...");

    ThirdPartyDocumentParserFactory parserFactory = new ThirdPartyDocumentParserFactory() {
      @Override
      public boolean isValidDocumentFile(String filePath) {
        return filePath.matches(".*txt");
      }

      @Override
      public ThirdPartyDocumentParser create(DocumentNameGenerator documentNameGenerator, SentenceProcessor sentenceProcessor) {
        return new WebbaseDocumentParser(documentNameGenerator, sentenceProcessor);
      }
    };

    DocumentNameGenerator documentNameGenerator = new DocumentNameGenerator(Constants.DOCUMENTS_OUTPUT_PATH);
    RecursiveDocumentsParser recursiveParser = new RecursiveDocumentsParser(
        Constants.WEBBASE_ROOT_PATH, parserFactory, documentNameGenerator, SentenceProcessor.instance);
    recursiveParser.parseDocuments();

    Log.out("parseWebbaseDocuments finished");
  }

  private static void aggregateLemmaVariance() {
    final String WIKI_FILENAME = "global_lemma_occurance_statistics_wiki.txt";

    Log.out("aggregateLemmaVariance running...");

    final LemmaOccuranceStatsAggregator lemmaVarianceAggregator = new LemmaOccuranceStatsAggregator();
    try {
      if (lemmaVarianceAggregator.tryLoadFromDisk(WIKI_FILENAME)) {
        Log.out("loaded LemmaVariances from disk");
        return;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    final Executor executor = Executors.newFixedThreadPool(12);
    final AtomicInteger numDocuments = new AtomicInteger(0);
    final Semaphore sem = new Semaphore(0);
    final Random rand = new Random();

    List<DocumentNameGenerator.DocumentType> docTypesToProcess =
        Lists.newArrayList(DocumentNameGenerator.DocumentType.WEBSITE);

    DocumentStream documentStream = new DocumentStream(Constants.DOCUMENTS_OUTPUT_PATH);
    documentStream.streamDocuments(docTypesToProcess, new DocumentStream.DocumentStreamOutput() {
      @Override
      public void processDocument(final Document document) {
        numDocuments.incrementAndGet();
        executor.execute(new Runnable() {
          @Override
          public void run() {
            try {
              lemmaVarianceAggregator.addDocument(document);
            } catch (Throwable e) {
              return;
            } finally {
              sem.release();
            }

            if (rand.nextInt()%5000 == 0) {
              System.gc();
            }
          }
        });

      }
    });

    Log.out("processed all docs");
    for (int i = 0; i < numDocuments.get(); i++) {
      try {
        sem.acquire();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    try {
      lemmaVarianceAggregator.save(WIKI_FILENAME);
    } catch (IOException e) {
      e.printStackTrace();
    }

    Log.out("aggregateLemmaVariance finished");
  }

  private static void aggregateLemmaQuality() {
    Log.out("aggregateLemmaQuality running...");

    final String WIKI_FILENAME = "lemma_quality_wiki.txt";

    final LemmaQuality lemmaQualityAggregator = new LemmaQuality();
    try {
      if (lemmaQualityAggregator.tryLoadFromDisk(WIKI_FILENAME)) {
        Log.out("loaded LemmaQualityAggregator from disk");
        return;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    final Executor executor = Executors.newFixedThreadPool(12);
    final AtomicInteger numDocuments = new AtomicInteger(0);
    final Semaphore sem = new Semaphore(0);
    final Random rand = new Random();

    List<DocumentNameGenerator.DocumentType> docTypesToProcess =
        Lists.newArrayList(DocumentNameGenerator.DocumentType.TOPICAL);

    DocumentStream documentStream = new DocumentStream(Constants.DOCUMENTS_OUTPUT_PATH);
    documentStream.streamDocuments(docTypesToProcess, new DocumentStream.DocumentStreamOutput() {
      @Override
      public void processDocument(final Document document) {
        numDocuments.incrementAndGet();
        executor.execute(new Runnable() {
          @Override
          public void run() {
            try {
              lemmaQualityAggregator.addDocument(document);
            } catch (Throwable e) {
              return;
            } finally {
              sem.release();
            }

            if (rand.nextInt()%5000 == 0) {
              System.gc();
            }
          }
        });

      }
    });

    Log.out("processed all docs");
    for (int i = 0; i < numDocuments.get(); i++) {
      try {
        sem.acquire();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    try {
      lemmaQualityAggregator.save(WIKI_FILENAME);
    } catch (IOException e) {
      e.printStackTrace();
    }

    Log.out("aggregateLemmaQuality finished");
  }

  /**
   * Goes through the whole document collection and outputs the text of each document to a single large file. Each word
   * is output as its lemma. The main use of the resulting file is to train word2vec.
   */
  private static void outputConcatenatedLemmatisedDocuments() {
    Log.out("outputConcatenatedLemmatisedDocuments running...");

    File aggregateDataFile = new File(Constants.AGGREGATE_DATA_PATH);
    String concatenatedDocumentsPath = aggregateDataFile.toPath().resolve("concatenated_documents.txt").toString();

    BufferedWriter bw = null;
    try {
      FileWriter fw = new FileWriter(concatenatedDocumentsPath);
      bw = new BufferedWriter(fw);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return;
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    if (bw == null) {
      return;
    }

    final BufferedWriter outputWriter = bw;

    DocumentStream documentStream = new DocumentStream(Constants.DOCUMENTS_OUTPUT_PATH);
    documentStream.streamDocuments(new DocumentStream.DocumentStreamOutput() {
      @Override
      public void processDocument(Document document) {
        try {
          document.writeSimplified(outputWriter);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });

    try {
      outputWriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    Log.out("outputConcatenatedLemmatisedDocuments finished");
  }

  private static void generateNounAssociations() {
    Log.out("generateNounAssociations running...");

    final NounAssociations nounAssociations = new NounAssociations();
    try {
      if (nounAssociations.tryLoad()) {
        Log.out("loaded NounAssociations from disk");
        return;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    final Executor executor = Executors.newFixedThreadPool(12);
    final AtomicInteger numDocuments = new AtomicInteger(0);
    final Semaphore sem = new Semaphore(0);

    DocumentStream documentStream = new DocumentStream(Constants.DOCUMENTS_OUTPUT_PATH);
    documentStream.streamDocuments(new DocumentStream.DocumentStreamOutput() {
      @Override
      public void processDocument(final Document document) {
        numDocuments.incrementAndGet();
        executor.execute(new Runnable() {
          @Override
          public void run() {
            nounAssociations.addDocument(document);
            sem.release();
          }
        });
      }
    });

    Log.out("processed all docs");
    for (int i = 0; i < numDocuments.get(); i++) {
      try {
        sem.acquire();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    try {
      nounAssociations.save();
    } catch (IOException e) {
      e.printStackTrace();
    }

    Log.out("generateNounAssociations finished");
  }

  private static void buildLemmaMorphologiesMap() {
    Log.out("buildLemmaMorphologiesMap running...");

    final LemmaMorphologies lemmaMorphologies = LemmaMorphologies.instance;
    try {
      if (lemmaMorphologies.tryLoad()) {
        Log.out("loaded LemmaMorphologies from disk");
        return;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    final Executor executor = Executors.newFixedThreadPool(8);
    final AtomicInteger numDocuments = new AtomicInteger(0);
    final Semaphore sem = new Semaphore(0);

    DocumentStream documentStream = new DocumentStream(Constants.DOCUMENTS_OUTPUT_PATH);
    documentStream.streamDocuments(new DocumentStream.DocumentStreamOutput() {
      @Override
      public void processDocument(final Document document) {
        numDocuments.incrementAndGet();

        executor.execute(new Runnable() {
          @Override
          public void run() {
            for (Sentence sentence : document.getSentences()) {
              for (Token token : sentence.tokens) {
                lemmaMorphologies.addToken(token);
              }
            }
            sem.release();
          }
        });
      }
    });

    Log.out("processed all docs");
    for (int i = 0; i < numDocuments.get(); i++) {
      try {
        sem.acquire();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    try {
      lemmaMorphologies.save();
    } catch (IOException e) {
      e.printStackTrace();
    }

    Log.out("buildLemmaMorphologiesMap finished");
  }

  private static void testYellowPagesCrawler() {
    YellowPagesCrawler crawler = new YellowPagesCrawler();
    Set<String> websiteUrls = crawler.crawlForWebsites();

//    BufferedWriter bw = null;
//    try {
//      FileWriter fw = new FileWriter("site_urls.txt");
//      bw = new BufferedWriter(fw);
//
//      for (String url : websiteUrls) {
//        bw.append(url + "\n");
//      }
//    } catch (FileNotFoundException e) {
//      e.printStackTrace();
//    } catch (IOException e) {
//      e.printStackTrace();
//    }
//
//    try {
//      if (bw != null) {
//        bw.close();
//      }
//    } catch (IOException e) {
//      e.printStackTrace();
//    }
  }
}
