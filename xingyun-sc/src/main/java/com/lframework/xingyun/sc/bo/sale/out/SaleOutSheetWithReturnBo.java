package com.lframework.xingyun.sc.bo.sale.out;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lframework.starter.common.functions.SFunction;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.common.utils.NumberUtil;
import com.lframework.starter.common.constants.StringPool;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.xingyun.basedata.entity.Customer;
import com.lframework.xingyun.basedata.entity.StoreCenter;
import com.lframework.xingyun.basedata.service.customer.CustomerService;
import com.lframework.xingyun.basedata.service.storecenter.StoreCenterService;
import com.lframework.starter.web.inner.entity.SysUser;
import com.lframework.xingyun.sc.dto.sale.SaleProductDto;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetWithReturnDto;
import com.lframework.xingyun.sc.entity.ProductStock;
import com.lframework.xingyun.sc.service.sale.SaleOrderService;
import com.lframework.xingyun.sc.service.stock.ProductStockService;
import com.lframework.starter.web.inner.service.system.SysUserService;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;

@Data
public class SaleOutSheetWithReturnBo extends BaseBo<SaleOutSheetWithReturnDto> {

  @ApiModelProperty("订单ID")
  private String id;

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

  @ApiModelProperty("订单明细")
  private List<DetailBo> details;

  public SaleOutSheetWithReturnBo() {

  }

  public SaleOutSheetWithReturnBo(SaleOutSheetWithReturnDto dto) {

    super(dto);
  }

  @Override
  public BaseBo<SaleOutSheetWithReturnDto> convert(SaleOutSheetWithReturnDto dto) {

    return super.convert(dto, SaleOutSheetWithReturnBo::getSalerId,
        SaleOutSheetWithReturnBo::getDetails);
  }

  @Override
  protected void afterInit(SaleOutSheetWithReturnDto dto) {

    StoreCenterService storeCenterService = ApplicationUtil.getBean(StoreCenterService.class);
    StoreCenter sc = storeCenterService.findById(dto.getScId());
    this.scName = sc.getName();

    this.customerName = StringPool.EMPTY_STR;
    if (StringUtil.isNotBlank(dto.getCustomerId())) {
      CustomerService customerService = ApplicationUtil.getBean(CustomerService.class);
      Customer customer = customerService.findById(dto.getCustomerId());
      if (customer != null) {
        this.customerName = customer.getName();
      }
    }

    if (!StringUtil.isBlank(dto.getSalerId())) {
      SysUserService userService = ApplicationUtil.getBean(SysUserService.class);
      SysUser saler = userService.findById(dto.getSalerId());

      this.salerId = saler.getId();
      this.salerName = saler.getName();
    }

    if (!CollectionUtil.isEmpty(dto.getDetails())) {
      this.details = dto.getDetails().stream().map(t -> new DetailBo(this.getScId(), t))
          .collect(Collectors.toList());
    }
  }

  @Data
  public static class DetailBo extends BaseBo<SaleOutSheetWithReturnDto.SheetDetailDto> {

    @ApiModelProperty("ID")
    private String id;

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

    @ApiModelProperty("剩余退货数量")
    private BigDecimal remainNum;

    @ApiModelProperty("是否赠品")
    private Boolean isGift;

    @ApiModelProperty("税率（%）")
    private BigDecimal taxRate;

    @ApiModelProperty("备注")
    private String description;

    @ApiModelProperty(value = "仓库ID", hidden = true)
    @JsonIgnore
    private String scId;

    public DetailBo(String scId, SaleOutSheetWithReturnDto.SheetDetailDto dto) {

      this.scId = scId;
      this.init(dto);
    }

    @Override
    public BaseBo<SaleOutSheetWithReturnDto.SheetDetailDto> convert(
        SaleOutSheetWithReturnDto.SheetDetailDto dto) {

      return this;
    }

    @Override
    public <A> BaseBo<SaleOutSheetWithReturnDto.SheetDetailDto> convert(
        SaleOutSheetWithReturnDto.SheetDetailDto dto, SFunction<A, ?>... columns) {

      return this;
    }

    @Override
    protected void afterInit(SaleOutSheetWithReturnDto.SheetDetailDto dto) {

      SaleOrderService saleOrderService = ApplicationUtil.getBean(SaleOrderService.class);
      SaleProductDto product = saleOrderService.getSaleById(dto.getProductId());

      this.id = dto.getId();
      this.productId = product.getId();
      this.productCode = product.getCode();
      this.productName = product.getName();
      this.unit = product.getUnit();
      this.spec = product.getSpec();
      this.categoryName = product.getCategoryName();
      this.brandName = product.getBrandName();

      this.outNum = dto.getOrderNum();
      this.salePrice = dto.getOriPrice();
      this.taxPrice = dto.getTaxPrice();
      this.discountRate = dto.getDiscountRate();
      this.remainNum = NumberUtil.sub(dto.getOrderNum(), dto.getReturnNum());
      this.isGift = dto.getIsGift();
      this.taxRate = dto.getTaxRate();
      this.description = dto.getDescription();

      ProductStockService productStockService = ApplicationUtil.getBean(ProductStockService.class);
      ProductStock productStock = productStockService.getByProductIdAndScId(this.getProductId(),
          this.getScId());
      this.stockNum = productStock == null ? BigDecimal.ZERO : productStock.getStockNum();
    }
  }
}
