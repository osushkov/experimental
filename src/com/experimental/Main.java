package com.experimental;

import com.experimental.crawler.CrawlerController;
import com.experimental.documentmodel.*;
import com.experimental.languagemodel.LemmaDB;
import com.experimental.languagemodel.LemmaQualityAggregator;
import com.experimental.languagemodel.NounAssociations;
import com.experimental.nlp.Demo;
import com.experimental.pageparser.PageParser;
import com.experimental.utils.Log;

import java.io.*;

public class Main {
  private static final LemmaDB lemmaDB = new LemmaDB();

  public static void main(String[] args) {
    //parseWikipediaDocuments();
//    aggregateLemmaQuality();
//    outputConcatenatedLemmatisedDocuments();
//    generateNounAssociations();

    parseWebbaseDocuments();
//    stanfordNlpDemo();
//    cssBoxExperiment();
  }

  public static void webCrawlExperiment() {
    CrawlerController crawlerController = new CrawlerController("crawlData/", 5);
    try {
      crawlerController.crawl("http://www.familylawyers.net.au");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void wordNetExperiment() {
    WordNet wordnet = new WordNet();
    try {
      wordnet.loadWordNet();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void cssBoxExperiment() {
    PageParser pageParser = new PageParser();
    pageParser.parsePage("http://www.familylawyers.net.au/contact", "page.html");
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
    SentenceProcessor sentenceProcessor = new SentenceProcessor();
    RecursiveDocumentsParser recursiveParser =
        new RecursiveDocumentsParser(Constants.WIKI_ROOT_PATH, parserFactory, documentNameGenerator, sentenceProcessor);
    recursiveParser.parseDocuments(12);

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
    SentenceProcessor sentenceProcessor = new SentenceProcessor();
    RecursiveDocumentsParser recursiveParser =
        new RecursiveDocumentsParser(Constants.WEBBASE_ROOT_PATH, parserFactory, documentNameGenerator, sentenceProcessor);
    recursiveParser.parseDocuments(4);

    Log.out("parseWebbaseDocuments finished");
  }

  private static void aggregateLemmaQuality() {
    Log.out("aggregateLemmaQuality running...");

    final LemmaQualityAggregator lemmaQualityAggregator = new LemmaQualityAggregator(lemmaDB);
    try {
      if (lemmaQualityAggregator.tryLoadFromDisk()) {
        Log.out("loaded LemmaQualityAggregator from disk");
        return;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    DocumentStream documentStream = new DocumentStream(Constants.DOCUMENTS_OUTPUT_PATH);
    documentStream.streamDocuments(new DocumentStream.DocumentStreamOutput() {
      @Override
      public void processDocument(Document document) {
        lemmaQualityAggregator.addDocument(document);
      }
    });

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

    final NounAssociations nounAssociations = new NounAssociations(lemmaDB);
    try {
      if (nounAssociations.tryLoad()) {
        Log.out("loaded NounAssociations from disk");
        return;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    DocumentStream documentStream = new DocumentStream(Constants.DOCUMENTS_OUTPUT_PATH);
    documentStream.streamDocuments(new DocumentStream.DocumentStreamOutput() {
      @Override
      public void processDocument(Document document) {
        nounAssociations.addDocument(document);
      }
    });

    try {
      nounAssociations.save();
    } catch (IOException e) {
      e.printStackTrace();
    }

    Log.out("generateNounAssociations finished");
  }
}
