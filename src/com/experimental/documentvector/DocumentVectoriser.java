package com.experimental.documentvector;

import com.experimental.documentmodel.Document;
import com.google.common.base.Preconditions;

/**
 * Created by sushkov on 12/01/15.
 */
public interface DocumentVectoriser {

  int getDimensionality();

  ConceptVector vectoriseDocument(Document document);

}
