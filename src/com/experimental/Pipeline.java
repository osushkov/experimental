package com.experimental;

import com.experimental.documentmodel.*;
import com.experimental.languagemodel.LemmaDB;
import com.experimental.languagemodel.LemmaIDFWeights;
import com.experimental.utils.Log;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by sushkov on 14/01/15.
 */
public class Pipeline {


  public static void buildLemmaIdfWeights() {
    Log.out("buildLemmaIdfWeights running...");

    List<DocumentNameGenerator.DocumentType> docTypesToProcess =
        Lists.newArrayList(DocumentNameGenerator.DocumentType.WEBSITE, DocumentNameGenerator.DocumentType.TOPICAL);

    final LemmaIDFWeights lemmaIDFWeights = new LemmaIDFWeights(LemmaDB.instance);

    final Executor executor = Executors.newFixedThreadPool(12);
    final AtomicInteger numDocuments = new AtomicInteger(0);
    final Semaphore sem = new Semaphore(0);
    final Random rand = new Random();

    DocumentStream documentStream = new DocumentStream(Constants.DOCUMENTS_OUTPUT_PATH);
    documentStream.streamDocuments(docTypesToProcess,
        new DocumentStream.DocumentStreamOutput() {
          @Override
          public void processDocument(final Document document) {
            numDocuments.incrementAndGet();
            executor.execute(new Runnable() {
              @Override
              public void run() {
                try {
                  if (!lemmaIDFWeights.isDocumentValid(document)) {
                    return;
                  }
                } catch (Throwable e) {
                  return;
                }

                if (document instanceof WebsiteDocument) {
                  lemmaIDFWeights.processDocument(document, 1.0);
                } else if (document instanceof TopicalDocument) {
                  lemmaIDFWeights.processDocument(document, 0.01);
                }

                sem.release();

                if (rand.nextInt()%5000 == 0) {
                  System.gc();
                }
              }
            });
          }
        });

    for (int i = 0; i < numDocuments.get(); i++) {
      try {
        sem.acquire();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    try {
      lemmaIDFWeights.save();
    } catch (IOException e) {
      e.printStackTrace();
    }

    Log.out("buildLemmaIdfWeights finished");
  }

}
