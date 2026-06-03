package com.lframework.xingyun.sc.listeners.app;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.web.inner.events.system.DeleteSysNotifyGroupEvent;
import com.lframework.xingyun.sc.entity.ProductStockWarningNotify;
import com.lframework.xingyun.sc.service.stock.warning.ProductStockWarningNotifyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class StockWarningForDeleteSysNotifyGroupListener implements
    ApplicationListener<DeleteSysNotifyGroupEvent> {

  @Autowired
  private ProductStockWarningNotifyService productStockWarningNotifyService;

  @Override
  public void onApplicationEvent(DeleteSysNotifyGroupEvent event) {
    Wrapper<ProductStockWarningNotify> queryWrapper = Wrappers.lambdaQuery(
            ProductStockWarningNotify.class)
        .eq(ProductStockWarningNotify::getNotifyGroupId, event.getEntity().getId());
    if (productStockWarningNotifyService.count(queryWrapper) > 0) {
      throw new DefaultClientException(
          "消息通知组：" + event.getEntity().getName()
              + "已被应用在库存预警，请先取消设置再删除该消息通知组！");
    }
  }
}
