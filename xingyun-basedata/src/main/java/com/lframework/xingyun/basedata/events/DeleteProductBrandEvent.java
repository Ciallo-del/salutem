package com.lframework.xingyun.basedata.events;

import com.lframework.starter.web.core.event.DataChangeEvent;
import com.lframework.starter.web.core.event.DataChangeType;
import com.lframework.xingyun.basedata.entity.ProductBrand;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteProductBrandEvent extends DataChangeEvent<ProductBrand> {

  public DeleteProductBrandEvent(Object source, ProductBrand entity,
      DataChangeType type) {
    super(source, entity, type);
  }
}
