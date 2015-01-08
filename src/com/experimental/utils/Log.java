package com.experimental.utils;

import java.io.PrintWriter;

/**
 * Created by sushkov on 8/01/15.
 */
public class Log {
  private static final PrintWriter outWriter = new PrintWriter(System.out, true);

  public static void out(String tag, String message) {
    outWriter.println(tag + " > " + message);
  }
}
