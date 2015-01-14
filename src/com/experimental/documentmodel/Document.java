package com.experimental.documentmodel;

import com.experimental.nlp.POSTag;
import com.experimental.utils.Log;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sushkov on 4/01/15.
 */
public abstract class Document {
  private static final String RAW_TEXT_FILENAME = "raw.txt";
  private static final String TOKENISED_SENTENCES_FILENAME = "sentences.txt";

  private final String rootDirectoryPath;

  private List<Sentence> sentences = null;
  //private String rawText = null;
  private BagOfWeightedLemmas bagOfLemmas = null;

  public static boolean isExistingDocumentDirectory(File dir) {
    if (!dir.exists() || !dir.isDirectory()) {
      return false;
    }

    File[] children = dir.listFiles();
    for (File child : children) {
      if (child.toPath().getFileName().toString().equals(RAW_TEXT_FILENAME)) {
        return true;
      }
      if (child.toPath().getFileName().toString().equals(TOKENISED_SENTENCES_FILENAME)) {
        return true;
      }
    }

    return false;
  }

  public Document(String rootDirectoryPath) {
    this.rootDirectoryPath = Preconditions.checkNotNull(rootDirectoryPath);
  }

//  public String getRawText() {
//    if (rawText == null) {
//      if (!loadRawText()) {
//        rawText = "";
//      }
//    }
//    return rawText;
//  }

  public List<Sentence> getSentences() {
    if (sentences == null) {
      sentences = new ArrayList<Sentence>();
      loadSentences();
    }
    return sentences;
  }

  public BagOfWeightedLemmas getBagOfLemmas() {
    if (bagOfLemmas == null) {
      generateBagOfLemmas();
    }

    return Preconditions.checkNotNull(bagOfLemmas);
  }

  public void addSentence(Sentence sentence) {
    addSentences(Lists.newArrayList(sentences));
  }

  public void addSentences(List<Sentence> sentences) {
    if (this.sentences == null) {
      this.sentences = new ArrayList<Sentence>();
    }
    this.sentences.addAll(Preconditions.checkNotNull(sentences));
  }

//  public void setText(String rawText) {
//    this.rawText = Preconditions.checkNotNull(rawText);
//  }

  public void save() throws IOException {
    File rootDir = new File(rootDirectoryPath);
    if (!rootDir.exists()) {
      rootDir.mkdirs();
    }
    writeRawText(rootDir.toPath().resolve(RAW_TEXT_FILENAME).toString());
    writeSentences(rootDir.toPath().resolve(TOKENISED_SENTENCES_FILENAME).toString());
    writeSpecificData();
  }

  public void writeSimplified(BufferedWriter bw) throws IOException {
    Preconditions.checkNotNull(bw);

    for (Sentence sentence : getSentences()) {
      for (Token token : sentence.tokens) {
        if (token.partOfSpeech != POSTag.OTHER) {
          bw.write(token.lemma);
          bw.write(" ");
        }
      }
    }

    bw.write("\n");
  }

  private void writeRawText(String filePath) throws IOException {
//    BufferedWriter bw = null;
//    try {
//      try {
//        FileWriter fw = new FileWriter(filePath);
//        bw = new BufferedWriter(fw);
//      } catch (FileNotFoundException e) {
//        e.printStackTrace();
//        return;
//      }
//
//      bw.write(rawText);
//    } finally {
//      if (bw != null) {
//        bw.close();
//      }
//    }
  }

  private void writeSentences(String filePath) throws IOException {
    BufferedWriter bw = null;
    try {
      try {
        FileWriter fw = new FileWriter(filePath);
        bw = new BufferedWriter(fw);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
        return;
      }

      bw.write(Integer.toString(sentences.size()) + "\n");
      for (Sentence sentence : sentences) {
        sentence.writeTo(bw);
      }
    } finally {
      if (bw != null) {
        bw.close();
      }
    }
  }

  protected abstract void writeSpecificData();


  private void generateBagOfLemmas() {
    bagOfLemmas = new BagOfWeightedLemmas(getSentences());
  }

//  private boolean loadRawText() {
//    File rootDir = new File(rootDirectoryPath);
//    if (!rootDir.exists()) {
//      return false;
//    }
//
//    String rawTextPath = rootDir.toPath().resolve(RAW_TEXT_FILENAME).toString();
//    File rawTextFile = new File(rawTextPath);
//    if (!rawTextFile.exists()) {
//      return false;
//    }
//
//    try {
//      loadRawText(rawTextFile);
//    } catch (IOException e) {
//      e.printStackTrace();
//      return false;
//    }
//
//    return true;
//  }

  private boolean loadSentences() {
    File rootDir = new File(rootDirectoryPath);
    if (!rootDir.exists()) {
      return false;
    }

    String sentencesPath = rootDir.toPath().resolve(TOKENISED_SENTENCES_FILENAME).toString();
    File sentencesFile = new File(sentencesPath);
    if (!sentencesFile.exists()) {
      return false;
    }

    try {
      loadSentences(sentencesFile);
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }

    return true;
  }

//  private void loadRawText(File file) throws IOException {
//    StringBuffer rawTextBuffer = new StringBuffer();
//
//    BufferedReader br = null;
//    try {
//      br = new BufferedReader(new FileReader(file.getAbsolutePath()));
//
//      String line = br.readLine();
//      while (line != null) {
//        rawTextBuffer.append(line).append("\n");
//        line = br.readLine();
//      }
//
//    } catch (FileNotFoundException e) {
//      e.printStackTrace();
//      return;
//    } finally {
//      if (br != null) {
//        br.close();
//      }
//    }
//
//    this.rawText = rawTextBuffer.toString();
//  }

  private void loadSentences(File file) throws IOException {
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(file.getAbsolutePath()));

      String line = br.readLine();
      if (line == null) {
        throw new IOException("sentences file does not contain expected data: " + file.getAbsolutePath());
      }

      for (int i = 0; i < Integer.parseInt(line); i++) {
        this.sentences.add(Sentence.readFrom(br));
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
}
