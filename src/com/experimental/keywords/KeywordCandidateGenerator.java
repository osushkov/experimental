package com.experimental.keywords;

import com.experimental.documentmodel.BagOfWeightedLemmas;
import com.experimental.documentmodel.Sentence;
import com.experimental.documentmodel.WebsiteDocument;
import com.experimental.languagemodel.Lemma;
import com.experimental.languagemodel.LemmaDB;
import com.experimental.languagemodel.NounPhrasesDB;
import com.experimental.nlp.NounPhrase;
import com.experimental.nlp.NounPhraseExtractor;
import com.experimental.nlp.SimplePOSTag;
import com.experimental.utils.Log;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.security.Key;
import java.util.*;

/**
 * Created by sushkov on 28/01/15.
 */
public class KeywordCandidateGenerator {

  public static class KeywordCandidate {
    public final List<Lemma> phraseLemmas;

    private KeywordCandidate(List<Lemma> phraseLemmas) {
      this.phraseLemmas = Preconditions.checkNotNull(phraseLemmas);
      Preconditions.checkArgument(phraseLemmas.size() > 0);
    }
  }

  private static class WeightedNounPhrase {
    final NounPhrase phrase;
    double weight;

    WeightedNounPhrase(NounPhrase phrase) {
      this(phrase, 0.0);
    }

    WeightedNounPhrase(NounPhrase phrase, double weight) {
      this.phrase = Preconditions.checkNotNull(phrase);
      this.weight = weight;
      Preconditions.checkArgument(weight >= 0.0);
    }

    KeywordCandidate toKeywordCandidate() {
      return new KeywordCandidate(phrase.getPhraseLemmas());
    }
  }

  private final NounPhraseExtractor nounPhraseExtractor = new NounPhraseExtractor(LemmaDB.instance);
  private final NounPhrasesDB nounPhraseDb;

  public KeywordCandidateGenerator(NounPhrasesDB nounPhraseDb) {
    this.nounPhraseDb = Preconditions.checkNotNull(nounPhraseDb);
  }

  public List<KeywordCandidate> generateCandidates(WebsiteDocument document) {
    List<KeywordCandidate> result = new ArrayList<KeywordCandidate>();

    result.addAll(getCandidatesFromNounPhrases(document));
    result.addAll(getCandidatesFromCompositedPhrases(document));

    return result;
  }

  private List<KeywordCandidate> getCandidatesFromNounPhrases(WebsiteDocument document) {
    List<WeightedNounPhrase> weightedPhrases = extractNounPhrases(document);
    List<KeywordCandidate> result = new ArrayList<KeywordCandidate>();
    for (int i = 0; i < Math.min(weightedPhrases.size(), 10); i++) {
      WeightedNounPhrase phrase = weightedPhrases.get(i);
      result.add(phrase.toKeywordCandidate());
    }
    return result;
  }

  private List<WeightedNounPhrase> extractNounPhrases(WebsiteDocument document) {
    Map<NounPhrase, WeightedNounPhrase> nounPhrasesMap = new HashMap<NounPhrase, WeightedNounPhrase>();

    for (Sentence sentence : document.getSentences()) {
      List<NounPhrase> sentencePhrases = nounPhraseExtractor.extractNounPhrases(sentence);
      for (NounPhrase phrase : sentencePhrases) {
        nounPhrasesMap.putIfAbsent(phrase, new WeightedNounPhrase(phrase));
        nounPhrasesMap.get(phrase).weight += sentence.emphasis;
      }
    }

    Comparator<WeightedNounPhrase> weightOrder =
        new Comparator<WeightedNounPhrase>() {
          public int compare(WeightedNounPhrase e1, WeightedNounPhrase e2) {
            return Double.compare(e2.weight, e1.weight);
          }
        };

    List<WeightedNounPhrase> result = new ArrayList<WeightedNounPhrase>(nounPhrasesMap.values());
    result.sort(weightOrder);

    return result;
  }

  private List<KeywordCandidate> getCandidatesFromCompositedPhrases(WebsiteDocument document) {
    List<KeywordCandidate> result = new ArrayList<KeywordCandidate>();

    List<BagOfWeightedLemmas.WeightedLemmaEntry> adjectives = getAllTaggedLemmas(document, SimplePOSTag.ADJECTIVE);
    List<BagOfWeightedLemmas.WeightedLemmaEntry> nouns = getAllTaggedLemmas(document, SimplePOSTag.NOUN);

    for (int i = 0; i < Math.min(5, adjectives.size()); i++) {
      for (int j = 0; j < Math.min(10, nouns.size()); j++) {
        List<Lemma> phrase = Lists.newArrayList(adjectives.get(i).lemma, nouns.get(j).lemma);
        if (isValidPhrase(phrase)) {
          result.add(new KeywordCandidate(phrase));
        }
      }
    }

    for (int i = 0; i < Math.min(10, nouns.size())-1; i++) {
      for (int j = i+1; j < Math.min(10, nouns.size()); j++) {
        List<Lemma> forwardPhrase = Lists.newArrayList(nouns.get(i).lemma, nouns.get(j).lemma);
        List<Lemma> backwardPhrase = Lists.newArrayList(nouns.get(j).lemma, nouns.get(i).lemma);
        boolean isForwardValid = isValidPhrase(forwardPhrase);
        boolean isBackwardValid = isValidPhrase(backwardPhrase);

        if (isForwardValid && isForwardValid) {
          NounPhrasesDB.NounPhraseEntry forwardEntry =
              nounPhraseDb.getPhraseEntry(new NounPhrase(forwardPhrase, LemmaDB.instance));
          NounPhrasesDB.NounPhraseEntry backwardEntry =
              nounPhraseDb.getPhraseEntry(new NounPhrase(backwardPhrase, LemmaDB.instance));

          if (forwardEntry.numOccurances.get() > backwardEntry.numOccurances.get()) {
            result.add(new KeywordCandidate(forwardPhrase));
          } else {
            result.add(new KeywordCandidate(backwardPhrase));
          }

        } else if (isForwardValid) {
          result.add(new KeywordCandidate(forwardPhrase));
        } else if (isBackwardValid) {
          result.add(new KeywordCandidate(backwardPhrase));
        }
      }
    }

    return result;
  }

  private boolean isValidPhrase(List<Lemma> phrase) {
    NounPhrase nounPhrase = new NounPhrase(phrase, LemmaDB.instance);
    NounPhrasesDB.NounPhraseEntry entry = nounPhraseDb.getPhraseEntry(nounPhrase);
    return entry != null && entry.numOccurances.get() > 5;
  }

  private List<BagOfWeightedLemmas.WeightedLemmaEntry> getAllTaggedLemmas(WebsiteDocument document, SimplePOSTag tag) {
    List<BagOfWeightedLemmas.WeightedLemmaEntry> result = new ArrayList<BagOfWeightedLemmas.WeightedLemmaEntry>();
    for (BagOfWeightedLemmas.WeightedLemmaEntry entry : document.getBagOfLemmas().getEntries()) {
      if (entry.lemma.tag.equals(tag)) {
        result.add(entry);
      }
    }

    Comparator<BagOfWeightedLemmas.WeightedLemmaEntry> orderFunc =
        new Comparator<BagOfWeightedLemmas.WeightedLemmaEntry>() {
          public int compare(BagOfWeightedLemmas.WeightedLemmaEntry e1, BagOfWeightedLemmas.WeightedLemmaEntry e2) {
            return Double.compare(e2.weight, e1.weight);
          }
        };

    result.sort(orderFunc);
    return result;
  }

}
