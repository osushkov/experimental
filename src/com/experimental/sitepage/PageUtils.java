package com.experimental.sitepage;

import com.experimental.utils.Log;
import com.experimental.utils.UrlUtil;
import com.google.common.base.Preconditions;
import org.fit.cssbox.layout.Box;
import org.fit.cssbox.layout.ElementBox;
import org.w3c.dom.Element;

import java.net.MalformedURLException;

/**
 * Created by sushkov on 5/01/15.
 */
public class PageUtils {

  public static boolean isElementALink(ElementBox element) {
    if (element.getElement().getTagName().equals("a")) {
      return true;
    }

    if (element.getParent() != null) {
      return isElementALink(element.getParent());
    }

    return false;
  }

  public static String getElementLinkDestination(ElementBox element) {
    if (element.getElement().getTagName().equals("a")) {
      return element.getElement().getAttribute("href");
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

  public static String constructAbsoluteUrl(String pageUrl, String elementUrl) {
    Preconditions.checkNotNull(pageUrl);
    Preconditions.checkNotNull(elementUrl);

    try {
      return UrlUtil.absoluteUrl(pageUrl, elementUrl);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }
}
