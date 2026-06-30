package com.lframework.xingyun.sc.vo.stock;

import com.lframework.starter.web.core.vo.SortPageVo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class QueryProductStockVo extends SortPageVo {

  /**
   * 仓库ID
   */
  @ApiModelProperty("仓库ID")
  private String scId;

  /**
   * 药品编号
   */
  @ApiModelProperty("药品编号")
  private String productCode;

  /**
   * 药品名称
   */
  @ApiModelProperty("药品名称")
  private String productName;

  /**
   * 药品分类ID
   */
  @ApiModelProperty("药品分类ID")
  private String categoryId;

  /**
   * 药品品牌ID
   */
  @ApiModelProperty("药品品牌ID")
  private String brandId;
}
