package com.experimental.documentmodel;

import com.experimental.nlp.NounPhrase;
import com.experimental.nlp.NounPhraseExtractor;
import com.experimental.nlp.POSTag;
import com.experimental.utils.Log;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by sushkov on 8/01/15.
 */
public class SentenceProcessor {
  private static final String TAG = "SentenceProcessor";
  private final NounPhraseExtractor nounPhraseExtractor = new NounPhraseExtractor();
  private final StanfordCoreNLP pipeline;

  public SentenceProcessor() {
    Properties props = new Properties();
    props.put("annotators", "tokenize, ssplit, pos, lemma");
    props.put("threads", "4");

    pipeline = new StanfordCoreNLP(props);
  }

  public List<Sentence> processString(String text, double emphasis) {
    Preconditions.checkNotNull(text);
    Preconditions.checkArgument(emphasis > 0.0);

//    Log.out(TAG, "processString: " + text);

    if (text.length() == 0) {
      return Lists.newArrayList();
    }

    Annotation annotation = new Annotation(text);
    pipeline.annotate(annotation);

    List<Sentence> result = new ArrayList<Sentence>();

    List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
    for(CoreMap sentence: sentences) {
      List<Token> resultSentenceTokens = new ArrayList<Token>();
      for (CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class)) {
        String posAnnotation = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
        POSTag posTag = posTagFromString(posAnnotation.toUpperCase());

        resultSentenceTokens.add(new Token(
            token.get(CoreAnnotations.TextAnnotation.class),
            token.get(CoreAnnotations.LemmaAnnotation.class),
            posTag
        ));
      }

      result.add(new Sentence(resultSentenceTokens, emphasis));
    }

    return result;
  }

  private POSTag posTagFromString(String posAnnotation) {
    try {
      return POSTag.valueOf(posAnnotation);
    } catch (IllegalArgumentException e) {
      if (posAnnotation.equals(POSTag.DOT.getTagString())) {
        return POSTag.DOT;
      }
      if (posAnnotation.equals(POSTag.COMMA.getTagString())) {
        return POSTag.COMMA;
      }
      if (posAnnotation.equals(POSTag.COLON.getTagString())) {
        return POSTag.COLON;
      }
      if (posAnnotation.equals(POSTag.LEFT_PAREN.getTagString())) {
        return POSTag.LEFT_PAREN;
      }
      if (posAnnotation.equals(POSTag.RIGHT_PAREN.getTagString())) {
        return POSTag.RIGHT_PAREN;
      }

      return POSTag.OTHER;
    }
  }

}
