package com.lframework.starter.web.inner.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lframework.starter.web.core.dto.BaseDto;
import com.lframework.starter.web.core.entity.BaseEntity;
import java.time.LocalDateTime;

@TableName("op_logs")
public class OpLogs extends BaseEntity implements BaseDto {

  private static final long serialVersionUID = 1L;

  private String id;

  private String name;

  private String nameKey;

  private String nameTemplate;

  private String nameParams;

  private Integer logType;

  private String ip;

  private String extra;

  @TableField(fill = FieldFill.INSERT)
  private String createById;

  @TableField(fill = FieldFill.INSERT)
  private String createBy;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createTime;

  public String getId() {

    return id;
  }

  public void setId(String id) {

    this.id = id;
  }

  public String getName() {

    return name;
  }

  public void setName(String name) {

    this.name = name;
  }

  public String getNameKey() {

    return nameKey;
  }

  public void setNameKey(String nameKey) {

    this.nameKey = nameKey;
  }

  public String getNameTemplate() {

    return nameTemplate;
  }

  public void setNameTemplate(String nameTemplate) {

    this.nameTemplate = nameTemplate;
  }

  public String getNameParams() {

    return nameParams;
  }

  public void setNameParams(String nameParams) {

    this.nameParams = nameParams;
  }

  public Integer getLogType() {

    return logType;
  }

  public void setLogType(Integer logType) {

    this.logType = logType;
  }

  public String getIp() {

    return ip;
  }

  public void setIp(String ip) {

    this.ip = ip;
  }

  public String getExtra() {

    return extra;
  }

  public void setExtra(String extra) {

    this.extra = extra;
  }

  public String getCreateById() {

    return createById;
  }

  public void setCreateById(String createById) {

    this.createById = createById;
  }

  public String getCreateBy() {

    return createBy;
  }

  public void setCreateBy(String createBy) {

    this.createBy = createBy;
  }

  public LocalDateTime getCreateTime() {

    return createTime;
  }

  public void setCreateTime(LocalDateTime createTime) {

    this.createTime = createTime;
  }
}
