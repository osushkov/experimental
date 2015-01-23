package com.experimental.documentmodel;

import com.experimental.utils.Log;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.io.File;
import java.util.List;

/**
 * Created by sushkov on 8/01/15.
 */
public class DocumentStream {

  public interface DocumentStreamOutput {
    void processDocument(Document document);
  }

  private final String rootPath;
  private int numDocumentsProcessed = 0;

  public DocumentStream(String rootPath) {
    this.rootPath = Preconditions.checkNotNull(rootPath);
  }

  public void streamDocuments(DocumentStreamOutput streamOutput) {
    List<DocumentNameGenerator.DocumentType> types = Lists.newArrayList(DocumentNameGenerator.DocumentType.values());
    streamDocuments(types, streamOutput);
  }

  public void streamDocuments(Iterable<DocumentNameGenerator.DocumentType> types, DocumentStreamOutput streamOutput) {
    Preconditions.checkNotNull(streamOutput);
    Preconditions.checkNotNull(types);

    File documentsDir = new File(rootPath);
    for (DocumentNameGenerator.DocumentType type : types) {
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
      numDocumentsProcessed++;
      if ((numDocumentsProcessed % 10000) == 0) {
        Log.out("docs processed: " + numDocumentsProcessed);
        System.gc();
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
