package com.lframework.xingyun.sc.bo.sale.out;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lframework.starter.common.constants.StringPool;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.xingyun.basedata.entity.Customer;
import com.lframework.xingyun.basedata.entity.StoreCenter;
import com.lframework.xingyun.basedata.service.customer.CustomerService;
import com.lframework.xingyun.basedata.service.storecenter.StoreCenterService;
import com.lframework.xingyun.sc.entity.SaleOutSheet;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class QuerySaleOutSheetWithReturnBo extends BaseBo<SaleOutSheet> {

  @ApiModelProperty("ID")
  private String id;

  @ApiModelProperty("单号")
  private String code;

  @ApiModelProperty("仓库编号")
  private String scCode;

  @ApiModelProperty("仓库名称")
  private String scName;

  @ApiModelProperty("收货方编号")
  private String customerCode;

  @ApiModelProperty("收货方名称")
  private String customerName;

  @ApiModelProperty("创建人")
  private String createBy;

  @ApiModelProperty("创建时间")
  @JsonFormat(pattern = StringPool.DATE_TIME_PATTERN)
  private LocalDateTime createTime;

  public QuerySaleOutSheetWithReturnBo() {

  }

  public QuerySaleOutSheetWithReturnBo(SaleOutSheet dto) {

    super(dto);
  }

  @Override
  public BaseBo<SaleOutSheet> convert(SaleOutSheet dto) {

    return super.convert(dto);
  }

  @Override
  protected void afterInit(SaleOutSheet dto) {

    StoreCenterService storeCenterService = ApplicationUtil.getBean(StoreCenterService.class);
    StoreCenter sc = storeCenterService.findById(dto.getScId());
    this.scCode = sc.getCode();
    this.scName = sc.getName();

    this.customerCode = StringPool.EMPTY_STR;
    this.customerName = StringPool.EMPTY_STR;
    if (StringUtil.isNotBlank(dto.getCustomerId())) {
      CustomerService customerService = ApplicationUtil.getBean(CustomerService.class);
      Customer customer = customerService.findById(dto.getCustomerId());
      if (customer != null) {
        this.customerCode = customer.getCode();
        this.customerName = customer.getName();
      }
    }
  }
}
