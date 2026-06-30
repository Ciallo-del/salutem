package com.lframework.xingyun.sc.mappers;

import com.lframework.starter.web.core.mapper.BaseMapper;
import com.lframework.starter.web.core.annotations.permission.DataPermission;
import com.lframework.starter.web.core.annotations.permission.DataPermissions;
import com.lframework.starter.web.core.annotations.sort.Sort;
import com.lframework.starter.web.core.annotations.sort.Sorts;
import com.lframework.starter.web.inner.components.permission.ProductDataPermissionDataPermissionType;
import com.lframework.xingyun.sc.dto.stock.ExpireWarningProductDto;
import com.lframework.xingyun.sc.entity.ProductStock;
import com.lframework.xingyun.sc.vo.stock.QueryProductStockVo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author zmj
 * @since 2021-09-12
 */
public interface ProductStockMapper extends BaseMapper<ProductStock> {

  /**
   * 查询列表
   *
   * @param vo
   * @return
   */
  @Sorts({
      @Sort(value = "scCode", alias = "sc.code"),
      @Sort(value = "productCode", alias = "g.code"),
      @Sort(value = "stockNum", alias = "gs.stock_num"),
  })
  @DataPermissions(type = ProductDataPermissionDataPermissionType.class, value = {
      @DataPermission(template = "product", alias = "g"),
      @DataPermission(template = "brand", alias = "b"),
      @DataPermission(template = "category", alias = "c")
  })
  List<ProductStock> query(@Param("vo") QueryProductStockVo vo);

  /**
   * 根据药品ID、仓库ID查询
   *
   * @param productId
   * @param scId
   * @return
   */
  ProductStock getByProductIdAndScId(@Param("productId") String productId,
      @Param("scId") String scId);

  /**
   * 根据药品ID、仓库ID查询
   *
   * @param productIds
   * @param scId
   * @return
   */
  List<ProductStock> getByProductIdsAndScId(@Param("productIds") List<String> productIds,
      @Param("scId") String scId, @Param("productType") Integer productType);

  /**
   * 入库
   *
   * @param productId
   * @param scId
   * @param stockNum
   * @param taxAmount
   * @param oriStockNum
   * @param oriTaxAmount
   * @return
   */
  int addStock(@Param("productId") String productId, @Param("scId") String scId,
      @Param("stockNum") BigDecimal stockNum,
      @Param("taxAmount") BigDecimal taxAmount,
      @Param("oriStockNum") BigDecimal oriStockNum, @Param("oriTaxAmount") BigDecimal oriTaxAmount,
      @Param("reCalcCostPrice") boolean reCalcCostPrice);

  /**
   * 出库
   *
   * @param productId
   * @param scId
   * @param stockNum
   * @param taxAmount
   * @param oriStockNum
   * @param oriTaxAmount
   * @return
   */
  int subStock(@Param("productId") String productId, @Param("scId") String scId,
      @Param("stockNum") BigDecimal stockNum,
      @Param("taxAmount") BigDecimal taxAmount,
      @Param("oriStockNum") BigDecimal oriStockNum, @Param("oriTaxAmount") BigDecimal oriTaxAmount,
      @Param("reCalcCostPrice") boolean reCalcCostPrice);

  /**
   * 查询截止时间在指定区间内、且当前有库存的药品（按药品聚合库存）
   *
   * @param from 区间起始日期（含）
   * @param to   区间结束日期（含）
   * @return 即将过期药品列表
   */
  List<ExpireWarningProductDto> listExpiringProducts(@Param("from") LocalDate from,
      @Param("to") LocalDate to);
}
