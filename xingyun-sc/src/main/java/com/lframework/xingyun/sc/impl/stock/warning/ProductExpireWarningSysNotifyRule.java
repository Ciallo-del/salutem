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
 * 药品即将过期提醒通知规则
 */
@Component
public class ProductExpireWarningSysNotifyRule implements SysNotifyRuleSys, SysNotifyRuleEmail {

  public static final Integer BIZ_TYPE = 201;

  @Override
  public String getTitle(SysNotifyParamsDto params) {
    Map<String, String> variables = getVariables(params.getVariables());
    String productCode = variables.get("productCode");
    String productName = variables.get("productName");
    if (RequestLocaleUtil.isEnLocale()) {
      return StringUtil.format("[Expiry Alert] Medicine Code: {}, Medicine Name: {}, Expiring Soon",
          productCode, productName);
    }

    return StringUtil.format("【药品过期提醒】药品编号：{}，药品名称：{}，即将过期",
        productCode, productName);
  }

  @Override
  public String getContent(SysNotifyParamsDto params) {
    Map<String, String> variables = getVariables(params.getVariables());
    String productCode = variables.get("productCode");
    String productName = variables.get("productName");
    String deadlineTime = variables.get("deadlineTime");
    String remainDays = variables.get("remainDays");
    String currentStock = variables.get("currentStock");

    if (RequestLocaleUtil.isEnLocale()) {
      return StringUtil.format(
          "Medicine Code: {}, Medicine Name: {}, Expiry Date: {}, Remaining Days: {}, Current Stock: {}, please handle as soon as possible",
          productCode, productName, deadlineTime, remainDays, currentStock);
    }

    return StringUtil.format(
        "药品编号：{}，药品名称：{}，截止日期：{}，剩余 {} 天，当前库存：{}，请尽快处理",
        productCode, productName, deadlineTime, remainDays, currentStock);
  }

  @Override
  public boolean match(Integer bizType) {
    return BIZ_TYPE.equals(bizType);
  }

  private Map<String, String> getVariables(Object vars) {
    return JsonUtil.parseMap(String.valueOf(vars), String.class, String.class);
  }
}
