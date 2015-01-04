package com.experimental.crawler;

import com.google.common.base.Preconditions;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.parser.ParseData;
import edu.uci.ics.crawler4j.url.WebURL;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by sushkov on 2/01/15.
 */
public class Crawler extends WebCrawler {
  private static final Pattern FILTERS = Pattern.compile(".*(\\.(bmp|gif|jpe?g"
      + "|png|tiff?|mid|mp2|mp3|mp4"
      + "|wav|avi|mov|mpeg|ram|m4v|pdf"
      + "|rm|smil|wmv|swf|wma|zip|rar|gz))$");

  @Override
  public boolean shouldVisit(WebURL url) {
    String href = url.getURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && startWithAllowedUrl(href);
  }

  private boolean startWithAllowedUrl(String href) {
    List<String> allowedUrls = (List<String>) this.getMyController().getCustomData();
    for (String allowedUrl : allowedUrls) {
      if (href.startsWith(allowedUrl)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public void visit(Page page) {
    String url = page.getWebURL().getURL();
    System.out.println("URL: " + url);

    URL pageUrl = null;
    try {
      pageUrl = new URL(url);
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    System.out.println(pageUrl.getHost());

    if (page.getParseData() instanceof HtmlParseData) {
      HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
      String text = htmlParseData.getText();
      String html = htmlParseData.getHtml();

      List<String> cssUrls = getCssUrls(html);
      for (String cssUrl : cssUrls) {
        System.out.println("css: " + cssUrl);
      }
    }
  }

  List<String> getCssUrls(String html) {
    Document doc = Jsoup.parse(html);
    Elements linkElements = doc.head().getElementsByTag("link");
    List<String> result = new ArrayList<String>();

    for (Element element : linkElements) {
      if (isCssLinkElement(element)) {
        result.add(element.attr("href"));
        element.attr("href", "fuckoff.css");
      }
    }

    System.out.println("change html: ");
    System.out.println(doc.html());
    return result;
  }

  private boolean isCssLinkElement(Element element) {
    return element.attr("rel").equals("stylesheet");
  }

}
