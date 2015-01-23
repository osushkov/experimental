package com.experimental.sitepage;

import com.experimental.utils.Log;
import com.google.common.base.Preconditions;
import org.fit.cssbox.layout.Box;
import org.fit.cssbox.layout.ElementBox;
import org.w3c.dom.Element;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by sushkov on 5/01/15.
 */
public class PageUtils {

  public static boolean isElementALink(ElementBox element) {
    if (element.getElement().getTagName().equals("a")) {
      String href = element.getElement().getAttribute("href").trim().toLowerCase();
      if (href.startsWith("mailto:") || href.startsWith("ftp:") ||
          href.startsWith("file:") || href.startsWith("tel:") ||
          href.startsWith("javascript"))  {
        return false;
      } else {
        return true;
      }
    }

    if (element.getParent() != null) {
      return isElementALink(element.getParent());
    }

    return false;
  }

  public static String getElementLinkDestination(ElementBox element) {
    if (element.getElement().getTagName().equals("a")) {
      return element.getElement().getAttribute("href").trim();
    }

    if (element.getParent() != null) {
      return getElementLinkDestination(element.getParent());
    }

    return null;
  }

  public static boolean isBoxVisible(Box box) {
    Preconditions.checkNotNull(box);
    return box.isDisplayed() && box.isDeclaredVisible();
  }

  public static URL constructAbsoluteUrl(String pageUrl, String elementUrl) throws MalformedURLException {
    Preconditions.checkNotNull(pageUrl);
    Preconditions.checkNotNull(elementUrl);

    URL page = new URL(pageUrl);
    return new URL(page, elementUrl);
  }
}
