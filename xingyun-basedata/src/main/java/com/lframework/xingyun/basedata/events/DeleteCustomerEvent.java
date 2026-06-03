package com.lframework.xingyun.basedata.events;

import com.lframework.starter.web.core.event.DataChangeEvent;
import com.lframework.starter.web.core.event.DataChangeType;
import com.lframework.xingyun.basedata.entity.Customer;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteCustomerEvent extends DataChangeEvent<Customer> {

  public DeleteCustomerEvent(Object source, Customer entity,
      DataChangeType type) {
    super(source, entity, type);
  }
}
