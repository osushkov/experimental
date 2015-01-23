package com.experimental.pageparser;

import com.experimental.Constants;
import com.experimental.documentmodel.DocumentNameGenerator;
import com.experimental.documentmodel.SentenceProcessor;
import com.experimental.documentmodel.WebsiteDocument;
import com.experimental.sitepage.SitePage;
import com.experimental.utils.Log;
import com.experimental.utils.TldUtils;
import com.google.common.base.Preconditions;
import javafx.util.Pair;
import org.apache.http.client.utils.URIBuilder;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by sushkov on 20/01/15.
 */
public class PageCrawler {

  private final DocumentNameGenerator documentNameGenerator =
      new DocumentNameGenerator(Constants.DOCUMENTS_OUTPUT_PATH);

  private final Executor executor = Executors.newFixedThreadPool(20);
  private final Semaphore doneSem = new Semaphore(0);
  private final AtomicInteger documentsProcessed = new AtomicInteger(0);

  public void crawlSites(List<String> urls) {
    Preconditions.checkNotNull(urls);

    Set<String> crawledHosts = new HashSet<String>();

    for (String url : urls) {
      URL targetUrl = null;
      try {
        targetUrl = new URL(url);
      } catch (MalformedURLException e) {
        e.printStackTrace();
        continue;
      }

      if (crawledHosts.contains(targetUrl.getHost())) {
        continue;
      }

      if (!TldUtils.shouldProcessUrl(targetUrl)) {
        continue;
      }

      crawledHosts.add(targetUrl.getHost());

      scheduleCrawlFor(url);
    }

    for (int i = 0; i < urls.size(); i++) {
      try {
        doneSem.acquire();
      } catch (InterruptedException e) {
        e.printStackTrace();
        return;
      }
    }
    Log.out("done crawling");
  }

  private void scheduleCrawlFor(final String url) {
    executor.execute(new Runnable() {
      @Override
      public void run() {
        processSite(url);
        doneSem.release();

        int processed = documentsProcessed.incrementAndGet();
        if (processed %100 == 0) {
          System.gc();
          Log.out("PROCESSED: " + processed);
        }

      }
    });

  }

  private void processSite(String url) {
    Log.out("processSite: " + url);

    String documentName = documentNameGenerator.getAndStoreNewDocumentName(DocumentNameGenerator.DocumentType.WEBSITE);
    Path documentPath =
        new File(documentNameGenerator.getAbsoluteRootPath(DocumentNameGenerator.DocumentType.WEBSITE, documentName))
            .toPath().toAbsolutePath().resolve(documentName);

    WebsiteDocument parentDocument = new WebsiteDocument(documentPath.toString());

    SitePage frontPage = null;
    try {
      frontPage = parsePage(new URL(url));
    } catch (Throwable e) {
      e.printStackTrace();
      return;
    }

    if (frontPage == null) {
      return;
    }
    parentDocument.setFrontPage(frontPage);

    List<SitePage.Link> allLinks = new ArrayList<SitePage.Link>();
    List<SitePage> childPages = new ArrayList<SitePage>();
    Set<URL> visitedPages = new HashSet<URL>();

    for (SitePage.Link link : frontPage.outgoingLinks) {
      allLinks.add(link);

      SitePage childPage = null;

      if (!visitedPages.contains(link.destination) && visitedPages.size() < 25) {
        try {
          childPage = parsePage(link.destination);
          visitedPages.add(link.destination);
        } catch (Throwable e) {
          e.printStackTrace();
          continue;
        }
      }

      if (childPage == null) {
        continue;
      }
      for (SitePage.Link childLink : childPage.outgoingLinks) {
        allLinks.add(childLink);
      }

      childPages.add(childPage);
      parentDocument.addChildPage(childPage);
    }

    for (SitePage childPage : childPages) {
      for (SitePage.Link link : allLinks) {
        URI childUri = null;
        try {
          childUri = new URI(childPage.url);
        } catch (URISyntaxException e) {
          e.printStackTrace();
          continue;
        }
        if (childUri.equals(link.destination)) {
          childPage.incomingLinks.addAll(link.linkText);
        }
      }
    }

    parentDocument.buildDocument();

    try {
      parentDocument.save();
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
  }

  private SitePage parsePage(URL url) throws IOException {
    org.jsoup.nodes.Document doc = Jsoup.connect(url.toString())
        .followRedirects(true)
        .timeout(2000)
        .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36")
        .get();

    URL redirectedUrl = new URL(doc.location());
    Elements meta = doc.select("html head meta");
    if (meta.attr("http-equiv").toLowerCase().contains("refresh")) {
      String redirect = meta.attr("content").split("=")[1].trim();
      redirectedUrl = new URL(redirectedUrl, redirect);
    }

    PageParser pageParser = new PageParser(redirectedUrl.toString(), SentenceProcessor.instance);
    return pageParser.parsePage();
  }

}
