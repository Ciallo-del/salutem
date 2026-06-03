package com.lframework.xingyun.basedata.events;

import com.lframework.starter.web.core.event.DataChangeEvent;
import com.lframework.starter.web.core.event.DataChangeType;
import com.lframework.xingyun.basedata.entity.Shop;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteShopEvent extends DataChangeEvent<Shop> {

  public DeleteShopEvent(Object source, Shop entity,
      DataChangeType type) {
    super(source, entity, type);
  }
}
