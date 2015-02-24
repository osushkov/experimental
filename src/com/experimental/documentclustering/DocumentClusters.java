package com.experimental.documentclustering;

import com.experimental.Constants;
import com.experimental.documentmodel.BagOfWeightedLemmas;
import com.experimental.documentmodel.Document;
import com.experimental.documentvector.ConceptVector;
import com.experimental.documentvector.ConceptVectorImpl;
import com.experimental.languagemodel.Lemma;
import com.experimental.languagemodel.LemmaDB;
import com.experimental.utils.Common;
import com.experimental.utils.Log;
import com.google.common.base.Preconditions;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by sushkov on 23/02/15.
 */
public class DocumentClusters {

  private static final String DOCUMENT_CLUSTERS_FILENAME = "document_clusters.txt";

  private static final int NUM_CLUSTERS = 1000;
  private static final int DIM = 1200;

  private List<DocumentCluster> builtClusters = new ArrayList<DocumentCluster>();
  private boolean isLoaded = false;

  public int getNumClusters() {
    return builtClusters.size();
  }

  public boolean tryLoad() throws IOException {
    if (isLoaded) {
      return true;
    }

    File aggregateDataFile = new File(Constants.AGGREGATE_DATA_PATH);
    String documentClustersFilePath = aggregateDataFile.toPath().resolve(DOCUMENT_CLUSTERS_FILENAME).toString();

    File documentClustersFile = new File(documentClustersFilePath);
    if (!documentClustersFile.exists()) {
      return false;
    }

    builtClusters.clear();
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(documentClustersFile.getAbsolutePath()));

      int numClusters = Integer.parseInt(Preconditions.checkNotNull(br.readLine()));
      Preconditions.checkState(numClusters > 0);

      for (int i = 0; i < numClusters; i++) {
        DocumentCluster cluster = DocumentCluster.readFrom(br);
        this.builtClusters.add(cluster);
      }

    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return false;
    } finally {
      if (br != null) {
        br.close();
      }
    }

    isLoaded = true;
    return true;
  }

  public void save() throws IOException {
    File aggregateDataFile = new File(Constants.AGGREGATE_DATA_PATH);
    String documentClustersFilePath = aggregateDataFile.toPath().resolve(DOCUMENT_CLUSTERS_FILENAME).toString();

    BufferedWriter bw = null;
    try {
      try {
        FileWriter fw = new FileWriter(documentClustersFilePath);
        bw = new BufferedWriter(fw);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
        return;
      }

      bw.write(Integer.toString(builtClusters.size()) + "\n");
      for (DocumentCluster cluster : builtClusters) {
        cluster.writeTo(bw);
      }
    } finally {
      if (bw != null) {
        bw.close();
      }
    }
  }

  public void clusterDocuments(List<Document> documents, int numIterations) {
    Preconditions.checkNotNull(documents);
    Preconditions.checkArgument(numIterations > 0);

    double bestQuality = 0.0;

    for (int i = 0; i < numIterations; i++) {
      Log.out("cluster iteration: " + i);
      Log.out("best quality: " + bestQuality);
      List<DocumentCluster> clusters = generateClusters(documents);
      double quality = evaluateClusterQuality(documents, clusters);
      if (quality > bestQuality) {
        bestQuality = quality;
        this.builtClusters = clusters;
      }
    }

    for (Document document : documents) {
      DocumentCluster matchingCluster = findMatchingCluster(document, builtClusters);
      matchingCluster.addDocumentToLemmaBag(document);
      document.freeSentences();

      if (Common.rnd.nextInt(1000) == 0) {
        System.gc();
      }
    }
  }

  public List<Double> getRawTermProbabilities(Lemma lemma) {
    Preconditions.checkNotNull(lemma);

    List<Double> result = new ArrayList<Double>();

    double sumWeight = 0.0;
    for (DocumentCluster cluster : builtClusters) {
      BagOfWeightedLemmas bag = cluster.getBagOfLemmas();

      if (bag.getBag().containsKey(lemma)) {
        double weight = bag.getBag().get(lemma).weight;
        result.add(weight);
        sumWeight += weight;
      } else {
        result.add(0.0);
      }
    }

    // Normalise the result distribution.
    if (sumWeight > Double.MIN_VALUE) {
      for (int i = 0; i < result.size(); i++) {
        result.set(i, result.get(i) / sumWeight);
      }
    }

    return result;
  }

  private double evaluateClusterQuality(List<Document> documents, List<DocumentCluster> clusters) {
    double result = 0.0;

    for (Document document : documents) {
      DocumentCluster matchingCluster = findMatchingCluster(document, clusters);
      double similarity = matchingCluster.getSimilarityWith(document.getConceptVector());
      result += similarity * similarity;
    }

    return result;
  }

  private List<DocumentCluster> generateClusters(List<Document> documents) {
    final List<DocumentCluster> clusters = createInitialClusters(documents);
    Preconditions.checkState(clusters.size() > 0);

    final Map<Document, DocumentCluster> documentToClusterMap = new ConcurrentHashMap<Document, DocumentCluster>();

    final Executor executor = Executors.newFixedThreadPool(12);

    final AtomicInteger numMismatches = new AtomicInteger(1);
    while (numMismatches.get() != 0) {
      numMismatches.set(0);

      final AtomicInteger numDocuments = new AtomicInteger(0);
      final Semaphore sem = new Semaphore(0);

      for (final Document document : documents) {
        numDocuments.incrementAndGet();
        executor.execute(new Runnable() {
          @Override
          public void run() {
            DocumentCluster matchedCluster = findMatchingCluster(document, clusters);
            if (!documentToClusterMap.containsKey(document) || documentToClusterMap.get(document) != matchedCluster) {
              numMismatches.incrementAndGet();
            }

            documentToClusterMap.put(document, matchedCluster);
            matchedCluster.addMappedVector(document.getConceptVector());

            sem.release();
          }
        });
      }

      for (int i = 0; i < numDocuments.get(); i++) {
        try {
          sem.acquire();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

      for (DocumentCluster cluster : clusters) {
        cluster.endOfIteration();
      }
    }

    return clusters;
  }

  private List<DocumentCluster> createInitialClusters(List<Document> documents) {
    List<DocumentCluster> result = new ArrayList<DocumentCluster>();

    for (int i = 0; i < NUM_CLUSTERS; i++) {
      int index = Common.rnd.nextInt(documents.size());
      ConceptVector offset = ConceptVectorImpl.createRandomUnitVector(DIM);
      offset.scale(0.05);

      ConceptVector clusterVec = new ConceptVectorImpl(DIM);
      clusterVec.overwriteWith(offset);
      clusterVec.add(documents.get(index).getConceptVector());
      clusterVec.normalise();

      DocumentCluster newCluster = new DocumentCluster(DIM);
      newCluster.setCentroid(clusterVec);

      result.add(newCluster);
    }

    return result;
  }

  private DocumentCluster findMatchingCluster(Document document, List<DocumentCluster> clusters) {
    DocumentCluster result = null;
    double bestSimilarity = 0.0;

    for (DocumentCluster cluster : clusters) {
      double similarity = cluster.getSimilarityWith(document.getConceptVector());
      if (similarity > bestSimilarity) {
        bestSimilarity = similarity;
        result = cluster;
      }
    }

    Preconditions.checkState(result != null);
    return result;
  }

}
