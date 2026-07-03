package com.lframework.xingyun.sc.impl.stock.warning.expire;



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

 * 药品过期提醒定时任务注册

 *

 * <p>应用启动后通过 {@link QrtzHandler} 幂等注册一个平台级 cron 任务，默认每天 08:00 执行。

 * test 环境若任务已存在且 cron 与配置不一致，会通过 Scheduler 同步 trigger。</p>

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

      log.info("药品过期提醒定时任务已存在，cron 与配置一致: {}", cron);

      return true;

    }



    CronTrigger newTrigger = TriggerBuilder.newTrigger()

        .withIdentity(triggerKey)

        .forJob(jobKey)

        .withSchedule(CronScheduleBuilder.cronSchedule(cron))

        .build();

    scheduler.rescheduleJob(triggerKey, newTrigger);

    log.info("药品过期提醒 cron 已同步为 {}", cron);

    return true;

  }

}


