package com.experimental.sitepage;

import com.experimental.geometry.Rectangle;
import com.experimental.utils.Log;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.fit.cssbox.layout.Box;
import org.fit.cssbox.layout.ElementBox;
import org.fit.cssbox.layout.TextBox;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.css.Rect;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sushkov on 4/01/15.
 */
public class BoxTree {

  private static class BoxTreeNode {
    final List<PageBox> childBoxes = new ArrayList<PageBox>();
    final List<BoxTreeNode> childNodes = new ArrayList<BoxTreeNode>();
    final BoxTreeNode parent;

    private BoxTreeNode(BoxTreeNode parent) {
      this.parent = parent;
      if (parent != null) {
        parent.childNodes.add(this);
      }
    }

    private List<BoxTreeNode> getAncestors() {
      List<BoxTreeNode> result = new ArrayList<BoxTreeNode>();
      result.add(this);

      BoxTreeNode cur = parent;
      while (cur != null) {
        result.add(cur);
        cur = cur.parent;
      }

      return result;
    }
  }

  private static class ElementContent {
    final Element element;
    final List<TextPageBox> containedText = new ArrayList<TextPageBox>();
    final List<ImagePageBox> containedImages = new ArrayList<ImagePageBox>();

    ElementContent(Element element) {
      this.element = Preconditions.checkNotNull(element);
    }
  }

  private final String pageUrl;
  private final BoxTreeNode root = new BoxTreeNode(null);

  public BoxTree(String pageUrl, Box rootBox, Node rootNode) {
    this.pageUrl = Preconditions.checkNotNull(pageUrl);

    Map<Element, ElementContent> elementContentsMap = new HashMap<Element, ElementContent>();
    populateElementContentMap(rootBox, elementContentsMap);

      buildTree(rootNode, root, elementContentsMap);
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

  private void buildTree(Node curNode, BoxTreeNode parent, Map<Element, ElementContent> elementContentsMap) {
    BoxTreeNode newNode = new BoxTreeNode(parent);
    if (curNode instanceof Element) {
      if (elementContentsMap.containsKey((Element) curNode)) {
        ElementContent elementContent = elementContentsMap.get((Element) curNode);

        newNode.childBoxes.addAll(elementContent.containedImages);

        TextPageBox textPageBox = mergeTextBoxes(elementContent.containedText);
        if (textPageBox != null) {
          newNode.childBoxes.add(textPageBox);
        }
      }
    }

    for (int i = 0; i < curNode.getChildNodes().getLength(); i++) {
      Node childNode = curNode.getChildNodes().item(i);
        buildTree(childNode, newNode, elementContentsMap);
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


  public BoxTree(String filepath) {
    this.pageUrl = null;
  }

  public List<PageBox> getAllBoxes() {
    return getAllBoxes(root);
  }

  private List<PageBox> getAllBoxes(BoxTreeNode node) {
    Preconditions.checkNotNull(node);
    List<PageBox> result = new ArrayList<PageBox>();

    result.addAll(node.childBoxes);
    for (BoxTreeNode child : node.childNodes) {
      result.addAll(getAllBoxes(child));
    }

    return result;
  }
}
