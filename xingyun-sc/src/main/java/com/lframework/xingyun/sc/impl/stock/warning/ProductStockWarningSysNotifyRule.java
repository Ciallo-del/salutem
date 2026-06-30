package com.lframework.xingyun.sc.impl.stock.warning;

import com.lframework.starter.common.utils.NumberUtil;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.utils.JsonUtil;
import com.lframework.starter.web.core.components.notify.SysNotifyRuleEmail;
import com.lframework.starter.web.core.components.notify.SysNotifyRuleSys;
import com.lframework.starter.web.inner.dto.notify.SysNotifyParamsDto;
import com.lframework.xingyun.core.utils.RequestLocaleUtil;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ProductStockWarningSysNotifyRule implements SysNotifyRuleSys,
    SysNotifyRuleEmail {

  public static final Integer BIZ_TYPE = 200;

  @Override
  public String getTitle(SysNotifyParamsDto params) {
    Map<String, String> variables = getVariables(params.getVariables());
    String productCode = variables.get("productCode");
    String productName = variables.get("productName");
    String bizType = variables.get("bizType");
    if (RequestLocaleUtil.isEnLocale()) {
      return StringUtil.format("[Stock Alert] Medicine Code: {}, Medicine Name: {}, {}",
          productCode, productName, "0".equals(bizType) ? "Stock Shortage" : "Stock Exceeds Limit");
    }

    return StringUtil.format("【库存预警】药品编号：{}，药品名称：{}，{}",
        productCode, productName, "0".equals(bizType) ? "库存不足" : "库存超限");
  }

  @Override
  public String getContent(SysNotifyParamsDto params) {
    Map<String, String> variables = getVariables(params.getVariables());
    String productCode = variables.get("productCode");
    String productName = variables.get("productName");
    String currentStock = variables.get("currentStock");
    String bizType = variables.get("bizType");
    BigDecimal minLimit = new BigDecimal(variables.get("minLimit"));
    BigDecimal maxLimit = new BigDecimal(variables.get("maxLimit"));
    boolean shortage = "0".equals(bizType);
    String limitValue = String.valueOf(NumberUtil.getNumber(shortage ? minLimit : maxLimit, 8));

    if (RequestLocaleUtil.isEnLocale()) {
      return StringUtil.format(
          "Medicine Code: {}, Medicine Name: {}, Current Stock: {}, {}: {}, {}",
          productCode, productName, currentStock,
          shortage ? "Alert Lower Limit Reached" : "Alert Upper Limit Reached",
          limitValue, shortage ? "Please restock as soon as possible" : "Please pay attention");
    }

    return StringUtil.format(
        "药品编号：{}，药品名称：{}，当前库存：{}，{}：{}，{}",
        productCode, productName, currentStock,
        shortage ? "已达到当前预警下限" : "已达到当前预警上限",
        limitValue, shortage ? "请尽快补货" : "请注意");
  }

  @Override
  public boolean match(Integer bizType) {
    return BIZ_TYPE.equals(bizType);
  }

  private Map<String, String> getVariables(Object vars) {
    return JsonUtil.parseMap(String.valueOf(vars), String.class, String.class);
  }
}
