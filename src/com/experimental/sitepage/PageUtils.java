package com.experimental.sitepage;

import com.google.common.base.Preconditions;
import org.fit.cssbox.layout.Box;
import org.fit.cssbox.layout.ElementBox;
import org.w3c.dom.Element;

/**
 * Created by sushkov on 5/01/15.
 */
public class PageUtils {

  public static boolean isElementALink(ElementBox element) {
    if (element.getElement().getTagName() == "a") {
      return true;
    }

    if (element.getParent() != null) {
      return isElementALink(element.getParent());
    }

    return false;
  }

  public static String getElementLinkDestination(ElementBox element) {
    if (element.getElement().getTagName() == "a") {
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
}
