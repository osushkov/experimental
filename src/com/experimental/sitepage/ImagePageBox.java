package com.experimental.sitepage;

import com.experimental.geometry.Rectangle;
import com.google.common.base.Preconditions;

/**
 * Created by sushkov on 4/01/15.
 */
public class ImagePageBox implements PageBox {

  public final String imageUrl;

  private final Rectangle rectangle;

  public ImagePageBox(String imageUrl, Rectangle rectangle) {
    this.imageUrl = Preconditions.checkNotNull(imageUrl);
    this.rectangle = Preconditions.checkNotNull(rectangle);
  }

  @Override
  public Rectangle getRectangle() {
    return rectangle;
  }

  @Override
  public double getNumElementsInBox() {
    return 1.0;
  }
}
