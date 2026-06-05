package com.lframework.xingyun.comp.bo;

import com.lframework.starter.web.core.bo.BaseBo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class JianyouMerchantProvisionBo extends BaseBo {

  @ApiModelProperty("目标租户ID")
  private Integer targetTenantId;

  @ApiModelProperty("目标星云用户ID")
  private String targetXingyunUserId;

  @ApiModelProperty("目标星云用户名")
  private String targetXingyunUsername;
}
