package com.lframework.xingyun.sc.impl.stock.warning.replenish;

import com.lframework.starter.web.core.components.qrtz.QrtzHandler;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 库存不足补货提醒定时任务注册
 *
 * <p>应用启动后通过 {@link QrtzHandler} 幂等注册一个平台级 cron 任务，默认每周一 09:00 执行。
 * test 环境若任务已存在且 cron 与配置不一致，会通过 Scheduler 同步 trigger。</p>
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

  @Value("${spring.profiles.active:}")
  private String activeProfile;

  @Autowired
  private Scheduler scheduler;

  @Override
  public void run(ApplicationArguments args) {

    try {
      JobKey jobKey = JobKey.jobKey(JOB_NAME, JOB_GROUP);
      if (QrtzHandler.getJob(JOB_NAME, JOB_GROUP) != null) {
        if (isTestProfile() && syncCronIfNeeded(jobKey)) {
          return;
        }
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

  private boolean isTestProfile() {
    if (activeProfile == null || activeProfile.isEmpty()) {
      return false;
    }
    return Arrays.stream(activeProfile.split(","))
        .map(String::trim)
        .anyMatch("test"::equals);
  }

  private boolean syncCronIfNeeded(JobKey jobKey) throws Exception {
    TriggerKey triggerKey = TriggerKey.triggerKey(TRIGGER_NAME, TRIGGER_GROUP);
    CronTrigger existingTrigger = (CronTrigger) scheduler.getTrigger(triggerKey);
    if (existingTrigger == null) {
      return false;
    }
    if (cron.equals(existingTrigger.getCronExpression())) {
      log.info("库存不足补货提醒定时任务已存在，cron 与配置一致: {}", cron);
      return true;
    }

    CronTrigger newTrigger = TriggerBuilder.newTrigger()
        .withIdentity(triggerKey)
        .forJob(jobKey)
        .withSchedule(CronScheduleBuilder.cronSchedule(cron))
        .build();
    scheduler.rescheduleJob(triggerKey, newTrigger);
    log.info("库存不足补货提醒 cron 已同步为 {}", cron);
    return true;
  }
}
