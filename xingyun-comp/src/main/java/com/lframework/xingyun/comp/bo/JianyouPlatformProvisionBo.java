package com.lframework.xingyun.comp.bo;

import com.lframework.starter.web.core.bo.BaseBo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class JianyouPlatformProvisionBo extends BaseBo {

  @ApiModelProperty("目标租户ID")
  private Integer targetTenantId;

  @ApiModelProperty("开放域客户端ID")
  private String openDomainClientId;

  @ApiModelProperty("默认部门ID列表")
  private List<String> defaultDeptIds;

  @ApiModelProperty("默认角色ID列表")
  private List<String> defaultRoleIds;
}
