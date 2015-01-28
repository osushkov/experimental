package com.experimental.documentmodel;

import com.experimental.documentvector.ConceptVector;
import com.experimental.documentvector.ConceptVectorImpl;
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
  private static final String TOKENISED_SENTENCES_FILENAME = "sentences.txt";
  private static final String CONCEPT_VECTOR_FILENAME = "concept_vector.txt";

  public final String rootDirectoryPath;

  private List<Sentence> sentences = null;
  private BagOfWeightedLemmas bagOfLemmas = null;
  private ConceptVector conceptVector = null;

  public static boolean isExistingDocumentDirectory(File dir) {
    if (!dir.exists() || !dir.isDirectory()) {
      return false;
    }

    File[] children = dir.listFiles();
    for (File child : children) {
      if (child.toPath().getFileName().toString().equals(TOKENISED_SENTENCES_FILENAME)) {
        return true;
      }

      if (child.toPath().getFileName().toString().equals(CONCEPT_VECTOR_FILENAME)) {
        return true;
      }
    }

    return false;
  }

  public Document(String rootDirectoryPath) {
    this.rootDirectoryPath = Preconditions.checkNotNull(rootDirectoryPath);
  }

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

  public ConceptVector getConceptVector() {
    if (conceptVector == null) {
      loadConceptVector();
    }

    return conceptVector;
  }

  public void addSentence(Sentence sentence) {
    addSentences(Lists.newArrayList(sentence));
  }

  public void addSentences(List<Sentence> sentences) {
    if (this.sentences == null) {
      this.sentences = new ArrayList<Sentence>();
    }
    this.sentences.addAll(Preconditions.checkNotNull(sentences));
  }

  public void setConceptVector(ConceptVector conceptVector) {
    this.conceptVector = Preconditions.checkNotNull(conceptVector);
  }

  public void save() throws IOException {
    File rootDir = new File(rootDirectoryPath);
    if (!rootDir.exists()) {
      rootDir.mkdirs();
    }
    writeSentences(rootDir.toPath().resolve(TOKENISED_SENTENCES_FILENAME).toString());
    writeConceptVector(rootDir.toPath().resolve(CONCEPT_VECTOR_FILENAME).toString());
    writeSpecificData();
  }

  public void writeSimplified(BufferedWriter bw) throws IOException {
    Preconditions.checkNotNull(bw);

    for (Sentence sentence : getSentences()) {
      for (Token token : sentence.tokens) {
        if (token.partOfSpeech != POSTag.OTHER) {
          bw.write(token.lemma.toLowerCase());
          bw.write(" ");
        }
      }
    }

    bw.write("\n");
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

  private void writeConceptVector(String filePath) throws IOException {
    if (conceptVector == null) {
      return;
    }

    BufferedWriter bw = null;
    try {
      try {
        FileWriter fw = new FileWriter(filePath);
        bw = new BufferedWriter(fw);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
        return;
      }

      bw.write(Integer.toString(conceptVector.dimensions()) + "\n");
      for (int i = 0; i < conceptVector.dimensions(); i++) {
        bw.write(Double.toString(conceptVector.getValue(i)) + "\n");
      }
    } finally {
      if (bw != null) {
        bw.close();
      }
    }
  }

  protected abstract void writeSpecificData() throws IOException;


  private void generateBagOfLemmas() {
    bagOfLemmas = new BagOfWeightedLemmas(getSentences());
  }

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

  private void loadSentences(File file) throws IOException {
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(file.getAbsolutePath()));

      String line = br.readLine();
      if (line == null) {
        throw new IOException("sentences file does not contain expected data: " + file.getAbsolutePath());
      }

      for (int i = 0; i < Integer.parseInt(line); i++) {
        Sentence newSentence = Sentence.readFrom(br);
        this.sentences.add(newSentence);

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

  private boolean loadConceptVector() {
    File rootDir = new File(rootDirectoryPath);
    if (!rootDir.exists()) {
      return false;
    }

    String vectorPath = rootDir.toPath().resolve(CONCEPT_VECTOR_FILENAME).toString();
    File vectorFile = new File(vectorPath);
    if (!vectorFile.exists()) {
      return false;
    }

    try {
      loadConceptVector(vectorFile);
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }

    return true;
  }

  private void loadConceptVector(File file) throws IOException {
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(file.getAbsolutePath()));

      Integer numDims = Integer.parseInt(Preconditions.checkNotNull(br.readLine()));
      Preconditions.checkState(numDims > 0);

      conceptVector = new ConceptVectorImpl(numDims);
      for (int i = 0; i < numDims; i++) {
        double val = Double.parseDouble(Preconditions.checkNotNull(br.readLine()));
        conceptVector.setValue(i, val);
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
