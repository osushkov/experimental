package com.experimental.documentmodel;

import com.experimental.utils.Log;
import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sushkov on 13/02/15.
 */
public class DocumentDB {

  private final Map<String, Document> documentMap = new HashMap<String, Document>();

  public static DocumentDB instance = new DocumentDB();

  private DocumentDB() {}

  public WebsiteDocument createWebsiteDocument(String path) {
    if (documentMap.containsKey(path)) {
      Log.out("reusing doc");
      Document doc = documentMap.get(path);
      Preconditions.checkState(doc instanceof WebsiteDocument);
      return (WebsiteDocument) doc;
    } else {
      WebsiteDocument doc = new WebsiteDocument(path);
      documentMap.put(path, doc);
      return doc;
    }
  }

  public TopicalDocument createTopicalDocument(String path) {
    if (documentMap.containsKey(path)) {
      Log.out("reusing doc");
      Document doc = documentMap.get(path);
      Preconditions.checkState(doc instanceof TopicalDocument);
      return (TopicalDocument) doc;
    } else {
      TopicalDocument doc = new TopicalDocument(path);
      documentMap.put(path, doc);
      return doc;
    }
  }

  public TextCollectionDocument createTextCollectionDocument(String path) {
    if (documentMap.containsKey(path)) {
      Log.out("reusing doc");
      Document doc = documentMap.get(path);
      Preconditions.checkState(doc instanceof TextCollectionDocument);
      return (TextCollectionDocument) doc;
    } else {
      TextCollectionDocument doc = new TextCollectionDocument(path);
      documentMap.put(path, doc);
      return doc;
    }
  }

}
