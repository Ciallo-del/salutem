package com.lframework.xingyun.sc.impl.stock.warning;

import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.mq.core.service.MqProducerService;
import com.lframework.starter.web.core.utils.JsonUtil;
import com.lframework.starter.web.inner.dto.notify.SysNotifyDto;
import com.lframework.xingyun.sc.dto.stock.StockReplenishCandidateDto;
import com.lframework.xingyun.sc.entity.ProductStockWarningNotify;
import com.lframework.xingyun.sc.enums.ProductStockBizType;
import com.lframework.xingyun.sc.mappers.ProductStockLogMapper;
import com.lframework.xingyun.sc.service.stock.warning.ProductStockReplenishWarningService;
import com.lframework.xingyun.sc.service.stock.warning.ProductStockWarningNotifyService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProductStockReplenishWarningServiceImpl implements ProductStockReplenishWarningService {

  @Autowired
  private ProductStockLogMapper productStockLogMapper;

  @Autowired
  private ProductStockWarningNotifyService productStockWarningNotifyService;

  @Autowired
  private ProductStockReplenishDedupCache dedupCache;

  @Autowired
  private MqProducerService mqProducerService;

  @Override
  public void scanAndNotify() {

    LastWeekRange range = resolveLastWeekRange();
    LocalDate currentWeekMonday = range.getCurrentWeekMonday();

    List<StockReplenishCandidateDto> candidates = productStockLogMapper.queryShortageProducts(
        range.getStartTime(), range.getEndTime(), ProductStockBizType.SALE.getCode());
    if (CollectionUtil.isEmpty(candidates)) {
      return;
    }

    List<ProductStockWarningNotify> notifyList = productStockWarningNotifyService.list();
    if (CollectionUtil.isEmpty(notifyList)) {
      log.info("没有设置预警通知组，不发送库存不足补货提醒");
      return;
    }

    for (StockReplenishCandidateDto candidate : candidates) {
      for (ProductStockWarningNotify notify : notifyList) {
        // 同一仓库、药品、通知组、同一周只提醒一次
        LocalDate lastNotifyWeek = dedupCache.getLastNotifyWeek(candidate.getScId(),
            candidate.getProductId(), notify.getNotifyGroupId());
        if (lastNotifyWeek != null && lastNotifyWeek.isEqual(currentWeekMonday)) {
          continue;
        }

        SysNotifyDto sysNotifyDto = new SysNotifyDto();
        Map<String, Object> variables = new HashMap<>(8, 1);
        variables.put("scName", candidate.getScName());
        variables.put("productCode", candidate.getProductCode());
        variables.put("productName", candidate.getProductName());
        variables.put("currentStock", String.valueOf(candidate.getCurrentStock()));
        variables.put("lastWeekOutbound", String.valueOf(candidate.getLastWeekOutbound()));

        sysNotifyDto.setVariables(JsonUtil.toJsonString(variables));
        sysNotifyDto.setBizType(ProductStockReplenishSysNotifyRule.BIZ_TYPE);
        sysNotifyDto.setNotifyGroupId(notify.getNotifyGroupId());

        try {
          mqProducerService.createSysNotify(sysNotifyDto);
          dedupCache.setLastNotifyWeek(currentWeekMonday, candidate.getScId(),
              candidate.getProductId(), notify.getNotifyGroupId());
        } catch (Exception e) {
          log.error("发送库存不足补货提醒失败，scId = {}, productId = {}, notifyGroupId = {}: {}",
              candidate.getScId(), candidate.getProductId(), notify.getNotifyGroupId(),
              e.getMessage(), e);
        }
      }
    }
  }

  /**
   * 上一周区间：[上周一 00:00, 本周一 00:00)
   */
  private LastWeekRange resolveLastWeekRange() {
    LocalDate today = LocalDate.now();
    LocalDate currentWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    LocalDate lastWeekMonday = currentWeekMonday.minusWeeks(1);
    return new LastWeekRange(lastWeekMonday.atStartOfDay(), currentWeekMonday.atStartOfDay(),
        currentWeekMonday);
  }

  @Getter
  @AllArgsConstructor
  private static class LastWeekRange {

    private final LocalDateTime startTime;

    private final LocalDateTime endTime;

    private final LocalDate currentWeekMonday;
  }
}
