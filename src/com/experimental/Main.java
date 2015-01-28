package com.experimental;

import com.experimental.documentmodel.*;
import com.experimental.documentmodel.Document;
import com.experimental.documentmodel.thirdparty.*;
import com.experimental.documentvector.ConceptVector;
import com.experimental.documentvector.DocumentVectorDB;
import com.experimental.documentvector.DocumentVectoriser;
import com.experimental.documentvector.Word2VecDB;
import com.experimental.keywords.KeywordCandidateGenerator;
import com.experimental.languagemodel.*;
import com.experimental.nlp.Demo;
import com.experimental.nlp.SimplePOSTag;
import com.experimental.pageparser.PageCrawler;
import com.experimental.pageparser.PageParser;
import com.experimental.sitepage.SitePage;
import com.experimental.utils.Log;
import com.google.common.collect.Lists;

import java.io.*;
import java.util.List;
import java.util.Random;
import java.util.Set;
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
    //findDocumentNearestNeighbours();

    generateNounPhrases();
//    testKeywordCandidateExtraction();
//    stanfordNlpDemo();

    Log.out("FINISHED");
  }

  private static void testKeywordCandidateExtraction() {
    KeywordCandidateGenerator candidateGenerator = new KeywordCandidateGenerator();

    WebsiteDocument testDocument =
        new WebsiteDocument("/home/sushkov/Programming/experimental/experimental/data/documents/website/1A0/1A02F1F");

    List<KeywordCandidateGenerator.KeywordCandidate> candidates = candidateGenerator.generateCandidates(testDocument);
//    for (KeywordCandidateGenerator.KeywordCandidate candidate : candidates) {
//      for (Lemma lemma : candidate.phraseLemmas) {
//        System.out.print(lemma.lemma + " ");
//      }
//      System.out.print("\n");
//    }

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

    BasisVector basisVector = basisVectorGenerator.buildBasisVector(600);
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
    List<DocumentNameGenerator.DocumentType> docTypesToProcess =
        Lists.newArrayList(DocumentNameGenerator.DocumentType.WEBSITE, DocumentNameGenerator.DocumentType.TOPICAL);

    final LemmaIDFWeights lemmaIDFWeights = new LemmaIDFWeights(LemmaDB.instance, LemmaMorphologies.instance);

    final Executor executor = Executors.newFixedThreadPool(8);
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
                    if (document instanceof WebsiteDocument) {
                      lemmaIDFWeights.processDocument(document, 1.0);
                    } else if (document instanceof TopicalDocument) {
                      lemmaIDFWeights.processDocument(document, 0.01);
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
      try {
        sem.acquire();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    try {
      lemmaIDFWeights.save();
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

  private static void aggregateLemmaQuality() {
    Log.out("aggregateLemmaQuality running...");

    final LemmaQuality lemmaQualityAggregator = new LemmaQuality();
    try {
      if (lemmaQualityAggregator.tryLoadFromDisk()) {
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
      lemmaQualityAggregator.save();
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
