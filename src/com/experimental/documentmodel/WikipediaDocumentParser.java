package com.experimental.documentmodel;

import com.experimental.utils.Log;
import com.google.common.base.Preconditions;

import java.io.*;
import java.util.List;

/**
 * Created by sushkov on 8/01/15.
 */
public class WikipediaDocumentParser {
  private static final String TAG = "WikipediaDocumentParser";

  private static final double TITLE_WEIGHT = 5.0;
  private static final double H2_WEIGHT = 3.0;
  private static final double H3_WEIGHT = 2.5;
  private static final double H4_WEIGHT = 2.0;
  private static final double H5_WEIGHT = 1.5;
  private static final double LIST_WEIGHT = 1.5;

  private final String rootDirectory;
  private final DocumentNameGenerator documentNameGenerator;
  private final SentenceProcessor sentenceProcessor;

  private StringBuffer sentenceBuffer = new StringBuffer();
  private StringBuffer rawTextBuffer = new StringBuffer();


  public WikipediaDocumentParser(String rootDirectory, DocumentNameGenerator documentNameGenerator,
                                 SentenceProcessor sentenceProcessor) {

    this.rootDirectory = Preconditions.checkNotNull(rootDirectory);
    this.documentNameGenerator = Preconditions.checkNotNull(documentNameGenerator);
    this.sentenceProcessor = Preconditions.checkNotNull(sentenceProcessor);
  }

  public void parseDocument() {
    parseDocuments(rootDirectory);
  }

  private void parseDocuments(String directoryPath) {
    Log.out(TAG, "parsing document in " + directoryPath);

    File dir = new File(directoryPath);
    if (!dir.exists()) {
      return;
    }
    File[] children = dir.listFiles();
    for (File child : children) {
      if (child.isDirectory()) {
        parseDocuments(directoryPath);
      } else {
        try {
          String filePath = child.getAbsolutePath();
          if (isWikipediaDataFile(filePath)) {
            parseDocument(filePath);
          }
        } catch (IOException e) {
          e.printStackTrace();
          continue;
        }
      }
    }
  }

  private void parseDocument(String documentPath) throws IOException {
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(documentPath));

      String line = br.readLine();
      while (line != null) {
        processLine(line);
        line = br.readLine();
      }

    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return;
    } finally {
      if (br != null) {
        br.close();
      }
    }
  }

  private void processLine(String line) {
    if (isLineStartOfDocument(line)) {

    } else if (isLineEndOfDocument(line)) {

    } else if (isLineWeighted(line)) {

    } else {
      String cleanLine = cleanupLine(line);
      sentenceBuffer.append(cleanLine).append("\n");
      rawTextBuffer.append(cleanLine).append("\n");
    }
  }

  private List<Sentence> processBuffer() {
    return null;
  }

  private boolean isLineWeighted(String line) {
    return isLineHeading(line) || isLineList(line);
  }

  private boolean isLineHeading(String line) {
    return line.matches(".*<h[2-9]>.*");
  }

  private boolean isLineList(String line) {
    return line.matches(".*<li>.*");
  }

  private boolean isLineStartOfDocument(String line) {
    return line.matches(".*<doc id=.*");
  }

  private boolean isLineEndOfDocument(String line) {
    return line.matches(".*</doc>.*");
  }

  private String getDocumentTitle(String line) {
    return line.replaceAll(".*<doc id=.*title=\"(.*)\">.*", "$1");
  }

  private double getLineWeight(String line) {
    if (line.matches(".*<li>.*")) {
      return LIST_WEIGHT;
    } else if (line.matches(".*<h2>.*")) {
      return H2_WEIGHT;
    } else if (line.matches(".*<h3>.*")) {
      return H3_WEIGHT;
    } else if (line.matches(".*<h4>.*")) {
      return H4_WEIGHT;
    } else if (line.matches(".*<h5>.*")) {
      return H5_WEIGHT;
    } else {
      return 1.0;
    }
  }

  private boolean isWikipediaDataFile(String path) {
    return path.matches(".*wiki_[0-9]*");
  }

  private String cleanupLine(String line) {
    String result = line.replaceAll("<[a-zA-Z0-9]*>", "");
    result = result.replaceAll("</[a-zA-Z0-9]*>", "");
    return result;
  }


}
