package com.experimental.documentmodel;

import com.google.common.base.Preconditions;

import java.io.*;
import java.nio.file.Path;
import java.util.List;

/**
 * Created by sushkov on 14/01/15.
 */
public class WebbaseDocumentParser implements ThirdPartyDocumentParser {
  private static final String TAG = "WebbaseDocumentsParser";

  private static final int NUM_PARAGRAPHS_PER_DOCUMENT = 100;

  private final DocumentNameGenerator documentNameGenerator;
  private final SentenceProcessor sentenceProcessor;

  private StringBuffer documentTextBuffer = new StringBuffer();
  private int numParagraphsInBuffer = 0;

  public WebbaseDocumentParser(DocumentNameGenerator documentNameGenerator,
                                 SentenceProcessor sentenceProcessor) {

    this.documentNameGenerator = Preconditions.checkNotNull(documentNameGenerator);
    this.sentenceProcessor = Preconditions.checkNotNull(sentenceProcessor);
  }

  @Override
  public void parseThirdPartyDocument(String documentFilePath) throws IOException {
    Preconditions.checkNotNull(documentFilePath);

    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(documentFilePath));

      String line = br.readLine();
      while (line != null) {
        processLine(line);
        line = br.readLine();
      }

      if (documentTextBuffer.length() > 0) {
        createDocumentFromBuffer();
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
    if (isParagraphBreakingLine(line)) {
      numParagraphsInBuffer++;
      documentTextBuffer.append("\n");

      if (numParagraphsInBuffer > NUM_PARAGRAPHS_PER_DOCUMENT) {
        if (documentTextBuffer.length() > 0) {
          createDocumentFromBuffer();
        }

        documentTextBuffer = new StringBuffer();
        numParagraphsInBuffer = 0;
      }
    } else {
      documentTextBuffer.append(line);
      documentTextBuffer.append(" ");
    }
  }

  private void createDocumentFromBuffer() {
    TextCollectionDocument document = createNewDocument();

    String rawText = documentTextBuffer.toString();
    List<Sentence> sentences = sentenceProcessor.processString(rawText, 1.0);

//    document.setText(rawText);
    document.addSentences(sentences);

    try {
      document.save();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private TextCollectionDocument createNewDocument() {
    String documentName =
        documentNameGenerator.getAndStoreNewDocumentName(DocumentNameGenerator.DocumentType.UNRELATED_COLLECTION);

    Path documentPath = new File(documentNameGenerator.getAbsoluteRootPath(
        DocumentNameGenerator.DocumentType.UNRELATED_COLLECTION, documentName))
        .toPath().toAbsolutePath().resolve(documentName);

    return new TextCollectionDocument(documentPath.toString());
  }

  private boolean isParagraphBreakingLine(String line) {
    return line.length() == 0;
  }
}
