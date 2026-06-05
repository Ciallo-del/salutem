package com.lframework.xingyun.comp.impl;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DataSourceProperty;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceProperties;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.web.core.components.tenant.TenantContextHolder;
import com.lframework.starter.web.core.event.ClearTenantEvent;
import com.lframework.starter.web.core.event.ReloadTenantEvent;
import com.lframework.starter.web.core.event.SetTenantEvent;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.utils.DataSourceUtil;
import com.lframework.starter.web.core.utils.EncryptUtil;
import com.lframework.starter.web.core.utils.IdUtil;
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
import com.lframework.xingyun.comp.bo.JianyouPlatformTenantInitBo;
import com.lframework.xingyun.comp.service.JianyouPlatformProvisionService;
import com.lframework.xingyun.comp.service.JianyouPlatformTenantBootstrapService;
import com.lframework.xingyun.comp.vo.JianyouPlatformProvisionVo;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
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

@Service
public class JianyouPlatformProvisionServiceImpl implements JianyouPlatformProvisionService {

  private static final LocalDateTime DEFAULT_EXPIRE_TIME = LocalDateTime.of(2099, 1, 1, 0, 0);
  private static final String TENANT_DB_PREFIX = "xingyun_jy_";
  private static final String[] REQUIRED_TENANT_TABLES = {"sys_dept", "sys_role_category", "sys_user"};
  private static final String TEMPLATE_ROLE_CODE = "001";

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

  @Override
  public JianyouPlatformProvisionBo provision(JianyouPlatformProvisionVo vo) {
    String platformOrgCard = StringUtils.trimToEmpty(vo.getPlatformOrgCard());
    String platformOrgName = StringUtils.trimToEmpty(vo.getPlatformOrgName());
    if (StringUtils.isBlank(platformOrgCard)) {
      throw new DefaultClientException("平台商标识不能为空！");
    }
    if (StringUtils.isBlank(platformOrgName)) {
      throw new DefaultClientException("平台商名称不能为空！");
    }

    String token = buildTenantToken(platformOrgCard);
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
    prepareTenantDatabase(tenantDbConfig, templateTenant);

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
      throw new DefaultClientException("创建平台商租户失败！");
    }
    return tenant;
  }

  private void validateExistingTenantConfig(Tenant tenant, String token) {
    if (tenant == null || tenant.getId() == null) {
      throw new DefaultClientException("平台商租户不存在！");
    }

    String expectedDatabaseName = TENANT_DB_PREFIX + token;
    String actualDatabaseName = extractDatabaseName(tenant.getJdbcUrl());
    if (StringUtils.isBlank(actualDatabaseName)) {
      throw new DefaultClientException("平台商租户 JDBC 地址不合法，请修正后重试！");
    }
    if (!StringUtils.equals(expectedDatabaseName, actualDatabaseName)) {
      throw new DefaultClientException("平台商租户未绑定独立租户库，请清理错误租户配置后重试！");
    }
  }

  private Tenant findTemplateTenant() {
    List<Tenant> tenants = tenantService.findAll();
    if (CollectionUtil.isEmpty(tenants)) {
      throw new DefaultClientException("未找到可复用的租户库模板！");
    }

    for (Tenant tenant : tenants) {
      if (tenant != null
          && StringUtils.isNotBlank(tenant.getJdbcUrl())
          && StringUtils.isNotBlank(tenant.getJdbcUsername())
          && StringUtils.isNotBlank(tenant.getJdbcPassword())) {
        return tenant;
      }
    }

    throw new DefaultClientException("未找到可复用的租户库模板！");
  }

  private TenantDbConfig buildTenantDbConfig(Tenant templateTenant, String token) {
    String templateJdbcUrl = templateTenant.getJdbcUrl();
    if (StringUtils.isBlank(templateJdbcUrl)) {
      throw new DefaultClientException("租户库模板 JDBC 地址为空！");
    }

    String dbName = extractDatabaseName(templateJdbcUrl);
    if (StringUtils.isBlank(dbName)) {
      throw new DefaultClientException("租户库模板 JDBC 地址不合法！");
    }

    int dbNameStart = templateJdbcUrl.indexOf("/" + dbName);
    if (dbNameStart < 0) {
      throw new DefaultClientException("租户库模板 JDBC 地址不合法！");
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

  private void prepareTenantDatabase(TenantDbConfig tenantDbConfig, Tenant templateTenant) {
    String username = templateTenant.getJdbcUsername();
    String password = EncryptUtil.decrypt(templateTenant.getJdbcPassword());
    JdbcTemplate rootJdbcTemplate = new JdbcTemplate(createStandaloneDataSource(
        tenantDbConfig.getRootJdbcUrl(), username, password));

    if (!databaseExists(rootJdbcTemplate, tenantDbConfig.getDatabaseName())) {
      createDatabase(rootJdbcTemplate, tenantDbConfig.getDatabaseName());
    }

    DataSource tenantDataSource = createStandaloneDataSource(tenantDbConfig.getJdbcUrl(), username, password);
    if (hasAllRequiredTables(tenantDataSource)) {
      return;
    }
    if (hasAnyRequiredTable(tenantDataSource)) {
      throw new DefaultClientException("平台商租户库初始化不完整，请清理后重试！");
    }

    initializeTenantSchema(tenantDataSource);
    if (!hasAllRequiredTables(tenantDataSource)) {
      throw new DefaultClientException("平台商租户库初始化失败！");
    }
  }

  private boolean databaseExists(JdbcTemplate jdbcTemplate, String databaseName) {
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = ?",
        Integer.class, databaseName);
    return count != null && count > 0;
  }

  private void createDatabase(JdbcTemplate jdbcTemplate, String databaseName) {
    try {
      jdbcTemplate.execute("CREATE DATABASE `" + databaseName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
    } catch (Exception e) {
      throw new DefaultClientException("平台商租户库创建权限不足或创建失败！");
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
      throw new DefaultClientException("平台商租户库初始化失败：tenant.sql 执行异常，原因：" + extractRootCauseMessage(e));
    } catch (Exception e) {
      throw new DefaultClientException("平台商租户库初始化失败：tenant.sql 执行异常，原因：" + extractRootCauseMessage(e));
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
      throw new DefaultClientException("平台商租户库初始化失败！");
    }
  }

  private DataSource createStandaloneDataSource(String jdbcUrl, String username, String password) {
    DataSourceProperty masterProperty = getMasterDataSourceProperty();
    if (masterProperty == null) {
      throw new DefaultClientException("主数据源未初始化完成！");
    }
    return DataSourceUtil.createDataSource(masterProperty, jdbcUrl, username, password);
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
        throw new DefaultClientException("模板租户数据源未初始化完成，请稍后重试！");
      }

      JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
      Integer count = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM sys_role WHERE code = ? AND available = 1",
          Integer.class, TEMPLATE_ROLE_CODE);
      if (count == null || count <= 0) {
        throw new DefaultClientException("模板默认角色不存在，请先在模板租户中配置角色编码 " + TEMPLATE_ROLE_CODE + "！");
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
      throw new DefaultClientException("平台商租户不存在！");
    }
    if (StringUtils.isBlank(tenant.getJdbcUrl())
        || StringUtils.isBlank(tenant.getJdbcUsername())
        || StringUtils.isBlank(tenant.getJdbcPassword())) {
      throw new DefaultClientException("平台商租户数据源配置不完整！");
    }

    DynamicRoutingDataSource dynamicRoutingDataSource = ApplicationUtil.safeGetBean(DynamicRoutingDataSource.class);
    if (dynamicRoutingDataSource == null) {
      throw new DefaultClientException("平台商租户数据源未初始化完成，请稍后重试！");
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
        throw new DefaultClientException("平台商租户数据源未初始化完成，请稍后重试！");
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
      throw new DefaultClientException("平台商租户 JDBC 地址不合法，请修正后重试！");
    }

    try (Connection connection = dataSource.getConnection()) {
      String currentDatabaseName = StringUtils.defaultIfBlank(connection.getCatalog(), connection.getSchema());
      if (StringUtils.isBlank(currentDatabaseName)) {
        throw new DefaultClientException("平台商租户数据源未切换到目标库，请稍后重试！");
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
        throw new DefaultClientException("平台商租户库缺少基础表 " + tableName + "！");
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
    List<SysOpenDomain> domains = sysOpenDomainService.list(Wrappers.lambdaQuery(SysOpenDomain.class)
        .eq(SysOpenDomain::getName, openDomainName)
        .eq(SysOpenDomain::getTenantId, tenantId));
    if (!CollectionUtil.isEmpty(domains)) {
      return domains.get(0);
    }

    CreateSysOpenDomainVo createVo = new CreateSysOpenDomainVo();
    createVo.setName(openDomainName);
    createVo.setApiSecret(DigestUtils.md5Hex(platformOrgCard + ":" + token));
    createVo.setTenantId(tenantId);
    createVo.setDescription("建佑平台商开放域：" + platformOrgName);
    String openDomainId = sysOpenDomainService.create(createVo);
    SysOpenDomain openDomain = sysOpenDomainService.findById(Integer.valueOf(openDomainId));
    if (openDomain == null) {
      throw new DefaultClientException("创建平台商开放域失败！");
    }
    return openDomain;
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

  private String buildTenantServerName(String platformOrgCard) {
    return "jianyou-" + StringUtils.lowerCase(platformOrgCard);
  }

  private String buildTenantName(String token) {
    return "建佑平台商租户-" + token;
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

}
