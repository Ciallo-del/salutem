package com.lframework.xingyun.sc.impl.stock.warning.expire;

import com.lframework.starter.web.core.components.qrtz.QrtzHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 药品过期提醒定时任务注册
 *
 * <p>应用启动后通过 {@link QrtzHandler} 幂等注册一个平台级每日 cron 任务，默认每天 08:00 执行。</p>
 */
@Slf4j
@Order
@Component
public class ProductExpireWarningJobConfiguration implements ApplicationRunner {

  private static final String JOB_NAME = "PRODUCT_EXPIRE_WARNING";
  private static final String JOB_GROUP = "PRODUCT_EXPIRE_WARNING_GROUP";
  private static final String TRIGGER_NAME = "PRODUCT_EXPIRE_WARNING_TRIGGER";
  private static final String TRIGGER_GROUP = "PRODUCT_EXPIRE_WARNING_TRIGGER_GROUP";

  @Value("${xingyun.expire-warning.cron:0 0 8 * * ?}")
  private String cron;

  @Override
  public void run(ApplicationArguments args) {

    try {
      if (QrtzHandler.getJob(JOB_NAME, JOB_GROUP) != null) {
        log.info("药品过期提醒定时任务已存在，跳过注册");
        return;
      }

      QrtzHandler.addJob(JOB_NAME, JOB_GROUP, ProductExpireWarningJob.class, TRIGGER_NAME,
          TRIGGER_GROUP, cron);
      log.info("药品过期提醒定时任务注册成功，cron = {}", cron);
    } catch (Exception e) {
      log.error("药品过期提醒定时任务注册失败: {}", e.getMessage(), e);
    }
  }
}
