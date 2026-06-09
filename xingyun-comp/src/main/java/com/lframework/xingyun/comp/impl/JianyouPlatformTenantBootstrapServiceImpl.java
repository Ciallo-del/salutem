package com.lframework.xingyun.comp.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.utils.IdUtil;
import com.lframework.starter.web.inner.entity.SysDept;
import com.lframework.starter.web.inner.entity.SysRole;
import com.lframework.starter.web.inner.entity.SysRoleCategory;
import com.lframework.starter.web.inner.service.system.SysDeptService;
import com.lframework.starter.web.inner.service.system.SysRoleCategoryService;
import com.lframework.starter.web.inner.service.system.SysRoleService;
import com.lframework.starter.web.inner.vo.system.dept.CreateSysDeptVo;
import com.lframework.starter.web.inner.vo.system.role.CreateSysRoleVo;
import com.lframework.xingyun.comp.bo.JianyouPlatformTenantInitBo;
import com.lframework.xingyun.comp.service.JianyouPlatformTenantBootstrapService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JianyouPlatformTenantBootstrapServiceImpl implements JianyouPlatformTenantBootstrapService {

  private static final String DEFAULT_ROLE_CATEGORY_ID = "1";
  private static final List<String> CORE_SYSTEM_PERMISSIONS = Collections.unmodifiableList(Arrays.asList(
      "system:user:query", "system:dept:query", "system:role:query"));

  @Autowired
  private SysDeptService sysDeptService;

  @Autowired
  private SysRoleCategoryService sysRoleCategoryService;

  @Autowired
  private SysRoleService sysRoleService;

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
  public JianyouPlatformTenantInitBo initializeTenantResources(Integer tenantId, String tenantJdbcUrl,
      String platformOrgCard, String token, Set<Integer> availableModuleIds) {
    if (tenantId == null) {
      throw new DefaultClientException("平台商租户不存在");
    }

    validateCurrentTenantDatabase(tenantId, tenantJdbcUrl);

    JianyouPlatformTenantInitBo bo = new JianyouPlatformTenantInitBo();
    bo.setDeptId(findOrCreateDept(platformOrgCard, token));
    bo.setRoleId(findOrCreateRole(platformOrgCard, token, availableModuleIds));
    return bo;
  }

  @Override
  public void ensureRoleMenus(List<String> roleIds, Set<Integer> availableModuleIds) {
    if (CollectionUtil.isEmpty(roleIds)) {
      throw new DefaultClientException("默认角色不存在，无法初始化菜单权限");
    }

    for (String roleId : roleIds) {
      if (StringUtils.isNotBlank(roleId)) {
        backfillRoleMenusIfNeeded(roleId, availableModuleIds);
      }
    }
  }

  private void validateCurrentTenantDatabase(Integer tenantId, String tenantJdbcUrl) {
    String expectedDatabaseName = extractDatabaseName(tenantJdbcUrl);
    if (StringUtils.isBlank(expectedDatabaseName)) {
      throw new DefaultClientException("平台商租户JDBC地址不合法，请修正后重试");
    }

    DataSource dataSource = ApplicationUtil.safeGetBean(DataSource.class);
    if (dataSource == null) {
      throw new DefaultClientException("平台商租户数据源未初始化完成，请稍后重试");
    }

    try (Connection connection = dataSource.getConnection()) {
      String currentDatabaseName = StringUtils.defaultIfBlank(connection.getCatalog(), connection.getSchema());
      if (StringUtils.isBlank(currentDatabaseName)) {
        throw new DefaultClientException("租户库业务初始化未切换到目标数据源，请稍后重试");
      }
      if (!StringUtils.equalsIgnoreCase(expectedDatabaseName, currentDatabaseName)) {
        throw new DefaultClientException("租户库业务初始化未切换到目标数据源，tenantId=" + tenantId
            + "，期望数据库：" + expectedDatabaseName + "，实际数据库：" + currentDatabaseName);
      }
    } catch (DefaultClientException e) {
      throw e;
    } catch (SQLException e) {
      throw new DefaultClientException("平台商租户库校验失败，原因：" + extractRootCauseMessage(e));
    }
  }

  private String findOrCreateDept(String platformOrgCard, String token) {
    String deptCode = buildDeptCode(token);
    SysDept dept = sysDeptService.findByCode(deptCode);
    if (dept != null) {
      return dept.getId();
    }

    CreateSysDeptVo createVo = new CreateSysDeptVo();
    createVo.setCode(deptCode);
    createVo.setName(buildDeptName(platformOrgCard));
    createVo.setShortName("建友");
    createVo.setDescription("建友平台商默认部门");
    return sysDeptService.create(createVo);
  }

  private String findOrCreateRole(String platformOrgCard, String token, Set<Integer> availableModuleIds) {
    String roleCode = buildRoleCode(token);
    List<SysRole> roles = sysRoleService.list(Wrappers.lambdaQuery(SysRole.class)
        .eq(SysRole::getCode, roleCode)
        .eq(SysRole::getAvailable, Boolean.TRUE));
    if (!CollectionUtil.isEmpty(roles)) {
      SysRole role = roles.get(0);
      String roleId = role.getId();
      backfillRoleMenusIfNeeded(roleId, availableModuleIds);
      return roleId;
    }

    SysRoleCategory defaultRoleCategory = findDefaultRoleCategory();
    if (defaultRoleCategory == null) {
      throw new DefaultClientException("默认角色分类不存在");
    }

    CreateSysRoleVo createVo = new CreateSysRoleVo();
    createVo.setCode(roleCode);
    createVo.setName(buildRoleName(platformOrgCard));
    createVo.setCategoryId(defaultRoleCategory.getId());
    createVo.setPermission("");
    createVo.setDescription("建友平台商默认角色");
    String roleId = sysRoleService.create(createVo);
    if (StringUtils.isBlank(roleId)) {
      throw new DefaultClientException("创建平台商默认角色失败");
    }
    backfillRoleMenusIfNeeded(roleId, availableModuleIds);
    return roleId;
  }

  private void backfillRoleMenusIfNeeded(String roleId, Set<Integer> availableModuleIds) {
    if (StringUtils.isBlank(roleId)) {
      throw new DefaultClientException("默认角色不存在，无法初始化菜单权限");
    }

    JdbcTemplate jdbcTemplate = currentTenantJdbcTemplate();
    backfillRoleMenusIncrementally(jdbcTemplate, roleId, availableModuleIds);
  }

  private List<String> loadAllAvailableMenuIds(JdbcTemplate jdbcTemplate, Set<Integer> availableModuleIds) {
    if (CollectionUtil.isEmpty(availableModuleIds)) {
      throw new DefaultClientException("当前租户可用模块为空，无法初始化默认角色权限");
    }

    Set<String> availableModuleIdStrings = availableModuleIds.stream()
        .filter(moduleId -> moduleId != null)
        .map(String::valueOf)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    if (CollectionUtil.isEmpty(availableModuleIdStrings)) {
      throw new DefaultClientException("当前租户可用模块为空，无法初始化默认角色权限");
    }

    List<Object> params = new ArrayList<>(availableModuleIdStrings);
    String inClause = availableModuleIdStrings.stream().map(item -> "?").collect(Collectors.joining(", "));
    String sql = "SELECT id FROM sys_menu WHERE available = 1 AND sys_module_id IN (" + inClause + ") ORDER BY code ASC, id ASC";
    return jdbcTemplate.query(sql, rs -> {
      List<String> menuIds = new ArrayList<>();
      while (rs.next()) {
        menuIds.add(rs.getString("id"));
      }
      return menuIds;
    }, params.toArray());
  }

  private JdbcTemplate currentTenantJdbcTemplate() {
    DataSource dataSource = ApplicationUtil.safeGetBean(DataSource.class);
    if (dataSource == null) {
      throw new DefaultClientException("平台商租户数据源未初始化完成，请稍后重试");
    }
    return new JdbcTemplate(dataSource);
  }

  private void backfillRoleMenusIncrementally(JdbcTemplate jdbcTemplate, String roleId, Set<Integer> availableModuleIds) {
    List<String> availableMenuIds = loadAllAvailableMenuIds(jdbcTemplate, availableModuleIds);
    if (CollectionUtil.isEmpty(availableMenuIds)) {
      throw new DefaultClientException("当前租户可用模块下没有任何可授权菜单");
    }

    Set<String> existingMenuIds = new LinkedHashSet<>(loadRoleMenuIds(jdbcTemplate, roleId));
    List<String> missingMenuIds = availableMenuIds.stream()
        .filter(menuId -> !existingMenuIds.contains(menuId))
        .collect(Collectors.toList());
    log.info("平台商默认角色菜单授权校验: roleId={}, targetMenuCount={}, existingMenuCount={}, missingMenuCount={}",
        roleId, availableMenuIds.size(), existingMenuIds.size(), missingMenuIds.size());

    if (!CollectionUtil.isEmpty(missingMenuIds)) {
      List<Object[]> params = new ArrayList<>();
      for (String menuId : missingMenuIds) {
        params.add(new Object[]{IdUtil.getId(), roleId, menuId});
      }
      jdbcTemplate.batchUpdate("INSERT INTO sys_role_menu (id, role_id, menu_id) VALUES (?, ?, ?)", params);
      log.info("平台商默认角色菜单授权补齐完成: roleId={}, appendedMenuCount={}", roleId, missingMenuIds.size());
    }

    validateRoleCorePermissions(jdbcTemplate, roleId);
  }

  private List<String> loadRoleMenuIds(JdbcTemplate jdbcTemplate, String roleId) {
    return jdbcTemplate.query("SELECT menu_id FROM sys_role_menu WHERE role_id = ?",
        rs -> {
          List<String> menuIds = new ArrayList<>();
          while (rs.next()) {
            menuIds.add(rs.getString("menu_id"));
          }
          return menuIds;
        }, roleId);
  }

  private void validateRoleCorePermissions(JdbcTemplate jdbcTemplate, String roleId) {
    String placeholders = CORE_SYSTEM_PERMISSIONS.stream().map(item -> "?").collect(Collectors.joining(", "));
    List<Object> params = new ArrayList<>();
    params.add(roleId);
    params.addAll(CORE_SYSTEM_PERMISSIONS);

    Set<String> grantedPermissions = jdbcTemplate.query(
        "SELECT DISTINCT m.permission FROM sys_role_menu rm "
            + "INNER JOIN sys_menu m ON rm.menu_id = m.id "
            + "WHERE rm.role_id = ? AND m.available = 1 AND m.permission IN (" + placeholders + ")",
        rs -> {
          Set<String> permissions = new LinkedHashSet<>();
          while (rs.next()) {
            permissions.add(StringUtils.trimToEmpty(rs.getString("permission")));
          }
          return permissions;
        }, params.toArray());

    List<String> missingPermissions = CORE_SYSTEM_PERMISSIONS.stream()
        .filter(permission -> !grantedPermissions.contains(permission))
        .collect(Collectors.toList());
    if (!missingPermissions.isEmpty()) {
      log.error("平台商默认角色缺少核心系统权限: roleId={}, missingPermissions={}", roleId, missingPermissions);
      throw new DefaultClientException("平台商默认角色缺少核心系统权限：" + String.join("、", missingPermissions));
    }
  }

  private SysRoleCategory findDefaultRoleCategory() {
    SysRoleCategory defaultRoleCategory = sysRoleCategoryService.findById(DEFAULT_ROLE_CATEGORY_ID);
    if (defaultRoleCategory != null) {
      return defaultRoleCategory;
    }

    List<SysRoleCategory> categories = sysRoleCategoryService.queryList();
    if (CollectionUtil.isEmpty(categories)) {
      return null;
    }

    for (SysRoleCategory category : categories) {
      if (category != null && StringUtils.equals("默认", category.getName())) {
        return category;
      }
    }
    return null;
  }

  private String buildDeptCode(String token) {
    return "jyd_" + token;
  }

  private String buildRoleCode(String token) {
    return "jyr_" + token;
  }

  private String buildDeptName(String platformOrgCard) {
    return "建友默认部门-" + platformOrgCard;
  }

  private String buildRoleName(String platformOrgCard) {
    return "建友默认角色-" + platformOrgCard;
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
}
