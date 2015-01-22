package com.experimental.sitepage;

import com.experimental.documentmodel.Sentence;
import com.experimental.geometry.Rectangle;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

/**
 * Created by sushkov on 4/01/15.
 */
public interface PageBox {

  Rectangle getRectangle();
  List<Sentence> getTextContent();

  void writeTo(BufferedWriter bw) throws IOException;

}
