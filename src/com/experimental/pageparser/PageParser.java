package com.experimental.pageparser;

import com.experimental.sitepage.BoxTree;
import com.experimental.sitepage.PageBox;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sushkov on 3/01/15.
 */
public class PageParser {
  private Map<Element, String> elementText = new HashMap<Element, String>();

  public void parsePage(String url, String outputPath) {
    try {
      //Open the network connection
      DocumentSource docSource = new DefaultDocumentSource(url);

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
      System.err.println("Computing style...");
      da.stylesToDomInherited();

      System.out.println("document uri: " + url);
      BoxTree boxTree = new BoxTree(url, browser.getViewport(), doc);
      List<PageBox> allBoxes = boxTree.getAllBoxes();
      for (PageBox pageBox : allBoxes) {
        System.out.println(pageBox);
      }

//      printTextBoxes2(browser.getViewport());
//      printTextBoxes(browser.getViewport(), da);

      OutputStream os = new FileOutputStream(outputPath);
      Output out = new NormalOutput(doc);
      out.dumpTo(os);
      os.close();

      docSource.close();

      System.err.println("Done.");

    } catch (Exception e) {
      System.err.println("Error: "+e.getMessage());
      e.printStackTrace();
    }


//    for (String line : elementText.values()) {
//      System.out.println(line + " ** ");
//    }
  }

  private static void printTextBoxes2(Box root)
  {
    if (root instanceof TextBox)
    {
      //text boxes are just printed
      TextBox text = (TextBox) root;
      if (isBoxVisible(root)) {
        System.out.println("x=" + text.getAbsoluteBounds().x + " y=" + text.getAbsoluteBounds().y + " text=" + text.getText());
      }
    }
    else if (root instanceof ElementBox)
    {
      //element boxes must be just traversed
      ElementBox el = (ElementBox) root;
      for (int i = el.getStartChild(); i < el.getEndChild(); i++)
        printTextBoxes2(el.getSubBox(i));
    }
  }

  private static boolean isBoxVisible(Box box) {
    if (!box.isDisplayed() || !box.isDeclaredVisible()) {
      return false;
    }


    if (box instanceof ElementBox) {
      ElementBox el = (ElementBox) box;

      System.out.println(el.getStyle().getProperty("display"));
      if ("none".equals(el.getStyle().getProperty("display"))) {
        return false;
      }
    }

    if (box.getParent() != null) {
      return isBoxVisible(box.getParent());
    }

    return true;
  }


  private void printTextBoxes(Box root, DOMAnalyzer da) {

    if (root instanceof TextBox)
    {
      //text boxes are just printed
      TextBox text = (TextBox) root;
      System.out.println(text.getText());
//      System.out.println(text.getVisualContext().getFont().getSize() + " " + text.getVisualContext().getFont().isBold() +
//          " " + text.getVisualContext().getFont().isItalic());


//      System.out.println("x=" + text.getAbsoluteBounds().x + " y=" + text.getAbsoluteBounds().y +
//          " w=" + text.getMinimalAbsoluteBounds().getWidth() + " h=" + text.getAbsoluteBounds().getHeight() +
//          " text=" + text.getText());
    }
    else if (root instanceof ElementBox) {
      //element boxes must be just traversed
      ElementBox el = (ElementBox) root;

//      printDirectChildText(el);

      if (el.getElement().getTagName().equals("img")) {
//        System.out.println("image=" + el.getElement().getAttribute("src"));
//        System.out.println("x=" + el.getAbsoluteBounds().x + " y=" + el.getAbsoluteBounds().y +
//            " w=" + el.getMinimalAbsoluteBounds().getWidth() + " h=" + el.getAbsoluteBounds().getHeight());
      } else {
        for (int i = el.getStartChild(); i < el.getEndChild(); i++) {
          printTextBoxes(el.getSubBox(i), da);
        }
      }
    }
  }

  private void printDirectChildText(ElementBox elementBox) {
    if (elementBox.isDisplayed() && elementBox.isDeclaredVisible()) {
      for (int i = elementBox.getStartChild(); i < elementBox.getEndChild(); i++) {
        Box childBox = elementBox.getSubBox(i);
        if (childBox instanceof TextBox) {
          Element elem = elementBox.getElement();
          if (elementText.get(elem) == null) {
            elementText.put(elem, childBox.getText());
          } else {
            elementText.put(elem, elementText.get(elem) + " " + childBox.getText());
          }
        }
      }
    }

//    for (String text : elementText) {
//      System.out.println(text + "*");
//    }
  }

}
