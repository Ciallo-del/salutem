package com.lframework.xingyun.sc.bo.sale.out;

import com.lframework.starter.common.constants.StringPool;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.common.utils.DateUtil;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.xingyun.basedata.entity.Customer;
import com.lframework.xingyun.basedata.entity.StoreCenter;
import com.lframework.xingyun.basedata.service.customer.CustomerService;
import com.lframework.xingyun.basedata.service.storecenter.StoreCenterService;
import com.lframework.xingyun.sc.dto.sale.SaleProductDto;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetFullDto;
import com.lframework.xingyun.sc.entity.SaleOrder;
import com.lframework.xingyun.sc.enums.SaleOutSheetStatus;
import com.lframework.xingyun.sc.service.sale.SaleOrderService;
import com.lframework.starter.web.inner.service.system.SysUserService;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;

@Data
public class PrintSaleOutSheetBo extends BaseBo<SaleOutSheetFullDto> {

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

  @ApiModelProperty("销售员姓名")
  private String salerName;

  @ApiModelProperty("付款日期")
  private String paymentDate;

  @ApiModelProperty("销售订单号")
  private String saleOrderCode;

  @ApiModelProperty("备注")
  private String description;

  @ApiModelProperty("创建人")
  private String createBy;

  @ApiModelProperty("创建时间")
  private String createTime;

  @ApiModelProperty("审核人")
  private String approveBy;

  @ApiModelProperty("审核时间")
  private String approveTime;

  @ApiModelProperty("订单明细")
  private List<OrderDetailBo> details;

  public PrintSaleOutSheetBo() {

  }

  public PrintSaleOutSheetBo(SaleOutSheetFullDto dto) {

    super(dto);
  }

  @Override
  public BaseBo<SaleOutSheetFullDto> convert(SaleOutSheetFullDto dto) {

    return super.convert(dto, PrintSaleOutSheetBo::getDetails);
  }

  @Override
  protected void afterInit(SaleOutSheetFullDto dto) {

    this.salerName = StringPool.EMPTY_STR;
    this.paymentDate = StringPool.EMPTY_STR;
    this.saleOrderCode = StringPool.EMPTY_STR;
    this.approveBy = StringPool.EMPTY_STR;
    this.approveTime = StringPool.EMPTY_STR;
    this.customerCode = StringPool.EMPTY_STR;
    this.customerName = StringPool.EMPTY_STR;

    StoreCenterService storeCenterService = ApplicationUtil.getBean(StoreCenterService.class);
    StoreCenter sc = storeCenterService.findById(dto.getScId());
    this.scCode = sc.getCode();
    this.scName = sc.getName();

    if (StringUtil.isNotBlank(dto.getCustomerId())) {
      CustomerService customerService = ApplicationUtil.getBean(CustomerService.class);
      Customer customer = customerService.findById(dto.getCustomerId());
      if (customer != null) {
        this.customerCode = customer.getCode();
        this.customerName = customer.getName();
      }
    }

    SysUserService userService = ApplicationUtil.getBean(SysUserService.class);
    if (!StringUtil.isBlank(dto.getSalerId())) {
      this.salerName = userService.findById(dto.getSalerId()).getName();
    }

    SaleOrderService saleOrderService = ApplicationUtil.getBean(SaleOrderService.class);
    if (!StringUtil.isBlank(dto.getSaleOrderId())) {
      SaleOrder saleOrder = saleOrderService.getById(dto.getSaleOrderId());
      this.saleOrderCode = saleOrder.getCode();
    }

    if (dto.getPaymentDate() != null) {
      this.paymentDate = DateUtil.formatDate(dto.getPaymentDate());
    }

    this.createTime = DateUtil.formatDateTime(dto.getCreateTime());

    if (!StringUtil.isBlank(dto.getApproveBy())
        && dto.getStatus() == SaleOutSheetStatus.APPROVE_PASS) {
      this.approveBy = userService.findById(dto.getApproveBy()).getName();
      this.approveTime = DateUtil.formatDateTime(dto.getApproveTime());
    }

    if (!CollectionUtil.isEmpty(dto.getDetails())) {
      this.details = dto.getDetails().stream().map(OrderDetailBo::new).collect(Collectors.toList());
    }
  }

  @Data
  public static class OrderDetailBo extends BaseBo<SaleOutSheetFullDto.SheetDetailDto> {

    @ApiModelProperty("药品编号")
    private String productCode;

    @ApiModelProperty("药品名称")
    private String productName;

    @ApiModelProperty("出库数量")
    private BigDecimal outNum;

    @ApiModelProperty("价格")
    private BigDecimal taxPrice;

    @ApiModelProperty("折扣")
    private BigDecimal outAmount;

    public OrderDetailBo(SaleOutSheetFullDto.SheetDetailDto dto) {

      super(dto);
    }

    @Override
    public BaseBo<SaleOutSheetFullDto.SheetDetailDto> convert(
        SaleOutSheetFullDto.SheetDetailDto dto) {

      return super.convert(dto);
    }

    @Override
    protected void afterInit(SaleOutSheetFullDto.SheetDetailDto dto) {

      this.outNum = dto.getOrderNum();
      this.taxPrice = dto.getTaxPrice();
      this.outAmount = dto.getTaxAmount();

      SaleOrderService saleOrderService = ApplicationUtil.getBean(SaleOrderService.class);
      SaleProductDto product = saleOrderService.getSaleById(dto.getProductId());

      this.productCode = product.getCode();
      this.productName = product.getName();
    }
  }
}
