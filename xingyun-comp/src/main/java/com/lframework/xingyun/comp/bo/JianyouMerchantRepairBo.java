package com.lframework.xingyun.comp.bo;

import com.lframework.starter.web.core.bo.BaseBo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class JianyouMerchantRepairBo extends BaseBo {

  @ApiModelProperty("目标租户ID")
  private Integer targetTenantId;

  @ApiModelProperty("目标星云用户ID")
  private String targetXingyunUserId;

  @ApiModelProperty("目标星云用户名")
  private String targetXingyunUsername;

  @ApiModelProperty("部门ID列表")
  private List<String> deptIds;

  @ApiModelProperty("角色ID列表")
  private List<String> roleIds;
}
