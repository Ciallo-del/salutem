package com.lframework.xingyun.basedata.events;

import com.lframework.starter.web.core.event.DataChangeEvent;
import com.lframework.starter.web.core.event.DataChangeType;
import com.lframework.xingyun.basedata.entity.ProductProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteProductPropertyEvent extends DataChangeEvent<ProductProperty> {

  public DeleteProductPropertyEvent(Object source, ProductProperty entity,
      DataChangeType type) {
    super(source, entity, type);
  }
}
