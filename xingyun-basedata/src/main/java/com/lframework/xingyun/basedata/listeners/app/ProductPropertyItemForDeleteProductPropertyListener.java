package com.lframework.xingyun.basedata.listeners.app;

import com.lframework.xingyun.basedata.entity.ProductPropertyItem;
import com.lframework.xingyun.basedata.events.DeleteProductPropertyEvent;
import com.lframework.xingyun.basedata.service.product.ProductPropertyItemService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProductPropertyItemForDeleteProductPropertyListener implements
    ApplicationListener<DeleteProductPropertyEvent> {

  @Autowired
  private ProductPropertyItemService productPropertyItemService;

  @Transactional(rollbackFor = Exception.class)
  @Override
  public void onApplicationEvent(DeleteProductPropertyEvent event) {

    List<ProductPropertyItem> productPropertyItemList = productPropertyItemService.getByPropertyId(
        event.getEntity().getId());
    for (ProductPropertyItem propertyItem : productPropertyItemList) {
      productPropertyItemService.deleteById(propertyItem.getId());
    }
  }
}
