package com.lframework.xingyun.sc.impl.stock.warning;

import java.time.LocalDate;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * 库存不足补货提醒去重缓存
 *
 * <p>用于保证同一仓库、同一药品在同一通知组、同一周内只提醒一次。缓存方式参考库存预警的最后通知时间实现。</p>
 */
@Component
public class ProductStockReplenishDedupCache {

  /**
   * 获取最后一次提醒所属周（周一日期）
   *
   * @param scId      仓库ID
   * @param productId 药品ID
   * @param groupId   通知组ID
   * @return 最后提醒所属周的周一日期，未提醒过返回null
   */
  @Cacheable(cacheNames = "product_stock_replenish_warning_notify", key = "@cacheVariables.tenantId() + 'lastNotifyWeek' + '_' + #scId + '_' + #productId + '_' + #groupId", unless = "#result == null")
  public LocalDate getLastNotifyWeek(String scId, String productId, String groupId) {
    return null;
  }

  /**
   * 记录最后一次提醒所属周
   *
   * @param week      本次提醒所属周的周一日期
   * @param scId      仓库ID
   * @param productId 药品ID
   * @param groupId   通知组ID
   * @return 本次提醒所属周的周一日期
   */
  @CachePut(cacheNames = "product_stock_replenish_warning_notify", key = "@cacheVariables.tenantId() + 'lastNotifyWeek' + '_' + #scId + '_' + #productId + '_' + #groupId")
  public LocalDate setLastNotifyWeek(LocalDate week, String scId, String productId, String groupId) {
    return week;
  }
}
