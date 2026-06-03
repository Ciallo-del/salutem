package com.lframework.xingyun.basedata.listeners.app;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lframework.xingyun.basedata.entity.ProductPropertyRelation;
import com.lframework.xingyun.basedata.events.DeleteProductPropertyItemEvent;
import com.lframework.xingyun.basedata.service.product.ProductPropertyRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProductPropertyRelationForDeleteProductPropertyItemListener implements
    ApplicationListener<DeleteProductPropertyItemEvent> {

  @Autowired
  private ProductPropertyRelationService productPropertyRelationService;

  @Transactional(rollbackFor = Exception.class)
  @Override
  public void onApplicationEvent(DeleteProductPropertyItemEvent event) {

    Wrapper<ProductPropertyRelation> queryWrapper = Wrappers.lambdaQuery(
            ProductPropertyRelation.class)
        .eq(ProductPropertyRelation::getPropertyId, event.getEntity().getPropertyId())
        .eq(ProductPropertyRelation::getPropertyItemId, event.getEntity().getId());
    if (productPropertyRelationService.count(queryWrapper) > 0) {
      productPropertyRelationService.remove(queryWrapper);
    }
  }
}
