package com.experimental.sitepage;

import com.experimental.documentmodel.Sentence;
import com.experimental.documentmodel.WordToken;
import com.experimental.geometry.Rectangle;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import cz.vutbr.web.css.CSSProperty;
import org.fit.cssbox.layout.TextBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sushkov on 4/01/15.
 */
public class TextPageBox implements PageBox {

  public static class TextStyle {
    public final double fontSize;
    public final boolean isBold;
    public final boolean isUnderlined;
    public final boolean isItalic;
    public final boolean isLink;

    public TextStyle (double fontSize, boolean isBold, boolean isUnderlined, boolean isItalic, boolean isLink) {
      this.fontSize = fontSize;
      this.isBold = isBold;
      this.isUnderlined = isUnderlined;
      this.isItalic = isItalic;
      this.isLink = isLink;
    }

    public TextStyle(TextBox textBox) {
      Preconditions.checkNotNull(textBox);

      this.fontSize = textBox.getVisualContext().getFont().getSize();
      this.isBold = textBox.getVisualContext().getFont().isBold();
      this.isItalic = textBox.getVisualContext().getFont().isItalic();
      this.isUnderlined = textBox.getVisualContext().getTextDecoration().contains(CSSProperty.TextDecoration.UNDERLINE);
      this.isLink = PageUtils.isElementALink(textBox.getParent());
    }
  }

  public final String text;
  public final TextStyle textStyle;

  private final Rectangle rectangle;

  public TextPageBox(String text, TextStyle textStyle, Rectangle rectangle) {
    this.text = Preconditions.checkNotNull(text);
    this.textStyle = Preconditions.checkNotNull(textStyle);
    this.rectangle = Preconditions.checkNotNull(rectangle);
  }

  @Override
  public Rectangle getRectangle() {
    return rectangle;
  }

  @Override
  public String toString() {
    return "[TextPageBox]\n" +
        rectangle.toString() + "\n" +
        text;
  }
}
