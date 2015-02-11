package com.experimental.keywords;

import com.experimental.documentmodel.BagOfWeightedLemmas;
import com.experimental.documentmodel.Sentence;
import com.experimental.documentmodel.WebsiteDocument;
import com.experimental.documentvector.DocumentVectorDB;
import com.experimental.languagemodel.*;
import com.experimental.nlp.NounPhrase;
import com.experimental.nlp.NounPhraseExtractor;
import com.experimental.nlp.SimplePOSTag;
import com.experimental.sitepage.SitePage;
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

    @Override
    public String toString() {
      StringBuffer buffer = new StringBuffer();
      for (Lemma lemma : phraseLemmas) {
        buffer.append(lemma.lemma).append(" ");
      }
      return buffer.toString();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      KeywordCandidate candidate = (KeywordCandidate) o;

      if (!phraseLemmas.equals(candidate.phraseLemmas)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      return phraseLemmas.hashCode();
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
  private final LemmaOccuranceStatsAggregator lemmaStats;
  private final KeywordSanityChecker keywordSanityChecker;
  private final DocumentVectorDB documentVectorDb;

  public KeywordCandidateGenerator(NounPhrasesDB nounPhraseDb, LemmaOccuranceStatsAggregator lemmaStats,
                                   KeywordSanityChecker keywordSanityChecker, DocumentVectorDB documentVectorDb) {
    this.nounPhraseDb = Preconditions.checkNotNull(nounPhraseDb);
    this.lemmaStats = Preconditions.checkNotNull(lemmaStats);
    this.keywordSanityChecker = Preconditions.checkNotNull(keywordSanityChecker);
    this.documentVectorDb = Preconditions.checkNotNull(documentVectorDb);
  }

  public Set<KeywordCandidate> generateCandidates(WebsiteDocument document) {
    Set<KeywordCandidate> result = new HashSet<KeywordCandidate>();

    result.addAll(getCandidatesFromNounPhrases(document));
    result.addAll(getCandidatesFromCompositedPhrases(document));
//    result.addAll(getCandidatesFromHeader(document));
//    result.addAll(getCandidatesFromSimilarDocuments(document));

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

//    for (WeightedNounPhrase entry : nounPhrasesMap.values()) {
//      entry.weight *= getPhraseQualityScale(entry.phrase);
//    }

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

    for (int i = 0; i < Math.min(10, nouns.size()); i++) {
      result.add(new KeywordCandidate(Lists.newArrayList(nouns.get(i).lemma)));
    }

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

        if (isForwardValid && isBackwardValid) {
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

  private List<KeywordCandidate> getCandidatesFromHeader(WebsiteDocument document) {
    List<KeywordCandidate> result = new ArrayList<KeywordCandidate>();

    List<SitePage> pages = document.getSitePages();
    if (pages.size() == 0) {
      return result;
    }

    SitePage frontPage = pages.get(0);
    for (Sentence sentence : frontPage.header.keywords) {
      List<NounPhrase> phrases = nounPhraseExtractor.extractNounPhrases(sentence);
      for (NounPhrase phrase : phrases) {
        result.add(new KeywordCandidate(phrase.getPhraseLemmas()));
      }
    }

    return result;
  }

  private List<KeywordCandidate> getCandidatesFromSimilarDocuments(WebsiteDocument document) {
    List<KeywordCandidate> result = new ArrayList<KeywordCandidate>();

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
        result.add(new BagOfWeightedLemmas.WeightedLemmaEntry(entry));
      }
    }

//    for (BagOfWeightedLemmas.WeightedLemmaEntry entry : result) {
//      LemmaOccuranceStatsAggregator.LemmaStats stats = lemmaStats.getLemmaStats(entry.lemma);
//      if (stats != null) {
//        entry.weight *= stats.weightStandardDeviation;
//      }
//    }

    Comparator<BagOfWeightedLemmas.WeightedLemmaEntry> orderFunc =
        new Comparator<BagOfWeightedLemmas.WeightedLemmaEntry>() {
          public int compare(BagOfWeightedLemmas.WeightedLemmaEntry e1, BagOfWeightedLemmas.WeightedLemmaEntry e2) {
            return Double.compare(e2.weight, e1.weight);
          }
        };

    result.sort(orderFunc);
    return result;
  }

  private double getPhraseQualityScale(NounPhrase phrase) {
    int num = 0;
    double sum = 0.0;
    for (Lemma lemma : phrase.getPhraseLemmas()) {
      LemmaOccuranceStatsAggregator.LemmaStats stats = lemmaStats.getLemmaStats(lemma);

      if (stats != null) {
        num++;
        sum += stats.weightStandardDeviation;
      }
    }
    if (num > 0) {
      return sum / num;
    } else {
      return 1.0;
    }
  }
}
