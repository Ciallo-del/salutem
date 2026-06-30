package com.lframework.xingyun.comp.impl;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DataSourceProperty;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceProperties;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.common.locker.LockBuilder;
import com.lframework.starter.common.locker.Locker;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.web.core.components.tenant.TenantContextHolder;
import com.lframework.starter.web.core.event.ClearTenantEvent;
import com.lframework.starter.web.core.event.ReloadTenantEvent;
import com.lframework.starter.web.core.event.SetTenantEvent;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.utils.DataSourceUtil;
import com.lframework.starter.web.core.utils.EncryptUtil;
import com.lframework.starter.web.inner.entity.SysModule;
import com.lframework.starter.web.inner.entity.SysModuleTenant;
import com.lframework.starter.web.inner.entity.SysOpenDomain;
import com.lframework.starter.web.inner.entity.Tenant;
import com.lframework.starter.web.inner.service.SysModuleService;
import com.lframework.starter.web.inner.service.SysModuleTenantService;
import com.lframework.starter.web.inner.service.TenantService;
import com.lframework.starter.web.inner.service.system.SysOpenDomainService;
import com.lframework.starter.web.inner.vo.system.module.SysModuleTenantVo;
import com.lframework.starter.web.inner.vo.system.open.CreateSysOpenDomainVo;
import com.lframework.starter.web.inner.vo.system.tenant.CreateTenantVo;
import com.lframework.xingyun.comp.bo.JianyouPlatformProvisionBo;
import com.lframework.xingyun.comp.bo.JianyouPlatformRepairBo;
import com.lframework.xingyun.comp.bo.JianyouPlatformTenantInitBo;
import com.lframework.xingyun.comp.service.JianyouPlatformProvisionService;
import com.lframework.xingyun.comp.service.JianyouPlatformTenantBootstrapService;
import com.lframework.xingyun.comp.vo.JianyouPlatformProvisionVo;
import com.lframework.xingyun.comp.vo.JianyouPlatformRepairVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptException;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JianyouPlatformProvisionServiceImpl implements JianyouPlatformProvisionService {

  private static final LocalDateTime DEFAULT_EXPIRE_TIME = LocalDateTime.of(2099, 1, 1, 0, 0);
  private static final String TENANT_DB_PREFIX = "xingyun_jy_";
  private static final String[] REQUIRED_TENANT_TABLES = {"sys_dept", "sys_role_category", "sys_user"};
  private static final String TEMPLATE_ROLE_CODE = "001";
  private static final String PLATFORM_PROVISION_LOCK_PREFIX = "jianyou_platform_provision_";
  private static final long PLATFORM_PROVISION_LOCK_EXPIRE_MILLIS = 300000L;
  private static final long PLATFORM_PROVISION_LOCK_WAIT_MILLIS = 1000L;

  @Autowired
  private TenantService tenantService;

  @Autowired
  private SysModuleService sysModuleService;

  @Autowired
  private SysModuleTenantService sysModuleTenantService;

  @Autowired
  private SysOpenDomainService sysOpenDomainService;

  @Autowired
  private JianyouPlatformTenantBootstrapService jianyouPlatformTenantBootstrapService;

  @Autowired
  private DynamicDataSourceProperties dynamicDataSourceProperties;

  @Autowired
  private LockBuilder lockBuilder;

  @Override
  public JianyouPlatformProvisionBo provision(JianyouPlatformProvisionVo vo) {
    String platformOrgCard = StringUtils.trimToEmpty(vo.getPlatformOrgCard());
    String platformOrgName = StringUtils.trimToEmpty(vo.getPlatformOrgName());
    if (StringUtils.isBlank(platformOrgCard)) {
      throw new DefaultClientException("平台机构标识不能为空");
    }
    if (StringUtils.isBlank(platformOrgName)) {
      throw new DefaultClientException("平台机构名称不能为空");
    }

    String token = buildTenantToken(platformOrgCard);
    String serverName = buildTenantServerName(platformOrgCard);
    Locker locker = lockBuilder.buildLocker(buildPlatformProvisionLockKey(serverName),
        PLATFORM_PROVISION_LOCK_EXPIRE_MILLIS, PLATFORM_PROVISION_LOCK_WAIT_MILLIS);
    if (!locker.lock()) {
      throw new DefaultClientException("平台商对应的星云租户正在初始化，请稍后重试");
    }

    try {
      Tenant tenant = findOrCreateTenant(platformOrgCard, token);
      Set<Integer> availableModuleIds = copyModuleAuthIfNeeded(tenant.getId());
      ensureTenantDataSourceReady(tenant);
      loadTemplateRole();

      JianyouPlatformTenantInitBo tenantInitBo;
      boolean tenantSwitched = false;
      try {
        switchToTenant(tenant.getId());
        TenantContextHolder.setTenantId(tenant.getId());
        tenantSwitched = true;
        tenantInitBo = jianyouPlatformTenantBootstrapService.initializeTenantResources(
            tenant.getId(), tenant.getJdbcUrl(), platformOrgCard, token, availableModuleIds);
      } finally {
        if (tenantSwitched) {
          TenantContextHolder.clearTenantId();
          clearTenant();
        }
      }

      SysOpenDomain openDomain = findOrCreateOpenDomain(platformOrgCard, token, tenant.getId(), platformOrgName);

      JianyouPlatformProvisionBo bo = new JianyouPlatformProvisionBo();
      bo.setTargetTenantId(tenant.getId());
      bo.setOpenDomainClientId(String.valueOf(openDomain.getId()));
      bo.setDefaultDeptIds(Collections.singletonList(tenantInitBo.getDeptId()));
      bo.setDefaultRoleIds(Collections.singletonList(tenantInitBo.getRoleId()));
      return bo;
    } finally {
      locker.unLock();
    }
  }

  @Override
  public JianyouPlatformRepairBo repairExisting(JianyouPlatformRepairVo vo) {
    SysOpenDomain openDomain = getExistingOpenDomain(vo.getOpenDomainClientId());
    validateOpenDomainBinding(openDomain, vo.getTenantId());

    Tenant tenant = findOrCreateRepairTenant(vo);
    refreshTenantBaseConfig(tenant, vo);
    persistTenantBaseConfig(tenant);

    Set<Integer> availableModuleIds = copyModuleAuthIfNeeded(tenant.getId());
    ensureTenantDataSourceReady(tenant);
    loadTemplateRole();

    String token = resolveRepairToken(vo, tenant, openDomain);
    String platformOrgCard = StringUtils.defaultIfBlank(StringUtils.trimToNull(vo.getPlatformOrgCard()), token);

    JianyouPlatformTenantInitBo tenantInitBo;
    boolean tenantSwitched = false;
    try {
      switchToTenant(tenant.getId());
      TenantContextHolder.setTenantId(tenant.getId());
      tenantSwitched = true;
      tenantInitBo = jianyouPlatformTenantBootstrapService.initializeTenantResources(
          tenant.getId(), tenant.getJdbcUrl(), platformOrgCard, token, availableModuleIds);
    } finally {
      if (tenantSwitched) {
        TenantContextHolder.clearTenantId();
        clearTenant();
      }
    }

    JianyouPlatformRepairBo bo = new JianyouPlatformRepairBo();
    bo.setTargetTenantId(tenant.getId());
    bo.setOpenDomainClientId(String.valueOf(openDomain.getId()));
    bo.setJdbcUrl(tenant.getJdbcUrl());
    bo.setDefaultDeptIds(Collections.singletonList(tenantInitBo.getDeptId()));
    bo.setDefaultRoleIds(Collections.singletonList(tenantInitBo.getRoleId()));
    return bo;
  }

  private Tenant findOrCreateTenant(String platformOrgCard, String token) {
    String serverName = buildTenantServerName(platformOrgCard);
    List<Tenant> tenants = tenantService.list(Wrappers.lambdaQuery(Tenant.class)
        .eq(Tenant::getServerName, serverName));
    if (!CollectionUtil.isEmpty(tenants)) {
      Tenant tenant = tenants.get(0);
      validateExistingTenantConfig(tenant, token);
      return tenant;
    }

    Tenant templateTenant = findTemplateTenant();
    TenantDbConfig tenantDbConfig = buildTenantDbConfig(templateTenant, token);
    prepareTenantDatabase(platformOrgCard, serverName, tenantDbConfig, templateTenant);

    CreateTenantVo createVo = new CreateTenantVo();
    createVo.setName(buildTenantName(token));
    createVo.setServerName(serverName);
    createVo.setJdbcUrl(tenantDbConfig.getJdbcUrl());
    createVo.setJdbcUsername(templateTenant.getJdbcUsername());
    createVo.setJdbcPassword(EncryptUtil.decrypt(templateTenant.getJdbcPassword()));
    createVo.setIsPlatform(Boolean.FALSE);

    Integer tenantId = tenantService.create(createVo);
    Tenant tenant = tenantService.findById(tenantId);
    if (tenant == null) {
      throw new DefaultClientException("创建平台商租户失败");
    }
    return tenant;
  }

  private SysOpenDomain getExistingOpenDomain(String openDomainClientId) {
    if (!StringUtils.isNumeric(openDomainClientId)) {
      throw new DefaultClientException("开放域客户端不存在");
    }
    SysOpenDomain openDomain = sysOpenDomainService.findById(Integer.valueOf(openDomainClientId));
    if (openDomain == null) {
      throw new DefaultClientException("开放域客户端不存在");
    }
    if (!Boolean.TRUE.equals(openDomain.getAvailable())) {
      throw new DefaultClientException("开放域客户端未启用");
    }
    return openDomain;
  }

  private Tenant findOrCreateRepairTenant(JianyouPlatformRepairVo vo) {
    if (vo.getTenantId() == null) {
      throw new DefaultClientException("租户不存在");
    }

    Tenant tenant = tenantService.findById(vo.getTenantId());
    if (tenant != null) {
      return tenant;
    }

    Tenant templateTenant = findTemplateTenant();
    tenant = new Tenant();
    tenant.setId(vo.getTenantId());
    tenant.setName(StringUtils.trimToNull(vo.getTenantName()));
    tenant.setServerName(StringUtils.trimToNull(vo.getServerName()));
    tenant.setJdbcUrl(StringUtils.trimToNull(vo.getJdbcUrl()));
    tenant.setJdbcUsername(templateTenant.getJdbcUsername());
    tenant.setJdbcPassword(templateTenant.getJdbcPassword());
    tenant.setIsPlatform(Boolean.FALSE);
    tenant.setAvailable(Boolean.TRUE);

    insertTenant(tenant);

    Tenant createdTenant = tenantService.findById(vo.getTenantId());
    if (createdTenant == null) {
      throw new DefaultClientException("补建租户失败");
    }
    return createdTenant;
  }

  private void insertTenant(Tenant tenant) {
    DataSource dataSource = ApplicationUtil.safeGetBean(DataSource.class);
    if (dataSource == null) {
      throw new DefaultClientException("主数据源未初始化完成，请稍后重试");
    }

    LocalDateTime now = LocalDateTime.now();
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    int rows = jdbcTemplate.update(
        "INSERT INTO tenant (id, name, server_name, jdbc_url, jdbc_username, jdbc_password, is_platform, available, create_time, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        tenant.getId(),
        tenant.getName(),
        tenant.getServerName(),
        tenant.getJdbcUrl(),
        tenant.getJdbcUsername(),
        tenant.getJdbcPassword(),
        tenant.getIsPlatform(),
        tenant.getAvailable(),
        now,
        now);
    if (rows <= 0) {
      throw new DefaultClientException("补建租户失败");
    }
  }

  private void validateOpenDomainBinding(SysOpenDomain openDomain, Integer tenantId) {
    if (tenantId == null) {
      throw new DefaultClientException("租户不存在");
    }
    if (openDomain.getTenantId() == null || !openDomain.getTenantId().equals(tenantId)) {
      throw new DefaultClientException("开放域客户端与租户不匹配");
    }
  }

  private void refreshTenantBaseConfig(Tenant tenant, JianyouPlatformRepairVo vo) {
    Tenant templateTenant = findTemplateTenant();
    String jdbcUrl = StringUtils.trimToNull(vo.getJdbcUrl());
    if (StringUtils.isBlank(jdbcUrl)) {
      jdbcUrl = tenant.getJdbcUrl();
    }
    if (StringUtils.isBlank(jdbcUrl)) {
      jdbcUrl = templateTenant.getJdbcUrl();
    }
    if (StringUtils.isBlank(jdbcUrl)) {
      throw new DefaultClientException("租户数据库JDBC地址未配置");
    }

    String tenantName = StringUtils.trimToNull(vo.getTenantName());
    if (StringUtils.isBlank(tenantName)) {
      tenantName = StringUtils.defaultIfBlank(tenant.getName(), "建友租户-" + tenant.getId());
    }

    String serverName = StringUtils.trimToNull(vo.getServerName());
    if (StringUtils.isBlank(serverName)) {
      serverName = StringUtils.defaultIfBlank(tenant.getServerName(), "jianyou-" + tenant.getId());
    }

    tenant.setName(tenantName);
    tenant.setServerName(serverName);
    tenant.setJdbcUrl(jdbcUrl);
    tenant.setJdbcUsername(StringUtils.defaultIfBlank(tenant.getJdbcUsername(), templateTenant.getJdbcUsername()));
    tenant.setJdbcPassword(StringUtils.defaultIfBlank(tenant.getJdbcPassword(), templateTenant.getJdbcPassword()));
    tenant.setIsPlatform(Boolean.FALSE);
    tenant.setAvailable(Boolean.TRUE);
  }

  private void persistTenantBaseConfig(Tenant tenant) {
    DataSource dataSource = ApplicationUtil.safeGetBean(DataSource.class);
    if (dataSource == null) {
      throw new DefaultClientException("主数据源未初始化完成，请稍后重试");
    }

    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.update(
        "UPDATE tenant SET name = ?, server_name = ?, jdbc_url = ?, jdbc_username = ?, jdbc_password = ?, is_platform = ?, available = ?, update_time = ? WHERE id = ?",
        tenant.getName(),
        tenant.getServerName(),
        tenant.getJdbcUrl(),
        tenant.getJdbcUsername(),
        tenant.getJdbcPassword(),
        tenant.getIsPlatform(),
        tenant.getAvailable(),
        LocalDateTime.now(),
        tenant.getId());
  }

  private String resolveRepairToken(JianyouPlatformRepairVo vo, Tenant tenant, SysOpenDomain openDomain) {
    String value = StringUtils.trimToNull(vo.getPlatformOrgCard());
    if (StringUtils.isNotBlank(value)) {
      return buildTenantToken(value);
    }

    value = StringUtils.trimToNull(vo.getPlatformOrgName());
    if (StringUtils.isNotBlank(value)) {
      return DigestUtils.md5Hex(StringUtils.lowerCase(value)).substring(0, 12);
    }

    value = StringUtils.trimToNull(openDomain.getName());
    if (StringUtils.isNotBlank(value)) {
      return DigestUtils.md5Hex(StringUtils.lowerCase(value)).substring(0, 12);
    }

    value = StringUtils.trimToNull(tenant.getServerName());
    if (StringUtils.isNotBlank(value)) {
      return DigestUtils.md5Hex(StringUtils.lowerCase(value)).substring(0, 12);
    }

    return DigestUtils.md5Hex("tenant:" + tenant.getId()).substring(0, 12);
  }

  private void validateExistingTenantConfig(Tenant tenant, String token) {
    if (tenant == null || tenant.getId() == null) {
      throw new DefaultClientException("平台商租户不存在");
    }

    String expectedDatabaseName = TENANT_DB_PREFIX + token;
    String actualDatabaseName = extractDatabaseName(tenant.getJdbcUrl());
    if (StringUtils.isBlank(actualDatabaseName)) {
      throw new DefaultClientException("平台商租户JDBC地址不合法，请修正后重试");
    }
    if (!StringUtils.equals(expectedDatabaseName, actualDatabaseName)) {
      throw new DefaultClientException("平台商租户未绑定独立租户库，请清理错误租户配置后重试");
    }
  }

  private Tenant findTemplateTenant() {
    List<Tenant> tenants = tenantService.findAll();
    if (CollectionUtil.isEmpty(tenants)) {
      throw new DefaultClientException("未找到可复用的租户库模板");
    }

    for (Tenant tenant : tenants) {
      if (tenant != null
          && StringUtils.isNotBlank(tenant.getJdbcUrl())
          && StringUtils.isNotBlank(tenant.getJdbcUsername())
          && StringUtils.isNotBlank(tenant.getJdbcPassword())) {
        return tenant;
      }
    }

    throw new DefaultClientException("未找到可复用的租户库模板");
  }

  private TenantDbConfig buildTenantDbConfig(Tenant templateTenant, String token) {
    String templateJdbcUrl = templateTenant.getJdbcUrl();
    if (StringUtils.isBlank(templateJdbcUrl)) {
      throw new DefaultClientException("租户库模板JDBC地址为空");
    }

    String dbName = extractDatabaseName(templateJdbcUrl);
    if (StringUtils.isBlank(dbName)) {
      throw new DefaultClientException("租户库模板JDBC地址不合法");
    }

    int dbNameStart = templateJdbcUrl.indexOf("/" + dbName);
    if (dbNameStart < 0) {
      throw new DefaultClientException("租户库模板JDBC地址不合法");
    }

    String query = "";
    int queryStart = templateJdbcUrl.indexOf('?', dbNameStart);
    if (queryStart >= 0) {
      query = templateJdbcUrl.substring(queryStart);
    }
    String jdbcPrefix = templateJdbcUrl.substring(0, dbNameStart);
    String tenantDbName = TENANT_DB_PREFIX + token;
    String rootJdbcUrl = jdbcPrefix + "/" + query;
    String tenantJdbcUrl = jdbcPrefix + "/" + tenantDbName + query;
    return new TenantDbConfig(tenantDbName, rootJdbcUrl, tenantJdbcUrl);
  }

  private void prepareTenantDatabase(String platformOrgCard, String serverName, TenantDbConfig tenantDbConfig,
      Tenant templateTenant) {
    String username = templateTenant.getJdbcUsername();
    String password = EncryptUtil.decrypt(templateTenant.getJdbcPassword());
    JdbcTemplate rootJdbcTemplate = null;
    boolean rebuilt = false;
    int tableCountBeforeInitialize = 0;
    try {
      rootJdbcTemplate = createRootJdbcTemplate(tenantDbConfig, username, password);

      if (!databaseExists(rootJdbcTemplate, tenantDbConfig.getDatabaseName())) {
        createDatabase(rootJdbcTemplate, tenantDbConfig.getDatabaseName());
      }

      DataSource tenantDataSource = createTenantDataSource(tenantDbConfig, username, password);
      TenantDatabaseSnapshot snapshot = inspectTenantDatabase(tenantDataSource);
      tableCountBeforeInitialize = snapshot.getTableCount();
      if (snapshot.getState() == TenantDatabaseState.READY) {
        return;
      }

      if (snapshot.getState() == TenantDatabaseState.DIRTY) {
        rebuilt = true;
        log.warn("检测到平台商星云租户库为半成品，准备自动清理重建，platformOrgCard={}, serverName={}, databaseName={}, tableCount={}",
            StringUtils.defaultIfBlank(platformOrgCard, "-"),
            StringUtils.defaultIfBlank(serverName, "-"),
            tenantDbConfig.getDatabaseName(),
            snapshot.getTableCount());
        rebuildTenantDatabase(rootJdbcTemplate, tenantDbConfig, templateTenant);
        tenantDataSource = createTenantDataSource(tenantDbConfig, username, password);
        snapshot = inspectTenantDatabase(tenantDataSource);
        tableCountBeforeInitialize = snapshot.getTableCount();
        if (snapshot.getState() != TenantDatabaseState.EMPTY) {
          throw new TenantDatabasePrepareException(TenantDatabaseStage.TENANT_CHECK,
              new IllegalStateException("租户库重建后仍不是空库"));
        }
      }

      initializeTenantSchema(tenantDataSource);
      ensureTenantSchemaInitialized(tenantDataSource);
    } catch (TenantDatabasePrepareException e) {
      if (rootJdbcTemplate != null && shouldCleanupFailedTenantDatabase(e)) {
        cleanupFailedTenantDatabase(rootJdbcTemplate, tenantDbConfig, templateTenant);
      }
      logTenantDatabasePrepareFailure(platformOrgCard, serverName, tenantDbConfig.getDatabaseName(), username,
          rebuilt, tableCountBeforeInitialize, e);
      throw new DefaultClientException(buildTenantDatabaseErrorMessage(e, rebuilt));
    }
  }

  private boolean databaseExists(JdbcTemplate jdbcTemplate, String databaseName) {
    try {
      Integer count = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = ?",
          Integer.class, databaseName);
      return count != null && count > 0;
    } catch (Exception e) {
      throw new TenantDatabasePrepareException(TenantDatabaseStage.ROOT_CHECK, e);
    }
  }

  private void createDatabase(JdbcTemplate jdbcTemplate, String databaseName) {
    try {
      jdbcTemplate.execute("CREATE DATABASE `" + databaseName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
    } catch (Exception e) {
      throw new TenantDatabasePrepareException(TenantDatabaseStage.CREATE_DATABASE, e);
    }
  }

  private void initializeTenantSchema(DataSource dataSource) {
    try {
      ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
      populator.setContinueOnError(false);
      populator.setIgnoreFailedDrops(false);
      populator.setSqlScriptEncoding("UTF-8");
      populator.addScript(new ClassPathResource("db/all/tenant.sql"));
      populator.execute(dataSource);
    } catch (ScriptException e) {
      throw new TenantDatabasePrepareException(TenantDatabaseStage.TENANT_INITIALIZE, e);
    } catch (Exception e) {
      throw new TenantDatabasePrepareException(TenantDatabaseStage.TENANT_INITIALIZE, e);
    }
  }

  private void ensureTenantSchemaInitialized(DataSource dataSource) {
    if (!hasAllRequiredTables(dataSource)) {
      throw new TenantDatabasePrepareException(TenantDatabaseStage.TENANT_INITIALIZE,
          new IllegalStateException("租户库初始化后缺少核心表"));
    }
  }

  private String extractRootCauseMessage(Throwable e) {
    Throwable root = e;
    while (root.getCause() != null && root.getCause() != root) {
      root = root.getCause();
    }

    String message = StringUtils.trimToEmpty(root.getMessage());
    if (StringUtils.isBlank(message)) {
      message = StringUtils.trimToEmpty(e.getMessage());
    }
    message = sanitizeSensitiveMessage(message);
    return StringUtils.defaultIfBlank(message, root.getClass().getSimpleName());
  }

  private boolean hasAllRequiredTables(DataSource dataSource) {
    for (String tableName : REQUIRED_TENANT_TABLES) {
      if (!tableExists(dataSource, tableName)) {
        return false;
      }
    }
    return true;
  }

  private boolean hasAnyRequiredTable(DataSource dataSource) {
    for (String tableName : REQUIRED_TENANT_TABLES) {
      if (tableExists(dataSource, tableName)) {
        return true;
      }
    }
    return false;
  }

  private boolean tableExists(DataSource dataSource, String tableName) {
    try (Connection connection = dataSource.getConnection()) {
      DatabaseMetaData metaData = connection.getMetaData();
      try (ResultSet resultSet = metaData.getTables(connection.getCatalog(), null, tableName, new String[]{"TABLE"})) {
        return resultSet.next();
      }
    } catch (SQLException e) {
      throw new TenantDatabasePrepareException(TenantDatabaseStage.TENANT_CHECK, e);
    }
  }

  private TenantDatabaseSnapshot inspectTenantDatabase(DataSource dataSource) {
    int tableCount = countTables(dataSource);
    if (tableCount <= 0) {
      return new TenantDatabaseSnapshot(TenantDatabaseState.EMPTY, 0);
    }
    if (hasAllRequiredTables(dataSource)) {
      return new TenantDatabaseSnapshot(TenantDatabaseState.READY, tableCount);
    }
    return new TenantDatabaseSnapshot(TenantDatabaseState.DIRTY, tableCount);
  }

  private int countTables(DataSource dataSource) {
    try (Connection connection = dataSource.getConnection()) {
      DatabaseMetaData metaData = connection.getMetaData();
      int count = 0;
      try (ResultSet resultSet = metaData.getTables(connection.getCatalog(), null, "%", new String[]{"TABLE"})) {
        while (resultSet.next()) {
          count++;
        }
      }
      return count;
    } catch (SQLException e) {
      throw new TenantDatabasePrepareException(TenantDatabaseStage.TENANT_CHECK, e);
    }
  }

  private void rebuildTenantDatabase(JdbcTemplate rootJdbcTemplate, TenantDbConfig tenantDbConfig, Tenant templateTenant) {
    dropDatabase(rootJdbcTemplate, tenantDbConfig.getDatabaseName(), templateTenant);
    createDatabase(rootJdbcTemplate, tenantDbConfig.getDatabaseName());
  }

  private void cleanupFailedTenantDatabase(JdbcTemplate rootJdbcTemplate, TenantDbConfig tenantDbConfig,
      Tenant templateTenant) {
    try {
      if (databaseExists(rootJdbcTemplate, tenantDbConfig.getDatabaseName())) {
        dropDatabase(rootJdbcTemplate, tenantDbConfig.getDatabaseName(), templateTenant);
      }
    } catch (TenantDatabasePrepareException e) {
      log.warn("平台商星云租户库初始化失败后清理数据库未成功，databaseName={}, reason={}",
          tenantDbConfig.getDatabaseName(), extractRootCauseMessage(e), e);
    }
  }

  private boolean shouldCleanupFailedTenantDatabase(TenantDatabasePrepareException e) {
    TenantDatabaseStage stage = e.getStage();
    return stage == TenantDatabaseStage.TENANT_INITIALIZE || stage == TenantDatabaseStage.TENANT_CHECK;
  }

  private void dropDatabase(JdbcTemplate jdbcTemplate, String databaseName, Tenant templateTenant) {
    if (!isSafeTenantDatabaseName(databaseName, templateTenant)) {
      throw new TenantDatabasePrepareException(TenantDatabaseStage.DROP_DATABASE,
          new IllegalStateException("目标租户库名称不安全，拒绝删除"));
    }
    try {
      jdbcTemplate.execute("DROP DATABASE IF EXISTS `" + databaseName + "`");
    } catch (Exception e) {
      throw new TenantDatabasePrepareException(TenantDatabaseStage.DROP_DATABASE, e);
    }
  }

  private boolean isSafeTenantDatabaseName(String databaseName, Tenant templateTenant) {
    if (StringUtils.isBlank(databaseName) || !StringUtils.startsWith(databaseName, TENANT_DB_PREFIX)) {
      return false;
    }
    if (databaseName.length() != TENANT_DB_PREFIX.length() + 12) {
      return false;
    }
    String suffix = databaseName.substring(TENANT_DB_PREFIX.length());
    if (!StringUtils.isAlphanumeric(suffix) || !StringUtils.equals(suffix, StringUtils.lowerCase(suffix))) {
      return false;
    }

    String templateDatabaseName = extractDatabaseName(templateTenant.getJdbcUrl());
    return !StringUtils.equalsIgnoreCase(databaseName, templateDatabaseName);
  }

  private DataSource createStandaloneDataSource(String jdbcUrl, String username, String password) {
    DataSourceProperty masterProperty = getMasterDataSourceProperty();
    if (masterProperty == null) {
      throw new DefaultClientException("主数据源未初始化完成，请稍后重试");
    }
    return DataSourceUtil.createDataSource(masterProperty, jdbcUrl, username, password);
  }

  private JdbcTemplate createRootJdbcTemplate(TenantDbConfig tenantDbConfig, String username, String password) {
    try {
      return new JdbcTemplate(createStandaloneDataSource(tenantDbConfig.getRootJdbcUrl(), username, password));
    } catch (TenantDatabasePrepareException e) {
      throw e;
    } catch (DefaultClientException e) {
      throw e;
    } catch (Exception e) {
      throw new TenantDatabasePrepareException(TenantDatabaseStage.ROOT_CONNECT, e);
    }
  }

  private DataSource createTenantDataSource(TenantDbConfig tenantDbConfig, String username, String password) {
    try {
      return createStandaloneDataSource(tenantDbConfig.getJdbcUrl(), username, password);
    } catch (TenantDatabasePrepareException e) {
      throw e;
    } catch (DefaultClientException e) {
      throw e;
    } catch (Exception e) {
      throw new TenantDatabasePrepareException(TenantDatabaseStage.TENANT_CONNECT, e);
    }
  }

  private DataSourceProperty getMasterDataSourceProperty() {
    if (dynamicDataSourceProperties == null || dynamicDataSourceProperties.getDatasource() == null) {
      return null;
    }
    return dynamicDataSourceProperties.getDatasource().get("master");
  }

  private Set<Integer> copyModuleAuthIfNeeded(Integer tenantId) {
    List<SysModuleTenant> existingMappings = sysModuleTenantService.getByTenantId(tenantId);
    if (!CollectionUtil.isEmpty(existingMappings)) {
      return extractAvailableModuleIds(existingMappings);
    }

    Tenant templateTenant = findTemplateTenant();
    List<SysModuleTenant> templateMappings = sysModuleTenantService.getByTenantId(templateTenant.getId());
    if (CollectionUtil.isEmpty(templateMappings)) {
      return Collections.emptySet();
    }

    Set<Integer> platformModuleIds = findPlatformModuleIds(templateMappings);
    SysModuleTenantVo settingVo = new SysModuleTenantVo();
    settingVo.setTenantId(tenantId);
    List<SysModuleTenantVo.SysModuleVo> modules = new ArrayList<>();
    for (SysModuleTenant item : templateMappings) {
      if (item == null || item.getModuleId() == null || platformModuleIds.contains(item.getModuleId())) {
        continue;
      }

      SysModuleTenantVo.SysModuleVo moduleVo = new SysModuleTenantVo.SysModuleVo();
      moduleVo.setModuleId(item.getModuleId());
      moduleVo.setExpireTime(item.getExpireTime() == null ? DEFAULT_EXPIRE_TIME : item.getExpireTime());
      modules.add(moduleVo);
    }

    if (!modules.isEmpty()) {
      settingVo.setModules(modules);
      sysModuleTenantService.setting(settingVo);
    }
    return modules.stream()
        .map(SysModuleTenantVo.SysModuleVo::getModuleId)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private Set<Integer> findPlatformModuleIds(List<SysModuleTenant> templateMappings) {
    Set<Integer> moduleIds = templateMappings.stream()
        .filter(item -> item != null && item.getModuleId() != null)
        .map(SysModuleTenant::getModuleId)
        .collect(Collectors.toSet());
    if (CollectionUtil.isEmpty(moduleIds)) {
      return Collections.emptySet();
    }

    List<SysModule> platformModules = sysModuleService.list(Wrappers.lambdaQuery(SysModule.class)
        .in(SysModule::getId, moduleIds)
        .eq(SysModule::getIsPlatform, Boolean.TRUE));
    if (CollectionUtil.isEmpty(platformModules)) {
      return Collections.emptySet();
    }

    return platformModules.stream()
        .map(SysModule::getId)
        .collect(Collectors.toSet());
  }

  private Set<Integer> extractAvailableModuleIds(List<SysModuleTenant> mappings) {
    return mappings.stream()
        .filter(item -> item != null && item.getModuleId() != null)
        .map(SysModuleTenant::getModuleId)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private void loadTemplateRole() {
    Tenant templateTenant = findTemplateTenant();
    boolean tenantSwitched = false;
    try {
      ensureTenantDataSourceReady(templateTenant);
      switchToTenant(templateTenant.getId());
      TenantContextHolder.setTenantId(templateTenant.getId());
      tenantSwitched = true;

      DataSource dataSource = ApplicationUtil.safeGetBean(DataSource.class);
      if (dataSource == null) {
        throw new DefaultClientException("模板租户数据源未初始化完成，请稍后重试");
      }

      JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
      Integer count = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM sys_role WHERE code = ? AND available = 1",
          Integer.class, TEMPLATE_ROLE_CODE);
      if (count == null || count <= 0) {
        throw new DefaultClientException("模板默认角色不存在，请先在模板租户中配置角色编码 " + TEMPLATE_ROLE_CODE);
      }
    } catch (DefaultClientException e) {
      throw e;
    } catch (Exception e) {
      throw new DefaultClientException("读取模板默认角色失败，原因：" + extractRootCauseMessage(e));
    } finally {
      if (tenantSwitched) {
        TenantContextHolder.clearTenantId();
        clearTenant();
      }
    }
  }

  private void ensureTenantDataSourceReady(Tenant tenant) {
    if (tenant == null || tenant.getId() == null) {
      throw new DefaultClientException("平台商租户不存在");
    }
    if (StringUtils.isBlank(tenant.getJdbcUrl())
        || StringUtils.isBlank(tenant.getJdbcUsername())
        || StringUtils.isBlank(tenant.getJdbcPassword())) {
      throw new DefaultClientException("平台商租户数据源配置不完整");
    }

    DynamicRoutingDataSource dynamicRoutingDataSource = ApplicationUtil.safeGetBean(DynamicRoutingDataSource.class);
    if (dynamicRoutingDataSource == null) {
      throw new DefaultClientException("平台商租户数据源未初始化完成，请稍后重试");
    }

    String tenantDataSourceKey = String.valueOf(tenant.getId());
    if (dynamicRoutingDataSource.getDataSources().containsKey(tenantDataSourceKey)) {
      dynamicRoutingDataSource.removeDataSource(tenantDataSourceKey);
    }

    ApplicationUtil.publishEvent(new ReloadTenantEvent(this, tenant.getId(), tenant.getJdbcUrl(),
        tenant.getJdbcUsername(), EncryptUtil.decrypt(tenant.getJdbcPassword())));

    validateTenantTables(tenant);
  }

  private void validateTenantTables(Tenant tenant) {
    boolean tenantSwitched = false;
    try {
      switchToTenant(tenant.getId());
      TenantContextHolder.setTenantId(tenant.getId());
      tenantSwitched = true;

      DataSource dataSource = ApplicationUtil.safeGetBean(DataSource.class);
      if (dataSource == null) {
        throw new DefaultClientException("平台商租户数据源未初始化完成，请稍后重试");
      }

      validateCurrentTenantDatabase(dataSource, tenant);
      for (String tableName : REQUIRED_TENANT_TABLES) {
        ensureTenantTableExists(dataSource, tableName);
      }
    } finally {
      if (tenantSwitched) {
        TenantContextHolder.clearTenantId();
        clearTenant();
      }
    }
  }

  private void validateCurrentTenantDatabase(DataSource dataSource, Tenant tenant) {
    String expectedDatabaseName = extractDatabaseName(tenant.getJdbcUrl());
    if (StringUtils.isBlank(expectedDatabaseName)) {
      throw new DefaultClientException("平台商租户JDBC地址不合法，请修正后重试");
    }

    try (Connection connection = dataSource.getConnection()) {
      String currentDatabaseName = StringUtils.defaultIfBlank(connection.getCatalog(), connection.getSchema());
      if (StringUtils.isBlank(currentDatabaseName)) {
        throw new DefaultClientException("平台商租户数据源未切换到目标库，请稍后重试");
      }
      if (!StringUtils.equalsIgnoreCase(expectedDatabaseName, currentDatabaseName)) {
        throw new DefaultClientException("当前命中数据库不是平台商租户库，期望：" + expectedDatabaseName
            + "，实际：" + currentDatabaseName);
      }
    } catch (DefaultClientException e) {
      throw e;
    } catch (SQLException e) {
      throw new DefaultClientException("平台商租户库校验失败，原因：" + extractRootCauseMessage(e));
    }
  }

  private void ensureTenantTableExists(DataSource dataSource, String tableName) {
    try {
      if (!tableExists(dataSource, tableName)) {
        throw new DefaultClientException("平台商租户库缺少基础表：" + tableName);
      }
    } catch (DefaultClientException e) {
      throw e;
    } catch (Exception e) {
      throw new DefaultClientException("平台商租户库校验失败，原因：" + extractRootCauseMessage(e));
    }
  }

  private SysOpenDomain findOrCreateOpenDomain(String platformOrgCard, String token, Integer tenantId,
      String platformOrgName) {
    String openDomainName = buildOpenDomainName(token);
    SysOpenDomain existing = findOpenDomainByName(openDomainName);
    if (existing != null) {
      ensureOpenDomainTenantBinding(existing, tenantId, platformOrgCard);
      return existing;
    }

    CreateSysOpenDomainVo createVo = new CreateSysOpenDomainVo();
    createVo.setName(openDomainName);
    createVo.setApiSecret(DigestUtils.md5Hex(platformOrgCard + ":" + token));
    createVo.setTenantId(tenantId);
    createVo.setDescription("建友平台商开放域：" + platformOrgName);
    try {
      String openDomainId = sysOpenDomainService.create(createVo);
      SysOpenDomain openDomain = sysOpenDomainService.findById(Integer.valueOf(openDomainId));
      if (openDomain == null) {
        throw new DefaultClientException("创建平台商开放域失败");
      }
      return openDomain;
    } catch (DuplicateKeyException e) {
      SysOpenDomain concurrent = findOpenDomainByName(openDomainName);
      if (concurrent == null) {
        throw new DefaultClientException("创建平台商开放域失败");
      }
      ensureOpenDomainTenantBinding(concurrent, tenantId, platformOrgCard);
      return concurrent;
    }
  }

  private SysOpenDomain findOpenDomainByName(String openDomainName) {
    List<SysOpenDomain> domains = sysOpenDomainService.list(Wrappers.lambdaQuery(SysOpenDomain.class)
        .eq(SysOpenDomain::getName, openDomainName));
    if (CollectionUtil.isEmpty(domains)) {
      return null;
    }
    return domains.get(0);
  }

  private void ensureOpenDomainTenantBinding(SysOpenDomain openDomain, Integer tenantId, String platformOrgCard) {
    if (tenantId == null) {
      throw new DefaultClientException("租户不存在");
    }
    if (openDomain.getTenantId() != null && openDomain.getTenantId().equals(tenantId)) {
      return;
    }

    String expectedServerName = buildTenantServerName(platformOrgCard);
    if (openDomain.getTenantId() == null) {
      log.warn("开放域未绑定租户，自动绑定: openDomainId={}, tenantId={}", openDomain.getId(), tenantId);
    } else {
      Tenant boundTenant = tenantService.findById(openDomain.getTenantId());
      if (boundTenant != null) {
        if (StringUtils.equals(expectedServerName, boundTenant.getServerName())) {
          log.warn("开放域租户ID与当前租户不一致，自动修正绑定: openDomainId={}, oldTenantId={}, newTenantId={}",
              openDomain.getId(), openDomain.getTenantId(), tenantId);
        } else {
          throw new DefaultClientException("开放域已绑定其他租户，请使用修复接口 /xy/xingyun/jianyou/platform/repair");
        }
      } else {
        log.warn("开放域绑定租户已不存在，自动修正: openDomainId={}, oldTenantId={}, newTenantId={}",
            openDomain.getId(), openDomain.getTenantId(), tenantId);
      }
    }

    updateOpenDomainTenantId(Integer.valueOf(openDomain.getId()), tenantId);
    openDomain.setTenantId(tenantId);
  }

  private void updateOpenDomainTenantId(Integer openDomainId, Integer tenantId) {
    DataSource dataSource = ApplicationUtil.safeGetBean(DataSource.class);
    if (dataSource == null) {
      throw new DefaultClientException("主数据源未初始化完成，请稍后重试");
    }

    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    int rows = jdbcTemplate.update(
        "UPDATE sys_open_domain SET tenant_id = ?, update_time = ? WHERE id = ?",
        tenantId,
        LocalDateTime.now(),
        openDomainId);
    if (rows <= 0) {
      throw new DefaultClientException("修正开放域租户绑定失败");
    }
  }

  private void switchToTenant(Integer tenantId) {
    ApplicationUtil.publishEvent(new SetTenantEvent(this, tenantId));
  }

  private void clearTenant() {
    ApplicationUtil.publishEvent(new ClearTenantEvent(this));
  }

  private String buildTenantToken(String platformOrgCard) {
    return DigestUtils.md5Hex(StringUtils.lowerCase(platformOrgCard)).substring(0, 12);
  }

  private String buildPlatformProvisionLockKey(String serverName) {
    return PLATFORM_PROVISION_LOCK_PREFIX + serverName;
  }

  private String buildTenantServerName(String platformOrgCard) {
    return "jianyou-" + StringUtils.lowerCase(platformOrgCard);
  }

  private String buildTenantName(String token) {
    return "建友平台商租户-" + token;
  }

  private String buildOpenDomainName(String token) {
    return "jyod_" + token;
  }

  private String extractDatabaseName(String jdbcUrl) {
    if (StringUtils.isBlank(jdbcUrl)) {
      return null;
    }

    int schemeIndex = jdbcUrl.indexOf("://");
    if (schemeIndex < 0) {
      return null;
    }
    int slashIndex = jdbcUrl.indexOf('/', schemeIndex + 3);
    if (slashIndex < 0 || slashIndex == jdbcUrl.length() - 1) {
      return null;
    }

    String dbPart = jdbcUrl.substring(slashIndex + 1);
    int queryIndex = dbPart.indexOf('?');
    if (queryIndex >= 0) {
      dbPart = dbPart.substring(0, queryIndex);
    }
    return StringUtils.trimToNull(dbPart);
  }

  private void logTenantDatabasePrepareFailure(String platformOrgCard, String serverName, String databaseName,
      String jdbcUsername, boolean rebuilt, int tableCountBeforeInitialize, TenantDatabasePrepareException e) {
    log.error("平台商星云租户库初始化失败，platformOrgCard={}, serverName={}, databaseName={}, jdbcUsername={}, stage={}, rebuilt={}, tableCountBeforeInitialize={}, reason={}",
        StringUtils.defaultIfBlank(platformOrgCard, "-"),
        StringUtils.defaultIfBlank(serverName, "-"),
        StringUtils.defaultIfBlank(databaseName, "-"),
        StringUtils.defaultIfBlank(jdbcUsername, "-"),
        e.getStage().getDescription(),
        rebuilt,
        tableCountBeforeInitialize,
        extractRootCauseMessage(e),
        e);
  }

  private String buildTenantDatabaseErrorMessage(TenantDatabasePrepareException e, boolean rebuilt) {
    TenantDatabaseStage stage = e.getStage();
    if (stage == TenantDatabaseStage.ROOT_CONNECT || stage == TenantDatabaseStage.ROOT_CHECK) {
      if (isAccessDenied(e) || isCommunicationFailure(e)) {
        return "平台商星云租户库初始化失败：租户库账号无权连接数据库，请检查 jdbc_username、jdbc_password 或数据库白名单";
      }
      return "平台商星云租户库初始化失败，原因：" + extractRootCauseMessage(e);
    }
    if (stage == TenantDatabaseStage.CREATE_DATABASE) {
      if (isCreateDatabasePermissionDenied(e) || isAccessDenied(e)) {
        return "平台商星云租户库初始化失败：租户库账号没有创建数据库的权限";
      }
      return "平台商星云租户库初始化失败，原因：" + extractRootCauseMessage(e);
    }
    if (stage == TenantDatabaseStage.DROP_DATABASE) {
      return "检测到半成品租户库，但自动清理失败，请检查数据库删除权限后重试，原因：" + extractRootCauseMessage(e);
    }
    if (stage == TenantDatabaseStage.TENANT_CONNECT
        || stage == TenantDatabaseStage.TENANT_CHECK
        || stage == TenantDatabaseStage.TENANT_INITIALIZE) {
      if (isAccessDenied(e) || isCommunicationFailure(e) || isUnknownDatabase(e)) {
        return "平台商星云租户库初始化失败：租户数据库不可访问，请检查数据库权限或实例状态";
      }
      if (rebuilt) {
        return "检测到半成品租户库，已自动清理并重建后再次初始化，但仍然失败，原因：" + extractRootCauseMessage(e);
      }
      return "平台商星云租户库初始化失败，原因：" + extractRootCauseMessage(e);
    }
    return "平台商星云租户库初始化失败，原因：" + extractRootCauseMessage(e);
  }

  private boolean isAccessDenied(Throwable e) {
    return containsIgnoreCase(extractRootCauseMessage(e), "Access denied for user");
  }

  private boolean isCreateDatabasePermissionDenied(Throwable e) {
    String message = extractRootCauseMessage(e);
    return containsIgnoreCase(message, "CREATE command denied")
        || containsIgnoreCase(message, "command denied to user");
  }

  private boolean isUnknownDatabase(Throwable e) {
    return containsIgnoreCase(extractRootCauseMessage(e), "Unknown database");
  }

  private boolean isCommunicationFailure(Throwable e) {
    String message = extractRootCauseMessage(e);
    return containsIgnoreCase(message, "Communications link failure")
        || containsIgnoreCase(message, "Connection refused")
        || containsIgnoreCase(message, "connect timed out")
        || containsIgnoreCase(message, "Connection timed out")
        || containsIgnoreCase(message, "Could not create connection to database server");
  }

  private boolean containsIgnoreCase(String text, String keyword) {
    return StringUtils.containsIgnoreCase(StringUtils.trimToEmpty(text), keyword);
  }

  private String sanitizeSensitiveMessage(String message) {
    if (StringUtils.isBlank(message)) {
      return message;
    }
    String sanitized = message.replaceAll("(?i)jdbc:[^\\s,;]+", "jdbc:***");
    sanitized = sanitized.replaceAll("(?i)(password\\s*[:=]\\s*)([^\\s,;]+)", "$1***");
    return sanitized;
  }

  private static class TenantDbConfig {
    private final String databaseName;
    private final String rootJdbcUrl;
    private final String jdbcUrl;

    private TenantDbConfig(String databaseName, String rootJdbcUrl, String jdbcUrl) {
      this.databaseName = databaseName;
      this.rootJdbcUrl = rootJdbcUrl;
      this.jdbcUrl = jdbcUrl;
    }

    public String getDatabaseName() {
      return databaseName;
    }

    public String getRootJdbcUrl() {
      return rootJdbcUrl;
    }

    public String getJdbcUrl() {
      return jdbcUrl;
    }
  }

  private static class TenantDatabaseSnapshot {
    private final TenantDatabaseState state;
    private final int tableCount;

    private TenantDatabaseSnapshot(TenantDatabaseState state, int tableCount) {
      this.state = state;
      this.tableCount = tableCount;
    }

    public TenantDatabaseState getState() {
      return state;
    }

    public int getTableCount() {
      return tableCount;
    }
  }

  private enum TenantDatabaseState {
    READY,
    EMPTY,
    DIRTY
  }

  private enum TenantDatabaseStage {
    ROOT_CONNECT("根库连接"),
    ROOT_CHECK("根库校验"),
    CREATE_DATABASE("创建租户库"),
    DROP_DATABASE("清理租户库"),
    TENANT_CONNECT("租户库连接"),
    TENANT_CHECK("租户库校验"),
    TENANT_INITIALIZE("租户库初始化");

    private final String description;

    TenantDatabaseStage(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  private static class TenantDatabasePrepareException extends RuntimeException {
    private final TenantDatabaseStage stage;

    private TenantDatabasePrepareException(TenantDatabaseStage stage, Throwable cause) {
      super(cause);
      this.stage = stage;
    }

    public TenantDatabaseStage getStage() {
      return stage;
    }
  }
}
