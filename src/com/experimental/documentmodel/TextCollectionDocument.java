package com.experimental.documentmodel;

import com.google.common.base.Preconditions;

import java.io.IOException;

/**
 * Created by sushkov on 6/01/15.
 */
public class TextCollectionDocument extends Document {


  public TextCollectionDocument(String rootDirectoryPath) {
    super(rootDirectoryPath);
  }

  public static TextCollectionDocument readFrom(String rootDirectoryPath) throws IOException {
    Preconditions.checkNotNull(rootDirectoryPath);

    TextCollectionDocument newDocument = new TextCollectionDocument(rootDirectoryPath);
    newDocument.loadFromDirectory();

    return newDocument;
  }

  @Override
  protected void writeSpecificData() {
    // No specific data to write.
  }
}
