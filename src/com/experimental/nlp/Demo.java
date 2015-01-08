package com.experimental.nlp;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.ArrayCoreMap;
import edu.stanford.nlp.util.CoreMap;

import java.io.PrintWriter;
import java.util.List;
import java.util.Properties;

/**
 * Created by sushkov on 6/01/15.
 */
public class Demo {
  public static void runDemo() {
    PrintWriter out = new PrintWriter(System.out, true);

    PrintWriter xmlOut = null;

    Properties props = new Properties();
    props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse");
    props.put("threads", "4");
    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
    Annotation annotation = new Annotation("The area around Sydney has been inhabited by indigenous Australians for tens " +
        "of millennia. The first British settlers arrived in 1788 with Captain Arthur Phillip and founded Sydney as " +
        "a penal colony. Successive colonial Governors assisted to transform the settlement into a thriving and " +
        "independent metropolis. Since convict transportation ended in the mid 1800s the city has become a global " +
        "cultural and economic centre. The population of Sydney at the time of the 2011 census was 4.39 million. " +
        "About 1.5 million of this total were born overseas and represent many different countries from around the world. " +
        "There are more than 250 different languages spoken in Sydney and about one-third of residents speak a language " +
        "other than English at home. dsfsdfsfsf");

    pipeline.annotate(annotation);
    pipeline.prettyPrint(annotation, out);

    // An Annotation is a Map and you can get and use the various analyses individually.
    // For instance, this gets the parse tree of the first sentence in the text.
    out.println();
    // The toString() method on an Annotation just prints the text of the Annotation
    // But you can see what is in it with other methods like toShorterString()

    List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
    for (CoreMap sentence : sentences) {

//      Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
//      tree.pennPrint();

//      for (Tree subTree : tree) {
//        out.println(subTree.nodeString() + " " + tree.leftCharEdge(subTree) + " - " + tree.rightCharEdge(subTree));
//      }
//      tree.pennPrint(out);

//      for (CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class)) {
//        String word = token.get(CoreAnnotations.TextAnnotation.class);
//        String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);
//        String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
//        String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
//
//        out.println(word + " : " + lemma + " " + pos + " " + ne);
//      }
    }

//    if (sentences != null && sentences.size() > 0) {
//      ArrayCoreMap sentence = (ArrayCoreMap) sentences.get(0);
//      out.println("The first sentence is:");
//      out.println(sentence.toShorterString());
//      Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
//      out.println();
//      out.println("The first sentence tokens are:");
//      for (CoreMap token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
//        ArrayCoreMap aToken = (ArrayCoreMap) token;
//        out.println(aToken.toShorterString());
//      }
//      out.println("The first sentence parse tree is:");
//      tree.pennPrint(out);
//      out.println("The first sentence basic dependencies are:");
//      System.out.println(sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class).toString(SemanticGraph.OutputFormat.LIST));
//      out.println("The first sentence collapsed, CC-processed dependencies are:");
//      SemanticGraph graph = sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
//      System.out.println(graph.toString(SemanticGraph.OutputFormat.LIST));
//    }
  }

  public static void runDemo2() {
    Properties props = new Properties();
    props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

    // read some text in the text variable
    String text = "Kosgi Santosh sent an email to Stanford University. He didn't get a reply.";

    // create an empty Annotation just with the given text
    Annotation document = new Annotation(text);

    // run all Annotators on this text
    pipeline.annotate(document);

    // these are all the sentences in this document
    // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
    List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

    for(CoreMap sentence: sentences) {
      // traversing the words in the current sentence
      // a CoreLabel is a CoreMap with additional token-specific methods
      for (CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class)) {
        // this is the text of the token
        String word = token.get(CoreAnnotations.TextAnnotation.class);
        // this is the POS tag of the token
        String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
        // this is the NER label of the token
        String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
      }

      // this is the parse tree of the current sentence
      Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);

      // this is the Stanford dependency graph of the current sentence
      SemanticGraph dependencies = sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
    }
  }
}
