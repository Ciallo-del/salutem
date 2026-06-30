package com.lframework.xingyun.basedata.excel.stockcell.product;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.lframework.starter.web.core.annotations.excel.ExcelRequired;
import com.lframework.starter.web.core.components.excel.ExcelModel;
import lombok.Data;

@Data
public class StockCellProductImportByStockCellModel implements ExcelModel {

  /**
   * 药品ID
   */
  @ExcelIgnore
  private String productId;

  /**
   * 药品编号
   */
  @ExcelRequired
  @ExcelProperty("药品编号")
  private String productCode;
}
