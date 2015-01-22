package com.experimental.pageparser;

import com.experimental.Constants;
import com.experimental.documentmodel.DocumentNameGenerator;
import com.experimental.documentmodel.WebsiteDocument;
import com.experimental.sitepage.SitePage;
import com.google.common.base.Preconditions;
import javafx.util.Pair;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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

  private final Executor executor = Executors.newFixedThreadPool(12);
  private final Semaphore doneSem = new Semaphore(0);

  public void crawlSites(List<String> urls) {
    Preconditions.checkNotNull(urls);

    for (String url : urls) {
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
  }

  private void scheduleCrawlFor(final String url) {
    executor.execute(new Runnable() {
      @Override
      public void run() {
        processSite(url);
      }
    });

  }

  private void processSite(String url) {
    String documentName = documentNameGenerator.getAndStoreNewDocumentName(DocumentNameGenerator.DocumentType.WEBSITE);
    Path documentPath =
        new File(documentNameGenerator.getAbsoluteRootPath(DocumentNameGenerator.DocumentType.WEBSITE, documentName))
            .toPath().toAbsolutePath().resolve(documentName);

    WebsiteDocument parentDocument = new WebsiteDocument(documentPath.toString());

    SitePage frontPage = null;
    try {
      frontPage = parsePage(new URI(url));
    } catch (URISyntaxException e) {
      e.printStackTrace();
      return;
    }

    if (frontPage == null) {
      return;
    }
    parentDocument.setFrontPage(frontPage);

    List<SitePage.Link> allLinks = new ArrayList<SitePage.Link>();
    List<SitePage> childPages = new ArrayList<SitePage>();

    for (SitePage.Link link : frontPage.outgoingLinks) {
      allLinks.add(link);
      SitePage childPage = parsePage(link.destination);
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

  private SitePage parsePage(URI uri) {
    return null;
  }

}
