package com.experimental.sitepage;

import com.experimental.geometry.Rectangle;

/**
 * Created by sushkov on 4/01/15.
 */
public interface PageBox {

  Rectangle getRectangle();

  // Amount of information contained in this box. For a text box this would generally be
  // proportional to the number of words. For an image box, it should be some constant.
  double getNumElementsInBox();

}
