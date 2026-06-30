package com.lframework.xingyun.basedata.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.lframework.starter.web.core.enums.BaseEnum;

public enum ProductType implements BaseEnum<Integer> {
  NORMAL(1, "普通药品"), BUNDLE(2, "组合药品");

  @EnumValue
  private final Integer code;

  private final String desc;

  ProductType(Integer code, String desc) {

    this.code = code;
    this.desc = desc;
  }

  @Override
  public Integer getCode() {

    return this.code;
  }

  @Override
  public String getDesc() {

    return this.desc;
  }
}
