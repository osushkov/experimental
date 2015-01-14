package com.experimental.documentmodel;

import com.experimental.utils.Log;
import com.google.common.base.Preconditions;

import java.io.File;

/**
 * Created by sushkov on 8/01/15.
 */
public class DocumentStream {

  public interface DocumentStreamOutput {
    void processDocument(Document document);
  }

  private final String rootPath;

  public DocumentStream(String rootPath) {
    this.rootPath = Preconditions.checkNotNull(rootPath);
  }

  public void streamDocuments(DocumentStreamOutput streamOutput) {
    Preconditions.checkNotNull(streamOutput);

    File documentsDir = new File(rootPath);
    for (DocumentNameGenerator.DocumentType type : DocumentNameGenerator.DocumentType.values()) {
      String typeDirPath = documentsDir.toPath().resolve(type.getLabel()).toString();

      File typeDir = new File(typeDirPath);
      if (typeDir.exists()) {
        streamDocumentsIn(typeDir, type, streamOutput);
      }
    }
  }

  private void streamDocumentsIn(File dir, DocumentNameGenerator.DocumentType type, DocumentStreamOutput streamOutput) {
    if (Document.isExistingDocumentDirectory(dir)) {
      switch (type)  {
        case TOPICAL:
          streamOutput.processDocument(new TopicalDocument(dir.getAbsolutePath()));
          break;
        case UNRELATED_COLLECTION:
          streamOutput.processDocument(new TextCollectionDocument(dir.getAbsolutePath()));
          break;
        case WEBSITE:
          streamOutput.processDocument(new WebsiteDocument(dir.getAbsolutePath()));
          break;
      }
      return;
    }

    if (!dir.isDirectory() || !dir.exists()) {
      return;
    }

    File[] children = dir.listFiles();
    for (File child : children) {
      if (child.isDirectory()) {
        streamDocumentsIn(child, type, streamOutput);
      }
    }
  }

}
