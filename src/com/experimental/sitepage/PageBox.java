package com.experimental.sitepage;

import com.experimental.geometry.Rectangle;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Created by sushkov on 4/01/15.
 */
public interface PageBox {

  Rectangle getRectangle();

  void writeTo(BufferedWriter bw) throws IOException;

}
