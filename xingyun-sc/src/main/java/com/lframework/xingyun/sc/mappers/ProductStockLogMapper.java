package com.lframework.xingyun.sc.mappers;

import com.lframework.starter.web.core.mapper.BaseMapper;
import com.lframework.starter.web.core.annotations.permission.DataPermission;
import com.lframework.starter.web.core.annotations.permission.DataPermissions;
import com.lframework.starter.web.core.annotations.sort.Sort;
import com.lframework.starter.web.core.annotations.sort.Sorts;
import com.lframework.starter.web.inner.components.permission.ProductDataPermissionDataPermissionType;
import com.lframework.xingyun.sc.dto.stock.StockReplenishCandidateDto;
import com.lframework.xingyun.sc.entity.ProductStockLog;
import com.lframework.xingyun.sc.vo.stock.log.QueryProductStockLogVo;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author zmj
 * @since 2021-10-14
 */
public interface ProductStockLogMapper extends BaseMapper<ProductStockLog> {

  /**
   * 查询列表
   *
   * @param vo
   * @return
   */
  @Sorts({
      @Sort(value = "scCode", alias = "sc.code"),
      @Sort(value = "productCode", alias = "g.code"),
      @Sort(value = "oriStockNum", alias = "gsl", autoParse = true),
      @Sort(value = "curStockNum", alias = "gsl", autoParse = true),
      @Sort(value = "stockNum", alias = "gsl", autoParse = true),
      @Sort(value = "oriTaxPrice", alias = "gsl", autoParse = true),
      @Sort(value = "curTaxPrice", alias = "gsl", autoParse = true),
      @Sort(value = "taxAmount", alias = "gsl", autoParse = true),
      @Sort(value = "createTime", alias = "gsl", autoParse = true),
      @Sort(value = "bizCode", alias = "gsl", autoParse = true),
      @Sort(value = "bizType", alias = "gsl", autoParse = true),
  })
  @DataPermissions(type = ProductDataPermissionDataPermissionType.class, value = {
      @DataPermission(template = "product", alias = "g"),
      @DataPermission(template = "brand", alias = "b"),
      @DataPermission(template = "category", alias = "c")
  })
  List<ProductStockLog> query(@Param("vo") QueryProductStockLogVo vo);

  /**
   * 查询当前库存低于指定区间出库量的药品（按仓库、药品维度），用于库存不足补货提醒。
   *
   * <p>出库日志的 stock_num 存为负数，出库量取 {@code SUM(-stock_num)}。</p>
   *
   * @param startTime 出库统计起始时间（含）
   * @param endTime   出库统计截止时间（不含）
   * @param bizType   出库业务类型（销售出库）
   * @return 待补货药品列表
   */
  List<StockReplenishCandidateDto> queryShortageProducts(
      @Param("startTime") LocalDateTime startTime,
      @Param("endTime") LocalDateTime endTime,
      @Param("bizType") Integer bizType);
}
