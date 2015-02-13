package com.experimental.documentvector;

import com.experimental.Constants;
import com.experimental.documentmodel.BagOfWeightedLemmas;
import com.experimental.documentmodel.Document;
import com.experimental.documentmodel.DocumentNameGenerator;
import com.experimental.documentmodel.DocumentStream;
import com.experimental.languagemodel.Lemma;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.*;

/**
 * Created by sushkov on 25/01/15.
 */
public class DocumentVectorDB {

  private static class VectoredDocument {
    final ConceptVector vector;
    final Document document;

    VectoredDocument(ConceptVector vector, Document document) {
      this.vector = Preconditions.checkNotNull(vector);
      this.document = Preconditions.checkNotNull(document);
    }
  }

  public static class DocumentSimilarityPair {
    public final Document document;
    public final double similarity;

    private DocumentSimilarityPair(Document document, double similarity) {
      this.document = Preconditions.checkNotNull(document);
      this.similarity = similarity;
    }
  }

  private static final Comparator<DocumentSimilarityPair> SIMILARITY_DESCENDING_ORDER =
      new Comparator<DocumentSimilarityPair>() {
        public int compare(DocumentSimilarityPair e1, DocumentSimilarityPair e2) {
          return Double.compare(e2.similarity, e1.similarity);
        }
      };

  private static final Comparator<DocumentSimilarityPair> DISTANCE_ASCENDING_ORDER =
      new Comparator<DocumentSimilarityPair>() {
        public int compare(DocumentSimilarityPair e1, DocumentSimilarityPair e2) {
          return Double.compare(e1.similarity, e2.similarity);
        }
      };

  private final List<VectoredDocument> vectoredDocuments = new ArrayList<VectoredDocument>();
  private Map<Document, List<DocumentSimilarityPair>> similarDocumentsCache =
      new HashMap<Document, List<DocumentSimilarityPair>>();


  public void load() {
    List<DocumentNameGenerator.DocumentType> docTypesToProcess =
        Lists.newArrayList(DocumentNameGenerator.DocumentType.WEBSITE);

    DocumentStream documentStream = new DocumentStream(Constants.DOCUMENTS_OUTPUT_PATH);
    documentStream.streamDocuments(docTypesToProcess,
        new DocumentStream.DocumentStreamOutput() {
          @Override
          public void processDocument(final Document document) {
            ConceptVector documentVector = document.getConceptVector();
            if (documentVector != null && !Double.isNaN(documentVector.length()) &&
                Double.isFinite(documentVector.length())) {
              vectoredDocuments.add(new VectoredDocument(documentVector, document));
            }
          }
        });
  }

  public List<DocumentSimilarityPair> getNearestDocuments(Document document, int num) {
    Preconditions.checkNotNull(document);

    if (similarDocumentsCache.containsKey(document)) {
      List<DocumentSimilarityPair> cached = similarDocumentsCache.get(document);
      if (cached.size() > num) {
        return cached.subList(0, Math.min(num, cached.size()));
      } else if (cached.size() == num) {
        return cached;
      }
    }

    ConceptVector targetVector = document.getConceptVector();
    List<DocumentSimilarityPair> result = new ArrayList<DocumentSimilarityPair>();

    if (targetVector == null) {
      return result;
    }

    for (VectoredDocument dbDocument : vectoredDocuments) {
      if (dbDocument.document.rootDirectoryPath.equals(document.rootDirectoryPath)) {
        continue;
      }

      double similarity = targetVector.dotProduct(dbDocument.vector);
//      double distance = targetVector.distanceTo(dbDocument.vector);

      result.add(new DocumentSimilarityPair(dbDocument.document, similarity));
    }

    result.sort(SIMILARITY_DESCENDING_ORDER);

    if (num > 0) {
      List<DocumentSimilarityPair> trimmedResult = result.subList(0, Math.min(num, result.size()));
      similarDocumentsCache.put(document, trimmedResult);
      return trimmedResult;
    } else {
      similarDocumentsCache.put(document, result);
      return result;
    }
  }

  public double getTermDiscriminationValue(Lemma term, Document document) {
    double similaritySum = 0.0;
    double sumWeight = 0.0;

    for (VectoredDocument dbDocument : vectoredDocuments) {
      BagOfWeightedLemmas.WeightedLemmaEntry entry = dbDocument.document.getBagOfLemmas().getBag().get(term);
      if (entry != null) {
        sumWeight += entry.weight;
        similaritySum += entry.weight * dbDocument.vector.dotProduct(document.getConceptVector());
      }
    }

    if (sumWeight < Double.MIN_VALUE) {
      return 0.0;
    } else {
      return similaritySum / sumWeight;
    }
  }
}
