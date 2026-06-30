package com.lframework.xingyun.sc.service.stock.warning;

/**
 * 库存不足补货提醒
 */
public interface ProductStockReplenishWarningService {

  /**
   * 扫描当前租户库存，依据上一周（上周一至本周一）的销售出库量判断是否需要补货，
   * 当前库存低于上一周出库量时，向库存预警通知组发送补货提醒。
   *
   * <p>调用前需保证已设置好租户上下文。</p>
   */
  void scanAndNotify();
}
