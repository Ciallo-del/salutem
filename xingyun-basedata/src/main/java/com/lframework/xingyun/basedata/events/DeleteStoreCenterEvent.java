package com.lframework.xingyun.basedata.events;

import com.lframework.starter.web.core.event.DataChangeEvent;
import com.lframework.starter.web.core.event.DataChangeType;
import com.lframework.xingyun.basedata.entity.StoreCenter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteStoreCenterEvent extends DataChangeEvent<StoreCenter> {

  public DeleteStoreCenterEvent(Object source, StoreCenter entity,
      DataChangeType type) {
    super(source, entity, type);
  }
}
