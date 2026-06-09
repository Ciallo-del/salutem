package com.lframework.xingyun.comp.vo;

import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
public class JianyouMerchantRepairVo implements BaseVo, Serializable {

  private static final long serialVersionUID = 1L;

  @ApiModelProperty(value = "租户ID", required = true)
  @NotNull(message = "租户ID不能为空")
  private Integer tenantId;

  @ApiModelProperty(value = "开放域客户端ID", required = true)
  @NotBlank(message = "开放域客户端ID不能为空")
  private String openDomainClientId;

  @ApiModelProperty(value = "目标星陨用户ID", required = true)
  @NotBlank(message = "目标星陨用户ID不能为空")
  private String targetUserId;

  @ApiModelProperty(value = "用户名", required = true)
  @NotBlank(message = "用户名不能为空")
  private String username;

  @ApiModelProperty(value = "姓名", required = true)
  @NotBlank(message = "姓名不能为空")
  private String name;

  @ApiModelProperty("邮箱")
  private String email;

  @ApiModelProperty("手机号")
  private String telephone;

  @ApiModelProperty("部门ID列表")
  private List<String> deptIds;

  @ApiModelProperty("角色ID列表")
  private List<String> roleIds;

  @ApiModelProperty(value = "商户域名", required = true)
  @NotBlank(message = "商户域名不能为空")
  private String domain;

  @ApiModelProperty(value = "商户名称", required = true)
  @NotBlank(message = "商户名称不能为空")
  private String orgName;
}
