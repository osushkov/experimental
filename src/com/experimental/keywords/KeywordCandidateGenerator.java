package com.experimental.keywords;

import com.experimental.documentmodel.BagOfWeightedLemmas;
import com.experimental.documentmodel.Sentence;
import com.experimental.documentmodel.WebsiteDocument;
import com.experimental.languagemodel.Lemma;
import com.experimental.languagemodel.LemmaDB;
import com.experimental.nlp.NounPhrase;
import com.experimental.nlp.NounPhraseExtractor;
import com.experimental.nlp.SimplePOSTag;
import com.experimental.utils.Log;
import com.google.common.base.Preconditions;

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

  public List<KeywordCandidate> generateCandidates(WebsiteDocument document) {
    List<KeywordCandidate> result = new ArrayList<KeywordCandidate>();

    result.addAll(getCandidatesFromNounPhrases(document));

    return result;
  }

  private List<KeywordCandidate> getCandidatesFromNounPhrases(WebsiteDocument document) {
    List<WeightedNounPhrase> weightedPhrases = extractNounPhrases(document);
    List<KeywordCandidate> result = new ArrayList<KeywordCandidate>();
    for (WeightedNounPhrase phrase : weightedPhrases) {
      if (phrase.weight > 1.0) {
        Log.out(phrase.phrase.toString() + " " + phrase.weight);
        result.add(phrase.toKeywordCandidate());
      }
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

  private List<BagOfWeightedLemmas.WeightedLemmaEntry> getAllTaggedLemmas(WebsiteDocument document, SimplePOSTag tag) {
    List<BagOfWeightedLemmas.WeightedLemmaEntry> result = new ArrayList<BagOfWeightedLemmas.WeightedLemmaEntry>();
    for (BagOfWeightedLemmas.WeightedLemmaEntry entry : document.getBagOfLemmas().getEntries()) {
      if (entry.lemma.tag.equals(tag)) {
        result.add(entry);
      }
    }
    return result;
  }

}
