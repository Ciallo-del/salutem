package com.lframework.xingyun.sc.vo.stock.log;

import com.lframework.starter.web.core.components.validation.IsEnum;
import com.lframework.starter.web.core.vo.SortPageVo;
import com.lframework.xingyun.sc.enums.ProductStockBizType;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class QueryProductStockLogVo extends SortPageVo {

  private static final long serialVersionUID = 1L;

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

  /**
   * 创建起始时间
   */
  @ApiModelProperty("创建起始时间")
  private LocalDateTime createStartTime;

  /**
   * 创建截止时间
   */
  @ApiModelProperty("创建截止时间")
  private LocalDateTime createEndTime;

  /**
   * 业务类型
   */
  @ApiModelProperty("业务类型")
  @IsEnum(message = "业务类型不存在！", enumClass = ProductStockBizType.class)
  private Integer bizType;
}
