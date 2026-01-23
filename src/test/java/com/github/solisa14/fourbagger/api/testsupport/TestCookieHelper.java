package com.github.solisa14.fourbagger.api.testsupport;

import jakarta.servlet.http.Cookie;
import java.util.List;

public final class TestCookieHelper {

  private TestCookieHelper() {}

  public static String extractCookieValue(List<String> setCookieHeaders, String name) {
    if (setCookieHeaders == null) {
      return null;
    }
    for (String header : setCookieHeaders) {
      if (header.startsWith(name + "=")) {
        String[] parts = header.split(";", 2);
        return parts[0].substring(name.length() + 1);
      }
    }
    return null;
  }

  public static boolean hasClearedCookie(List<String> setCookieHeaders, String name) {
    if (setCookieHeaders == null) {
      return false;
    }
    for (String header : setCookieHeaders) {
      if (header.startsWith(name + "=") && header.contains("Max-Age=0")) {
        return true;
      }
    }
    return false;
  }

  public static Cookie cookie(String name, String value) {
    return new Cookie(name, value);
  }
}
