package com.experimental.documentmodel;

import com.experimental.utils.Log;
import com.google.common.base.Preconditions;

import java.io.*;
import java.nio.file.Path;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by sushkov on 9/01/15.
 */
public class WikipediaDocumentsParser {
  private static final String TAG = "WikipediaDocumentsParser";

  private final String rootDirectory;
  private final DocumentNameGenerator documentNameGenerator;
  private final SentenceProcessor sentenceProcessor;

  private final Executor executor = Executors.newFixedThreadPool(4);


  public WikipediaDocumentsParser(String rootDirectory, DocumentNameGenerator documentNameGenerator,
                                 SentenceProcessor sentenceProcessor) {

    this.rootDirectory = Preconditions.checkNotNull(rootDirectory);
    this.documentNameGenerator = Preconditions.checkNotNull(documentNameGenerator);
    this.sentenceProcessor = Preconditions.checkNotNull(sentenceProcessor);
  }

  public void parseDocuments() {
    parseDocuments(rootDirectory);
  }

  private void parseDocuments(String directoryPath) {
    Log.out(TAG, "parsing document in " + directoryPath);

    File dir = new File(directoryPath);
    if (!dir.exists()) {
      return;
    }
    File[] children = dir.listFiles();
    for (File child : children) {
      if (child.isDirectory()) {
        parseDocuments(child.getAbsolutePath());
      } else {
        String filePath = child.getAbsolutePath();
        if (isWikipediaDataFile(filePath)) {
          parseDocument(filePath);
        }
      }
    }
  }

  private void parseDocument(final String documentPath) {
    executor.execute(new Runnable() {
      @Override
      public void run() {
        WikipediaDocumentParser documentParser =
            new WikipediaDocumentParser(documentNameGenerator, sentenceProcessor);

        try {
          documentParser.parseDocument(documentPath);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });
  }

  private boolean isWikipediaDataFile(String path) {
    return path.matches(".*wiki_[0-9]*");
  }

}
