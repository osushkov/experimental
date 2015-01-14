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
public class RecursiveDocumentsParser {
  private static final String TAG = "RecursiveDocumentsParser";

  private final String rootDirectory;
  private final ThirdPartyDocumentParserFactory parserFactory;
  private final DocumentNameGenerator documentNameGenerator;
  private final SentenceProcessor sentenceProcessor;

  private final Executor executor = Executors.newFixedThreadPool(4);


  public RecursiveDocumentsParser(String rootDirectory,
                                  ThirdPartyDocumentParserFactory parserFactory,
                                  DocumentNameGenerator documentNameGenerator,
                                  SentenceProcessor sentenceProcessor) {

    this.rootDirectory = Preconditions.checkNotNull(rootDirectory);
    this.parserFactory = Preconditions.checkNotNull(parserFactory);
    this.documentNameGenerator = Preconditions.checkNotNull(documentNameGenerator);
    this.sentenceProcessor = Preconditions.checkNotNull(sentenceProcessor);
  }

  public void parseDocuments() {
    parseDocuments(-1);
  }

  public void parseDocuments(int limit) {
    parseDocuments(rootDirectory, limit);
  }

  private int parseDocuments(String directoryPath, int limit) {
    Log.out(TAG, "parsing document in " + directoryPath);

    File dir = new File(directoryPath);
    if (!dir.exists() || limit == 0) {
      return limit;
    }
    File[] children = dir.listFiles();
    for (File child : children) {
      if (limit == 0) {
        return limit;
      }

      if (child.isDirectory()) {
        limit = parseDocuments(child.getAbsolutePath(), limit);
      } else {
        String filePath = child.getAbsolutePath();
        if (parserFactory.isValidDocumentFile(filePath)) {
          parseDocument(filePath);
          limit--;
        }
      }
    }

    return limit;
  }

  private void parseDocument(final String documentPath) {
    executor.execute(new Runnable() {
      @Override
      public void run() {
        ThirdPartyDocumentParser documentParser =
            parserFactory.create(documentNameGenerator, sentenceProcessor);

        try {
          documentParser.parseThirdPartyDocument(documentPath);
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
