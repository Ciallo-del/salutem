package com.lframework.xingyun.basedata.vo.product.info;

import com.lframework.starter.web.core.components.validation.IsEnum;
import com.lframework.starter.web.core.vo.BaseVo;
import com.lframework.starter.web.core.vo.SortPageVo;
import com.lframework.xingyun.basedata.enums.ProductType;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class QueryProductVo extends SortPageVo implements BaseVo, Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * 编号
   */
  @ApiModelProperty("编号")
  private String code;

  /**
   * 名称
   */
  @ApiModelProperty("名称")
  private String name;

  /**
   * 简称
   */
  @ApiModelProperty("简称")
  private String shortName;

  /**
   * 品牌ID
   */
  @ApiModelProperty("品牌ID")
  private String brandId;

  /**
   * 分类ID
   */
  @ApiModelProperty("分类ID")
  private String categoryId;

  /**
   * 药品类型
   */
  @ApiModelProperty("药品类型")
  @IsEnum(message = "药品类型格式错误！", enumClass = ProductType.class)
  private Integer productType;

  /**
   * 创建起始时间
   */
  @ApiModelProperty("创建起始时间")
  private LocalDateTime startTime;

  /**
   * 创建截止时间
   */
  @ApiModelProperty("创建截止时间")
  private LocalDateTime endTime;

  /**
   * 生产时间起始
   */
  @ApiModelProperty("生产时间起始")
  private LocalDate productionTimeStart;

  /**
   * 生产时间截止
   */
  @ApiModelProperty("生产时间截止")
  private LocalDate productionTimeEnd;

  /**
   * 截止时间起始
   */
  @ApiModelProperty("截止时间起始")
  private LocalDate deadlineTimeStart;

  /**
   * 截止时间截止
   */
  @ApiModelProperty("截止时间截止")
  private LocalDate deadlineTimeEnd;
}
