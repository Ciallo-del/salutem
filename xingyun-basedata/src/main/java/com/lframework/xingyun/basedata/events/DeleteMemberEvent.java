package com.lframework.xingyun.basedata.events;

import com.lframework.starter.web.core.event.DataChangeEvent;
import com.lframework.starter.web.core.event.DataChangeType;
import com.lframework.xingyun.basedata.entity.Member;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteMemberEvent extends DataChangeEvent<Member> {

  public DeleteMemberEvent(Object source, Member entity,
      DataChangeType type) {
    super(source, entity, type);
  }
}
