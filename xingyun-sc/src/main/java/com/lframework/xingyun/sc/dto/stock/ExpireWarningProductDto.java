package com.lframework.xingyun.sc.dto.stock;

import com.lframework.starter.web.core.dto.BaseDto;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * 即将过期药品（按药品聚合库存）
 */
@Data
public class ExpireWarningProductDto implements BaseDto, Serializable {

  private static final long serialVersionUID = 1L;

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
   * 截止时间
   */
  private LocalDate deadlineTime;

  /**
   * 当前库存数量（多仓库合计）
   */
  private BigDecimal stockNum;
}
