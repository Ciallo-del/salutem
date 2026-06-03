package com.lframework.xingyun.basedata.listeners.app;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lframework.xingyun.basedata.entity.Address;
import com.lframework.xingyun.basedata.enums.AddressEntityType;
import com.lframework.xingyun.basedata.events.DeleteMemberEvent;
import com.lframework.xingyun.basedata.service.address.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AddressForDeleteMemberListener implements
    ApplicationListener<DeleteMemberEvent> {

  @Autowired
  private AddressService addressService;

  @Transactional(rollbackFor = Exception.class)
  @Override
  public void onApplicationEvent(DeleteMemberEvent event) {

    Wrapper<Address> deleteWrapper = Wrappers.lambdaQuery(Address.class)
        .eq(Address::getEntityId, event.getEntity().getId())
        .eq(Address::getEntityType, AddressEntityType.MEMBER);
    addressService.remove(deleteWrapper);
  }
}
