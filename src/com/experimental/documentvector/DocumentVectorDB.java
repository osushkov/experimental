package com.experimental.documentvector;

import com.experimental.Constants;
import com.experimental.documentmodel.Document;
import com.experimental.documentmodel.DocumentNameGenerator;
import com.experimental.documentmodel.DocumentStream;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

    ConceptVector targetVector = document.getConceptVector();
    List<DocumentSimilarityPair> result = new ArrayList<DocumentSimilarityPair>();

    if (targetVector == null) {
      return result;
    }

    for (VectoredDocument dbDocument : vectoredDocuments) {
      double similarity = targetVector.dotProduct(dbDocument.vector);
//      double distance = targetVector.distanceTo(dbDocument.vector);

      result.add(new DocumentSimilarityPair(dbDocument.document, similarity));
    }

    result.sort(SIMILARITY_DESCENDING_ORDER);
    if (num > 0) {
      return result.subList(0, Math.min(num, result.size()));
    } else {
      return result;
    }
  }

}
