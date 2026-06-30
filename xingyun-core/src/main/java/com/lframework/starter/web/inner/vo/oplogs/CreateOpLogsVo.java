package com.lframework.starter.web.inner.vo.oplogs;

import com.lframework.starter.web.core.vo.BaseVo;
import java.io.Serializable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class CreateOpLogsVo implements BaseVo, Serializable {

  private static final long serialVersionUID = 1L;

  @NotBlank(message = "日志名称不能为空！")
  private String name;

  @NotNull(message = "日志类型不能为空！")
  private Integer logType;

  private String createBy;

  private String createById;

  @NotBlank(message = "IP地址不能为空！")
  private String ip;

  private String extra;

  private String nameKey;

  private String nameTemplate;

  private String nameParams;

  public String getName() {

    return name;
  }

  public void setName(String name) {

    this.name = name;
  }

  public Integer getLogType() {

    return logType;
  }

  public void setLogType(Integer logType) {

    this.logType = logType;
  }

  public String getCreateBy() {

    return createBy;
  }

  public void setCreateBy(String createBy) {

    this.createBy = createBy;
  }

  public String getCreateById() {

    return createById;
  }

  public void setCreateById(String createById) {

    this.createById = createById;
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
}
