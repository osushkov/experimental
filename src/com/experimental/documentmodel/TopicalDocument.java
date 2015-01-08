package com.experimental.documentmodel;

import com.google.common.base.Preconditions;

import java.io.File;
import java.io.IOException;

/**
 * Created by sushkov on 6/01/15.
 */
public class TopicalDocument extends Document {

  public TopicalDocument(String rootDirectoryPath) {
    super(rootDirectoryPath);
  }

  public static TopicalDocument readFrom(String rootDirectoryPath) throws IOException {
    Preconditions.checkNotNull(rootDirectoryPath);

    TopicalDocument newDocument = new TopicalDocument(rootDirectoryPath);
    newDocument.loadFromDirectory();

    return newDocument;
  }

  @Override
  protected void writeSpecificData() {
    // No specific data to write.
  }
}
