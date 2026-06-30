package com.lframework.xingyun.sc.impl.stock.warning;

import java.time.LocalDate;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * 药品过期提醒去重缓存
 *
 * <p>用于保证同一药品在同一通知组、同一天内只提醒一次。缓存方式参考库存预警的最后通知时间实现。</p>
 */
@Component
public class ProductExpireWarningDedupCache {

  /**
   * 获取最后一次提醒的日期
   *
   * @param productId 药品ID
   * @param groupId   通知组ID
   * @return 最后提醒日期，未提醒过返回null
   */
  @Cacheable(cacheNames = "product_expire_warning_notify", key = "@cacheVariables.tenantId() + 'lastNotifyDate' + '_' + #productId + '_' + #groupId", unless = "#result == null")
  public LocalDate getLastNotifyDate(String productId, String groupId) {
    return null;
  }

  /**
   * 记录最后一次提醒的日期为今天
   *
   * @param productId 药品ID
   * @param groupId   通知组ID
   * @return 今天
   */
  @CachePut(cacheNames = "product_expire_warning_notify", key = "@cacheVariables.tenantId() + 'lastNotifyDate' + '_' + #productId + '_' + #groupId")
  public LocalDate setLastNotifyDate(String productId, String groupId) {
    return LocalDate.now();
  }
}
