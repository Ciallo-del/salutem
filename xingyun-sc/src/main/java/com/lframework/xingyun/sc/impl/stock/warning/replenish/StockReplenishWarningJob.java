package com.lframework.xingyun.sc.impl.stock.warning.replenish;

import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.web.core.components.qrtz.QrtzJob;
import com.lframework.starter.web.core.components.tenant.TenantContextHolder;
import com.lframework.starter.web.inner.entity.Tenant;
import com.lframework.starter.web.inner.service.TenantService;
import com.lframework.xingyun.sc.service.stock.warning.ProductStockReplenishWarningService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 库存不足补货提醒定时任务（平台级，逐租户扫描）
 */
@Slf4j
public class StockReplenishWarningJob extends QrtzJob {

  @Autowired
  private TenantService tenantService;

  @Autowired
  private ProductStockReplenishWarningService productStockReplenishWarningService;

  @Override
  public void onExecute(JobExecutionContext context) {

    List<Tenant> tenants = tenantService.findAll();
    if (CollectionUtil.isEmpty(tenants)) {
      return;
    }

    for (Tenant tenant : tenants) {
      if (tenant == null || tenant.getId() == null) {
        continue;
      }
      if (Boolean.FALSE.equals(tenant.getAvailable()) || Boolean.TRUE.equals(
          tenant.getIsPlatform())) {
        // 跳过停用租户与平台租户
        continue;
      }

      try {
        TenantContextHolder.setTenantId(tenant.getId());
        productStockReplenishWarningService.scanAndNotify();
      } catch (Exception e) {
        log.error("租户 {} 库存不足补货提醒执行失败: {}", tenant.getId(), e.getMessage(), e);
      } finally {
        TenantContextHolder.clearTenantId();
      }
    }
  }
}
