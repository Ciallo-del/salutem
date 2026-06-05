package com.lframework.xingyun.comp.vo;

import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
public class JianyouMerchantProvisionVo implements BaseVo, Serializable {

  private static final long serialVersionUID = 1L;

  @ApiModelProperty(value = "租户ID", required = true)
  @NotNull(message = "租户ID不能为空")
  private Integer tenantId;

  @ApiModelProperty(value = "开放域客户端ID", required = true)
  @NotBlank(message = "开放域客户端ID不能为空")
  private String openDomainClientId;

  @ApiModelProperty(value = "用户名", required = true)
  @NotBlank(message = "用户名不能为空")
  private String username;

  @ApiModelProperty(value = "姓名", required = true)
  @NotBlank(message = "姓名不能为空")
  private String name;

  @ApiModelProperty(value = "原始密码", required = true)
  @NotBlank(message = "原始密码不能为空")
  private String rawPassword;

  @ApiModelProperty("邮箱")
  private String email;

  @ApiModelProperty("手机号")
  private String telephone;

  @ApiModelProperty(value = "部门ID列表", required = true)
  @NotEmpty(message = "部门ID列表不能为空")
  private List<String> deptIds;

  @ApiModelProperty(value = "角色ID列表", required = true)
  @NotEmpty(message = "角色ID列表不能为空")
  private List<String> roleIds;

  @ApiModelProperty(value = "商户域名", required = true)
  @NotBlank(message = "商户域名不能为空")
  private String domain;

  @ApiModelProperty(value = "商户名称", required = true)
  @NotBlank(message = "商户名称不能为空")
  private String orgName;
}
