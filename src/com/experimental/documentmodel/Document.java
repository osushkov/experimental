package com.experimental.documentmodel;

import com.google.common.base.Preconditions;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sushkov on 4/01/15.
 */
public abstract class Document {
  private static final String RAW_TEXT_FILENAME = "raw.txt";
  private static final String TOKENISED_SENTENCES_FILENAME = "sentences.txt";
  private static final String BAG_OF_LEMMAS_FILENAME = "bag_of_lemmas.txt";

  private String rootDirectoryPath;
  private final List<Sentence> sentences = new ArrayList<Sentence>();
  private String rawText = "";
  private BagOfWeightedLemmas bagOfLemmas = null;

  public Document(String rootDirectoryPath) {
    this.rootDirectoryPath = Preconditions.checkNotNull(rootDirectoryPath);
  }

  // Usually this is the title of the document.
  public Sentence getMostImportantSentence() {
    Sentence result = null;
    double highestEmphasis = 0.0;
    for (Sentence sentence : sentences) {
      if (sentence.emphasis > highestEmphasis) {
        highestEmphasis = sentence.emphasis;
        result = sentence;
      }
    }
    return result;
  }

  public String getRawText() {
    return rawText;
  }

  public List<Sentence> getSentences() {
    return sentences;
  }

  public BagOfWeightedLemmas getBagOfLemmas() {
    if (bagOfLemmas == null) {
      generateBagOfLemmas();
    }

    return Preconditions.checkNotNull(bagOfLemmas);
  }

  public void addSentence(Sentence sentence) {
    this.sentences.add(Preconditions.checkNotNull(sentence));
  }

  public void addSentences(List<Sentence> sentences) {
    this.sentences.addAll(Preconditions.checkNotNull(sentences));
  }

  public void setText(String rawText) {
    this.rawText = Preconditions.checkNotNull(rawText);
  }

  public void save() throws IOException {
    File rootDir = new File(rootDirectoryPath);
    if (!rootDir.exists()) {
      rootDir.mkdirs();
    }
    writeRawText(rootDir.toPath().resolve(RAW_TEXT_FILENAME).toString());
    writeSentences(rootDir.toPath().resolve(TOKENISED_SENTENCES_FILENAME).toString());
    writeSpecificData();
  }

  private void generateBagOfLemmas() {
    bagOfLemmas = new BagOfWeightedLemmas(sentences);
  }

  private void writeRawText(String filePath) throws IOException {
    BufferedWriter bw = null;
    try {
      try {
        FileWriter fw = new FileWriter(filePath);
        bw = new BufferedWriter(fw);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
        return;
      }

      bw.write(rawText);
    } finally {
      if (bw != null) {
        bw.close();
      }
    }
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

  protected boolean loadFromDirectory() throws IOException {
    File rootDirectory = new File(rootDirectoryPath);
    if (!rootDirectory.exists()) {
      throw new IOException("root document directory does not exist: " + rootDirectoryPath);
    }

    boolean haveRawText = false;
    boolean haveSentences = false;

    for (File child : rootDirectory.listFiles()) {
      if (child.getAbsolutePath().endsWith(RAW_TEXT_FILENAME)) {
        loadRawText(child);
        haveRawText = true;
      } else if (child.getAbsolutePath().endsWith(TOKENISED_SENTENCES_FILENAME)) {
        loadSentences(child);
        haveSentences = true;
      }
    }

    return haveRawText && haveSentences;
  }

  private void loadRawText(File file) throws IOException {
    StringBuffer rawTextBuffer = new StringBuffer();

    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(file.getAbsolutePath()));

      String line = br.readLine();
      while (line != null) {
        rawTextBuffer.append(line).append("\n");
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

    this.rawText = rawTextBuffer.toString();
  }

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
