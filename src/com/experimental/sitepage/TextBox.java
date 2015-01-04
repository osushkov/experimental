package com.experimental.sitepage;

import com.experimental.geometry.Rectangle;
import com.google.common.base.Preconditions;

/**
 * Created by sushkov on 4/01/15.
 */
public class TextBox implements Box {

  public final String text;

  private final Rectangle rectangle;

  public TextBox(String text, Rectangle rectangle) {
    this.text = Preconditions.checkNotNull(text);
    this.rectangle = Preconditions.checkNotNull(rectangle);
  }

  @Override
  public Rectangle getRectangle() {
    return rectangle;
  }
}
