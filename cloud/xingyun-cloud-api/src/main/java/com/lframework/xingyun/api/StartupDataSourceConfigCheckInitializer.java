package com.lframework.xingyun.api;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

/**
 * 启动前校验关键数据源配置，避免在租户数据源初始化阶段出现难以定位的空指针。
 */
public class StartupDataSourceConfigCheckInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {

    ConfigurableEnvironment env = applicationContext.getEnvironment();
    List<String> missingKeys = new ArrayList<>();
    checkRequiredProperty(env, missingKeys, "spring.datasource.dynamic.datasource.master.url");
    checkRequiredProperty(env, missingKeys, "spring.datasource.dynamic.datasource.master.username");
    checkRequiredProperty(env, missingKeys,
        "spring.datasource.dynamic.datasource.master.driver-class-name");

    if (!missingKeys.isEmpty()) {
      String profile = env.getProperty("spring.profiles.active", "未设置");
      String namespace = env.getProperty("spring.cloud.nacos.config.namespace", profile);
      String nacosAddr = env.getProperty("spring.cloud.nacos.config.server-addr", "未设置");
      String message = String.format(
          "启动失败：缺少主数据源配置 %s。当前 profile=%s，Nacos namespace=%s，Nacos 地址=%s。"
              + "请检查 Nacos 中 namespace=%s 下的 db.yaml 是否存在且内容完整。",
          String.join(", ", missingKeys), profile, namespace, nacosAddr, namespace);
      throw new IllegalStateException(message);
    }
  }

  private void checkRequiredProperty(ConfigurableEnvironment env, List<String> missingKeys,
      String key) {

    String value = env.getProperty(key);
    if (!StringUtils.hasText(value)) {
      missingKeys.add(key);
    }
  }
}
