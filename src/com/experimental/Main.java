package com.experimental;

import com.experimental.crawler.CrawlerController;
import com.experimental.nlp.Demo;
import com.experimental.pageparser.PageParser;

import java.io.IOException;

public class Main {
  public static void main(String[] args) {
    stanfordNlpDemo();
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

}
