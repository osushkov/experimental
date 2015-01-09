package com.experimental.documentmodel;

import com.google.common.base.Preconditions;

import java.io.File;
import java.util.*;

/**
 * Created by sushkov on 8/01/15.
 */
public class DocumentNameGenerator {

  public static enum DocumentType {
    UNRELATED_COLLECTION ("unrelated_collection"),
    TOPICAL ("topical"),
    WEBSITE ("website");

    private final String label;

    private DocumentType(String label) {
      this.label = Preconditions.checkNotNull(label);
    }

    public String getLabel() {
      return label;
    }
  }

  private final Random rand = new Random();
  private final String rootDocumentsDirPath;
  private final Map<DocumentType, Set<String>> existingDocumentNames =
      new HashMap<DocumentType, Set<String>>();

  public DocumentNameGenerator(String rootDocumentsDirPath) {
    if (rootDocumentsDirPath.endsWith("/")) {
      this.rootDocumentsDirPath = Preconditions.checkNotNull(rootDocumentsDirPath);
    } else {
      this.rootDocumentsDirPath = Preconditions.checkNotNull(rootDocumentsDirPath) + "/";
    }
    preloadExistingNames();
  }

  private void preloadExistingNames() {
    for (DocumentType documentType : DocumentType.values()) {
      existingDocumentNames.put(documentType, new HashSet<String>());

      File documentDirectory = new File(rootDocumentsDirPath + documentType.getLabel());
      if (!documentDirectory.exists()) {
        documentDirectory.mkdirs();
        continue;
      } else {
        File[] documentContents = documentDirectory.listFiles();
        for (File file : documentContents) {
          if (file.isDirectory()) {
            existingDocumentNames.get(documentType).add(file.getName());
          }
        }
      }
    }
  }

  public String getAbsoluteRootPath(DocumentType type, String documentName) {
    Preconditions.checkNotNull(type);
    Preconditions.checkNotNull(documentName);
    return rootDocumentsDirPath + type.getLabel() + "/" + documentName.substring(0, 2);
  }

  public synchronized String getAndStoreNewDocumentName(DocumentType type) {
    String newName = Integer.toHexString(rand.nextInt(100000000)).toUpperCase();
    while (existingDocumentNames.get(type).contains(newName)) {
      newName = Integer.toHexString(rand.nextInt(100000000)).toUpperCase();
    }

    existingDocumentNames.get(type).add(newName);
    return newName;
  }

}
