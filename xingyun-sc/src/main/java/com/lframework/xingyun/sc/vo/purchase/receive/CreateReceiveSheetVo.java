package com.lframework.xingyun.sc.vo.purchase.receive;

import com.lframework.starter.common.exceptions.impl.InputErrorException;
import com.lframework.starter.common.utils.NumberUtil;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.starter.web.core.vo.BaseVo;
import com.lframework.xingyun.sc.dto.purchase.receive.GetPaymentDateDto;
import com.lframework.xingyun.sc.service.purchase.ReceiveSheetService;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateReceiveSheetVo implements BaseVo, Serializable {

  private static final long serialVersionUID = 1L;

  @ApiModelProperty(value = "仓库ID", required = true)
  @NotBlank(message = "仓库ID不能为空！")
  private String scId;

  @ApiModelProperty(value = "供应商ID", required = true)
  @NotBlank(message = "供应商ID不能为空！")
  private String supplierId;

  @ApiModelProperty("采购员ID")
  private String purchaserId;

  @ApiModelProperty("付款日期")
  private LocalDate paymentDate;

  @ApiModelProperty("是否允许修改付款日期")
  private Boolean allowModifyPaymentDate = Boolean.FALSE;

  @ApiModelProperty(value = "到货日期", required = true)
  @NotNull(message = "到货日期不能为空！")
  private LocalDate receiveDate;

  @ApiModelProperty("采购订单ID")
  private String purchaseOrderId;

  @ApiModelProperty(value = "商品信息", required = true)
  @Valid
  @NotEmpty(message = "商品不能为空！")
  private List<ReceiveProductVo> products;

  @ApiModelProperty("备注")
  private String description;

  @ApiModelProperty("是否必须关联采购订单（采购入库固定为否）")
  private Boolean required;

  public void validate() {
    this.required = Boolean.FALSE;
    this.purchaseOrderId = null;
    this.validate(false);
  }

  protected void validate(boolean requirePurchase) {

    ReceiveSheetService receiveSheetService = ApplicationUtil.getBean(ReceiveSheetService.class);
    GetPaymentDateDto paymentDate = receiveSheetService.getPaymentDate(this.supplierId);
    if (paymentDate.getAllowModify() && this.getPaymentDate() == null) {
      throw new InputErrorException("付款日期不能为空！");
    }

    if (requirePurchase && StringUtil.isBlank(this.getPurchaseOrderId())) {
      throw new InputErrorException("采购订单不能为空！");
    }

    int orderNo = 1;
    for (ReceiveProductVo product : this.products) {

      if (StringUtil.isBlank(product.getProductId())) {
        throw new InputErrorException("第" + orderNo + "行商品不能为空！");
      }

      if (product.getReceiveNum() == null) {
        throw new InputErrorException("第" + orderNo + "行商品收货数量不能为空！");
      }

      if (NumberUtil.le(product.getReceiveNum(), BigDecimal.ZERO)) {
        throw new InputErrorException("第" + orderNo + "行商品收货数量必须大于0！");
      }

      if (!NumberUtil.isNumberPrecision(product.getReceiveNum(), 8)) {
        throw new InputErrorException("第" + orderNo + "行商品收货数量最多允许8位小数！");
      }

      if (!requirePurchase) {
        product.setPurchaseOrderDetailId(null);

        if (product.getPurchasePrice() == null) {
          throw new InputErrorException("第" + orderNo + "行商品采购价不能为空！");
        }

        if (NumberUtil.lt(product.getPurchasePrice(), BigDecimal.ZERO)) {
          throw new InputErrorException("第" + orderNo + "行商品采购价不允许小于0！");
        }

        if (!NumberUtil.isNumberPrecision(product.getPurchasePrice(), 6)) {
          throw new InputErrorException("第" + orderNo + "行商品采购价最多允许6位小数！");
        }
      }

      orderNo++;
    }
  }
}
