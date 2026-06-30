package com.lframework.xingyun.sc.bo.sale.out;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutProductDto;
import com.lframework.xingyun.sc.entity.ProductStock;
import com.lframework.xingyun.sc.service.stock.ProductStockService;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class SaleOutProductBo extends BaseBo<SaleOutProductDto> {

  @ApiModelProperty("ID")
  private String productId;

  @ApiModelProperty("编号")
  private String productCode;

  @ApiModelProperty("名称")
  private String productName;

  @ApiModelProperty("分类名称")
  private String categoryName;

  @ApiModelProperty("品牌名称")
  private String brandName;

  @ApiModelProperty("是否多销售属性")
  private Boolean multiSaleProp;

  @ApiModelProperty("规格")
  private String spec;

  @ApiModelProperty("单位")
  private String unit;

  @ApiModelProperty("销售价")
  private BigDecimal salePrice;

  @ApiModelProperty("库存数量")
  private BigDecimal stockNum;

  @ApiModelProperty("税率")
  private BigDecimal taxRate;

  @ApiModelProperty(value = "仓库ID", hidden = true)
  @JsonIgnore
  private String scId;

  public SaleOutProductBo(String scId, SaleOutProductDto dto) {
    this.scId = scId;
    this.init(dto);
  }

  @Override
  protected void afterInit(SaleOutProductDto dto) {
    this.productId = dto.getId();
    this.productCode = dto.getCode();
    this.productName = dto.getName();
    this.categoryName = dto.getCategoryName();
    this.brandName = dto.getBrandName();
    this.multiSaleProp = Boolean.FALSE;
    this.spec = dto.getSpec();
    this.unit = dto.getUnit();
    this.salePrice = dto.getSalePrice();
    this.taxRate = dto.getTaxRate();

    ProductStockService productStockService = ApplicationUtil.getBean(ProductStockService.class);
    ProductStock productStock = productStockService.getByProductIdAndScId(this.productId,
        this.scId);
    this.stockNum = productStock == null ? BigDecimal.ZERO : productStock.getStockNum();
  }
}
