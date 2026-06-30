package com.lframework.xingyun.sc.impl.stock.warning;

import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.mq.core.service.MqProducerService;
import com.lframework.starter.web.core.utils.JsonUtil;
import com.lframework.starter.web.inner.dto.notify.SysNotifyDto;
import com.lframework.xingyun.sc.dto.stock.ExpireWarningProductDto;
import com.lframework.xingyun.sc.entity.ProductStockWarningNotify;
import com.lframework.xingyun.sc.service.stock.ProductStockService;
import com.lframework.xingyun.sc.service.stock.warning.ProductExpireWarningService;
import com.lframework.xingyun.sc.service.stock.warning.ProductStockWarningNotifyService;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProductExpireWarningServiceImpl implements ProductExpireWarningService {

  @Autowired
  private ProductStockService productStockService;

  @Autowired
  private ProductStockWarningNotifyService productStockWarningNotifyService;

  @Autowired
  private ProductExpireWarningDedupCache dedupCache;

  @Autowired
  private MqProducerService mqProducerService;

  @Override
  public void scanAndNotify() {

    LocalDate today = LocalDate.now();
    LocalDate deadline = today.plusDays(ADVANCE_DAYS);

    List<ExpireWarningProductDto> products = productStockService.listExpiringProducts(today,
        deadline);
    if (CollectionUtil.isEmpty(products)) {
      return;
    }

    List<ProductStockWarningNotify> notifyList = productStockWarningNotifyService.list();
    if (CollectionUtil.isEmpty(notifyList)) {
      log.info("没有设置预警通知组，不发送药品过期提醒");
      return;
    }

    for (ExpireWarningProductDto product : products) {
      long remainDays = ChronoUnit.DAYS.between(today, product.getDeadlineTime());
      for (ProductStockWarningNotify notify : notifyList) {
        // 同一药品、同一通知组、同一天只提醒一次
        LocalDate lastNotifyDate = dedupCache.getLastNotifyDate(product.getProductId(),
            notify.getNotifyGroupId());
        if (lastNotifyDate != null && lastNotifyDate.isEqual(today)) {
          continue;
        }

        SysNotifyDto sysNotifyDto = new SysNotifyDto();
        Map<String, Object> variables = new HashMap<>(8, 1);
        variables.put("productCode", product.getProductCode());
        variables.put("productName", product.getProductName());
        variables.put("deadlineTime", String.valueOf(product.getDeadlineTime()));
        variables.put("remainDays", String.valueOf(remainDays));
        variables.put("currentStock", String.valueOf(product.getStockNum()));

        sysNotifyDto.setVariables(JsonUtil.toJsonString(variables));
        sysNotifyDto.setBizType(ProductExpireWarningSysNotifyRule.BIZ_TYPE);
        sysNotifyDto.setNotifyGroupId(notify.getNotifyGroupId());

        try {
          mqProducerService.createSysNotify(sysNotifyDto);
          dedupCache.setLastNotifyDate(product.getProductId(), notify.getNotifyGroupId());
        } catch (Exception e) {
          log.error("发送药品过期提醒失败，productId = {}, notifyGroupId = {}: {}",
              product.getProductId(), notify.getNotifyGroupId(), e.getMessage(), e);
        }
      }
    }
  }
}
