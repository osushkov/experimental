package com.experimental.crawler;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

import java.util.List;

/**
 * Created by sushkov on 2/01/15.
 */
public class CrawlerController {
  private static final String USER_AGENT =
      "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36";

  private final String storageFolderPath;
  private final int numCrawlers;


  public CrawlerController(String storageFolderPath, int numCrawlers) {
    this.storageFolderPath = Preconditions.checkNotNull(storageFolderPath);
    this.numCrawlers = numCrawlers;
    Preconditions.checkArgument(numCrawlers > 0);
  }

  public void crawl(String seedUrl) throws Exception {
    crawl(Lists.newArrayList(seedUrl));
  }

  public void crawl(List<String> seedUrls) throws Exception {
    CrawlConfig config = new CrawlConfig();

    config.setCrawlStorageFolder(storageFolderPath);

    config.setMaxDepthOfCrawling(10);
    config.setMaxPagesToFetch(1);
    config.setIncludeBinaryContentInCrawling(false);
    config.setUserAgentString(USER_AGENT);

    config.setResumableCrawling(false);

    PageFetcher pageFetcher = new PageFetcher(config);
    RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
    RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
    CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);

    for (String seedUrl : seedUrls) {
      controller.addSeed(seedUrl);
    }

    System.out.println("starting the crawl");
    controller.setCustomData(seedUrls);
    controller.start(Crawler.class, numCrawlers);
    System.out.println("finished");
  }
}
