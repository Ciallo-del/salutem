package com.lframework.xingyun.basedata.events;

import com.lframework.starter.web.core.event.DataChangeEvent;
import com.lframework.starter.web.core.event.DataChangeType;
import com.lframework.xingyun.basedata.entity.LogisticsCompany;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteLogisticsCompanyEvent extends DataChangeEvent<LogisticsCompany> {

  public DeleteLogisticsCompanyEvent(Object source, LogisticsCompany entity,
      DataChangeType type) {
    super(source, entity, type);
  }
}
