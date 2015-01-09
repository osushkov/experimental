package com.experimental.nlp;

import com.experimental.documentmodel.Token;
import com.google.common.base.Preconditions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sushkov on 9/01/15.
 */
public class NounPhrase {
  private final List<Token> phraseTokens = new ArrayList<Token>();

  public NounPhrase(List<Token> phraseTokens) {
    this.phraseTokens.addAll(generateSimplifiedPhrase(Preconditions.checkNotNull(phraseTokens)));
  }

  public static NounPhrase readFrom(BufferedReader in) throws IOException {
    Preconditions.checkNotNull(in);

    List<Token> phraseTokens = new ArrayList<Token>();

    String line = Preconditions.checkNotNull(in.readLine());
    int numPhraseTokens = Integer.parseInt(line);
    for (int i = 0; i < numPhraseTokens; i++) {
      phraseTokens.add(Token.readFrom(in));
    }

    return new NounPhrase(phraseTokens);
  }

  public void writeTo(BufferedWriter bw) throws IOException {
    bw.write(Integer.toString(phraseTokens.size())); bw.write("\n");
    for (Token token : phraseTokens) {
      token.writeTo(bw);
    }
  }

  public List<Token> getPhraseTokens() {
    return phraseTokens;
  }

  @Override
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    for (Token token : phraseTokens) {
      buffer.append(token.raw).append(" ");
    }
    return buffer.toString();
  }

  private List<Token> generateSimplifiedPhrase(List<Token> phrase) {
    List<Token> result = new ArrayList<Token>();

    for (Token token : phrase) {
      if (isSimpleToken(token)) {
        result.add(token);
      }
    }

    return result;
  }

  private boolean isSimpleToken(Token token) {
    return token.partOfSpeech != POSTag.DT;
  }

  public boolean isCompositePhrase() {
    return phraseTokens.size() > 1;
  }

}
