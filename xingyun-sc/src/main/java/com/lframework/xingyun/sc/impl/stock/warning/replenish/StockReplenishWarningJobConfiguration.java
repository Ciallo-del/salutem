package com.lframework.xingyun.sc.impl.stock.warning.replenish;

import com.lframework.starter.web.core.components.qrtz.QrtzHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 库存不足补货提醒定时任务注册
 *
 * <p>应用启动后通过 {@link QrtzHandler} 幂等注册一个平台级每周 cron 任务，默认每周一 09:00 执行。</p>
 */
@Slf4j
@Order
@Component
public class StockReplenishWarningJobConfiguration implements ApplicationRunner {

  private static final String JOB_NAME = "STOCK_REPLENISH_WARNING";
  private static final String JOB_GROUP = "STOCK_REPLENISH_WARNING_GROUP";
  private static final String TRIGGER_NAME = "STOCK_REPLENISH_WARNING_TRIGGER";
  private static final String TRIGGER_GROUP = "STOCK_REPLENISH_WARNING_TRIGGER_GROUP";

  @Value("${xingyun.replenish-warning.cron:0 0 9 ? * MON}")
  private String cron;

  @Override
  public void run(ApplicationArguments args) {

    try {
      if (QrtzHandler.getJob(JOB_NAME, JOB_GROUP) != null) {
        log.info("库存不足补货提醒定时任务已存在，跳过注册");
        return;
      }

      QrtzHandler.addJob(JOB_NAME, JOB_GROUP, StockReplenishWarningJob.class, TRIGGER_NAME,
          TRIGGER_GROUP, cron);
      log.info("库存不足补货提醒定时任务注册成功，cron = {}", cron);
    } catch (Exception e) {
      log.error("库存不足补货提醒定时任务注册失败: {}", e.getMessage(), e);
    }
  }
}
