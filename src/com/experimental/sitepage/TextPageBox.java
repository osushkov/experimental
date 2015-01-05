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
    final double fontSize;
    final boolean isBold;
    final boolean isUnderlined;
    final boolean isItalic;

    public TextStyle (double fontSize, boolean isBold, boolean isUnderlined, boolean isItalic) {
      this.fontSize = fontSize;
      this.isBold = isBold;
      this.isUnderlined = isUnderlined;
      this.isItalic = isItalic;
    }

    public TextStyle(TextBox textBox) {
      Preconditions.checkNotNull(textBox);

      this.fontSize = textBox.getVisualContext().getFont().getSize();
      this.isBold = textBox.getVisualContext().getFont().isBold();
      this.isItalic = textBox.getVisualContext().getFont().isItalic();
      this.isUnderlined = textBox.getVisualContext().getTextDecoration().contains(CSSProperty.TextDecoration.UNDERLINE);
    }
  }

  public final List<Sentence> sentences;
  public final TextStyle textStyle;

  private final Rectangle rectangle;


  public TextPageBox(String rawText, TextStyle textStyle, Rectangle rectangle) {
    this.sentences = tokenizeRawText(Preconditions.checkNotNull(rawText));
    this.textStyle = Preconditions.checkNotNull(textStyle);
    this.rectangle = Preconditions.checkNotNull(rectangle);
  }

  // TODO: tokenize the string properly using Stanford NLP library.
  private List<Sentence> tokenizeRawText(String rawText) {
    List<WordToken> sentence = new ArrayList<WordToken>();

    String[] tokens = rawText.split(" ");
    for (int i = 0; i < tokens.length; i++) {
      if (tokens[i].length() > 0) {
        sentence.add(new WordToken(tokens[i]));
      }
    }

    return Lists.newArrayList(new Sentence(sentence));
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
