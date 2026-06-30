package com.lframework.xingyun.comp.migration;

import com.lframework.starter.web.inner.entity.Tenant;
import com.lframework.starter.web.inner.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 启动时为各租户库执行 classpath:db/migration/tenant/ 下未记录的增量 SQL。
 * 默认仅执行 version &gt;= 1.32，避免对老库重复跑 V1.0 全量脚本。
 */
@Slf4j
@Component
@Order(100)
@ConditionalOnProperty(name = "xingyun.tenant-sql-migration.enabled", havingValue = "true")
public class TenantSqlMigrationRunner implements ApplicationRunner {

  @Autowired
  private TenantService tenantService;

  @Autowired
  private TenantSqlMigrationService tenantSqlMigrationService;

  @Autowired
  private TenantSqlMigrationProperties properties;

  @Override
  public void run(ApplicationArguments args) {
    BigDecimal minVersion = properties.resolveMinVersion();
    log.info("租户库增量SQL迁移开始，minVersion={}", minVersion);
    List<Tenant> tenants = tenantService.findAll();
    int tenantCount = 0;
    int scriptCount = 0;
    for (Tenant tenant : tenants) {
      if (tenant == null || tenant.getId() == null || !Boolean.TRUE.equals(tenant.getAvailable())) {
        continue;
      }
      if (StringUtils.isAnyBlank(tenant.getJdbcUrl(), tenant.getJdbcUsername(), tenant.getJdbcPassword())) {
        log.warn("跳过租户（JDBC 配置不完整）: tenantId={}, name={}", tenant.getId(), tenant.getName());
        continue;
      }
      try {
        String password = tenantSqlMigrationService.decryptPassword(tenant.getJdbcPassword());
        int applied = tenantSqlMigrationService.migrateTenant(
            tenant.getJdbcUrl(), tenant.getJdbcUsername(), password, minVersion);
        tenantCount++;
        scriptCount += applied;
        if (applied > 0) {
          log.info("租户库增量SQL完成: tenantId={}, name={}, appliedScripts={}",
              tenant.getId(), tenant.getName(), applied);
        }
      } catch (Exception e) {
        log.error("租户库增量SQL失败: tenantId={}, name={}, reason={}",
            tenant.getId(), tenant.getName(), e.getMessage(), e);
        if (properties.isFailFast()) {
          throw e;
        }
      }
    }
    log.info("租户库增量SQL迁移结束，tenants={}, totalAppliedScripts={}", tenantCount, scriptCount);
  }
}
