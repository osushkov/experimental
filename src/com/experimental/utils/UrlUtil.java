package com.experimental.utils;

import com.google.common.base.Preconditions;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by sushkov on 5/01/15.
 */
public class UrlUtil {

  public static String absoluteUrl(String pageUrl, String elementSrc) throws MalformedURLException {
    Preconditions.checkNotNull(pageUrl);
    Preconditions.checkNotNull(elementSrc);

    URL page = new URL(pageUrl);

    if (elementSrc.startsWith("/")) {
      URL elementUrl = new URL(page.getProtocol(), page.getHost(), page.getPort(), elementSrc);
      return elementUrl.toString();
    } else {
      return elementSrc;
    }
  }

}
