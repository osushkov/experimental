package com.experimental.sitepage;

import com.experimental.geometry.Rectangle;
import com.google.common.base.Preconditions;

/**
 * Created by sushkov on 4/01/15.
 */
public class ImageBox implements Box {

  public final String imageUrl;

  private final Rectangle rectangle;

  public ImageBox(String imageUrl, Rectangle rectangle) {
    this.imageUrl = Preconditions.checkNotNull(imageUrl);
    this.rectangle = Preconditions.checkNotNull(rectangle);
  }

  @Override
  public Rectangle getRectangle() {
    return rectangle;
  }
}
