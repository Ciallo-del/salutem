package com.lframework.xingyun.core.utils;

import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.annotations.oplog.OpLog;
import com.lframework.starter.web.core.utils.JsonUtil;
import java.util.LinkedHashMap;
import java.util.Map;

public class StructuredOpLogUtil {

  private static final String STRUCTURED_FLAG = "__structured";
  private static final String NAME_KEY = "nameKey";
  private static final String NAME_TEMPLATE = "nameTemplate";
  private static final String NAME_PARAMS = "nameParams";

  private StructuredOpLogUtil() {
  }

  public static String buildNameKey(String logTypeName, String template) {

    return "oplog." + normalize(logTypeName) + "." + normalize(template);
  }

  public static String buildStructuredExtra(String nameKey, String nameTemplate, Object[] params,
      String extra) {

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put(STRUCTURED_FLAG, true);
    payload.put(NAME_KEY, nameKey);
    payload.put(NAME_TEMPLATE, nameTemplate);
    payload.put(NAME_PARAMS, params);

    if (StringUtil.isNotBlank(extra)) {
      if (JsonUtil.isJsonObject(extra)) {
        payload.put("extraData", JsonUtil.parseObject(extra, Map.class));
      } else {
        payload.put("extraText", extra);
      }
    }

    return JsonUtil.toJsonString(payload);
  }

  public static String resolveNameKey(OpLog opLog) {

    return buildNameKey(opLog.type().getSimpleName(), opLog.name());
  }

  public static String parseNameKey(String extra) {

    Map<String, Object> payload = parseStructuredExtra(extra);
    return payload == null ? null : convertToString(payload.get(NAME_KEY));
  }

  public static String parseNameTemplate(String extra) {

    Map<String, Object> payload = parseStructuredExtra(extra);
    return payload == null ? null : convertToString(payload.get(NAME_TEMPLATE));
  }

  public static String parseNameParams(String extra) {

    Map<String, Object> payload = parseStructuredExtra(extra);
    if (payload == null || !payload.containsKey(NAME_PARAMS)) {
      return null;
    }
    return JsonUtil.toJsonString(payload.get(NAME_PARAMS));
  }

  public static String parseOriginalExtra(String extra) {

    Map<String, Object> payload = parseStructuredExtra(extra);
    if (payload == null) {
      return extra;
    }

    if (payload.containsKey("extraData")) {
      return JsonUtil.toJsonString(payload.get("extraData"));
    }

    return convertToString(payload.get("extraText"));
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> parseStructuredExtra(String extra) {

    if (!JsonUtil.isJsonObject(extra)) {
      return null;
    }

    Map<String, Object> payload = JsonUtil.parseObject(extra, Map.class);
    Object structured = payload.get(STRUCTURED_FLAG);
    if (!(structured instanceof Boolean) || !((Boolean) structured)) {
      return null;
    }
    return payload;
  }

  private static String convertToString(Object value) {

    return value == null ? null : String.valueOf(value);
  }

  private static String normalize(String value) {

    if (StringUtil.isBlank(value)) {
      return "unknown";
    }

    return value.replace("：", ":")
        .replace("，", ",")
        .replace(" ", "")
        .replace("{}", "arg")
        .replaceAll("[^0-9a-zA-Z\\u4e00-\\u9fa5]+", "_")
        .replaceAll("_+", "_")
        .replaceAll("^_|_$", "")
        .toLowerCase();
  }
}
