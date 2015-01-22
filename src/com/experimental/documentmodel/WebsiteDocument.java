package com.experimental.documentmodel;

import com.experimental.sitepage.SitePage;
import com.google.common.base.Preconditions;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sushkov on 6/01/15.
 */
public class WebsiteDocument extends Document {

  private static final double FRONT_PAGE_EMPHASIS_MULTIPLIER = 2.0;
  private static final String SITES_FILENAME = "sites.txt";

  private SitePage frontPage = null;
  private List<SitePage> sitePages = null;

  public WebsiteDocument(String rootDirectoryPath) {
    super(rootDirectoryPath);
  }

  public List<SitePage> getSitePages() {
    if (sitePages == null) {
      sitePages = new ArrayList<SitePage>();
      tryLoadSitePages();
    }

    return sitePages;
  }

  public void setFrontPage(SitePage page) {
    this.frontPage = Preconditions.checkNotNull(page);
  }

  public void addChildPage(SitePage page) {
    if (sitePages == null) {
      sitePages = new ArrayList<SitePage>();
    }

    sitePages.add(Preconditions.checkNotNull(page));
  }

  public void buildDocument() {
    List<Sentence> frontPageSentences = frontPage.getFlatSentences();
    for (Sentence frontPageSentence : frontPageSentences) {
      Sentence weightedSentence =
          new Sentence(frontPageSentence.tokens, frontPageSentence.emphasis * FRONT_PAGE_EMPHASIS_MULTIPLIER);
      this.addSentence(weightedSentence);
    }

    if (sitePages != null) {
      for (SitePage page : sitePages) {
        this.addSentences(page.getFlatSentences());
      }
    }
  }

  @Override
  protected void writeSpecificData() throws IOException {
    File rootDir = new File(rootDirectoryPath);
    Preconditions.checkState(rootDir.exists());

    String siteFilePath = rootDir.toPath().resolve(SITES_FILENAME).toString();
    BufferedWriter bw = null;
    try {
      try {
        FileWriter fw = new FileWriter(siteFilePath);
        bw = new BufferedWriter(fw);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
        return;
      }

      frontPage.writeTo(bw);

      if (sitePages == null) {
        bw.write("0\n");
      } else {
        bw.write(Integer.toString(sitePages.size()) + "\n");
        for (SitePage page : sitePages) {
          page.writeTo(bw);
        }
      }
    } finally {
      if (bw != null) {
        bw.close();
      }
    }

  }

  private boolean tryLoadSitePages() {
    return false;
  }
}
