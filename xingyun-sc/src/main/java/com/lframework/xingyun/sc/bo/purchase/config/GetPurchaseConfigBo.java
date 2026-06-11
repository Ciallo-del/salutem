package com.lframework.xingyun.sc.bo.purchase.config;

import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.xingyun.sc.entity.PurchaseConfig;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class GetPurchaseConfigBo extends BaseBo<PurchaseConfig> {

  @ApiModelProperty("采购收货单是否关联采购订单")
  private Boolean receiveRequirePurchase;

  @ApiModelProperty("采购退货单是否关联采购收货单")
  private Boolean purchaseReturnRequireReceive;

  @ApiModelProperty("采购退货单是否多次关联采购收货单")
  private Boolean purchaseReturnMultipleRelateReceive;

  @ApiModelProperty("采购订单是否开启审批流程")
  private Boolean purchaseRequireBpm;

  @ApiModelProperty("采购订单关联的审批流程ID")
  private String purchaseBpmProcessId;

  @ApiModelProperty("采购订单关联的审批流程编号")
  private String purchaseBpmProcessCode;

  public GetPurchaseConfigBo() {

  }

  public GetPurchaseConfigBo(PurchaseConfig dto) {
    super(dto);
  }
}
