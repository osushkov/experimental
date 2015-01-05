package com.experimental.sitepage;

import com.experimental.geometry.Rectangle;
import com.experimental.utils.UrlUtil;
import com.google.common.base.Preconditions;

import java.net.MalformedURLException;

/**
 * Created by sushkov on 4/01/15.
 */
public class ImagePageBox implements PageBox {

  public final String imageUrl;

  private final Rectangle rectangle;

  public ImagePageBox(String pageUrl, String imageSrc, Rectangle rectangle) {
    this.imageUrl = constructImageUrl(pageUrl, imageSrc);
    this.rectangle = Preconditions.checkNotNull(rectangle);
  }

  private String constructImageUrl(String pageUrl, String imageSrc) {
    Preconditions.checkNotNull(pageUrl);
    Preconditions.checkNotNull(imageSrc);

    try {
      return UrlUtil.absoluteUrl(pageUrl, imageSrc);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Rectangle getRectangle() {
    return rectangle;
  }

  @Override
  public String toString() {
    return "[ImagePageBox]\n" +
        rectangle.toString() + "\n" +
        imageUrl;
  }
}
