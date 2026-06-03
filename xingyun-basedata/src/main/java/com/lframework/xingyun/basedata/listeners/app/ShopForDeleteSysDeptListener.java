package com.lframework.xingyun.basedata.listeners.app;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.web.inner.events.system.DeleteSysDeptEvent;
import com.lframework.xingyun.basedata.entity.Shop;
import com.lframework.xingyun.basedata.service.shop.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ShopForDeleteSysDeptListener implements
    ApplicationListener<DeleteSysDeptEvent> {

  @Autowired
  private ShopService shopService;

  @Override
  public void onApplicationEvent(DeleteSysDeptEvent event) {

    Wrapper<Shop> queryWrapper = Wrappers.lambdaQuery(Shop.class)
        .eq(Shop::getDeptId, event.getEntity().getId()).eq(Shop::getAvailable, true);
    if (shopService.count(queryWrapper) > 0) {
      throw new DefaultClientException("部门：" + event.getEntity().getName() + "下存在门店，不允许删除！");
    }
  }
}
