package com.experimental.languagemodel;

import com.experimental.documentmodel.Document;
import com.google.common.base.Preconditions;

/**
 * Created by sushkov on 11/01/15.
 */
public class LanguageModel {


  public void addDocument(Document document) {
    Preconditions.checkNotNull(document);

  }


}
