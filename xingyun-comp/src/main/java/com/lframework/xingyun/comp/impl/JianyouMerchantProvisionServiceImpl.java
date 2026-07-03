package com.lframework.xingyun.comp.impl;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.components.security.AbstractUserDetails;
import com.lframework.starter.web.core.components.security.UserDetailsService;
import com.lframework.starter.web.core.components.tenant.TenantContextHolder;
import com.lframework.starter.web.core.event.ClearTenantEvent;
import com.lframework.starter.web.core.event.ReloadTenantEvent;
import com.lframework.starter.web.core.event.SetTenantEvent;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.utils.EncryptUtil;
import com.lframework.starter.web.core.utils.IdUtil;
import com.lframework.starter.web.inner.entity.SysModuleTenant;
import com.lframework.starter.web.inner.entity.SysOpenDomain;
import com.lframework.starter.web.inner.entity.SysUser;
import com.lframework.starter.web.inner.entity.Tenant;
import com.lframework.starter.web.inner.service.SysModuleTenantService;
import com.lframework.starter.web.inner.service.TenantService;
import com.lframework.starter.web.inner.service.system.SysOpenDomainService;
import com.lframework.starter.web.inner.service.system.SysUserService;
import com.lframework.starter.web.inner.vo.system.user.CreateSysUserVo;
import com.lframework.starter.web.inner.vo.system.user.QuerySysUserVo;
import com.lframework.xingyun.comp.bo.JianyouMerchantProvisionBo;
import com.lframework.xingyun.comp.bo.JianyouMerchantRepairBo;
import com.lframework.xingyun.comp.bo.JianyouMerchantUserSyncBo;
import com.lframework.xingyun.comp.service.JianyouMerchantProvisionService;
import com.lframework.xingyun.comp.service.JianyouPlatformTenantBootstrapService;
import com.lframework.xingyun.comp.vo.JianyouMerchantProvisionVo;
import com.lframework.xingyun.comp.vo.JianyouMerchantRepairVo;
import com.lframework.xingyun.comp.vo.JianyouMerchantUserDisableVo;
import com.lframework.xingyun.comp.vo.JianyouMerchantUserUpdateVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JianyouMerchantProvisionServiceImpl implements JianyouMerchantProvisionService {

  private static final String DEFAULT_REPAIR_PASSWORD = "jianyou88";
  private static final String DEFAULT_REPAIR_PASSWORD_HASH = "$2a$10$IJtHluhnhAYkgvM4PdKuZek5PWbtuxtjB9pB.twZdxg/qrlR4s4q6";
  private static final String SYSTEM_USER_ID = "1";
  private static final String SYSTEM_USER_NAME = "系统管理员";
  private static final List<String> CORE_SYSTEM_PERMISSIONS = Collections.unmodifiableList(Arrays.asList(
      "system:user:query", "system:dept:query", "system:role:query"));
  private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

  @Autowired
  private SysOpenDomainService sysOpenDomainService;

  @Autowired
  private SysUserService sysUserService;

  @Autowired
  private SysModuleTenantService sysModuleTenantService;

  @Autowired
  private JianyouPlatformTenantBootstrapService jianyouPlatformTenantBootstrapService;

  @Autowired
  private UserDetailsService userDetailsService;

  @Autowired
  private TenantService tenantService;

  @Autowired
  private DynamicRoutingDataSource dynamicRoutingDataSource;

  @Override
  public JianyouMerchantProvisionBo provision(JianyouMerchantProvisionVo vo) {
    SysOpenDomain openDomain = getOpenDomain(vo.getOpenDomainClientId());
    if (openDomain == null) {
      throw new DefaultClientException("开放域客户端不存在");
    }
    if (!Boolean.TRUE.equals(openDomain.getAvailable())) {
      throw new DefaultClientException("开放域客户端未启用");
    }
    if (openDomain.getTenantId() == null || !openDomain.getTenantId().equals(vo.getTenantId())) {
      throw new DefaultClientException("开放域客户端与租户不匹配");
    }

    boolean tenantSwitched = false;
    try {
      ApplicationUtil.publishEvent(new SetTenantEvent(this, vo.getTenantId()));
      TenantContextHolder.setTenantId(vo.getTenantId());
      tenantSwitched = true;

      List<String> roleIds = resolveRoleIds(vo.getRoleIds());
      if (CollectionUtil.isEmpty(roleIds)) {
        throw new DefaultClientException("角色ID列表不能为空");
      }
      Set<Integer> availableModuleIds = loadAvailableModuleIds(vo.getTenantId());
      jianyouPlatformTenantBootstrapService.ensureRoleMenus(roleIds, availableModuleIds);

      QuerySysUserVo queryVo = new QuerySysUserVo();
      queryVo.setUsername(vo.getUsername());
      List<SysUser> existsUsers = sysUserService.query(queryVo);
      if (!CollectionUtil.isEmpty(existsUsers)) {
        // 幂等：用户名已存在（通常是上一次初始化中断后被定时任务重新驱动），复用已有用户并补齐绑定后按成功返回
        SysUser existsUser = existsUsers.get(0);
        ensureUserDeptBindings(existsUser.getId(), vo.getDeptIds());
        ensureUserRoleBindings(existsUser.getId(), roleIds);
        log.info("建友商户星陨用户已存在，复用并补齐绑定: tenantId={}, userId={}, username={}, deptIds={}, roleIds={}",
            vo.getTenantId(), existsUser.getId(), vo.getUsername(), vo.getDeptIds(), vo.getRoleIds());

        JianyouMerchantProvisionBo existsBo = new JianyouMerchantProvisionBo();
        existsBo.setTargetTenantId(vo.getTenantId());
        existsBo.setTargetXingyunUserId(existsUser.getId());
        existsBo.setTargetXingyunUsername(existsUser.getUsername());
        return existsBo;
      }

      CreateSysUserVo createVo = new CreateSysUserVo();
      createVo.setCode(vo.getUsername());
      createVo.setUsername(vo.getUsername());
      createVo.setName(vo.getName());
      createVo.setPassword(vo.getRawPassword());
      createVo.setEmail(StringUtils.trimToNull(vo.getEmail()));
      createVo.setTelephone(StringUtils.trimToNull(vo.getTelephone()));
      createVo.setDeptIds(vo.getDeptIds());
      createVo.setRoleIds(roleIds);
      createVo.setDescription(buildDescription(vo));

      log.info("建友商户星陨用户初始化开始: tenantId={}, username={}, deptIds={}, roleIds={}",
          vo.getTenantId(), vo.getUsername(), vo.getDeptIds(), roleIds);
      String userId = sysUserService.create(createVo);
      if (StringUtils.isBlank(userId)) {
        throw new DefaultClientException("创建星陨用户失败");
      }

      ensureUserRoleBindings(userId, roleIds);
      log.info("建友商户星陨用户初始化完成: tenantId={}, userId={}, username={}, roleIds={}",
          vo.getTenantId(), userId, vo.getUsername(), vo.getRoleIds());

      JianyouMerchantProvisionBo bo = new JianyouMerchantProvisionBo();
      bo.setTargetTenantId(vo.getTenantId());
      bo.setTargetXingyunUserId(userId);
      bo.setTargetXingyunUsername(vo.getUsername());
      return bo;
    } finally {
      if (tenantSwitched) {
        TenantContextHolder.clearTenantId();
        ApplicationUtil.publishEvent(new ClearTenantEvent(this));
      }
    }
  }

  @Override
  public JianyouMerchantRepairBo ensureUserPermissions(JianyouMerchantRepairVo vo) {
    SysOpenDomain openDomain = getOpenDomain(vo.getOpenDomainClientId());
    if (openDomain == null) {
      throw new DefaultClientException("开放域客户端不存在");
    }
    if (!Boolean.TRUE.equals(openDomain.getAvailable())) {
      throw new DefaultClientException("开放域客户端未启用");
    }
    if (openDomain.getTenantId() == null || !openDomain.getTenantId().equals(vo.getTenantId())) {
      throw new DefaultClientException("开放域客户端与租户不匹配");
    }

    Set<Integer> availableModuleIds = loadAvailableModuleIds(vo.getTenantId());

    boolean tenantSwitched = false;
    try {
      ensureTenantDataSourceReady(vo.getTenantId());
      ApplicationUtil.publishEvent(new SetTenantEvent(this, vo.getTenantId()));
      TenantContextHolder.setTenantId(vo.getTenantId());
      tenantSwitched = true;

      List<String> deptIds = resolveDeptIds(vo.getDeptIds());
      List<String> roleIds = resolveRoleIds(vo.getRoleIds());
      if (CollectionUtil.isEmpty(deptIds)) {
        throw new DefaultClientException("当前租户下未找到可用部门，无法补齐菜单权限");
      }
      if (CollectionUtil.isEmpty(roleIds)) {
        throw new DefaultClientException("当前租户下未找到可用角色，无法补齐菜单权限");
      }

      jianyouPlatformTenantBootstrapService.ensureRoleMenus(roleIds, availableModuleIds);

      SysUser targetUser = sysUserService.findById(vo.getTargetUserId());
      if (targetUser == null) {
        throw new DefaultClientException("目标星陨用户不存在");
      }

      ensureUserDeptBindings(vo.getTargetUserId(), deptIds);
      ensureUserRoleBindings(vo.getTargetUserId(), roleIds);
      assertUserHasPermissions(vo.getTargetUserId(), roleIds, availableModuleIds, targetUser.getUsername());

      JianyouMerchantRepairBo bo = new JianyouMerchantRepairBo();
      bo.setTargetTenantId(vo.getTenantId());
      bo.setTargetXingyunUserId(targetUser.getId());
      bo.setTargetXingyunUsername(targetUser.getUsername());
      bo.setDeptIds(deptIds);
      bo.setRoleIds(roleIds);
      log.info("建友商户星陨用户权限补齐完成: tenantId={}, userId={}, username={}, roleIds={}",
          vo.getTenantId(), targetUser.getId(), targetUser.getUsername(), roleIds);
      return bo;
    } finally {
      if (tenantSwitched) {
        TenantContextHolder.clearTenantId();
        ApplicationUtil.publishEvent(new ClearTenantEvent(this));
      }
    }
  }

  @Override
  public void backfillSsoUserPermissions(String userId, List<String> preferredRoleIds, List<String> preferredDeptIds) {
    if (StringUtils.isBlank(userId)) {
      throw new DefaultClientException("目标星陨用户ID不能为空");
    }
    Integer tenantId = TenantContextHolder.getTenantId();
    if (tenantId == null) {
      throw new DefaultClientException("租户上下文未初始化，无法补齐 SSO 权限");
    }

    ensureTenantDataSourceReady(tenantId);

    SysUser targetUser = sysUserService.findById(userId);
    if (targetUser == null) {
      throw new DefaultClientException("目标星陨用户不存在");
    }

    List<String> roleIds = loadBoundRoleIds(userId);
    if (CollectionUtil.isEmpty(roleIds)) {
      roleIds = resolveRoleIds(preferredRoleIds);
    } else if (!CollectionUtil.isEmpty(preferredRoleIds)) {
      roleIds = mergeDistinctIds(roleIds, preferredRoleIds);
    }
    if (CollectionUtil.isEmpty(roleIds)) {
      throw new DefaultClientException("当前租户下未找到可用角色，无法补齐 SSO 权限");
    }

    List<String> deptIds = loadBoundDeptIds(userId);
    if (CollectionUtil.isEmpty(deptIds)) {
      deptIds = resolveDeptIds(preferredDeptIds);
    }
    if (CollectionUtil.isEmpty(deptIds)) {
      throw new DefaultClientException("当前租户下未找到可用部门，无法补齐 SSO 权限");
    }

    Set<Integer> availableModuleIds = loadAvailableModuleIds(tenantId);
    jianyouPlatformTenantBootstrapService.ensureRoleMenus(roleIds, availableModuleIds);
    ensureUserDeptBindings(userId, deptIds);
    ensureUserRoleBindings(userId, roleIds);
    Set<String> jdbcPermissions = loadUserPermissionCodes(userId);
    log.info("建友 SSO 用户权限自动补齐完成: tenantId={}, userId={}, username={}, roleIds={}, jdbcPermissionCount={}",
        tenantId, userId, targetUser.getUsername(), roleIds, jdbcPermissions.size());
  }

  @Override
  public Set<String> loadUserPermissionCodes(String userId) {
    if (StringUtils.isBlank(userId)) {
      return new LinkedHashSet<>();
    }
    return new LinkedHashSet<>(loadUserPermissionCodes(currentTenantJdbcTemplate(), userId));
  }

  @Override
  public JianyouMerchantRepairBo repairExisting(JianyouMerchantRepairVo vo) {
    SysOpenDomain openDomain = getOpenDomain(vo.getOpenDomainClientId());
    if (openDomain == null) {
      throw new DefaultClientException("开放域客户端不存在");
    }
    if (!Boolean.TRUE.equals(openDomain.getAvailable())) {
      throw new DefaultClientException("开放域客户端未启用");
    }
    if (openDomain.getTenantId() == null || !openDomain.getTenantId().equals(vo.getTenantId())) {
      throw new DefaultClientException("开放域客户端与租户不匹配");
    }

    Set<Integer> availableModuleIds = loadAvailableModuleIds(vo.getTenantId());

    boolean tenantSwitched = false;
    try {
      ApplicationUtil.publishEvent(new SetTenantEvent(this, vo.getTenantId()));
      TenantContextHolder.setTenantId(vo.getTenantId());
      tenantSwitched = true;

      List<String> deptIds = resolveDeptIds(vo.getDeptIds());
      List<String> roleIds = resolveRoleIds(vo.getRoleIds());
      if (CollectionUtil.isEmpty(deptIds)) {
        throw new DefaultClientException("当前租户下未找到可用部门，无法完成用户修复");
      }
      if (CollectionUtil.isEmpty(roleIds)) {
        throw new DefaultClientException("当前租户下未找到可用角色，无法完成用户修复");
      }

      jianyouPlatformTenantBootstrapService.ensureRoleMenus(roleIds, availableModuleIds);

      SysUser targetUser = sysUserService.findById(vo.getTargetUserId());
      if (targetUser == null) {
        createRepairUser(vo);
      } else {
        updateRepairUser(vo);
      }

      ensureUserDeptBindings(vo.getTargetUserId(), deptIds);
      ensureUserRoleBindings(vo.getTargetUserId(), roleIds);

      SysUser repairedUser = sysUserService.findById(vo.getTargetUserId());
      if (repairedUser == null) {
        throw new DefaultClientException("修复星陨用户失败");
      }

      JianyouMerchantRepairBo bo = new JianyouMerchantRepairBo();
      bo.setTargetTenantId(vo.getTenantId());
      bo.setTargetXingyunUserId(repairedUser.getId());
      bo.setTargetXingyunUsername(repairedUser.getUsername());
      bo.setDeptIds(deptIds);
      bo.setRoleIds(roleIds);
      return bo;
    } finally {
      if (tenantSwitched) {
        TenantContextHolder.clearTenantId();
        ApplicationUtil.publishEvent(new ClearTenantEvent(this));
      }
    }
  }

  @Override
  public JianyouMerchantUserSyncBo updateUser(JianyouMerchantUserUpdateVo vo) {
    validateOpenDomain(vo.getOpenDomainClientId(), vo.getTenantId());

    boolean tenantSwitched = false;
    try {
      ApplicationUtil.publishEvent(new SetTenantEvent(this, vo.getTenantId()));
      TenantContextHolder.setTenantId(vo.getTenantId());
      tenantSwitched = true;

      SysUser targetUser = sysUserService.findById(vo.getTargetUserId());
      if (targetUser == null) {
        throw new DefaultClientException("目标星陨用户不存在");
      }

      JdbcTemplate jdbcTemplate = currentTenantJdbcTemplate();
      String now = LocalDateTime.now().format(DATETIME_FORMATTER);
      String name = StringUtils.defaultIfBlank(StringUtils.trimToNull(vo.getName()), targetUser.getName());
      String email = vo.getEmail() != null ? StringUtils.trimToNull(vo.getEmail()) : targetUser.getEmail();
      String telephone = vo.getTelephone() != null ? StringUtils.trimToNull(vo.getTelephone()) : targetUser.getTelephone();

      if (StringUtils.isNotBlank(vo.getRawPassword())) {
        String encodedPassword = PASSWORD_ENCODER.encode(vo.getRawPassword());
        jdbcTemplate.update(
            "UPDATE sys_user SET name = ?, email = ?, telephone = ?, password = ?, available = 1, lock_status = 0, update_by = ?, update_by_id = ?, update_time = ? WHERE id = ?",
            name, email, telephone, encodedPassword, SYSTEM_USER_NAME, SYSTEM_USER_ID, now, vo.getTargetUserId());
      } else {
        jdbcTemplate.update(
            "UPDATE sys_user SET name = ?, email = ?, telephone = ?, update_by = ?, update_by_id = ?, update_time = ? WHERE id = ?",
            name, email, telephone, SYSTEM_USER_NAME, SYSTEM_USER_ID, now, vo.getTargetUserId());
      }

      log.info("建友商户星陨用户更新完成: tenantId={}, userId={}, username={}",
          vo.getTenantId(), vo.getTargetUserId(), targetUser.getUsername());

      JianyouMerchantUserSyncBo bo = new JianyouMerchantUserSyncBo();
      bo.setTargetTenantId(vo.getTenantId());
      bo.setTargetXingyunUserId(vo.getTargetUserId());
      bo.setTargetXingyunUsername(targetUser.getUsername());
      return bo;
    } finally {
      if (tenantSwitched) {
        TenantContextHolder.clearTenantId();
        ApplicationUtil.publishEvent(new ClearTenantEvent(this));
      }
    }
  }

  @Override
  public JianyouMerchantUserSyncBo disableUser(JianyouMerchantUserDisableVo vo) {
    validateOpenDomain(vo.getOpenDomainClientId(), vo.getTenantId());

    boolean tenantSwitched = false;
    try {
      ApplicationUtil.publishEvent(new SetTenantEvent(this, vo.getTenantId()));
      TenantContextHolder.setTenantId(vo.getTenantId());
      tenantSwitched = true;

      SysUser targetUser = sysUserService.findById(vo.getTargetUserId());
      if (targetUser == null) {
        throw new DefaultClientException("目标星陨用户不存在");
      }

      JdbcTemplate jdbcTemplate = currentTenantJdbcTemplate();
      String now = LocalDateTime.now().format(DATETIME_FORMATTER);
      jdbcTemplate.update(
          "UPDATE sys_user SET available = 0, lock_status = 1, update_by = ?, update_by_id = ?, update_time = ? WHERE id = ?",
          SYSTEM_USER_NAME, SYSTEM_USER_ID, now, vo.getTargetUserId());

      log.info("建友商户星陨用户已禁用: tenantId={}, userId={}, username={}",
          vo.getTenantId(), vo.getTargetUserId(), targetUser.getUsername());

      JianyouMerchantUserSyncBo bo = new JianyouMerchantUserSyncBo();
      bo.setTargetTenantId(vo.getTenantId());
      bo.setTargetXingyunUserId(vo.getTargetUserId());
      bo.setTargetXingyunUsername(targetUser.getUsername());
      return bo;
    } finally {
      if (tenantSwitched) {
        TenantContextHolder.clearTenantId();
        ApplicationUtil.publishEvent(new ClearTenantEvent(this));
      }
    }
  }

  private void validateOpenDomain(String openDomainClientId, Integer tenantId) {
    SysOpenDomain openDomain = getOpenDomain(openDomainClientId);
    if (openDomain == null) {
      throw new DefaultClientException("开放域客户端不存在");
    }
    if (!Boolean.TRUE.equals(openDomain.getAvailable())) {
      throw new DefaultClientException("开放域客户端未启用");
    }
    if (openDomain.getTenantId() == null || !openDomain.getTenantId().equals(tenantId)) {
      throw new DefaultClientException("开放域客户端与租户不匹配");
    }
  }

  private SysOpenDomain getOpenDomain(String openDomainClientId) {
    if (!StringUtil.isNotBlank(openDomainClientId) || !StringUtils.isNumeric(openDomainClientId)) {
      return null;
    }
    return sysOpenDomainService.findById(Integer.valueOf(openDomainClientId));
  }

  private String buildDescription(JianyouMerchantProvisionVo vo) {
    return "建友商户初始化，商户名称：" + vo.getOrgName() + "，商户域名：" + vo.getDomain();
  }

  private String buildRepairDescription(JianyouMerchantRepairVo vo) {
    String orgName = StringUtils.defaultIfBlank(StringUtils.trimToNull(vo.getOrgName()), "建友商户");
    String domain = StringUtils.defaultIfBlank(StringUtils.trimToNull(vo.getDomain()), "-");
    return "建友商户修复，商户名称：" + orgName + "，商户域名：" + domain;
  }

  private void createRepairUser(JianyouMerchantRepairVo vo) {
    QuerySysUserVo queryVo = new QuerySysUserVo();
    queryVo.setUsername(vo.getUsername());
    List<SysUser> existsUsers = sysUserService.query(queryVo);
    if (!CollectionUtil.isEmpty(existsUsers)) {
      throw new DefaultClientException("当前租户下用户名已存在，但目标用户ID不存在，请先核对目标用户");
    }

    JdbcTemplate jdbcTemplate = currentTenantJdbcTemplate();
    String now = LocalDateTime.now().format(DATETIME_FORMATTER);
    jdbcTemplate.update("INSERT INTO sys_user (id, code, name, username, password, email, telephone, gender, available, lock_status, description, create_by, create_by_id, create_time, update_by, update_by_id, update_time) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, 0, 1, 0, ?, ?, ?, ?, ?, ?, ?)",
        vo.getTargetUserId(),
        vo.getUsername(),
        vo.getName(),
        vo.getUsername(),
        DEFAULT_REPAIR_PASSWORD_HASH,
        StringUtils.trimToNull(vo.getEmail()),
        StringUtils.trimToNull(vo.getTelephone()),
        buildRepairDescription(vo),
        SYSTEM_USER_NAME,
        SYSTEM_USER_ID,
        now,
        SYSTEM_USER_NAME,
        SYSTEM_USER_ID,
        now);
  }

  private void updateRepairUser(JianyouMerchantRepairVo vo) {
    JdbcTemplate jdbcTemplate = currentTenantJdbcTemplate();
    jdbcTemplate.update("UPDATE sys_user SET code = ?, name = ?, username = ?, password = ?, email = ?, telephone = ?, available = 1, lock_status = 0, description = ?, update_by = ?, update_by_id = ?, update_time = ? WHERE id = ?",
        vo.getUsername(),
        vo.getName(),
        vo.getUsername(),
        DEFAULT_REPAIR_PASSWORD_HASH,
        StringUtils.trimToNull(vo.getEmail()),
        StringUtils.trimToNull(vo.getTelephone()),
        buildRepairDescription(vo),
        SYSTEM_USER_NAME,
        SYSTEM_USER_ID,
        LocalDateTime.now().format(DATETIME_FORMATTER),
        vo.getTargetUserId());
  }

  private void ensureUserRoleBindings(String userId, List<String> roleIds) {
    if (StringUtils.isBlank(userId) || CollectionUtil.isEmpty(roleIds)) {
      throw new DefaultClientException("星陨用户角色绑定参数不完整");
    }

    JdbcTemplate jdbcTemplate = currentTenantJdbcTemplate();
    Set<String> existingRoleIds = jdbcTemplate.query(
        "SELECT role_id FROM sys_user_role WHERE user_id = ?",
        rs -> {
          Set<String> values = new LinkedHashSet<>();
          while (rs.next()) {
            values.add(rs.getString("role_id"));
          }
          return values;
        }, userId);

    List<String> missingRoleIds = new ArrayList<>();
    for (String roleId : roleIds) {
      if (StringUtils.isNotBlank(roleId) && !existingRoleIds.contains(roleId)) {
        missingRoleIds.add(roleId);
      }
    }

    if (!missingRoleIds.isEmpty()) {
      List<Object[]> params = new ArrayList<>();
      for (String roleId : missingRoleIds) {
        params.add(new Object[]{IdUtil.getId(), userId, roleId});
      }
      jdbcTemplate.batchUpdate("INSERT INTO sys_user_role (id, user_id, role_id) VALUES (?, ?, ?)", params);
      log.warn("检测到星陨用户缺少角色绑定，已自动补齐: userId={}, missingRoleIds={}", userId, missingRoleIds);
    }

    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM sys_user_role WHERE user_id = ?",
        Integer.class, userId);
    if (count == null || count <= 0) {
      throw new DefaultClientException("星陨用户角色绑定失败");
    }
  }

  private void ensureUserDeptBindings(String userId, List<String> deptIds) {
    if (StringUtils.isBlank(userId) || CollectionUtil.isEmpty(deptIds)) {
      throw new DefaultClientException("星陨用户部门绑定参数不完整");
    }

    JdbcTemplate jdbcTemplate = currentTenantJdbcTemplate();
    Set<String> existingDeptIds = jdbcTemplate.query(
        "SELECT dept_id FROM sys_user_dept WHERE user_id = ?",
        rs -> {
          Set<String> values = new LinkedHashSet<>();
          while (rs.next()) {
            values.add(rs.getString("dept_id"));
          }
          return values;
        }, userId);

    List<String> missingDeptIds = new ArrayList<>();
    for (String deptId : deptIds) {
      if (StringUtils.isNotBlank(deptId) && !existingDeptIds.contains(deptId)) {
        missingDeptIds.add(deptId);
      }
    }

    if (!missingDeptIds.isEmpty()) {
      List<Object[]> params = new ArrayList<>();
      for (String deptId : missingDeptIds) {
        params.add(new Object[]{IdUtil.getId(), userId, deptId});
      }
      jdbcTemplate.batchUpdate("INSERT INTO sys_user_dept (id, user_id, dept_id) VALUES (?, ?, ?)", params);
    }
  }

  private List<String> resolveDeptIds(List<String> deptIds) {
    if (!CollectionUtil.isEmpty(deptIds)) {
      return deptIds;
    }

    return currentTenantJdbcTemplate().query(
        "SELECT id FROM sys_dept WHERE available = 1 ORDER BY code ASC, id ASC LIMIT 1",
        rs -> {
          List<String> results = new ArrayList<>();
          while (rs.next()) {
            results.add(rs.getString("id"));
          }
          return results;
        });
  }

  private List<String> resolveRoleIds(List<String> roleIds) {
    if (!CollectionUtil.isEmpty(roleIds)) {
      return roleIds;
    }

    return currentTenantJdbcTemplate().query(
        "SELECT id FROM sys_role WHERE available = 1 ORDER BY code ASC, id ASC LIMIT 1",
        rs -> {
          List<String> results = new ArrayList<>();
          while (rs.next()) {
            results.add(rs.getString("id"));
          }
          return results;
        });
  }

  private Set<Integer> loadAvailableModuleIds(Integer tenantId) {
    List<SysModuleTenant> mappings = sysModuleTenantService.getByTenantId(tenantId);
    if (CollectionUtil.isEmpty(mappings)) {
      return new LinkedHashSet<>();
    }
    return mappings.stream()
        .filter(item -> item != null && item.getModuleId() != null)
        .map(SysModuleTenant::getModuleId)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private JdbcTemplate currentTenantJdbcTemplate() {
    DataSource dataSource = ApplicationUtil.safeGetBean(DataSource.class);
    if (dataSource == null) {
      throw new DefaultClientException("平台商租户数据源未初始化完成，请稍后重试");
    }
    return new JdbcTemplate(dataSource);
  }

  private void ensureTenantDataSourceReady(Integer tenantId) {
    Tenant tenant = tenantService.findById(tenantId);
    if (tenant == null || !Boolean.TRUE.equals(tenant.getAvailable())) {
      throw new DefaultClientException("星云租户未配置或不可用");
    }
    if (StringUtils.isAnyBlank(tenant.getJdbcUrl(), tenant.getJdbcUsername(), tenant.getJdbcPassword())) {
      throw new DefaultClientException("星云租户数据源配置不完整");
    }

    String tenantDataSourceKey = String.valueOf(tenantId);
    if (dynamicRoutingDataSource != null
        && dynamicRoutingDataSource.getDataSources().containsKey(tenantDataSourceKey)) {
      return;
    }

    ApplicationUtil.publishEvent(new ReloadTenantEvent(this, tenantId, tenant.getJdbcUrl(),
        tenant.getJdbcUsername(), EncryptUtil.decrypt(tenant.getJdbcPassword())));
  }

  private void assertUserHasPermissions(String userId, List<String> roleIds,
      Set<Integer> availableModuleIds, String username) {
    JdbcTemplate jdbcTemplate = currentTenantJdbcTemplate();
    List<String> permissions = loadUserPermissionCodes(jdbcTemplate, userId);
    if (!CollectionUtil.isEmpty(permissions)) {
      log.info("建友用户权限校验通过: userId={}, jdbcPermissionCount={}", userId, permissions.size());
      warnIfUserDetailsServiceEmpty(userId, username, permissions.size());
      return;
    }

    if (CollectionUtil.isEmpty(availableModuleIds)) {
      throw new DefaultClientException("用户权限补齐后仍为空：租户未分配可用模块，tenantModuleCount=0");
    }

    Integer userRoleCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM sys_user_role WHERE user_id = ?",
        Integer.class, userId);
    if (userRoleCount == null || userRoleCount <= 0) {
      throw new DefaultClientException("用户权限补齐后仍为空：用户未绑定角色，userId=" + userId
          + "，roleIds=" + roleIds + "，availableModuleCount=" + availableModuleIds.size());
    }

    Integer grantedMenuCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(DISTINCT rm.menu_id) FROM sys_user_role ur "
            + "INNER JOIN sys_role_menu rm ON ur.role_id = rm.role_id "
            + "WHERE ur.user_id = ?",
        Integer.class, userId);
    if (grantedMenuCount != null && grantedMenuCount > 0) {
      throw new DefaultClientException("用户权限补齐后仍为空：角色已绑定菜单但 permission 字段为空，userId="
          + userId + "，grantedMenuCount=" + grantedMenuCount + "，roleIds=" + roleIds);
    }

    List<String> missingCorePermissions = loadMissingCorePermissionsForRoles(jdbcTemplate, roleIds);
    if (!missingCorePermissions.isEmpty()) {
      throw new DefaultClientException("用户权限补齐后仍为空：默认角色缺少核心系统权限 "
          + String.join("、", missingCorePermissions) + "，userId=" + userId + "，roleIds=" + roleIds);
    }

    throw new DefaultClientException("用户权限补齐后仍为空：请检查租户模块与默认角色，userId=" + userId
        + "，roleIds=" + roleIds + "，userRoleCount=" + userRoleCount
        + "，availableModuleCount=" + availableModuleIds.size());
  }

  private List<String> loadUserPermissionCodes(JdbcTemplate jdbcTemplate, String userId) {
    return jdbcTemplate.query(
        "SELECT DISTINCT m.permission FROM sys_user_role ur "
            + "INNER JOIN sys_role_menu rm ON ur.role_id = rm.role_id "
            + "INNER JOIN sys_menu m ON rm.menu_id = m.id "
            + "WHERE ur.user_id = ? AND m.available = 1 "
            + "AND m.permission IS NOT NULL AND m.permission <> ''",
        rs -> {
          List<String> results = new ArrayList<>();
          while (rs.next()) {
            String permission = StringUtils.trimToNull(rs.getString("permission"));
            if (permission != null) {
              results.add(permission);
            }
          }
          return results;
        }, userId);
  }

  private List<String> loadMissingCorePermissionsForRoles(JdbcTemplate jdbcTemplate, List<String> roleIds) {
    if (CollectionUtil.isEmpty(roleIds)) {
      return new ArrayList<>(CORE_SYSTEM_PERMISSIONS);
    }

    Set<String> grantedPermissions = new LinkedHashSet<>();
    for (String roleId : roleIds) {
      if (StringUtils.isBlank(roleId)) {
        continue;
      }
      List<Object> params = new ArrayList<>();
      params.add(roleId);
      params.addAll(CORE_SYSTEM_PERMISSIONS);
      String placeholders = CORE_SYSTEM_PERMISSIONS.stream().map(item -> "?").collect(Collectors.joining(", "));
      jdbcTemplate.query(
          "SELECT DISTINCT m.permission FROM sys_role_menu rm "
              + "INNER JOIN sys_menu m ON rm.menu_id = m.id "
              + "WHERE rm.role_id = ? AND m.available = 1 AND m.permission IN (" + placeholders + ")",
          rs -> {
            while (rs.next()) {
              grantedPermissions.add(StringUtils.trimToEmpty(rs.getString("permission")));
            }
            return null;
          }, params.toArray());
    }

    return CORE_SYSTEM_PERMISSIONS.stream()
        .filter(permission -> !grantedPermissions.contains(permission))
        .collect(Collectors.toList());
  }

  private void warnIfUserDetailsServiceEmpty(String userId, String username, int jdbcPermissionCount) {
    if (StringUtils.isBlank(username)) {
      return;
    }
    try {
      AbstractUserDetails userDetails = userDetailsService.loadUserByUsername(username);
      int udsPermissionCount = userDetails == null || userDetails.getPermissions() == null
          ? 0
          : userDetails.getPermissions().size();
      if (udsPermissionCount == 0) {
        log.warn("建友用户权限 JDBC 已通过但 UserDetailsService 仍为空: userId={}, username={}, jdbcPermissionCount={}",
            userId, username, jdbcPermissionCount);
      }
    } catch (Exception e) {
      log.warn("建友用户权限 UserDetailsService 对比跳过: userId={}, username={}, error={}",
          userId, username, e.getMessage());
    }
  }

  private List<String> loadBoundRoleIds(String userId) {
    return currentTenantJdbcTemplate().query(
        "SELECT role_id FROM sys_user_role WHERE user_id = ? ORDER BY role_id ASC",
        rs -> {
          List<String> results = new ArrayList<>();
          while (rs.next()) {
            results.add(rs.getString("role_id"));
          }
          return results;
        }, userId);
  }

  private List<String> loadBoundDeptIds(String userId) {
    return currentTenantJdbcTemplate().query(
        "SELECT dept_id FROM sys_user_dept WHERE user_id = ? ORDER BY dept_id ASC",
        rs -> {
          List<String> results = new ArrayList<>();
          while (rs.next()) {
            results.add(rs.getString("dept_id"));
          }
          return results;
        }, userId);
  }

  private List<String> mergeDistinctIds(List<String> existing, List<String> preferred) {
    LinkedHashSet<String> merged = new LinkedHashSet<>();
    if (!CollectionUtil.isEmpty(existing)) {
      merged.addAll(existing);
    }
    if (!CollectionUtil.isEmpty(preferred)) {
      for (String id : preferred) {
        if (StringUtils.isNotBlank(id)) {
          merged.add(id);
        }
      }
    }
    return new ArrayList<>(merged);
  }
}
