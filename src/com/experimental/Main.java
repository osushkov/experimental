package com.experimental;

import com.experimental.crawler.CrawlerController;
import com.experimental.documentmodel.DocumentNameGenerator;
import com.experimental.documentmodel.SentenceProcessor;
import com.experimental.documentmodel.WikipediaDocumentParser;
import com.experimental.documentmodel.WikipediaDocumentsParser;
import com.experimental.nlp.Demo;
import com.experimental.pageparser.PageParser;

import java.io.IOException;

public class Main {
  public static void main(String[] args) {
    wikiParserDemo();
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

  public static void wikiParserDemo() {
    DocumentNameGenerator documentNameGeneratpr = new DocumentNameGenerator(Constants.DOCUMENTS_OUTPUT_PATH);
    SentenceProcessor sentenceProcessor = new SentenceProcessor();
    WikipediaDocumentsParser wikiParser =
        new WikipediaDocumentsParser(Constants.WIKI_ROOT_PATH, documentNameGeneratpr, sentenceProcessor);
    wikiParser.parseDocuments();

  }
}
