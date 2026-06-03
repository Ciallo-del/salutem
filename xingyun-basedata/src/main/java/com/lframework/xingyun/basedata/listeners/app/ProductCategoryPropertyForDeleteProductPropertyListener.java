package com.lframework.xingyun.basedata.listeners.app;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lframework.xingyun.basedata.entity.ProductCategoryProperty;
import com.lframework.xingyun.basedata.events.DeleteProductPropertyEvent;
import com.lframework.xingyun.basedata.service.product.ProductCategoryPropertyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProductCategoryPropertyForDeleteProductPropertyListener implements
    ApplicationListener<DeleteProductPropertyEvent> {

  @Autowired
  private ProductCategoryPropertyService productCategoryPropertyService;

  @Transactional(rollbackFor = Exception.class)
  @Override
  public void onApplicationEvent(DeleteProductPropertyEvent event) {

    Wrapper<ProductCategoryProperty> queryWrapper = Wrappers.lambdaQuery(
            ProductCategoryProperty.class)
        .eq(ProductCategoryProperty::getPropertyId, event.getEntity().getId());
    if (productCategoryPropertyService.count(queryWrapper) > 0) {
      productCategoryPropertyService.remove(queryWrapper);
    }
  }
}
