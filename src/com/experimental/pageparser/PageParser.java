package com.experimental.pageparser;

import com.experimental.documentmodel.Sentence;
import com.experimental.documentmodel.SentenceProcessor;
import com.experimental.geometry.Rectangle;
import com.experimental.sitepage.*;
import com.experimental.utils.Log;
import com.google.common.base.Preconditions;
import com.sun.xml.internal.ws.client.sei.ResponseBuilder;
import cz.vutbr.web.css.NodeData;
import org.fit.cssbox.css.CSSNorm;
import org.fit.cssbox.css.DOMAnalyzer;
import org.fit.cssbox.css.NormalOutput;
import org.fit.cssbox.css.Output;
import org.fit.cssbox.io.DOMSource;
import org.fit.cssbox.io.DefaultDOMSource;
import org.fit.cssbox.io.DefaultDocumentSource;
import org.fit.cssbox.io.DocumentSource;
import org.fit.cssbox.layout.Box;
import org.fit.cssbox.layout.BrowserCanvas;
import org.fit.cssbox.layout.ElementBox;
import org.fit.cssbox.layout.TextBox;
import org.jsoup.Jsoup;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.html.HTMLDocument;
import org.xml.sax.SAXException;

import javax.xml.soap.Text;
import java.awt.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sushkov on 3/01/15.
 */
public class PageParser {
  private static class ElementContent {
    final Element element;
    final List<TextPageBox> containedText = new ArrayList<TextPageBox>();
    final List<ImagePageBox> containedImages = new ArrayList<ImagePageBox>();

    ElementContent(Element element) {
      this.element = Preconditions.checkNotNull(element);
    }
  }

  private final String pageUrl;
  private final SentenceProcessor sentenceProcessor;

  public PageParser(String pageUrl, SentenceProcessor sentenceProcessor) {
    this.pageUrl = Preconditions.checkNotNull(pageUrl);
    this.sentenceProcessor = Preconditions.checkNotNull(sentenceProcessor);
  }

  public SitePage parsePage() {
    try {
      //Open the network connection
      DocumentSource docSource = new DefaultDocumentSource(pageUrl);

      //Parse the input document
      DOMSource parser = new DefaultDOMSource(docSource);
      Document doc = parser.parse();

      //Create the CSS analyzer
      DOMAnalyzer da = new DOMAnalyzer(doc, docSource.getURL());
      da.attributesToStyles(); //convert the HTML presentation attributes to inline styles
      da.addStyleSheet(null, CSSNorm.stdStyleSheet(), DOMAnalyzer.Origin.AGENT); //use the standard style sheet
      da.addStyleSheet(null, CSSNorm.userStyleSheet(), DOMAnalyzer.Origin.AGENT); //use the additional style sheet
      da.getStyleSheets(); //load the author style sheets

      //Create the browser canvas
      BrowserCanvas browser = new BrowserCanvas(da.getRoot(), da, docSource.getURL());
      //Disable the image loading
      browser.getConfig().setLoadImages(true);
      browser.getConfig().setLoadBackgroundImages(true);


      //Create the layout for 1000x600 pixels
      browser.createLayout(new java.awt.Dimension(1366, 768));

      //Compute the styles
      da.stylesToDomInherited();

      Map<Element, ElementContent> elementContentsMap = new HashMap<Element, ElementContent>();
      populateElementContentMap(browser.getViewport(), elementContentsMap);

      List<PageBox> allPageBoxes = new ArrayList<PageBox>();
      for (ElementContent elementContent : elementContentsMap.values()) {
        if (elementContent.containedText.size() > 0) {
          TextPageBox mergedTextBox = mergeTextBoxes(elementContent.containedText);
          if (mergedTextBox != null) {
            allPageBoxes.add(mergedTextBox);
          }
        }

        allPageBoxes.addAll(elementContent.containedImages);
      }

      processPageBoxes(allPageBoxes);
      List<SitePage.Link> outgoingLinks = getLinksFromPageBoxes(allPageBoxes);


      SitePage.HeaderInfo header = getHeaderFor(doc);
      for (Sentence sentence : header.title) {
        Log.out(sentence.toString());
      }

      for (Sentence sentence : header.description) {
        Log.out(sentence.toString());
      }

      for (Sentence sentence : header.keywords) {
        Log.out(sentence.toString());
      }

      docSource.close();

    } catch (Exception e) {
      System.err.println("Error: "+e.getMessage());
      e.printStackTrace();
      return null;
    }

    return null;
  }

  private void populateElementContentMap(Box box, Map<Element, ElementContent> elementContentsMap) {
    Preconditions.checkNotNull(box);
    Preconditions.checkNotNull(elementContentsMap);

    if (!PageUtils.isBoxVisible(box)) {
      return;
    }

    if (box instanceof ElementBox) {
      ElementBox elementBox = (ElementBox) box;
      Element element = elementBox.getElement();

      List<TextPageBox> childTextBoxes = textBoxesFromElement(elementBox);
      List<ImagePageBox> childImageBoxes = imageBoxesFromElement(elementBox);

      if (childTextBoxes.size() > 0 || childImageBoxes.size() > 0) {
        if (!elementContentsMap.containsKey(element)) {
          elementContentsMap.put(element, new ElementContent(element));
        }

        ElementContent elementContent = elementContentsMap.get(element);
        elementContent.containedImages.addAll(childImageBoxes);
        elementContent.containedText.addAll(childTextBoxes);
      }

      for (int i = elementBox.getStartChild(); i < elementBox.getEndChild(); i++) {
        populateElementContentMap(elementBox.getSubBox(i), elementContentsMap);
      }
    }
  }

  private void processPageBoxes(List<PageBox> allPageBoxes) {
    double weightSum = 0.0;
    int totalCharacters = 0;

    for (PageBox box : allPageBoxes) {
      if (box instanceof TextPageBox) {
        TextPageBox textBox = (TextPageBox) box;
        int numCharacters = textBox.text.length();

        weightSum += textBox.textStyle.computeAbsoluteEmphasis() * numCharacters;
        totalCharacters += numCharacters;
      }
    }

    double averageEmphasis = weightSum / totalCharacters;
    for (PageBox box : allPageBoxes) {
      if (box instanceof TextPageBox) {
        TextPageBox textBox = (TextPageBox) box;
        textBox.buildSentences(sentenceProcessor, textBox.textStyle.computeAbsoluteEmphasis() / averageEmphasis);
      } else if (box instanceof ImagePageBox) {
        ((ImagePageBox) box).buildSentences(sentenceProcessor, averageEmphasis);
      }
    }
  }

  private TextPageBox mergeTextBoxes(List<TextPageBox> boxes) {
    Preconditions.checkNotNull(boxes);

    if (boxes.size() == 0) {
      return null;
    }

    StringBuffer mergedText = new StringBuffer();
    List<Rectangle> allRectangles = new ArrayList<Rectangle>();
    TextPageBox.TextStyle textStyle = boxes.get(0).textStyle;

    for (TextPageBox box : boxes) {
      if (box != boxes.get(0)) {
        mergedText.append(" ");
      }
      mergedText.append(box.text);
      allRectangles.add(box.getRectangle());
    }

    return new TextPageBox(mergedText.toString(), textStyle, new Rectangle(allRectangles));
  }

  private List<TextPageBox> textBoxesFromElement(ElementBox elementBox) {
    List<TextPageBox> result = new ArrayList<TextPageBox>();
    for (int i = elementBox.getStartChild(); i < elementBox.getEndChild(); i++) {
      Box childBox = elementBox.getSubBox(i);
      if (childBox instanceof TextBox) {
        TextBox childTextBox = (TextBox) childBox;

        Rectangle rect = new Rectangle(
            childTextBox.getAbsoluteBounds().getX(), childTextBox.getAbsoluteBounds().getY(),
            childTextBox.getAbsoluteBounds().getWidth(), childTextBox.getAbsoluteBounds().getHeight());

        result.add(new TextPageBox(childTextBox.getText(), new TextPageBox.TextStyle(childTextBox), rect));
      }
    }

    return result;
  }

  private List<ImagePageBox> imageBoxesFromElement(ElementBox elementBox) {
    List<ImagePageBox> result = new ArrayList<ImagePageBox>();

    for (int i = elementBox.getStartChild(); i < elementBox.getEndChild(); i++) {
      Box childBox = elementBox.getSubBox(i);
      if (childBox instanceof ElementBox) {
        ElementBox childElementBox = (ElementBox) childBox;
        if (childElementBox.getElement().getTagName().equals("img")) {
          String imageSrc = childElementBox.getElement().getAttribute("src");
          String imageAltText = childElementBox.getElement().getAttribute("alt");
          if (imageSrc != null) {
            Rectangle imageRect = new Rectangle(
                childElementBox.getAbsoluteBounds().getX(), childElementBox.getAbsoluteBounds().getY(),
                childElementBox.getAbsoluteBounds().getWidth(), childElementBox.getAbsoluteBounds().getHeight());
            result.add(new ImagePageBox(pageUrl, imageSrc, imageAltText, imageRect));
          }
        }
      }
    }

    return result;
  }

  private SitePage.HeaderInfo getHeaderFor(Document doc) {
    List<Sentence> titleSentences = new ArrayList<Sentence>();

    List<Sentence> descriptionSentences = new ArrayList<Sentence>();
    List<Sentence> keywordSentences = new ArrayList<Sentence>();

    NodeList titleNodes = doc.getElementsByTagName("title");
    for (int i = 0; i < titleNodes.getLength(); i++) {
      Element titleElement = (Element) titleNodes.item(i);
      String titleText = titleElement.getTextContent();
      titleText = titleText.replace('|', '.'); // StanfordNLP doesnt handle pipe very well. Replace with period.
      titleSentences.addAll(sentenceProcessor.processString(titleText, 1.0, true));
    }

    NodeList metaNodes = doc.getElementsByTagName("meta");
    for (int i = 0; i < metaNodes.getLength(); i++) {
      Element metaElement = (Element) metaNodes.item(i);

      if (metaElement.getAttribute("name").toLowerCase().contains("description")) {
        String descriptionText = metaElement.getAttribute("content");
        descriptionSentences.addAll(sentenceProcessor.processString(descriptionText, 1.0, true));
      }

      if (metaElement.getAttribute("name").toLowerCase().contains("keywords")) {
        String keywordsValue = metaElement.getAttribute("content");
        String[] keywords = keywordsValue.split(",");
        for (String keyword : keywords) {
          keywordSentences.addAll(sentenceProcessor.processString(keyword, 1.0, true));
        }
      }
    }

    return new SitePage.HeaderInfo(titleSentences, descriptionSentences, keywordSentences);
  }

  private List<SitePage.Link> getLinksFromPageBoxes(List<PageBox> pageBoxes) {
    Map<URI, List<Sentence>> links = new HashMap<URI, List<Sentence>>();
    for (PageBox box : pageBoxes) {
      if (box instanceof TextPageBox) {
        TextPageBox textBox = (TextPageBox) box;
        if (textBox.textStyle.isLink) {
          URI linkUri = null;
          try {
            linkUri = getLinkAbsoluteUrl(textBox.textStyle.linkUrl);
          } catch (URISyntaxException e) {
            e.printStackTrace();
            continue;
          }

          if (isRelevantLinkUrl(linkUri)) {
            links.putIfAbsent(linkUri, new ArrayList<Sentence>());
            links.get(linkUri).addAll(textBox.sentences);
          }
        }
      }
    }

    List<SitePage.Link> result = new ArrayList<SitePage.Link>();
    for (Map.Entry<URI, List<Sentence>> entry : links.entrySet()) {
      result.add(new SitePage.Link(entry.getValue(), entry.getKey()));
    }
    return result;
  }

  private URI getLinkAbsoluteUrl(String url) throws URISyntaxException {
    return new URI(PageUtils.constructAbsoluteUrl(pageUrl, url));
  }

  private boolean isRelevantLinkUrl(URI uri) {
    if (uri.toString().contains("#")) {
      return false;
    }

    URI pageUri = null;
    try {
      pageUri = new URI(pageUrl);
    } catch (URISyntaxException e) {
      e.printStackTrace();
      return false;
    }

    if (!pageUri.getScheme().equals(uri.getScheme())) {
      return false;
    }

    if (!uri.getHost().equals(pageUri.getHost())) {
      return false;
    }

    return true;
  }
}
