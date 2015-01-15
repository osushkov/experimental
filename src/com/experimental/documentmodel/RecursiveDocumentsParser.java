package com.experimental.documentmodel;

import com.experimental.utils.Log;
import com.google.common.base.Preconditions;

import java.io.*;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

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

  private final Semaphore tasksSem = new Semaphore(0);
  private int numTasks = 0;

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

    for (int i = 0; i < numTasks; i++) {
      try {
        Log.out("Tasks Remaining: " + (numTasks - i));
        tasksSem.acquire();
      } catch (InterruptedException e) {
        e.printStackTrace();
        return;
      }
    }
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
    numTasks++;
    executor.execute(new Runnable() {
      @Override
      public void run() {
        ThirdPartyDocumentParser documentParser =
            parserFactory.create(documentNameGenerator, sentenceProcessor);

        try {
          documentParser.parseThirdPartyDocument(documentPath);
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
          tasksSem.release();
        }

      }
    });
  }

}
