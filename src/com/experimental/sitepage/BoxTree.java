package com.experimental.sitepage;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sushkov on 4/01/15.
 */
public class BoxTree {

  private static class BoxTreeNode {
    List<Box> childBoxes;
    List<BoxTreeNode> childNodes;
    BoxTreeNode parent;
    double numElements;

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

  private final BoxTreeNode root = new BoxTreeNode();
  private final Map<Box, BoxTreeNode> boxMap = new HashMap<Box, BoxTreeNode>();

  public BoxTree() {

  }

  public List<Box> getAllBoxes() {
    return null;
  }

  public double similarityBetween(Box boxA, Box boxB) {
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
