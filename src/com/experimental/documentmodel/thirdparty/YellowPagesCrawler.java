package com.experimental.documentmodel.thirdparty;

import com.experimental.utils.Log;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.http.client.utils.URIBuilder;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Created by sushkov on 19/01/15.
 */
public class YellowPagesCrawler {
  private static final String USER_AGENT =
      "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36";

  private static final String YELLOW_PAGES_AU_PATH = "www.yellowpages.com.au";
  private static final String YELLOW_PAGES_US_PATH = "www.yellowpages.com";

  private final Set<String> siteUrls = new HashSet<String>();
  private final Semaphore doneSem = new Semaphore(0);
  private final Executor executor = Executors.newFixedThreadPool(2);

  public Set<String> crawlForWebsites() {
    crawlYellowPagesAU();
    crawlYellowPagesUS();

    try {
      doneSem.acquire();
      doneSem.acquire();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    Log.out("finished");
    return siteUrls;
  }

  private void crawlYellowPagesAU() {
    executor.execute(new Runnable() {
      @Override
      public void run() {
        for (String query : YellowPagesSearchTerms.SEARCH_TERMS) {
          for (int page = 1; page < 20; page++) {
            try {
              try {
                Thread.sleep(1000);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }

              URI queryUri = null;
              try {
                queryUri = buildUrlForAU(query, page);
              } catch (URISyntaxException e) {
                e.printStackTrace();
                break;
              }
              List<String> links = null;
              try {
                links = extractLinkedWebsites(queryUri);
              } catch (IOException e) {
                e.printStackTrace();
                break;
              }

              int numAdded = 0;
              for (String link : links) {
                if (addUrl(link)) {
                  Log.out(link);
                  numAdded++;
                }
              }

              if (numAdded == 0) {
                break;
              }
            } catch (Throwable e) {
              break;
            }
          }
        }

        doneSem.release();
      }
    });
  }

  private URI buildUrlForAU(String searchTerm, int page) throws URISyntaxException {
    return new URIBuilder()
        .setScheme("http")
        .setHost(YELLOW_PAGES_AU_PATH)
        .setPath("/search/listings")
        .addParameter("clue", searchTerm)
        .addParameter("pageNumber", Integer.toString(page))
        //.addParameter("selectedViewMode", "list")
        //.addParameter("locationClue", "All States")
        .build();
  }

  private void crawlYellowPagesUS() {
    executor.execute(new Runnable() {
      @Override
      public void run() {
        for (String query : YellowPagesSearchTerms.SEARCH_TERMS) {
          for (int page = 1; page < 20; page++) {
            try {
              try {
                Thread.sleep(1000);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }

              URI queryUri = null;
              try {
                queryUri = buildUrlForUS(query, page);
              } catch (URISyntaxException e) {
                e.printStackTrace();
                break;
              }
              List<String> links = null;
              try {
                links = extractLinkedWebsites(queryUri);
              } catch (IOException e) {
                e.printStackTrace();
                break;
              }

              int numAdded = 0;
              for (String link : links) {
                if (addUrl(link)) {
                  Log.out(link);
                  numAdded++;
                }
              }

              if (numAdded == 0) {
                break;
              }
            } catch (Throwable e) {
              break;
            }
          }
        }

        doneSem.release();
      }
    });

  }

  private URI buildUrlForUS(String searchTerm, int page) throws URISyntaxException {
    return new URIBuilder()
        .setScheme("http")
        .setHost(YELLOW_PAGES_US_PATH)
        .setPath("/search")
        .addParameter("search_terms", searchTerm)
        .addParameter("geo_location_terms", "Any place")
        .addParameter("page", Integer.toString(page))
        .build();
  }

  private synchronized boolean addUrl(String url) {
    if (siteUrls.contains(url)) {
      return false;
    } else {
      siteUrls.add(url);
      return true;
    }
  }

  private List<String> extractLinkedWebsites(URI targetUri) throws IOException {
    Connection connection = Jsoup.connect(targetUri.toString());
    connection.timeout(10000);
    connection.userAgent(USER_AGENT);
    Document doc = connection.get();
    Elements searchResultsDivs = doc.select("div.search-results");

    List<String> result = new ArrayList<String>();
    for (Element searchResultDiv : searchResultsDivs) {
      Elements links = searchResultDiv.select("a[href]");
      for (Element link : links) {
        String dstUrl = link.attr("abs:href");
        String linkText = link.text();

        if ((linkText.toLowerCase().equals("website") || link.hasClass("contact-url"))
            && shouldIncludeLink(dstUrl)) {
          result.add(dstUrl);
        }
      }
    }

    return result;
  }

  private boolean shouldIncludeLink(String url) {
    URI uri;
    try {
      uri = new URI(url);
    } catch (URISyntaxException e) {
      e.printStackTrace();
      return false;
    }

    if (uri.getHost() == null) {
      return false;
    }

    if (uri.getHost().equals("local.yp.com")) {
      return false;
    }

    if (uri.getScheme() == null) {
      return false;
    }

    if (!uri.getScheme().equals("http")) {
      return false;
    }

    String baseUrl = getBaseUrl(url);
    return baseUrl.equals(url);
  }

  private String getBaseUrl(String url) {
    URI uri;
    try {
      uri = new URI(url);
    } catch (URISyntaxException e) {
      e.printStackTrace();
      return null;
    }

    URI result;
    try {
      result = new URIBuilder().setScheme("http").setHost(uri.getHost()).build();
    } catch (URISyntaxException e) {
      e.printStackTrace();
      return null;
    }
    return result.toString();
  }

}
