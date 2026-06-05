package com.lframework.xingyun.comp.bo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@ApiModel("建友 SSO 当前用户信息")
public class SsoJianyouCurrentUserBo implements Serializable {

  private static final long serialVersionUID = 1L;

  @ApiModelProperty("用户ID")
  private String userId;

  @ApiModelProperty("用户名")
  private String username;

  @ApiModelProperty("姓名")
  private String name;

  @ApiModelProperty("头像")
  private String avatar;

  @ApiModelProperty("首页路径")
  private String homePath;

  @ApiModelProperty("权限码集合")
  private List<String> permissions;
}
