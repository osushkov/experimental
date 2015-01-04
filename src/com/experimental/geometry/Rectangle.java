package com.experimental.geometry;

import com.google.common.base.Preconditions;

import java.util.List;

/**
 * Created by sushkov on 4/01/15.
 */
public class Rectangle {
  /**
   * top left position.
   */
  public final double x;

  /**
   * top left position.
   */
  public final double y;

  /**
   * extending from top left to bottom right.
   */
  public final double width;

  /**
   * extending from top left to bottom right.
   */
  public final double height;

  public Rectangle(double x, double y, double width, double height) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;

    Preconditions.checkArgument(width >= 0.0);
    Preconditions.checkArgument(height >= 0.0);
  }

  public Rectangle(List<Rectangle> boundRectangles) {
    Preconditions.checkNotNull(boundRectangles);
    Preconditions.checkArgument(boundRectangles.size() > 0);

    double minX = Double.MAX_VALUE;
    double minY = Double.MAX_VALUE;

    double maxX = -Double.MAX_VALUE;
    double maxY = -Double.MAX_VALUE;

    for (Rectangle rect : boundRectangles) {
      minX = Math.min(minX, rect.x);
      minY = Math.min(minY, rect.y);

      maxX = Math.max(maxX, rect.x + rect.width);
      maxY = Math.max(maxY, rect.y + rect.height);
    }

    Preconditions.checkState(maxX >= minX);
    Preconditions.checkState(maxY >= minY);

    this.x = minX;
    this.y = minY;

    this.width = maxX - minX;
    this.height = maxY - minY;
  }

  double distanceTo(Rectangle other) {
    Preconditions.checkNotNull(other);

    double xDist = Math.max(this.x, other.x) - Math.min(this.x + this.width, other.x + other.width);
    xDist = Math.max(xDist, 0.0);

    double yDist = Math.max(this.y, other.y) - Math.min(this.y + this.height, other.y + other.height);
    yDist = Math.max(yDist, 0.0);

    return Math.sqrt(xDist * xDist + yDist * yDist);
  }
}
