package com.experimental.sitepage;

import com.google.common.base.Preconditions;
import org.fit.cssbox.layout.Box;

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

  private final BoxTreeNode root = new BoxTreeNode(null);
  private final Map<PageBox, BoxTreeNode> boxMap = new HashMap<PageBox, BoxTreeNode>();

  public BoxTree(Box rootBox) {
    insertBoxIntoTree(rootBox, root);
    populateNumElements(root);
  }

  private void insertBoxIntoTree(Box box, BoxTreeNode parent) {
    Preconditions.checkNotNull(box);
    Preconditions.checkNotNull(parent);

    BoxTreeNode newNode = new BoxTreeNode(parent);
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
