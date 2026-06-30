package com.lframework.xingyun.sc.dto.stock;

import com.lframework.starter.web.core.dto.BaseDto;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 库存不足待补货药品（按仓库、药品维度）
 *
 * <p>当前库存低于上一周出库量，需要补货。</p>
 */
@Data
public class StockReplenishCandidateDto implements BaseDto, Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * 仓库ID
   */
  private String scId;

  /**
   * 仓库名称
   */
  private String scName;

  /**
   * 药品ID
   */
  private String productId;

  /**
   * 药品编号
   */
  private String productCode;

  /**
   * 药品名称
   */
  private String productName;

  /**
   * 当前库存数量
   */
  private BigDecimal currentStock;

  /**
   * 上一周出库量
   */
  private BigDecimal lastWeekOutbound;
}
