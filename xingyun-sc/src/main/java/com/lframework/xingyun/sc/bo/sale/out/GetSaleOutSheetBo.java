package com.lframework.xingyun.sc.bo.sale.out;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lframework.starter.common.constants.StringPool;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.common.utils.NumberUtil;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.xingyun.basedata.entity.Customer;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.service.customer.CustomerService;
import com.lframework.xingyun.basedata.service.product.ProductService;
import com.lframework.xingyun.basedata.service.storecenter.StoreCenterService;
import com.lframework.xingyun.sc.bo.paytype.OrderPayTypeBo;
import com.lframework.xingyun.sc.dto.sale.SaleProductDto;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetFullDto;
import com.lframework.xingyun.sc.entity.OrderPayType;
import com.lframework.xingyun.sc.entity.ProductStock;
import com.lframework.xingyun.sc.entity.SaleOrder;
import com.lframework.xingyun.sc.entity.SaleOrderDetail;
import com.lframework.xingyun.sc.service.paytype.OrderPayTypeService;
import com.lframework.xingyun.sc.service.sale.SaleOrderDetailService;
import com.lframework.xingyun.sc.service.sale.SaleOrderService;
import com.lframework.xingyun.sc.service.stock.ProductStockService;
import com.lframework.starter.web.inner.service.system.SysUserService;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;

@Data
public class GetSaleOutSheetBo extends BaseBo<SaleOutSheetFullDto> {

  @ApiModelProperty("ID")
  private String id;

  @ApiModelProperty("单号")
  private String code;

  @ApiModelProperty("仓库ID")
  private String scId;

  @ApiModelProperty("仓库名称")
  private String scName;

  @ApiModelProperty("收货方ID")
  private String customerId;

  @ApiModelProperty("收货方名称")
  private String customerName;

  @ApiModelProperty("销售员ID")
  private String salerId;

  @ApiModelProperty("销售员姓名")
  private String salerName;

  @ApiModelProperty("付款日期")
  @JsonFormat(pattern = StringPool.DATE_PATTERN)
  private LocalDate paymentDate;

  @ApiModelProperty("销售订单ID")
  private String saleOrderId;

  @ApiModelProperty("销售订单号")
  private String saleOrderCode;

  @ApiModelProperty("销售数量")
  private BigDecimal totalNum;

  @ApiModelProperty("赠品数量")
  private BigDecimal giftNum;

  @ApiModelProperty("销售金额")
  private BigDecimal totalAmount;

  @ApiModelProperty("支付方式")
  private List<OrderPayTypeBo> payTypes;

  @ApiModelProperty("备注")
  private String description;

  @ApiModelProperty("创建人")
  private String createBy;

  @ApiModelProperty("创建时间")
  @JsonFormat(pattern = StringPool.DATE_TIME_PATTERN)
  private LocalDateTime createTime;

  @ApiModelProperty("审核人")
  private String approveBy;

  @ApiModelProperty("审核时间")
  @JsonFormat(pattern = StringPool.DATE_TIME_PATTERN)
  private LocalDateTime approveTime;

  @ApiModelProperty("状态")
  private Integer status;

  @ApiModelProperty("拒绝原因")
  private String refuseReason;

  @ApiModelProperty("结算状态")
  private Integer settleStatus;

  @ApiModelProperty("订单明细")
  private List<OrderDetailBo> details;

  public GetSaleOutSheetBo() {

  }

  public GetSaleOutSheetBo(SaleOutSheetFullDto dto) {

    super(dto);
  }

  @Override
  public BaseBo<SaleOutSheetFullDto> convert(SaleOutSheetFullDto dto) {

    return super.convert(dto, GetSaleOutSheetBo::getStatus, GetSaleOutSheetBo::getSettleStatus,
        GetSaleOutSheetBo::getDetails);
  }

  @Override
  protected void afterInit(SaleOutSheetFullDto dto) {

    StoreCenterService storeCenterService = ApplicationUtil.getBean(StoreCenterService.class);
    this.scName = storeCenterService.findById(dto.getScId()).getName();

    this.customerName = StringPool.EMPTY_STR;
    if (StringUtil.isNotBlank(dto.getCustomerId())) {
      CustomerService customerService = ApplicationUtil.getBean(CustomerService.class);
      Customer customer = customerService.findById(dto.getCustomerId());
      if (customer != null) {
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

    if (!StringUtil.isBlank(dto.getApproveBy())) {
      this.approveBy = userService.findById(dto.getApproveBy()).getName();
    }

    this.status = dto.getStatus().getCode();
    this.settleStatus = dto.getSettleStatus().getCode();

    this.totalNum = dto.getTotalNum();
    this.giftNum = dto.getTotalGiftNum();
    this.totalAmount = dto.getTotalAmount();

    if (!CollectionUtil.isEmpty(dto.getDetails())) {
      this.details = dto.getDetails().stream().map(t -> new OrderDetailBo(this.getScId(), t))
          .collect(Collectors.toList());
    }

    OrderPayTypeService orderPayTypeService = ApplicationUtil.getBean(OrderPayTypeService.class);
    List<OrderPayType> orderPayTypes = orderPayTypeService.findByOrderId(dto.getId());
    this.payTypes = orderPayTypes.stream().map(OrderPayTypeBo::new).collect(Collectors.toList());
  }

  @Data
  public static class OrderDetailBo extends BaseBo<SaleOutSheetFullDto.SheetDetailDto> {

    @ApiModelProperty("明细ID")
    private String id;

    @ApiModelProperty("组合药品ID")
    private String mainProductId;

    @ApiModelProperty("组合药品名称")
    private String mainProductName;

    @ApiModelProperty("药品ID")
    private String productId;

    @ApiModelProperty("药品编号")
    private String productCode;

    @ApiModelProperty("药品名称")
    private String productName;

    @ApiModelProperty("单位")
    private String unit;

    @ApiModelProperty("规格")
    private String spec;

    @ApiModelProperty("分类名称")
    private String categoryName;

    @ApiModelProperty("品牌名称")
    private String brandName;

    @ApiModelProperty("销售数量")
    private BigDecimal orderNum;

    @ApiModelProperty("剩余出库数量")
    private BigDecimal remainNum;

    @ApiModelProperty("出库数量")
    private BigDecimal outNum;

    @ApiModelProperty("原价")
    private BigDecimal salePrice;

    @ApiModelProperty("价格")
    private BigDecimal taxPrice;

    @ApiModelProperty("折扣")
    private BigDecimal discountRate;

    @ApiModelProperty("库存数量")
    private BigDecimal stockNum;

    @ApiModelProperty("是否赠品")
    private Boolean isGift;

    @ApiModelProperty("税率")
    private BigDecimal taxRate;

    @ApiModelProperty("备注")
    private String description;

    @ApiModelProperty("销售订单明细ID")
    private String saleOrderDetailId;

    @ApiModelProperty(value = "仓库ID", hidden = true)
    @JsonIgnore
    private String scId;

    public OrderDetailBo(String scId, SaleOutSheetFullDto.SheetDetailDto dto) {

      this.scId = scId;
      this.init(dto);
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
      this.salePrice = dto.getOriPrice();

      SaleOrderService saleOrderService = ApplicationUtil.getBean(SaleOrderService.class);
      SaleProductDto product = saleOrderService.getSaleById(dto.getProductId());

      this.productCode = product.getCode();
      this.productName = product.getName();
      this.unit = product.getUnit();
      this.spec = product.getSpec();
      this.categoryName = product.getCategoryName();
      this.brandName = product.getBrandName();

      if (!StringUtil.isBlank(dto.getSaleOrderDetailId())) {
        SaleOrderDetailService saleOrderDetailService = ApplicationUtil.getBean(
            SaleOrderDetailService.class);
        SaleOrderDetail saleOrderDetail = saleOrderDetailService.getById(
            dto.getSaleOrderDetailId());
        this.orderNum = saleOrderDetail.getOrderNum();
        this.remainNum = NumberUtil.sub(saleOrderDetail.getOrderNum(), saleOrderDetail.getOutNum());
      }

      ProductStockService productStockService = ApplicationUtil.getBean(
          ProductStockService.class);
      ProductStock productStock = productStockService.getByProductIdAndScId(this.getProductId(),
          this.getScId());
      this.stockNum = productStock == null ? BigDecimal.ZERO : productStock.getStockNum();

      if (StringUtil.isNotBlank(dto.getMainProductId())) {
        ProductService productService = ApplicationUtil.getBean(ProductService.class);
        Product mainProduct = productService.findById(dto.getMainProductId());
        this.mainProductId = dto.getMainProductId();
        this.mainProductName = mainProduct.getName();
      }
    }
  }
}
