package com.lframework.xingyun.comp.vo;

import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class JianyouPlatformProvisionVo implements BaseVo, Serializable {

  private static final long serialVersionUID = 1L;

  @ApiModelProperty(value = "平台商标识", required = true)
  @NotBlank(message = "平台商标识不能为空")
  private String platformOrgCard;

  @ApiModelProperty(value = "平台商名称", required = true)
  @NotBlank(message = "平台商名称不能为空")
  private String platformOrgName;
}
