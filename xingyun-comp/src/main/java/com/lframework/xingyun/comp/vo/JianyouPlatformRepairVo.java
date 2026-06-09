package com.lframework.xingyun.comp.vo;

import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class JianyouPlatformRepairVo implements BaseVo, Serializable {

  private static final long serialVersionUID = 1L;

  @ApiModelProperty(value = "租户ID", required = true)
  @NotNull(message = "租户ID不能为空")
  private Integer tenantId;

  @ApiModelProperty(value = "开放域客户端ID", required = true)
  @NotBlank(message = "开放域客户端ID不能为空")
  private String openDomainClientId;

  @ApiModelProperty(value = "平台机构标识", required = true)
  @NotBlank(message = "平台机构标识不能为空")
  private String platformOrgCard;

  @ApiModelProperty(value = "平台机构名称", required = true)
  @NotBlank(message = "平台机构名称不能为空")
  private String platformOrgName;

  @ApiModelProperty(value = "租户名称", required = true)
  @NotBlank(message = "租户名称不能为空")
  private String tenantName;

  @ApiModelProperty(value = "租户域名", required = true)
  @NotBlank(message = "租户域名不能为空")
  private String serverName;

  @ApiModelProperty(value = "租户数据库JDBC地址", required = true)
  @NotBlank(message = "租户数据库JDBC地址不能为空")
  private String jdbcUrl;
}
