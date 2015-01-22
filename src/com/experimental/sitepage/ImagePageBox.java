package com.experimental.sitepage;

import com.experimental.documentmodel.Sentence;
import com.experimental.documentmodel.SentenceProcessor;
import com.experimental.geometry.Rectangle;
import com.experimental.utils.UrlUtil;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sushkov on 4/01/15.
 */
public class ImagePageBox implements PageBox {

  public final String imageUrl;
  public final String altText;
  public List<Sentence> sentences = null;
  private final Rectangle rectangle;

  public ImagePageBox(String pageUrl, String imageSrc, String altText, Rectangle rectangle) {
    this.imageUrl = constructImageUrl(pageUrl, imageSrc);
    this.altText = Preconditions.checkNotNull(altText);
    this.rectangle = Preconditions.checkNotNull(rectangle);
  }

  public ImagePageBox(String imageUrl, String altText, Rectangle rectangle) {
    this.imageUrl = Preconditions.checkNotNull(imageUrl);
    this.altText = Preconditions.checkNotNull(altText);
    this.rectangle = Preconditions.checkNotNull(rectangle);
  }

  public static ImagePageBox readFrom(BufferedReader in) throws IOException {
    Preconditions.checkNotNull(in);

    String imageUrl = Preconditions.checkNotNull(in.readLine());
    String altText = Preconditions.checkNotNull(in.readLine());

    int numSentences = Integer.parseInt(Preconditions.checkNotNull(in.readLine()));
    Preconditions.checkState(numSentences >= 0);

    List<Sentence> sentences = new ArrayList<Sentence>();
    for (int i = 0; i < numSentences; i++) {
      Sentence sentence = Sentence.readFrom(in);
      sentences.add(sentence);
    }

    Rectangle rectangle = Rectangle.readFrom(in);

    ImagePageBox result = new ImagePageBox(imageUrl, altText, rectangle);
    result.sentences = sentences;

    return result;
  }

  @Override
  public void writeTo(BufferedWriter bw) throws IOException {
    Preconditions.checkNotNull(bw);

    bw.write(imageUrl + "\n");
    bw.write(altText + "\n");

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
    return "[ImagePageBox] " + altText + "\n" +
        imageUrl;
  }

  public void buildSentences(SentenceProcessor sentenceProcessor, double imageBoxEmphasis) {
    if (altText.length() == 0) {
      this.sentences = Lists.newArrayList();
    } else {
      this.sentences = sentenceProcessor.processString(altText, imageBoxEmphasis, true);
    }
  }
}
