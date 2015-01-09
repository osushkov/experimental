package com.experimental.nlp;

import com.google.common.base.Preconditions;

/**
 * Created by sushkov on 9/01/15.
 * Penn Treebank tag-set. Taken from http://www.clips.ua.ac.be/pages/mbsp-tags
 */
public enum POSTag {
  CC("CC"),   // Conjunction
  CD("CD"),   // Cardinal number
  DT("DT"),   // Determiner
  EX("EX"),   // Existential there
  FW("FW"),   // Foreign word
  IN("IN"),   // Preposition
  JJ("JJ"),   // Adjective
  JJR("JJR"),  // Adjective, comparative
  JJS("JJS"), // Adjective, superlative
  LS("LS"), // List item marker
  MD("MD"), // Modal
  NN("NN"), // Noun
  NNS("NNS"), // Noun plural
  NNP("NNP"), // Proper Noun
  NNPS("NNPS"), // Proper Noun plural
  PDT("PDT"), // Predeterminer
  POS("POS"), // Possessive ending
  PRP("PRP"), // Personal pronoun
  PRP$("PRP$"), // Possessive pronoun
  RB("RB"), // Adverb
  RBR("RBR"), // Adverb, comparative
  RBS("RBS"), // Adverb, superlative
  RP("RP"), // Particle
  SYM("SYM"), // Symbol
  TO("TO"), // to
  UH("UH"), // Interjection
  VB("VB"), // Verb, base form
  VBZ("VBZ"), // Verb, 3rd person singular present
  VBP("VBP"), // Verb, non-3rd person singular present
  VBD("VBD"), // Verb, past tense
  VBN("VBN"), // Verb, past participle
  VBG("VBG"), // Verb, gerund or present participle
  WDT("WDT"), // Wh-determiner
  WP("WP"), // Wh-pronoun
  WP$("WP$"), // Possessive wh-pronoun
  WRB("WRB"), // Wh-adverb
  DOT("."),
  COMMA(","),
  COLON(":"),
  LEFT_PAREN("("),
  RIGHT_PAREN(")"),
  OTHER("OTHER");

  private final String tag;

  private POSTag(String tag) {
    this.tag = Preconditions.checkNotNull(tag);
  }

  public String getTagString() {
    return tag;
  }

  public boolean isNoun() {
    return this == POSTag.NN || this == POSTag.NNS || this == POSTag.NNP || this == POSTag.NNPS;
  }

  public boolean isVerb() {
    return this == POSTag.VBZ || this == POSTag.VBP || this == POSTag.VBD ||
        this == POSTag.VBN || this == POSTag.VBG;
  }

  public boolean isAdjective() {
    return this == POSTag.JJ || this == POSTag.JJR || this == POSTag.JJS;
  }

}
