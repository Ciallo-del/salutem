package com.lframework.xingyun.comp.controller;

import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.web.core.components.resp.InvokeResult;
import com.lframework.starter.web.core.components.resp.InvokeResultBuilder;
import com.lframework.starter.web.core.annotations.openapi.OpenApi;
import com.lframework.starter.web.core.controller.DefaultBaseController;
import com.lframework.xingyun.comp.bo.JianyouInventoryMenuTemplateBo;
import com.lframework.xingyun.comp.bo.JianyouMerchantProvisionBo;
import com.lframework.xingyun.comp.bo.JianyouMerchantRepairBo;
import com.lframework.xingyun.comp.bo.JianyouMerchantUserSyncBo;
import com.lframework.xingyun.comp.bo.JianyouPlatformProvisionBo;
import com.lframework.xingyun.comp.bo.JianyouPlatformRepairBo;
import com.lframework.xingyun.comp.bo.JianyouUserMenuPermissionsBo;
import com.lframework.xingyun.comp.service.JianyouInventoryMenuPermissionService;
import com.lframework.xingyun.comp.service.JianyouMerchantProvisionService;
import com.lframework.xingyun.comp.service.JianyouPlatformProvisionService;
import com.lframework.xingyun.comp.vo.JianyouInventoryMenuTemplateVo;
import com.lframework.xingyun.comp.vo.JianyouMerchantProvisionVo;
import com.lframework.xingyun.comp.vo.JianyouMerchantRepairVo;
import com.lframework.xingyun.comp.vo.JianyouMerchantUserDisableVo;
import com.lframework.xingyun.comp.vo.JianyouMerchantUserUpdateVo;
import com.lframework.xingyun.comp.vo.JianyouPlatformProvisionVo;
import com.lframework.xingyun.comp.vo.JianyouPlatformRepairVo;
import com.lframework.xingyun.comp.vo.JianyouUserMenuPermissionsVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Api(tags = "建友星陨初始化接口")
@OpenApi
@Validated
@RestController
@RequestMapping("/xy/xingyun/jianyou")
public class JianyouMerchantProvisionController extends DefaultBaseController {

  private static final String SECRET_HEADER = "X-Jianyou-Api-Secret";

  @Value("${xingyun.sso.jianyou.provision-secret:}")
  private String provisionSecret;

  @Autowired
  private JianyouMerchantProvisionService jianyouMerchantProvisionService;

  @Autowired
  private JianyouPlatformProvisionService jianyouPlatformProvisionService;

  @Autowired
  private JianyouInventoryMenuPermissionService jianyouInventoryMenuPermissionService;

  @ApiOperation("建友平台商资源初始化")
  @PostMapping("/platform/provision")
  public InvokeResult<JianyouPlatformProvisionBo> provisionPlatform(
      @RequestHeader(value = SECRET_HEADER, required = false) String apiSecret,
      @Valid @RequestBody JianyouPlatformProvisionVo vo) {

    validateSecret(apiSecret);
    return InvokeResultBuilder.success(jianyouPlatformProvisionService.provision(vo));
  }

  @ApiOperation("修复已存在的建友平台商资源")
  @PostMapping("/platform/repair")
  public InvokeResult<JianyouPlatformRepairBo> repairPlatform(
      @RequestHeader(value = SECRET_HEADER, required = false) String apiSecret,
      @Valid @RequestBody JianyouPlatformRepairVo vo) {

    validateSecret(apiSecret);
    return InvokeResultBuilder.success(jianyouPlatformProvisionService.repairExisting(vo));
  }

  @ApiOperation("建友商户初始化")
  @PostMapping("/merchant/provision")
  public InvokeResult<JianyouMerchantProvisionBo> provisionMerchant(
      @RequestHeader(value = SECRET_HEADER, required = false) String apiSecret,
      @Valid @RequestBody JianyouMerchantProvisionVo vo) {

    validateSecret(apiSecret);
    return InvokeResultBuilder.success(jianyouMerchantProvisionService.provision(vo));
  }

  @ApiOperation("修复已存在的建友商户用户")
  @PostMapping("/merchant/repair")
  public InvokeResult<JianyouMerchantRepairBo> repairMerchant(
      @RequestHeader(value = SECRET_HEADER, required = false) String apiSecret,
      @Valid @RequestBody JianyouMerchantRepairVo vo) {

    validateSecret(apiSecret);
    return InvokeResultBuilder.success(jianyouMerchantProvisionService.repairExisting(vo));
  }

  @ApiOperation("补齐建友商户用户菜单权限（不修改密码）")
  @PostMapping("/merchant/ensure-permissions")
  public InvokeResult<JianyouMerchantRepairBo> ensureMerchantPermissions(
      @RequestHeader(value = SECRET_HEADER, required = false) String apiSecret,
      @Valid @RequestBody JianyouMerchantRepairVo vo) {

    validateSecret(apiSecret);
    return InvokeResultBuilder.success(jianyouMerchantProvisionService.ensureUserPermissions(vo));
  }

  @ApiOperation("更新建友商户子账号星陨用户")
  @PostMapping("/merchant/user/update")
  public InvokeResult<JianyouMerchantUserSyncBo> updateMerchantUser(
      @RequestHeader(value = SECRET_HEADER, required = false) String apiSecret,
      @Valid @RequestBody JianyouMerchantUserUpdateVo vo) {

    validateSecret(apiSecret);
    return InvokeResultBuilder.success(jianyouMerchantProvisionService.updateUser(vo));
  }

  @ApiOperation("禁用建友商户子账号星陨用户")
  @PostMapping("/merchant/user/disable")
  public InvokeResult<JianyouMerchantUserSyncBo> disableMerchantUser(
      @RequestHeader(value = SECRET_HEADER, required = false) String apiSecret,
      @Valid @RequestBody JianyouMerchantUserDisableVo vo) {

    validateSecret(apiSecret);
    return InvokeResultBuilder.success(jianyouMerchantProvisionService.disableUser(vo));
  }

  @ApiOperation("获取主商户可见的 Inventory 菜单模板")
  @PostMapping("/merchant/inventory-menu-template")
  public InvokeResult<JianyouInventoryMenuTemplateBo> inventoryMenuTemplate(
      @RequestHeader(value = SECRET_HEADER, required = false) String apiSecret,
      @Valid @RequestBody JianyouInventoryMenuTemplateVo vo) {

    validateSecret(apiSecret);
    return InvokeResultBuilder.success(jianyouInventoryMenuPermissionService.loadMenuTemplate(vo));
  }

  @ApiOperation("按 menuKey 更新子账号 Inventory 菜单权限")
  @PostMapping("/merchant/user-menu-permissions")
  public InvokeResult<JianyouUserMenuPermissionsBo> updateUserMenuPermissions(
      @RequestHeader(value = SECRET_HEADER, required = false) String apiSecret,
      @Valid @RequestBody JianyouUserMenuPermissionsVo vo) {

    validateSecret(apiSecret);
    return InvokeResultBuilder.success(jianyouInventoryMenuPermissionService.updateUserMenuPermissions(vo));
  }

  private void validateSecret(String apiSecret) {
    if (StringUtils.isBlank(provisionSecret) || !StringUtils.equals(provisionSecret, apiSecret)) {
      throw new DefaultClientException("接口认证失败");
    }
  }
}
