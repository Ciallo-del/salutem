package com.lframework.xingyun.basedata.events;

import com.lframework.starter.web.core.event.DataChangeEvent;
import com.lframework.starter.web.core.event.DataChangeType;
import com.lframework.xingyun.basedata.entity.PayType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeletePayTypeEvent extends DataChangeEvent<PayType> {

  public DeletePayTypeEvent(Object source, PayType entity,
      DataChangeType type) {
    super(source, entity, type);
  }
}
