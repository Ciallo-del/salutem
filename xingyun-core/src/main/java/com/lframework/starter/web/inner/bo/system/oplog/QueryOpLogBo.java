package com.lframework.starter.web.inner.bo.system.oplog;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.starter.web.inner.entity.OpLogs;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;

public class QueryOpLogBo extends BaseBo<OpLogs> {

  @ApiModelProperty("ID")
  private String id;

  @ApiModelProperty("日志名称")
  private String name;

  @ApiModelProperty("日志模板Key")
  private String nameKey;

  @ApiModelProperty("日志模板")
  private String nameTemplate;

  @ApiModelProperty("日志参数(JSON)")
  private String nameParams;

  @ApiModelProperty("类别")
  private Integer logType;

  @ApiModelProperty("IP地址")
  private String ip;

  @ApiModelProperty("创建人")
  private String createBy;

  @ApiModelProperty("创建时间")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime createTime;

  public QueryOpLogBo() {
  }

  public QueryOpLogBo(OpLogs dto) {

    super(dto);
  }

  @Override
  protected void afterInit(OpLogs dto) {

    this.nameKey = dto.getNameKey();
    this.nameTemplate = dto.getNameTemplate();
    this.nameParams = dto.getNameParams();
  }

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
