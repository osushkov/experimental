package com.experimental.documentmodel;

import com.experimental.utils.Log;
import com.google.common.base.Preconditions;

import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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
  private static final double DEFAULT_EMPHASIS = 1.0;

  private final DocumentNameGenerator documentNameGenerator;
  private final SentenceProcessor sentenceProcessor;

  private StringBuffer sentenceBuffer = new StringBuffer();
  private StringBuffer rawTextBuffer = new StringBuffer();
  private TopicalDocument currentDocument = null;

  private String previousEmphasisedLine = null;

  public WikipediaDocumentParser(DocumentNameGenerator documentNameGenerator,
                                 SentenceProcessor sentenceProcessor) {

    this.documentNameGenerator = Preconditions.checkNotNull(documentNameGenerator);
    this.sentenceProcessor = Preconditions.checkNotNull(sentenceProcessor);
  }

  public void parseDocument(String documentPath) throws IOException {
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
      Preconditions.checkState(currentDocument == null);
      currentDocument = createNewDocument();

      String title = getDocumentTitle(line);
      previousEmphasisedLine = title;
      rawTextBuffer.append(title).append("\n");
      currentDocument.addSentences(sentenceProcessor.processString(title, TITLE_WEIGHT));

    } else if (isLineEndOfDocument(line)) {
      Preconditions.checkState(currentDocument != null);

      currentDocument.setText(rawTextBuffer.toString());
      String sentencesString = sentenceBuffer.toString();
      if (sentencesString.length() > 0) {
        currentDocument.addSentences(sentenceProcessor.processString(sentencesString, DEFAULT_EMPHASIS));
      }

      if (shouldSaveDocument(currentDocument)) {
        try {
          currentDocument.save();
        } catch (IOException e) {
          e.printStackTrace();
          Log.out(TAG, "Failed to save document: " + currentDocument.getRawText());
        }
      }

      rawTextBuffer = new StringBuffer();
      sentenceBuffer = new StringBuffer();
      currentDocument = null;
      previousEmphasisedLine = null;
    } else if (isLineWeighted(line)) {
      String sentenceBufferString = sentenceBuffer.toString();
      if (sentenceBufferString.length() > 0) {
        currentDocument.addSentences(sentenceProcessor.processString(sentenceBufferString, DEFAULT_EMPHASIS));
      }
      sentenceBuffer = new StringBuffer();

      String cleanLine = cleanupLine(line);
      previousEmphasisedLine = cleanLine;

      rawTextBuffer.append(cleanLine).append("\n");
      currentDocument.addSentences(sentenceProcessor.processString(cleanLine, getLineWeight(line)));

    } else {
      String cleanLine = cleanupLine(line);
      if (!cleanLine.equals(previousEmphasisedLine) &&
          !cleanLine.equals(previousEmphasisedLine.substring(0, previousEmphasisedLine.length()-1))) {
        sentenceBuffer.append(cleanLine).append("\n");
        rawTextBuffer.append(cleanLine).append("\n");
      }
      previousEmphasisedLine = null;
    }
  }

  private TopicalDocument createNewDocument() {
    String documentName = documentNameGenerator.getAndStoreNewDocumentName(DocumentNameGenerator.DocumentType.TOPICAL);
    Path documentPath =
        new File(documentNameGenerator.getAbsoluteRootPath(DocumentNameGenerator.DocumentType.TOPICAL, documentName))
            .toPath().toAbsolutePath().resolve(documentName);

    return new TopicalDocument(documentPath.toString());
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

  private boolean shouldSaveDocument(Document document) {
    if (document.getSentences().size() < 5) {
      return false;
    }

    int numTokens = 0;
    for (Sentence sentence : document.getSentences()) {
      numTokens += sentence.tokens.size();
    }

    if (numTokens < 20) {
      return false;
    }

    return true;
  }


}
