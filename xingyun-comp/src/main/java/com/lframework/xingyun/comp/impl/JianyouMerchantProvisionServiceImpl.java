package com.lframework.xingyun.comp.impl;

import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.components.tenant.TenantContextHolder;
import com.lframework.starter.web.core.event.ClearTenantEvent;
import com.lframework.starter.web.core.event.SetTenantEvent;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.inner.entity.SysOpenDomain;
import com.lframework.starter.web.inner.entity.SysUser;
import com.lframework.starter.web.inner.service.system.SysOpenDomainService;
import com.lframework.starter.web.inner.service.system.SysUserService;
import com.lframework.starter.web.inner.vo.system.user.CreateSysUserVo;
import com.lframework.starter.web.inner.vo.system.user.QuerySysUserVo;
import com.lframework.xingyun.comp.bo.JianyouMerchantProvisionBo;
import com.lframework.xingyun.comp.service.JianyouMerchantProvisionService;
import com.lframework.xingyun.comp.vo.JianyouMerchantProvisionVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class JianyouMerchantProvisionServiceImpl implements JianyouMerchantProvisionService {

  @Autowired
  private SysOpenDomainService sysOpenDomainService;

  @Autowired
  private SysUserService sysUserService;

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

      QuerySysUserVo queryVo = new QuerySysUserVo();
      queryVo.setUsername(vo.getUsername());
      List<SysUser> existsUsers = sysUserService.query(queryVo);
      if (!CollectionUtil.isEmpty(existsUsers)) {
        throw new DefaultClientException("该租户下用户名已存在");
      }

      CreateSysUserVo createVo = new CreateSysUserVo();
      createVo.setCode(vo.getUsername());
      createVo.setUsername(vo.getUsername());
      createVo.setName(vo.getName());
      createVo.setPassword(vo.getRawPassword());
      createVo.setEmail(StringUtils.trimToNull(vo.getEmail()));
      createVo.setTelephone(StringUtils.trimToNull(vo.getTelephone()));
      createVo.setDeptIds(vo.getDeptIds());
      createVo.setRoleIds(vo.getRoleIds());
      createVo.setDescription(buildDescription(vo));

      log.info("平台商星云用户初始化开始: tenantId={}, username={}, deptIds={}, roleIds={}",
          vo.getTenantId(), vo.getUsername(), vo.getDeptIds(), vo.getRoleIds());
      String userId = sysUserService.create(createVo);
      if (StringUtils.isBlank(userId)) {
        throw new DefaultClientException("创建星云用户失败");
      }

      ensureUserRoleBindings(userId, vo.getRoleIds());
      log.info("平台商星云用户初始化完成: tenantId={}, userId={}, username={}, roleIds={}",
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

  private SysOpenDomain getOpenDomain(String openDomainClientId) {
    if (!StringUtil.isNotBlank(openDomainClientId) || !StringUtils.isNumeric(openDomainClientId)) {
      return null;
    }
    return sysOpenDomainService.findById(Integer.valueOf(openDomainClientId));
  }

  private String buildDescription(JianyouMerchantProvisionVo vo) {
    return "建友商户初始化，商户名称：" + vo.getOrgName() + "，商户域名：" + vo.getDomain();
  }
  private void ensureUserRoleBindings(String userId, List<String> roleIds) {
    if (StringUtils.isBlank(userId) || CollectionUtil.isEmpty(roleIds)) {
      throw new DefaultClientException("星云用户角色绑定参数不完整");
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
        params.add(new Object[] {com.lframework.starter.web.core.utils.IdUtil.getId(), userId, roleId});
      }
      jdbcTemplate.batchUpdate("INSERT INTO sys_user_role (id, user_id, role_id) VALUES (?, ?, ?)", params);
      log.warn("检测到星云用户缺少角色绑定，已自动补齐: userId={}, missingRoleIds={}", userId, missingRoleIds);
    }

    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM sys_user_role WHERE user_id = ?",
        Integer.class, userId);
    if (count == null || count <= 0) {
      throw new DefaultClientException("星云用户角色绑定失败");
    }
  }

  private JdbcTemplate currentTenantJdbcTemplate() {
    DataSource dataSource = ApplicationUtil.safeGetBean(DataSource.class);
    if (dataSource == null) {
      throw new DefaultClientException("平台商租户数据源未初始化完成，请稍后重试");
    }
    return new JdbcTemplate(dataSource);
  }
}
