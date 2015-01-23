package com.experimental.sitepage;

import com.experimental.documentmodel.Sentence;
import com.experimental.documentmodel.SentenceProcessor;
import com.experimental.geometry.Rectangle;
import com.google.common.base.Preconditions;
import cz.vutbr.web.css.CSSProperty;
import org.fit.cssbox.layout.TextBox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
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
    public String linkHref;

    public TextStyle(double fontSize, boolean isBold, boolean isUnderlined, boolean isItalic,
                     boolean isLink, String linkUrl) {
      this.fontSize = fontSize;
      Preconditions.checkState(fontSize > 0.0);

      this.isBold = isBold;
      this.isUnderlined = isUnderlined;
      this.isItalic = isItalic;
      this.isLink = isLink;
      this.linkHref = linkUrl;
    }

    public TextStyle(TextBox textBox) {
      Preconditions.checkNotNull(textBox);

      this.fontSize = textBox.getVisualContext().getFont().getSize();
      Preconditions.checkState(fontSize > 0.0);

      this.isBold = textBox.getVisualContext().getFont().isBold();
      this.isItalic = textBox.getVisualContext().getFont().isItalic();
      this.isUnderlined = textBox.getVisualContext().getTextDecoration().contains(CSSProperty.TextDecoration.UNDERLINE);
      this.isLink = PageUtils.isElementALink(textBox.getParent());
      this.linkHref = PageUtils.getElementLinkDestination(textBox.getParent());
    }

    public static TextStyle readFrom(BufferedReader in) throws IOException {
      Preconditions.checkNotNull(in);

      double fontSize = Double.parseDouble(Preconditions.checkNotNull(in.readLine()));
      Preconditions.checkState(fontSize > 0.0);

      boolean isBold = Boolean.parseBoolean(Preconditions.checkNotNull(in.readLine()));
      boolean isUnderlined = Boolean.parseBoolean(Preconditions.checkNotNull(in.readLine()));
      boolean isItalic = Boolean.parseBoolean(Preconditions.checkNotNull(in.readLine()));
      boolean isLink = Boolean.parseBoolean(Preconditions.checkNotNull(in.readLine()));
      String linkUrl = Preconditions.checkNotNull(in.readLine());

      return new TextStyle(fontSize, isBold, isUnderlined, isItalic, isLink, linkUrl);
    }

    public void writeTo(BufferedWriter bw) throws IOException {
      Preconditions.checkNotNull(bw);

      bw.append(Double.toString(fontSize) + "\n");
      bw.append(Boolean.toString(isBold) + "\n");
      bw.append(Boolean.toString(isUnderlined) + "\n");
      bw.append(Boolean.toString(isItalic) + "\n");
      bw.append(Boolean.toString(isLink) + "\n");

      if (linkHref == null) {
        bw.append("\n");
      } else {
        bw.append(linkHref + "\n");
      }
    }

    public double computeAbsoluteEmphasis() {

      double result = Math.sqrt(fontSize);
//      if (isBold) {
//        result *= 1.2;
//      }
      if (isUnderlined) {
        result *= 1.2;
      }
      if (isLink) {
        result *= 2.0;
      }
      return result;
    }
  }

  public final String text;
  public final TextStyle textStyle;
  public List<Sentence> sentences = null;

  private final Rectangle rectangle;

  public TextPageBox(String text, TextStyle textStyle, Rectangle rectangle) {
    this.text = Preconditions.checkNotNull(text);
    this.textStyle = Preconditions.checkNotNull(textStyle);
    this.rectangle = Preconditions.checkNotNull(rectangle);
  }

  public static TextPageBox readFrom(BufferedReader in) throws IOException {
    Preconditions.checkNotNull(in);

    String text = Preconditions.checkNotNull(in.readLine());
    TextStyle textStyle = TextStyle.readFrom(in);

    int numSentences = Integer.parseInt(Preconditions.checkNotNull(in.readLine()));
    Preconditions.checkState(numSentences >= 0);

    List<Sentence> sentences = new ArrayList<Sentence>();
    for (int i = 0; i < numSentences; i++) {
      Sentence sentence = Sentence.readFrom(in);
      sentences.add(sentence);
    }

    Rectangle rectangle = Rectangle.readFrom(in);

    TextPageBox result = new TextPageBox(text, textStyle, rectangle);
    result.sentences = sentences;

    return result;
  }

  public void writeTo(BufferedWriter bw) throws IOException {
    Preconditions.checkNotNull(bw);

    String[] lines = text.split("\n");
    StringBuffer joinedLines = new StringBuffer();
    for (String line : lines) {
      joinedLines.append(line).append(" ");
    }
    bw.write(joinedLines.toString() + "\n");

    textStyle.writeTo(bw);

    if (sentences == null) {
      bw.write("0\n");
    } else {
      bw.write(Integer.toString(sentences.size()) + "\n");
      for (Sentence sentence : sentences) {
        sentence.writeTo(bw);
      }
    }

    rectangle.writeTo(bw);
  }

  @Override
  public Rectangle getRectangle() {
    return rectangle;
  }

  @Override
  public List<Sentence> getTextContent() {
    Preconditions.checkState(sentences != null);
    return sentences;
  }

  @Override
  public String toString() {
    if (sentences != null) {
      StringBuffer buffer = new StringBuffer();
      buffer.append("[TextPageBox]\n");
      for (Sentence sentence : sentences) {
        buffer.append(sentence.toString()).append("\n");
      }

      return buffer.toString();

    } else {
      return "[TextPageBox] " + textStyle.computeAbsoluteEmphasis() + "\n" + text;
    }
  }

  public void buildSentences(SentenceProcessor sentenceProcessor, double textBoxEmphasis) {
    this.sentences = sentenceProcessor.processString(text, textBoxEmphasis, true);
  }
}
