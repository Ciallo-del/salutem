package com.lframework.xingyun.sc.impl.stock.warning.replenish;

import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.web.core.components.qrtz.QrtzJob;
import com.lframework.starter.web.core.components.tenant.TenantContextHolder;
import com.lframework.starter.web.inner.entity.Tenant;
import com.lframework.starter.web.inner.service.TenantService;
import com.lframework.xingyun.sc.service.stock.warning.ProductStockReplenishWarningService;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 库存不足补货提醒定时任务（平台级，逐可用租户扫描，含平台租户）
 */
@Slf4j
public class StockReplenishWarningJob extends QrtzJob {

  @Autowired
  private TenantService tenantService;

  @Autowired
  private ProductStockReplenishWarningService productStockReplenishWarningService;

  @Override
  public void onExecute(JobExecutionContext context) {

    Date fireTime = context != null && context.getFireTime() != null
        ? context.getFireTime() : new Date();
    log.info("【补货提醒】定时任务开始，fireTime={}", fireTime);

    List<Tenant> tenants = tenantService.findAll();
    if (CollectionUtil.isEmpty(tenants)) {
      log.info("【补货提醒】定时任务结束：无可用租户");
      return;
    }

    int scannedCount = 0;
    int skippedCount = 0;
    int failedCount = 0;

    for (Tenant tenant : tenants) {
      if (tenant == null || tenant.getId() == null) {
        skippedCount++;
        continue;
      }
      if (Boolean.FALSE.equals(tenant.getAvailable())) {
        skippedCount++;
        continue;
      }

      try {
        log.info("【补货提醒】租户 {} 开始扫描", tenant.getId());
        TenantContextHolder.setTenantId(tenant.getId());
        productStockReplenishWarningService.scanAndNotify();
        scannedCount++;
      } catch (Exception e) {
        failedCount++;
        log.error("【补货提醒】租户 {} 库存不足补货提醒执行失败: {}", tenant.getId(), e.getMessage(), e);
      } finally {
        TenantContextHolder.clearTenantId();
      }
    }

    log.info("【补货提醒】定时任务结束：扫描租户={}，跳过={}，失败={}",
        scannedCount, skippedCount, failedCount);
  }
}
