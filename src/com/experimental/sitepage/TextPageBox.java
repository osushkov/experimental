package com.experimental.sitepage;

import com.experimental.documentmodel.Sentence;
import com.experimental.geometry.Rectangle;
import com.google.common.base.Preconditions;

import java.util.List;

/**
 * Created by sushkov on 4/01/15.
 */
public class TextPageBox implements PageBox {

  public final List<Sentence> sentences;

  private final Rectangle rectangle;

  public TextPageBox(List<Sentence> sentences, Rectangle rectangle) {
    this.sentences = Preconditions.checkNotNull(sentences);
    this.rectangle = Preconditions.checkNotNull(rectangle);
  }

  @Override
  public Rectangle getRectangle() {
    return rectangle;
  }

  @Override
  public double getNumElementsInBox() {
    double sum = 0.0;
    for (Sentence s : sentences) {
      sum += s.tokens.size();
    }
    return sum;
  }
}
