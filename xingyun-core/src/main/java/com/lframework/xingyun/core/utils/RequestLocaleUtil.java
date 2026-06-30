package com.lframework.xingyun.core.utils;

import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class RequestLocaleUtil {

  private static final String HEADER_X_LOCALE = "X-Locale";
  private static final String HEADER_ACCEPT_LANGUAGE = "Accept-Language";
  private static final String LOCALE_ZH_CN = "zh_CN";
  private static final String LOCALE_EN = "en";

  private RequestLocaleUtil() {
  }

  public static String resolveLocale() {
    HttpServletRequest request = getRequest();
    if (request == null) {
      return LOCALE_ZH_CN;
    }

    String locale = normalizeLocale(request.getHeader(HEADER_X_LOCALE));
    if (locale != null) {
      return locale;
    }

    locale = normalizeLocale(request.getHeader(HEADER_ACCEPT_LANGUAGE));
    if (locale != null) {
      return locale;
    }

    Locale requestLocale = request.getLocale();
    if (requestLocale != null) {
      locale = normalizeLocale(requestLocale.toLanguageTag());
      if (locale != null) {
        return locale;
      }
    }

    return LOCALE_ZH_CN;
  }

  public static boolean isEnLocale() {
    return LOCALE_EN.equals(resolveLocale());
  }

  private static HttpServletRequest getRequest() {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    if (attributes instanceof ServletRequestAttributes) {
      return ((ServletRequestAttributes) attributes).getRequest();
    }
    return null;
  }

  private static String normalizeLocale(String locale) {
    if (locale == null) {
      return null;
    }

    String normalized = locale.trim();
    if (normalized.isEmpty()) {
      return null;
    }

    normalized = normalized.replace('-', '_');
    int commaIndex = normalized.indexOf(',');
    if (commaIndex >= 0) {
      normalized = normalized.substring(0, commaIndex);
    }
    int semicolonIndex = normalized.indexOf(';');
    if (semicolonIndex >= 0) {
      normalized = normalized.substring(0, semicolonIndex);
    }
    normalized = normalized.trim().toLowerCase(Locale.ROOT);

    if (normalized.startsWith("en")) {
      return LOCALE_EN;
    }
    if (normalized.startsWith("zh")) {
      return LOCALE_ZH_CN;
    }

    return null;
  }
}
