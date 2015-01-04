package com.experimental.sitepage;

import com.google.common.base.Preconditions;
import com.sun.xml.internal.ws.client.sei.ResponseBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sushkov on 4/01/15.
 */
public class SitePage {

  public static class HeaderInfo {
    public final String title;
    public final String description;
    public final List<String> keywords;

    public HeaderInfo(String title, String description, List<String> keywords) {
      this.title = Preconditions.checkNotNull(title);
      this.description = Preconditions.checkNotNull(description);
      this.keywords = Preconditions.checkNotNull(keywords);
    }
  }

  public final String url;
  public final String hostname;
  public final String html;
  public final HeaderInfo header;

  public final List<String> incomingLinks = new ArrayList<String>();

  public SitePage(String url, String hostname, String html, HeaderInfo header, List<String> incomingLinks) {
    this.url = Preconditions.checkNotNull(url);
    this.hostname = Preconditions.checkNotNull(hostname);
    this.html = Preconditions.checkNotNull(html);
    this.header = Preconditions.checkNotNull(header);
    this.incomingLinks.addAll(Preconditions.checkNotNull(incomingLinks));
  }

}
