package com.lframework.xingyun.comp.migration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Data
@Component
@ConfigurationProperties(prefix = "xingyun.tenant-sql-migration")
public class TenantSqlMigrationProperties {

  /** 是否启用启动时租户库增量 SQL 迁移 */
  private boolean enabled = true;

  /** 仅执行该版本及以上的脚本（默认 1.32，对应菜单乱码修复） */
  private String minVersion = "1.32";

  /** 任一租户迁移失败时是否中断启动 */
  private boolean failFast = false;

  public BigDecimal resolveMinVersion() {
    return new BigDecimal(minVersion);
  }
}
