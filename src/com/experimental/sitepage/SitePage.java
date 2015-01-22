package com.experimental.sitepage;

import com.experimental.documentmodel.Sentence;
import com.google.common.base.Preconditions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sushkov on 4/01/15.
 */
public class SitePage {

  public static class Link {
    public final List<Sentence> linkText;
    public final URL destination;

    public Link(List<Sentence> linkText, URL destination) {
      this.linkText = Preconditions.checkNotNull(linkText);
      this.destination = Preconditions.checkNotNull(destination);
    }

    public void writeTo(BufferedWriter bw) throws IOException {
      bw.write(Integer.toString(linkText.size()) + "\n");
      for (Sentence sentence : linkText) {
        sentence.writeTo(bw);
      }
      bw.write(destination.toString() + "\n");
    }

    public static Link readFrom(BufferedReader in) throws IOException {
      int numSentences = Integer.parseInt(Preconditions.checkNotNull(in.readLine()));
      Preconditions.checkState(numSentences >= 0);

      List<Sentence> linkText = new ArrayList<Sentence>();
      for (int i = 0; i < numSentences; i++) {
        Sentence newSentence = Sentence.readFrom(in);
        linkText.add(newSentence);
      }

      URL destination = new URL(Preconditions.checkNotNull(in.readLine()));
      return new Link(linkText, destination);
    }

    @Override
    public String toString() {
      StringBuffer buffer = new StringBuffer();
      buffer.append(destination.toString() + "\n");
      for (Sentence sentence : linkText) {
        buffer.append(sentence.toString() + "\n");
      }
      return buffer.toString();
    }

    
  }

  public static class HeaderInfo {
    public final List<Sentence> title;
    public final List<Sentence> description;
    public final List<Sentence> keywords;

    public HeaderInfo(List<Sentence> title, List<Sentence> description, List<Sentence> keywords) {
      this.title = Preconditions.checkNotNull(title);
      this.description = Preconditions.checkNotNull(description);
      this.keywords = Preconditions.checkNotNull(keywords);
    }

    public void writeTo(BufferedWriter bw) throws IOException {
      Preconditions.checkNotNull(bw);

      bw.write(Integer.toString(title.size()) + "\n");
      for (Sentence sentence : title) {
        sentence.writeTo(bw);
      }

      bw.write(Integer.toString(description.size()) + "\n");
      for (Sentence sentence : description) {
        sentence.writeTo(bw);
      }

      bw.write(Integer.toString(keywords.size()) + "\n");
      for (Sentence sentence : keywords) {
        sentence.writeTo(bw);
      }
    }

    public static HeaderInfo readFrom(BufferedReader in) throws IOException {
      int numSentences = Integer.parseInt(Preconditions.checkNotNull(in.readLine()));
      Preconditions.checkState(numSentences >= 0);

      List<Sentence> title = new ArrayList<Sentence>();
      for (int i = 0; i < numSentences; i++) {
        Sentence newSentence = Sentence.readFrom(in);
        title.add(newSentence);
      }


      numSentences = Integer.parseInt(Preconditions.checkNotNull(in.readLine()));
      Preconditions.checkState(numSentences >= 0);

      List<Sentence> description = new ArrayList<Sentence>();
      for (int i = 0; i < numSentences; i++) {
        Sentence newSentence = Sentence.readFrom(in);
        description.add(newSentence);
      }


      numSentences = Integer.parseInt(Preconditions.checkNotNull(in.readLine()));
      Preconditions.checkState(numSentences >= 0);

      List<Sentence> keywords = new ArrayList<Sentence>();
      for (int i = 0; i < numSentences; i++) {
        Sentence newSentence = Sentence.readFrom(in);
        keywords.add(newSentence);
      }

      return new HeaderInfo(title, description, keywords);
    }
  }

  public final String url;
  public final HeaderInfo header;

  public final List<Link> outgoingLinks = new ArrayList<Link>();
  public final List<Sentence> incomingLinks = new ArrayList<Sentence>();
  public final List<PageBox> pageBoxes = new ArrayList<PageBox>();

  public SitePage(String url, HeaderInfo header) {
    this.url = Preconditions.checkNotNull(url);
    this.header = Preconditions.checkNotNull(header);
  }

  public List<Sentence> getFlatSentences() {
    List<Sentence> result = new ArrayList<Sentence>();
    result.addAll(header.title);
    result.addAll(header.description);
    result.addAll(header.keywords);

    for (PageBox box : pageBoxes) {
      result.addAll(box.getTextContent());
    }

    return result;
  }

  public void writeTo(BufferedWriter bw) throws IOException {
    Preconditions.checkNotNull(bw);

    bw.write(url + "\n");
    header.writeTo(bw);

    bw.write(Integer.toString(outgoingLinks.size()) + "\n");
    for (Link link : outgoingLinks) {
      link.writeTo(bw);
    }

    bw.write(Integer.toString(incomingLinks.size()) + "\n");
    for (Sentence link : incomingLinks) {
      link.writeTo(bw);
    }

    bw.write(Integer.toString(pageBoxes.size()) + "\n");
    for (PageBox box : pageBoxes) {
      if (box instanceof ImagePageBox) {
        bw.write("ImagePageBox\n");
      } else if (box instanceof  TextPageBox) {
        bw.write("TextPageBox\n");
      } else {
        assert(false);
      }

      box.writeTo(bw);
    }
  }

  public static SitePage readFrom(BufferedReader in) throws IOException {
    String url = Preconditions.checkNotNull(in.readLine());
    HeaderInfo header = HeaderInfo.readFrom(in);

    SitePage result = new SitePage(url, header);

    int numOutgoingLinks = Integer.parseInt(Preconditions.checkNotNull(in.readLine()));
    Preconditions.checkState(numOutgoingLinks >= 0);
    for (int i = 0; i < numOutgoingLinks; i++) {
      Link link = Link.readFrom(in);
      if (link != null) {
        result.outgoingLinks.add(link);
      }
    }

    int numIncomingLinks = Integer.parseInt(Preconditions.checkNotNull(in.readLine()));
    Preconditions.checkState(numIncomingLinks >= 0);
    for (int i = 0; i < numIncomingLinks; i++) {
      Sentence link = Sentence.readFrom(in);
      result.incomingLinks.add(link);
    }

    int numPageBoxes = Integer.parseInt(Preconditions.checkNotNull(in.readLine()));
    Preconditions.checkState(numPageBoxes >= 0);
    for (int i = 0; i < numPageBoxes; i++) {
      String type = Preconditions.checkNotNull(in.readLine());

      PageBox box = null;
      if (type.equals("ImagePageBox")) {
        box = ImagePageBox.readFrom(in);
      } else if (type.equals("TextPageBox")) {
        box = TextPageBox.readFrom(in);
      } else {
        assert(false);
      }

      result.pageBoxes.add(Preconditions.checkNotNull(box));
    }

    return result;
  }

}
