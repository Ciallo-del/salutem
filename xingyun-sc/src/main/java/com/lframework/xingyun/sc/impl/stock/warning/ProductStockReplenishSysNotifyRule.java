package com.lframework.xingyun.sc.impl.stock.warning;

import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.components.notify.SysNotifyRuleEmail;
import com.lframework.starter.web.core.components.notify.SysNotifyRuleSys;
import com.lframework.starter.web.core.utils.JsonUtil;
import com.lframework.starter.web.inner.dto.notify.SysNotifyParamsDto;
import com.lframework.xingyun.core.utils.RequestLocaleUtil;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 库存不足补货提醒通知规则
 */
@Component
public class ProductStockReplenishSysNotifyRule implements SysNotifyRuleSys, SysNotifyRuleEmail {

  public static final Integer BIZ_TYPE = 202;

  @Override
  public String getTitle(SysNotifyParamsDto params) {
    Map<String, String> variables = getVariables(params.getVariables());
    String productCode = variables.get("productCode");
    String productName = variables.get("productName");
    if (RequestLocaleUtil.isEnLocale()) {
      return StringUtil.format(
          "[Replenishment Alert] Medicine Code: {}, Medicine Name: {}, Stock Insufficient",
          productCode, productName);
    }

    return StringUtil.format("【补货提醒】药品编号：{}，药品名称：{}，库存不足",
        productCode, productName);
  }

  @Override
  public String getContent(SysNotifyParamsDto params) {
    Map<String, String> variables = getVariables(params.getVariables());
    String scName = variables.get("scName");
    String productCode = variables.get("productCode");
    String productName = variables.get("productName");
    String currentStock = variables.get("currentStock");
    String lastWeekOutbound = variables.get("lastWeekOutbound");

    if (RequestLocaleUtil.isEnLocale()) {
      return StringUtil.format(
          "Warehouse: {}, Medicine Code: {}, Medicine Name: {}, Current Stock: {}, Last Week Outbound: {}, the current stock is below last week's outbound, please replenish as soon as possible",
          scName, productCode, productName, currentStock, lastWeekOutbound);
    }

    return StringUtil.format(
        "仓库：{}，药品编号：{}，药品名称：{}，当前库存：{}，上周出库量：{}，当前库存低于上周出库量，请尽快补货",
        scName, productCode, productName, currentStock, lastWeekOutbound);
  }

  @Override
  public boolean match(Integer bizType) {
    return BIZ_TYPE.equals(bizType);
  }

  private Map<String, String> getVariables(Object vars) {
    return JsonUtil.parseMap(String.valueOf(vars), String.class, String.class);
  }
}
