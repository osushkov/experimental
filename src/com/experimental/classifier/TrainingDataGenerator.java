package com.experimental.classifier;

import com.experimental.documentmodel.WebsiteDocument;
import com.experimental.keywords.KeywordCandidateGenerator;
import com.experimental.languagemodel.Lemma;
import com.experimental.languagemodel.LemmaOccuranceStatsAggregator;
import com.experimental.languagemodel.LemmaQuality;
import com.experimental.languagemodel.NounPhrasesDB;
import com.experimental.sitepage.SitePage;
import com.experimental.utils.Log;
import com.google.common.base.Preconditions;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

/**
 * Created by sushkov on 31/01/15.
 */
public class TrainingDataGenerator {

  private final NounPhrasesDB nounPhraseDb;
  private final KeywordCandidateGenerator candidateGenerator;

  public TrainingDataGenerator(NounPhrasesDB nounPhraseDb, LemmaOccuranceStatsAggregator lemmaStats) {
    this.nounPhraseDb = Preconditions.checkNotNull(nounPhraseDb);
    this.candidateGenerator = new KeywordCandidateGenerator(nounPhraseDb, lemmaStats);
  }

  public void outputTrainingData(WebsiteDocument document, BufferedWriter out) throws IOException {
    Preconditions.checkNotNull(document);
    Preconditions.checkNotNull(out);

    try {
      nounPhraseDb.tryLoad();
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    List<KeywordCandidateGenerator.KeywordCandidate> candidates = candidateGenerator.generateCandidates(document);
    if (candidates.size() == 0) {
      return;
    }

    String documentPath = document.rootDirectoryPath;
    List<SitePage> sitePages = document.getSitePages();
    if (sitePages.size() == 0) {
       return;
    }

    String frontPageUrl = sitePages.get(0).url;
    if (frontPageUrl.length() < 3) {
      return;
    }

    out.write(documentPath + "\n");
    out.write(frontPageUrl + "\n");

    for (KeywordCandidateGenerator.KeywordCandidate candidate : candidates) {
      for (int i = 0; i < candidate.phraseLemmas.size(); i++) {
        if (i > 0) {
          out.write(" ");
        }
        out.write(candidate.phraseLemmas.get(i).lemma);
      }
      out.write("\n");
    }
    out.write("\n");

  }
}
