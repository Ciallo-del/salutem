package com.lframework.xingyun.basedata.events;

import com.lframework.starter.web.core.event.DataChangeEvent;
import com.lframework.starter.web.core.event.DataChangeType;
import com.lframework.xingyun.basedata.entity.ProductCategory;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteProductCategoryEvent extends DataChangeEvent<ProductCategory> {

  public DeleteProductCategoryEvent(Object source, ProductCategory entity,
      DataChangeType type) {
    super(source, entity, type);
  }
}
