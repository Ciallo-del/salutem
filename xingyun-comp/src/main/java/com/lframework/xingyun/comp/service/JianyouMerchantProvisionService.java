package com.lframework.xingyun.comp.service;

import com.lframework.xingyun.comp.bo.JianyouMerchantProvisionBo;
import com.lframework.xingyun.comp.bo.JianyouMerchantRepairBo;
import com.lframework.xingyun.comp.bo.JianyouMerchantUserSyncBo;
import com.lframework.xingyun.comp.vo.JianyouMerchantProvisionVo;
import com.lframework.xingyun.comp.vo.JianyouMerchantRepairVo;
import com.lframework.xingyun.comp.vo.JianyouMerchantUserDisableVo;
import com.lframework.xingyun.comp.vo.JianyouMerchantUserUpdateVo;

import java.util.List;
import java.util.Set;

public interface JianyouMerchantProvisionService {

  JianyouMerchantProvisionBo provision(JianyouMerchantProvisionVo vo);

  JianyouMerchantRepairBo repairExisting(JianyouMerchantRepairVo vo);

  /**
   * 补齐默认角色的菜单授权与用户角色/部门绑定，不修改用户密码与基本信息。
   */
  JianyouMerchantRepairBo ensureUserPermissions(JianyouMerchantRepairVo vo);

  /**
   * SSO 登录前补齐用户角色菜单与绑定（调用方需已切换租户上下文）。
   */
  void backfillSsoUserPermissions(String userId, List<String> preferredRoleIds, List<String> preferredDeptIds);

  /**
   * 从租户库 JDBC 聚合用户 permission 码（调用方需已切换租户上下文）。
   */
  Set<String> loadUserPermissionCodes(String userId);

  JianyouMerchantUserSyncBo updateUser(JianyouMerchantUserUpdateVo vo);

  JianyouMerchantUserSyncBo disableUser(JianyouMerchantUserDisableVo vo);
}
