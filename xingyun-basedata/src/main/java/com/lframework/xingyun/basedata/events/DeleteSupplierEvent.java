package com.lframework.xingyun.basedata.events;

import com.lframework.starter.web.core.event.DataChangeEvent;
import com.lframework.starter.web.core.event.DataChangeType;
import com.lframework.xingyun.basedata.entity.Supplier;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteSupplierEvent extends DataChangeEvent<Supplier> {

  public DeleteSupplierEvent(Object source, Supplier entity,
      DataChangeType type) {
    super(source, entity, type);
  }
}
