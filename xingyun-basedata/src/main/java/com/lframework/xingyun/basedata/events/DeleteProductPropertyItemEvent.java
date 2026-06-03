package com.lframework.xingyun.basedata.events;

import com.lframework.starter.web.core.event.DataChangeEvent;
import com.lframework.starter.web.core.event.DataChangeType;
import com.lframework.xingyun.basedata.entity.ProductPropertyItem;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteProductPropertyItemEvent extends DataChangeEvent<ProductPropertyItem> {

  public DeleteProductPropertyItemEvent(Object source, ProductPropertyItem entity,
      DataChangeType type) {
    super(source, entity, type);
  }
}
