package com.lframework.xingyun.sc.dto.stock.take.plan;

import com.lframework.starter.web.core.dto.BaseDto;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 查询盘点任务详情中的药品信息 Dto
 */
@Data
public class GetTakeStockPlanDetailProductDto implements BaseDto, Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * 详情ID
   */
  private String id;

  /**
   * 药品ID
   */
  private String productId;

  /**
   * 库存数量
   */
  private BigDecimal stockNum;
}
