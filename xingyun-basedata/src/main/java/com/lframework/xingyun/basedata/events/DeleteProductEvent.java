package com.lframework.xingyun.basedata.events;

import com.lframework.starter.web.core.event.DataChangeEvent;
import com.lframework.starter.web.core.event.DataChangeType;
import com.lframework.xingyun.basedata.entity.Product;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteProductEvent extends DataChangeEvent<Product> {

  public DeleteProductEvent(Object source, Product entity,
      DataChangeType type) {
    super(source, entity, type);
  }
}
