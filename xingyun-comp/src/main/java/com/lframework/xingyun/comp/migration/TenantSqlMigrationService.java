package com.lframework.xingyun.comp.migration;

import com.lframework.starter.web.core.utils.EncryptUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class TenantSqlMigrationService {

  private static final Pattern VERSION_PATTERN = Pattern.compile("^[Vv](\\d+)\\.(\\d+)__.*\\.sql$");

  private static final String MIGRATION_TABLE_DDL = ""
      + "CREATE TABLE IF NOT EXISTS `sys_tenant_sql_migration` ("
      + "  `id` varchar(32) NOT NULL,"
      + "  `script_name` varchar(128) NOT NULL,"
      + "  `script_version` decimal(10,2) NOT NULL,"
      + "  `applied_time` datetime NOT NULL,"
      + "  PRIMARY KEY (`id`),"
      + "  UNIQUE KEY `uk_script_name` (`script_name`)"
      + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户库增量SQL执行记录';";

  public int migrateTenant(String jdbcUrl, String username, String password, BigDecimal minVersion) {
    if (StringUtils.isAnyBlank(jdbcUrl, username, password)) {
      return 0;
    }
    try (HikariDataSource dataSource = createDataSource(jdbcUrl, username, password)) {
      JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
      jdbcTemplate.execute(MIGRATION_TABLE_DDL);
      List<MigrationScript> scripts = loadScripts(minVersion);
      int applied = 0;
      for (MigrationScript script : scripts) {
        if (isApplied(jdbcTemplate, script.getScriptName())) {
          continue;
        }
        applyScript(dataSource, script);
        recordApplied(jdbcTemplate, script);
        applied++;
        log.info("租户库增量SQL已执行: script={}, version={}, jdbcUrl={}",
            script.getScriptName(), script.getVersion(), maskJdbcUrl(jdbcUrl));
      }
      return applied;
    } catch (Exception e) {
      throw new IllegalStateException("租户库增量SQL执行失败: " + maskJdbcUrl(jdbcUrl) + ", "
          + e.getMessage(), e);
    }
  }

  private List<MigrationScript> loadScripts(BigDecimal minVersion) throws Exception {
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    Resource[] resources = resolver.getResources("classpath:db/migration/tenant/V*.sql");
    List<MigrationScript> scripts = new ArrayList<>();
    for (Resource resource : resources) {
      String filename = resource.getFilename();
      if (StringUtils.isBlank(filename)) {
        continue;
      }
      BigDecimal version = parseVersion(filename);
      if (version == null || version.compareTo(minVersion) < 0) {
        continue;
      }
      scripts.add(new MigrationScript(filename, version, resource));
    }
    scripts.sort(Comparator.comparing(MigrationScript::getVersion));
    return scripts;
  }

  private BigDecimal parseVersion(String filename) {
    Matcher matcher = VERSION_PATTERN.matcher(filename);
    if (!matcher.matches()) {
      return null;
    }
    return new BigDecimal(matcher.group(1) + "." + matcher.group(2));
  }

  private boolean isApplied(JdbcTemplate jdbcTemplate, String scriptName) {
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(1) FROM sys_tenant_sql_migration WHERE script_name = ?",
        Integer.class, scriptName);
    return count != null && count > 0;
  }

  private void applyScript(DataSource dataSource, MigrationScript script) {
    try {
      ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
      populator.setContinueOnError(false);
      populator.setIgnoreFailedDrops(true);
      populator.setSqlScriptEncoding("UTF-8");
      populator.addScript(script.getResource());
      populator.execute(dataSource);
    } catch (Exception e) {
      throw new IllegalStateException("执行脚本失败: " + script.getScriptName(), e);
    }
  }

  private void recordApplied(JdbcTemplate jdbcTemplate, MigrationScript script) {
    jdbcTemplate.update(
        "INSERT INTO sys_tenant_sql_migration (id, script_name, script_version, applied_time) VALUES (REPLACE(UUID(), '-', ''), ?, ?, NOW())",
        script.getScriptName(), script.getVersion());
  }

  private HikariDataSource createDataSource(String jdbcUrl, String username, String password) {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(jdbcUrl);
    config.setUsername(username);
    config.setPassword(password);
    config.setMaximumPoolSize(2);
    config.setMinimumIdle(0);
    config.setConnectionTimeout(30000);
    return new HikariDataSource(config);
  }

  public String decryptPassword(String encryptedPassword) {
    if (StringUtils.isBlank(encryptedPassword)) {
      return encryptedPassword;
    }
    try {
      return EncryptUtil.decrypt(encryptedPassword);
    } catch (Exception e) {
      return encryptedPassword;
    }
  }

  private String maskJdbcUrl(String jdbcUrl) {
    if (StringUtils.isBlank(jdbcUrl)) {
      return jdbcUrl;
    }
    return jdbcUrl.replaceAll("(?i)(password=)[^&;]+", "$1***");
  }

  private static final class MigrationScript {
    private final String scriptName;
    private final BigDecimal version;
    private final Resource resource;

    private MigrationScript(String scriptName, BigDecimal version, Resource resource) {
      this.scriptName = scriptName;
      this.version = version;
      this.resource = resource;
    }

    public String getScriptName() {
      return scriptName;
    }

    public BigDecimal getVersion() {
      return version;
    }

    public Resource getResource() {
      return resource;
    }
  }
}
