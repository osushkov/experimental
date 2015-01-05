package com.experimental.sitepage;

import com.experimental.geometry.Rectangle;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.fit.cssbox.layout.Box;
import org.fit.cssbox.layout.ElementBox;
import org.fit.cssbox.layout.TextBox;
import org.w3c.dom.Element;
import org.w3c.dom.css.Rect;

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
    double numElements = 0.0;

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

  private final String pageUrl;
  private final BoxTreeNode root = new BoxTreeNode(null);
  private final Map<PageBox, BoxTreeNode> boxMap = new HashMap<PageBox, BoxTreeNode>();

  public BoxTree(String pageUrl, Box rootBox) {
    this.pageUrl = Preconditions.checkNotNull(pageUrl);

    insertBoxIntoTree(rootBox, root);
    populateNumElements(root);
  }

  private void insertBoxIntoTree(Box box, BoxTreeNode parent) {
    Preconditions.checkNotNull(box);
    Preconditions.checkNotNull(parent);

    if (!box.isDisplayed() || !box.isDeclaredVisible()) {
      return;
    }

    if (box instanceof ElementBox) {
      ElementBox elementBox = (ElementBox) box;

      BoxTreeNode newNode = new BoxTreeNode(parent);

      List<PageBox> childPageBoxes = Lists.newArrayList();
      childPageBoxes.addAll(textBoxesFromElement(elementBox));
      childPageBoxes.addAll(imageBoxesFromElement(elementBox));

      BoxTreeNode newParent = parent;
      if (childPageBoxes.size() > 0) {
        newParent = new BoxTreeNode(parent);
        newParent.childBoxes.addAll(childPageBoxes);
      }

      for (int i = elementBox.getStartChild(); i < elementBox.getEndChild(); i++) {
        insertBoxIntoTree(elementBox.getSubBox(i), newParent);
      }
    }
  }

  private List<TextPageBox> textBoxesFromElement(ElementBox elementBox) {
    String text = "";
    TextPageBox.TextStyle textStyle = null;
    List<Rectangle> boundRectangles = new ArrayList<Rectangle>();

    for (int i = elementBox.getStartChild(); i < elementBox.getEndChild(); i++) {
      Box childBox = elementBox.getSubBox(i);
      if (childBox instanceof TextBox) {
        TextBox childTextBox = (TextBox) childBox;
        text = text + " " + childTextBox.getText();

        if (textStyle == null) {
          textStyle = new TextPageBox.TextStyle(childTextBox);
        }

        boundRectangles.add(new Rectangle(childTextBox.getContentX(), childTextBox.getContentY(),
            childTextBox.getContentWidth(), childTextBox.getContentHeight()));
      }
    }

    if (text.length() > 0) {
      Preconditions.checkState(textStyle != null);
      Preconditions.checkState(boundRectangles.size() > 0);

      return Lists.newArrayList(new TextPageBox(text, textStyle, new Rectangle(boundRectangles)));
    } else {
      return Lists.newArrayList();
    }
  }

  private List<ImagePageBox> imageBoxesFromElement(ElementBox elementBox) {
    List<ImagePageBox> result = new ArrayList<ImagePageBox>();

    for (int i = elementBox.getStartChild(); i < elementBox.getEndChild(); i++) {
      Box childBox = elementBox.getSubBox(i);
      if (childBox instanceof ElementBox) {
        ElementBox childElementBox = (ElementBox) childBox;
        if (childElementBox.getElement().getTagName().equals("img")) {
          childElementBox.getElement().getAttribute("src");
        }
      }
    }

    return result;
  }

  private double populateNumElements(BoxTreeNode node) {
    double childrenSum = 0.0;
    for (BoxTreeNode child : node.childNodes) {
      childrenSum += populateNumElements(child);
    }

    double boxesSum = 0.0;
    for (PageBox childBox : node.childBoxes) {
      boxesSum += childBox.getNumElementsInBox();
    }

    node.numElements = childrenSum + boxesSum;
    return node.numElements;
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

  public double similarityBetween(PageBox boxA, PageBox boxB) {
    // implemented using the Lin similarity measure.
    // TODO: if the LCS is the root, then the similarity will be 0. This may not be desirable for a website.

    BoxTreeNode nodeA = Preconditions.checkNotNull(boxMap.get(boxA));
    BoxTreeNode nodeB = Preconditions.checkNotNull(boxMap.get(boxB));

    BoxTreeNode lcs = findFirstCommonAncestor(nodeA, nodeB);

    double numerator = 2.0 * Math.log(lcs.numElements / root.numElements);
    double denominator =
        Math.log(nodeA.numElements / root.numElements) + Math.log(nodeB.numElements / root.numElements);

    return numerator / denominator;
  }

  private BoxTreeNode findFirstCommonAncestor(BoxTreeNode nodeA, BoxTreeNode nodeB) {
    List<BoxTreeNode> ancestorsOfA = nodeA.getAncestors();
    List<BoxTreeNode> ancestorsOfB = nodeB.getAncestors();

    for (BoxTreeNode ancestorA : ancestorsOfA) {
      if (ancestorsOfB.contains(ancestorA)) {
        return ancestorA;
      }
    }

    // We should never get here with a property rooted tree.
    assert(false);
    return null;
  }

}
