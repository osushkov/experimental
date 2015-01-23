package com.experimental.utils;

import com.google.common.collect.Lists;

import java.net.URL;
import java.util.List;

/**
 * Created by sushkov on 23/01/15.
 */
public class TldUtils {

  private static final List<String> FILTERED_TLDS = Lists.newArrayList(
      "ac", "ad", "ae", "af", "ag", "ai", "am", "an", "ao", "aq", "ar", "as", "at", "aw", "ax", "az",
      "ba", "bb", "bd", "be", "bf", "bg", "bh", "bi", "bj", "bm", "bn", "bo", "bq", "br", "bs", "bt", "bv", "bw", "by",
      "bz", "cc", "cd", "cf", "cg", "ch", "ci", "ck", "ci", "cm", "cn", "co", "cr", "cs", "cu", "cv", "cw", "cx",
      "cy", "cz", "dd", "de", "dj", "dk", "dm", "do", "dz", "ec", "ee", "eg", "eh", "er", "es", "et", "eu", "fi",
      "fj", "fk", "fm", "fo", "fr", "ga", "gd", "ge", "gf", "gg", "gh", "gi", "gm", "gn", "gp", "gq", "gr", "gs", "gt",
      "gu", "gw", "gy", "hk", "hm", "hn", "hr", "ht", "hu", "id", "il", "im", "in", "io", "iq", "ir", "is", "it",
      "je", "jm", "jo", "jp", "ke", "kg", "kh", "ki", "km", "kn", "kp", "kr", "kw", "ky", "kz", "la", "lb", "lc",
      "li", "lk", "lr", "ls", "lt", "lu", "lv", "ly", "ma", "mc", "md", "me", "mg", "mh", "mk", "ml", "mm", "mn",
      "mo", "mp", "mq", "mr", "ms", "mt", "mu", "mv", "mw", "mx", "my", "mz", "na", "nc", "ne", "nf", "ng", "ni",
      "nl", "no", "np", "nr", "nu", "om", "pa", "pe", "pf", "pg", "ph", "pk", "pl", "pm", "pn", "pr", "ps", "pt",
      "pw", "py", "qa", "re", "ro", "rs", "ru", "rw", "sa", "sb", "sc", "sd", "se", "sg", "sh", "si", "sj", "sk",
      "sl", "sm", "sn", "so", "sr", "ss", "st", "su", "sv", "sx", "sy", "sz", "tc", "td", "tf", "tg", "th", "tk",
      "tk", "tl", "tm", "tn", "to", "tp", "tr", "tt", "tv", "tw", "tz", "ua", "ug", "uy", "uz", "va", "vc", "ve",
      "vg", "vi", "vn", "vu", "wf", "ws", "ye", "yt", "yu", "za", "zm", "zr", "zw");


  public static boolean shouldProcessUrl(URL url) {
    for (String filterTld : FILTERED_TLDS) {
      String regex = ".*\\." + filterTld + "$|[^a-z].*";
      if (url.getHost().toLowerCase().matches(regex)) {
        return false;
      }
    }
    return true;
  }

}
