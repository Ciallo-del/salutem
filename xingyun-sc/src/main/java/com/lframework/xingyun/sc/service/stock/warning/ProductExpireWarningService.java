package com.lframework.xingyun.sc.service.stock.warning;

/**
 * 药品即将过期提醒
 */
public interface ProductExpireWarningService {

  /**
   * 提前提醒天数（截止时间前30天）
   */
  int ADVANCE_DAYS = 30;

  /**
   * 扫描当前租户即将过期且有库存的药品，并向库存预警通知组发送提醒。
   *
   * <p>调用前需保证已设置好租户上下文。</p>
   */
  void scanAndNotify();
}
