package com.lframework.xingyun.sc.impl.sale;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageInfo;
import com.lframework.starter.common.constants.StringPool;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.common.exceptions.impl.InputErrorException;
import com.lframework.starter.common.utils.Assert;
import com.lframework.starter.common.utils.NumberUtil;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.annotations.oplog.OpLog;
import com.lframework.starter.web.core.annotations.timeline.OrderTimeLineLog;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.components.security.SecurityUtil;
import com.lframework.starter.web.core.impl.BaseMpServiceImpl;
import com.lframework.starter.web.core.utils.IdUtil;
import com.lframework.starter.web.core.utils.OpLogUtil;
import com.lframework.starter.web.core.utils.PageHelperUtil;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.starter.web.inner.components.timeline.ApprovePassOrderTimeLineBizType;
import com.lframework.starter.web.inner.components.timeline.ApproveReturnOrderTimeLineBizType;
import com.lframework.starter.web.inner.components.timeline.CreateOrderTimeLineBizType;
import com.lframework.starter.web.inner.components.timeline.UpdateOrderTimeLineBizType;
import com.lframework.starter.web.inner.entity.SysUser;
import com.lframework.starter.web.inner.service.GenerateCodeService;
import com.lframework.starter.web.inner.service.system.SysUserService;
import com.lframework.xingyun.basedata.entity.Customer;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.entity.ProductBundle;
import com.lframework.xingyun.basedata.entity.StoreCenter;
import com.lframework.xingyun.basedata.enums.ProductType;
import com.lframework.xingyun.basedata.enums.SettleType;
import com.lframework.xingyun.basedata.service.customer.CustomerService;
import com.lframework.xingyun.basedata.service.product.ProductBundleService;
import com.lframework.xingyun.basedata.service.product.ProductService;
import com.lframework.xingyun.basedata.service.storecenter.StoreCenterService;
import com.lframework.xingyun.core.utils.SplitNumberUtil;
import com.lframework.xingyun.sc.components.code.GenerateCodeTypePool;
import com.lframework.xingyun.sc.dto.purchase.receive.GetPaymentDateDto;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutProductDto;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetFullDto;
import com.lframework.xingyun.sc.dto.sale.out.SaleOutSheetWithReturnDto;
import com.lframework.xingyun.sc.dto.stock.ProductStockChangeDto;
import com.lframework.xingyun.sc.entity.LogisticsSheetDetail;
import com.lframework.xingyun.sc.entity.SaleConfig;
import com.lframework.xingyun.sc.entity.SaleOrder;
import com.lframework.xingyun.sc.entity.SaleOrderDetail;
import com.lframework.xingyun.sc.entity.SaleOrderDetailBundle;
import com.lframework.xingyun.sc.entity.SaleOutSheet;
import com.lframework.xingyun.sc.entity.SaleOutSheetDetail;
import com.lframework.xingyun.sc.entity.SaleOutSheetDetailBundle;
import com.lframework.xingyun.sc.entity.SaleOutSheetDetailLot;
import com.lframework.xingyun.sc.enums.LogisticsSheetDetailBizType;
import com.lframework.xingyun.sc.enums.ProductStockBizType;
import com.lframework.xingyun.sc.enums.SaleOpLogType;
import com.lframework.xingyun.sc.enums.SaleOutSheetStatus;
import com.lframework.xingyun.sc.enums.SettleStatus;
import com.lframework.xingyun.sc.mappers.SaleOutSheetMapper;
import com.lframework.xingyun.sc.service.logistics.LogisticsSheetDetailService;
import com.lframework.xingyun.sc.service.sale.SaleConfigService;
import com.lframework.xingyun.sc.service.sale.SaleOrderDetailBundleService;
import com.lframework.xingyun.sc.service.sale.SaleOrderDetailService;
import com.lframework.xingyun.sc.service.sale.SaleOrderService;
import com.lframework.xingyun.sc.service.sale.SaleOutSheetDetailBundleService;
import com.lframework.xingyun.sc.service.sale.SaleOutSheetDetailLotService;
import com.lframework.xingyun.sc.service.sale.SaleOutSheetDetailService;
import com.lframework.xingyun.sc.service.sale.SaleOutSheetService;
import com.lframework.xingyun.sc.service.stock.ProductStockService;
import com.lframework.xingyun.sc.vo.sale.out.ApprovePassSaleOutSheetVo;
import com.lframework.xingyun.sc.vo.sale.out.ApproveRefuseSaleOutSheetVo;
import com.lframework.xingyun.sc.vo.sale.out.CreateSaleOutSheetVo;
import com.lframework.xingyun.sc.vo.sale.out.QuerySaleOutProductVo;
import com.lframework.xingyun.sc.vo.sale.out.QuerySaleOutSheetVo;
import com.lframework.xingyun.sc.vo.sale.out.QuerySaleOutSheetWithReturnVo;
import com.lframework.xingyun.sc.vo.sale.out.SaleOutProductVo;
import com.lframework.xingyun.sc.vo.sale.out.SaleOutSheetSelectorVo;
import com.lframework.xingyun.sc.vo.sale.out.UpdateSaleOutSheetVo;
import com.lframework.xingyun.sc.vo.stock.SubProductStockVo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaleOutSheetServiceImpl extends
    BaseMpServiceImpl<SaleOutSheetMapper, SaleOutSheet> implements SaleOutSheetService {

  @Autowired
  private SaleOutSheetDetailService saleOutSheetDetailService;

  @Autowired
  private SaleOutSheetDetailLotService saleOutSheetDetailLotService;

  @Autowired
  private SaleOutSheetDetailBundleService saleOutSheetDetailBundleService;

  @Autowired
  private SaleOrderDetailBundleService saleOrderDetailBundleService;

  @Autowired
  private StoreCenterService storeCenterService;

  @Autowired
  private CustomerService customerService;

  @Autowired
  private SysUserService userService;

  @Autowired
  private SaleOrderService saleOrderService;

  @Autowired
  private ProductService productService;

  @Autowired
  private ProductBundleService productBundleService;

  @Autowired
  private GenerateCodeService generateCodeService;

  @Autowired
  private SaleConfigService saleConfigService;

  @Autowired
  private SaleOrderDetailService saleOrderDetailService;

  @Autowired
  private ProductStockService productStockService;

  @Autowired
  private LogisticsSheetDetailService logisticsSheetDetailService;

  @Override
  public PageResult<SaleOutSheet> query(Integer pageIndex, Integer pageSize,
      QuerySaleOutSheetVo vo) {

    Assert.greaterThanZero(pageIndex);
    Assert.greaterThanZero(pageSize);

    PageHelperUtil.startPage(pageIndex, pageSize);
    List<SaleOutSheet> datas = this.query(vo);

    return PageResultUtil.convert(new PageInfo<>(datas));
  }

  @Override
  public List<SaleOutSheet> query(QuerySaleOutSheetVo vo) {

    return getBaseMapper().query(vo);
  }

  @Override
  public PageResult<SaleOutSheet> selector(Integer pageIndex, Integer pageSize,
      SaleOutSheetSelectorVo vo) {

    Assert.greaterThanZero(pageIndex);
    Assert.greaterThanZero(pageSize);

    PageHelperUtil.startPage(pageIndex, pageSize);
    List<SaleOutSheet> datas = getBaseMapper().selector(vo);

    return PageResultUtil.convert(new PageInfo<>(datas));
  }

  @Override
  public GetPaymentDateDto getPaymentDate(String customerId) {

    if (StringUtil.isBlank(customerId)) {
      GetPaymentDateDto result = new GetPaymentDateDto();
      result.setAllowModify(Boolean.TRUE);
      result.setPaymentDate(LocalDate.now());
      return result;
    }

    Customer customer = customerService.findById(customerId);

    GetPaymentDateDto result = new GetPaymentDateDto();
    result.setAllowModify(customer.getSettleType() == SettleType.ARBITRARILY);

    if (customer.getSettleType() == SettleType.ARBITRARILY) {
      result.setPaymentDate(LocalDate.now().plusMonths(1));
    } else if (customer.getSettleType() == SettleType.CASH_ON_DELIVERY) {
      result.setPaymentDate(LocalDate.now());
    }

    return result;
  }

  @Override
  public SaleOutSheetFullDto getDetail(String id) {

    return getBaseMapper().getDetail(id);
  }

  @Override
  public SaleOutSheetWithReturnDto getWithReturn(String id) {

    SaleConfig saleConfig = saleConfigService.get();

    SaleOutSheetWithReturnDto sheet = getBaseMapper().getWithReturn(id,
        saleConfig.getSaleReturnRequireOutStock());
    if (sheet == null) {
      throw new InputErrorException("销售出库单不存在！");
    }

    return sheet;
  }

  @Override
  public PageResult<SaleOutSheet> queryWithReturn(Integer pageIndex, Integer pageSize,
      QuerySaleOutSheetWithReturnVo vo) {

    Assert.greaterThanZero(pageIndex);
    Assert.greaterThanZero(pageSize);

    SaleConfig saleConfig = saleConfigService.get();

    PageHelperUtil.startPage(pageIndex, pageSize);
    List<SaleOutSheet> datas = getBaseMapper().queryWithReturn(vo,
        saleConfig.getSaleReturnMultipleRelateOutStock());

    return PageResultUtil.convert(new PageInfo<>(datas));
  }

  @OpLog(type = SaleOpLogType.class, name = "创建销售出库单，单号：{}", params = "#code")
  @OrderTimeLineLog(type = CreateOrderTimeLineBizType.class, orderId = "#_result", name = "创建出库单")
  @Transactional(rollbackFor = Exception.class)
  @Override
  public String create(CreateSaleOutSheetVo vo) {

    SaleOutSheet sheet = new SaleOutSheet();
    sheet.setId(IdUtil.getId());
    sheet.setCode(generateCodeService.generate(GenerateCodeTypePool.SALE_OUT_SHEET));

    SaleConfig saleConfig = saleConfigService.get();

    this.create(sheet, vo, saleConfig.getOutStockRequireSale());

    sheet.setStatus(SaleOutSheetStatus.CREATED);

    getBaseMapper().insert(sheet);

    OpLogUtil.setVariable("code", sheet.getCode());
    OpLogUtil.setExtra(vo);

    return sheet.getId();
  }

  @OpLog(type = SaleOpLogType.class, name = "修改销售出库单，单号：{}", params = "#code")
  @OrderTimeLineLog(type = UpdateOrderTimeLineBizType.class, orderId = "#vo.id", name = "修改出库单")
  @Transactional(rollbackFor = Exception.class)
  @Override
  public void update(UpdateSaleOutSheetVo vo) {

    SaleOutSheet sheet = getBaseMapper().selectById(vo.getId());
    if (sheet == null) {
      throw new InputErrorException("销售出库单不存在！");
    }

    if (sheet.getStatus() != SaleOutSheetStatus.CREATED
        && sheet.getStatus() != SaleOutSheetStatus.APPROVE_REFUSE) {

      if (sheet.getStatus() == SaleOutSheetStatus.APPROVE_PASS) {
        throw new DefaultClientException("销售出库单已审核通过，无法修改！");
      }

      throw new DefaultClientException("销售出库单无法修改！");
    }

    boolean requireSale = !StringUtil.isBlank(sheet.getSaleOrderId());

    if (requireSale) {
      Wrapper<SaleOutSheetDetail> queryDetailWrapper = Wrappers.lambdaQuery(
          SaleOutSheetDetail.class).eq(SaleOutSheetDetail::getSheetId, sheet.getId());
      List<SaleOutSheetDetail> details = saleOutSheetDetailService.list(queryDetailWrapper);
      for (SaleOutSheetDetail detail : details) {
        if (!StringUtil.isBlank(detail.getSaleOrderDetailId())) {
          saleOrderDetailService.subOutNum(detail.getSaleOrderDetailId(), detail.getOrderNum());
        }
      }
    }

    Wrapper<SaleOutSheetDetail> deleteDetailWrapper = Wrappers.lambdaQuery(SaleOutSheetDetail.class)
        .eq(SaleOutSheetDetail::getSheetId, sheet.getId());
    saleOutSheetDetailService.remove(deleteDetailWrapper);

    Wrapper<SaleOutSheetDetailBundle> deleteDetailBundleWrapper = Wrappers.lambdaQuery(
        SaleOutSheetDetailBundle.class).eq(SaleOutSheetDetailBundle::getSheetId, sheet.getId());
    saleOutSheetDetailBundleService.remove(deleteDetailBundleWrapper);

    this.create(sheet, vo, requireSale);

    sheet.setStatus(SaleOutSheetStatus.CREATED);

    List<SaleOutSheetStatus> statusList = new ArrayList<>();
    statusList.add(SaleOutSheetStatus.CREATED);
    statusList.add(SaleOutSheetStatus.APPROVE_REFUSE);

    Wrapper<SaleOutSheet> updateOrderWrapper = Wrappers.lambdaUpdate(SaleOutSheet.class)
        .set(SaleOutSheet::getApproveBy, null).set(SaleOutSheet::getApproveTime, null)
        .set(SaleOutSheet::getRefuseReason, StringPool.EMPTY_STR)
        .eq(SaleOutSheet::getId, sheet.getId()).in(SaleOutSheet::getStatus, statusList);
    if (getBaseMapper().updateAllColumn(sheet, updateOrderWrapper) != 1) {
      throw new DefaultClientException("销售出库单信息已过期，请刷新重试！");
    }

    OpLogUtil.setVariable("code", sheet.getCode());
    OpLogUtil.setExtra(vo);
  }

  @OpLog(type = SaleOpLogType.class, name = "审核通过销售出库单，单号：{}", params = "#code")
  @OrderTimeLineLog(type = ApprovePassOrderTimeLineBizType.class, orderId = "#vo.id", name = "审核通过")
  @Transactional(rollbackFor = Exception.class)
  @Override
  public void approvePass(ApprovePassSaleOutSheetVo vo) {

    SaleOutSheet sheet = getBaseMapper().selectById(vo.getId());
    if (sheet == null) {
      throw new InputErrorException("销售出库单不存在！");
    }

    if (sheet.getStatus() != SaleOutSheetStatus.CREATED
        && sheet.getStatus() != SaleOutSheetStatus.APPROVE_REFUSE) {

      if (sheet.getStatus() == SaleOutSheetStatus.APPROVE_PASS) {
        throw new DefaultClientException("销售出库单已审核通过，不允许继续执行审核！");
      }

      throw new DefaultClientException("销售出库单无法审核通过！");
    }

    SaleConfig saleConfig = saleConfigService.get();

    if (!saleConfig.getOutStockMultipleRelateSale()) {
      Wrapper<SaleOutSheet> checkWrapper = Wrappers.lambdaQuery(SaleOutSheet.class)
          .eq(SaleOutSheet::getSaleOrderId, sheet.getSaleOrderId())
          .ne(SaleOutSheet::getId, sheet.getId());
      if (getBaseMapper().selectCount(checkWrapper) > 0) {
        SaleOrder purchaseOrder = saleOrderService.getById(sheet.getSaleOrderId());
        throw new DefaultClientException("销售订单号：" + purchaseOrder.getCode()
            + "，已关联其他销售出库单，不允许关联多个销售出库单！");
      }
    }

    if (saleConfig.getOutStockRequireLogistics()) {
      LogisticsSheetDetail logisticsSheetDetail = logisticsSheetDetailService.getByBizId(
          sheet.getId(), LogisticsSheetDetailBizType.SALE_OUT_SHEET);
      if (logisticsSheetDetail == null) {
        throw new DefaultClientException("销售出库单尚未发货，无法审核通过！");
      }
    }

    sheet.setStatus(SaleOutSheetStatus.APPROVE_PASS);

    List<SaleOutSheetStatus> statusList = new ArrayList<>();
    statusList.add(SaleOutSheetStatus.CREATED);
    statusList.add(SaleOutSheetStatus.APPROVE_REFUSE);

    LambdaUpdateWrapper<SaleOutSheet> updateOrderWrapper = Wrappers.lambdaUpdate(SaleOutSheet.class)
        .set(SaleOutSheet::getApproveBy, SecurityUtil.getCurrentUser().getId())
        .set(SaleOutSheet::getApproveTime, LocalDateTime.now())
        .eq(SaleOutSheet::getId, sheet.getId()).in(SaleOutSheet::getStatus, statusList);
    if (!StringUtil.isBlank(vo.getDescription())) {
      updateOrderWrapper.set(SaleOutSheet::getDescription, vo.getDescription());
    }
    if (getBaseMapper().updateAllColumn(sheet, updateOrderWrapper) != 1) {
      throw new DefaultClientException("销售出库单信息已过期，请刷新重试！");
    }

    Wrapper<SaleOutSheetDetail> queryDetailWrapper = Wrappers.lambdaQuery(SaleOutSheetDetail.class)
        .eq(SaleOutSheetDetail::getSheetId, sheet.getId())
        .orderByAsc(SaleOutSheetDetail::getOrderNo);
    List<SaleOutSheetDetail> details = saleOutSheetDetailService.list(queryDetailWrapper);

    BigDecimal totalNum = BigDecimal.ZERO;
    BigDecimal giftNum = BigDecimal.ZERO;
    BigDecimal totalAmount = BigDecimal.ZERO;

    int orderNo = 1;
    for (SaleOutSheetDetail detail : details) {
      boolean isGift = detail.getIsGift();
      totalAmount = NumberUtil.add(totalAmount,
          NumberUtil.mul(detail.getTaxPrice(), detail.getOrderNum()));

      Product product = productService.findById(detail.getProductId());
      if (product.getProductType() == ProductType.NORMAL) {
        SubProductStockVo subProductStockVo = new SubProductStockVo();
        subProductStockVo.setProductId(detail.getProductId());
        subProductStockVo.setScId(sheet.getScId());
        subProductStockVo.setStockNum(detail.getOrderNum());
        subProductStockVo.setBizId(sheet.getId());
        subProductStockVo.setBizDetailId(detail.getId());
        subProductStockVo.setBizCode(sheet.getCode());
        subProductStockVo.setBizType(ProductStockBizType.SALE.getCode());

        ProductStockChangeDto stockChange = productStockService.subStock(subProductStockVo);

        SaleOutSheetDetailLot detailLot = new SaleOutSheetDetailLot();

        detailLot.setId(IdUtil.getId());
        detailLot.setDetailId(detail.getId());
        detailLot.setOrderNum(detail.getOrderNum());
        detailLot.setCostTaxAmount(stockChange.getTaxAmount());
        detailLot.setSettleStatus(detail.getSettleStatus());
        detailLot.setOrderNo(orderNo);
        saleOutSheetDetailLotService.save(detailLot);

        if (isGift) {
          giftNum = NumberUtil.add(giftNum, detail.getOrderNum());
        } else {
          totalNum = NumberUtil.add(totalNum, detail.getOrderNum());
        }
      } else {
        Wrapper<SaleOutSheetDetailBundle> queryBundleWrapper = Wrappers.lambdaQuery(
                SaleOutSheetDetailBundle.class).eq(SaleOutSheetDetailBundle::getSheetId, sheet.getId())
            .eq(SaleOutSheetDetailBundle::getDetailId, detail.getId());
        List<SaleOutSheetDetailBundle> saleOutSheetDetailBundles = saleOutSheetDetailBundleService.list(
            queryBundleWrapper);
        Assert.notEmpty(saleOutSheetDetailBundles);

        for (SaleOutSheetDetailBundle saleOutSheetDetailBundle : saleOutSheetDetailBundles) {
          SaleOutSheetDetail newDetail = new SaleOutSheetDetail();
          newDetail.setId(IdUtil.getId());
          newDetail.setSheetId(sheet.getId());
          newDetail.setProductId(saleOutSheetDetailBundle.getProductId());
          newDetail.setOrderNum(saleOutSheetDetailBundle.getProductOrderNum());
          newDetail.setOriPrice(saleOutSheetDetailBundle.getProductOriPrice());
          newDetail.setTaxPrice(saleOutSheetDetailBundle.getProductTaxPrice());
          newDetail.setDiscountRate(detail.getDiscountRate());
          newDetail.setIsGift(detail.getIsGift());
          newDetail.setTaxRate(saleOutSheetDetailBundle.getProductTaxRate());
          newDetail.setDescription(detail.getDescription());
          newDetail.setOrderNo(detail.getOrderNo());
          newDetail.setSettleStatus(detail.getSettleStatus());
          newDetail.setTaxAmount(saleOutSheetDetailBundle.getProductTaxAmount());
          newDetail.setSaleOrderDetailId(detail.getSaleOrderDetailId());
          newDetail.setReturnNum(detail.getReturnNum());

          SubProductStockVo subProductStockVo = new SubProductStockVo();
          subProductStockVo.setProductId(newDetail.getProductId());
          subProductStockVo.setScId(sheet.getScId());
          subProductStockVo.setStockNum(newDetail.getOrderNum());
          subProductStockVo.setBizId(sheet.getId());
          subProductStockVo.setBizDetailId(newDetail.getId());
          subProductStockVo.setBizCode(sheet.getCode());
          subProductStockVo.setBizType(ProductStockBizType.SALE.getCode());

          ProductStockChangeDto stockChange = productStockService.subStock(subProductStockVo);

          SaleOutSheetDetailLot detailLot = new SaleOutSheetDetailLot();
          detailLot.setId(IdUtil.getId());
          detailLot.setDetailId(newDetail.getId());
          detailLot.setOrderNum(newDetail.getOrderNum());
          detailLot.setCostTaxAmount(stockChange.getTaxAmount());
          detailLot.setSettleStatus(newDetail.getSettleStatus());
          detailLot.setOrderNo(orderNo);
          saleOutSheetDetailLotService.save(detailLot);

          saleOutSheetDetailService.save(newDetail);
          saleOutSheetDetailService.removeById(detail.getId());

          saleOutSheetDetailBundle.setProductDetailId(newDetail.getId());
          saleOutSheetDetailBundleService.updateById(saleOutSheetDetailBundle);

          if (isGift) {
            giftNum = NumberUtil.add(giftNum, newDetail.getOrderNum());
          } else {
            totalNum = NumberUtil.add(totalNum, newDetail.getOrderNum());
          }
        }
      }
      orderNo++;
    }

    Wrapper<SaleOutSheet> updateWrapper = Wrappers.lambdaUpdate(SaleOutSheet.class)
        .set(SaleOutSheet::getTotalNum, totalNum).set(SaleOutSheet::getTotalGiftNum, giftNum)
        .set(SaleOutSheet::getTotalAmount, totalAmount).eq(SaleOutSheet::getId, sheet.getId());
    this.update(updateWrapper);

    OpLogUtil.setVariable("code", sheet.getCode());
    OpLogUtil.setExtra(vo);
  }

  @OrderTimeLineLog(type = ApprovePassOrderTimeLineBizType.class, orderId = "#_result", name = "直接审核通过")
  @Transactional(rollbackFor = Exception.class)
  @Override
  public String directApprovePass(CreateSaleOutSheetVo vo) {

    SaleOutSheetService thisService = getThis(this.getClass());

    String sheetId = thisService.create(vo);

    ApprovePassSaleOutSheetVo approvePassVo = new ApprovePassSaleOutSheetVo();
    approvePassVo.setId(sheetId);
    approvePassVo.setDescription(vo.getDescription());

    thisService.approvePass(approvePassVo);

    return sheetId;
  }

  @OpLog(type = SaleOpLogType.class, name = "审核拒绝销售出库单，单号：{}", params = "#code")
  @OrderTimeLineLog(type = ApproveReturnOrderTimeLineBizType.class, orderId = "#vo.id", name = "审核拒绝，拒绝理由：{}", params = "#vo.refuseReason")
  @Transactional(rollbackFor = Exception.class)
  @Override
  public void approveRefuse(ApproveRefuseSaleOutSheetVo vo) {

    SaleOutSheet sheet = getBaseMapper().selectById(vo.getId());
    if (sheet == null) {
      throw new InputErrorException("销售出库单不存在！");
    }

    if (sheet.getStatus() != SaleOutSheetStatus.CREATED) {

      if (sheet.getStatus() == SaleOutSheetStatus.APPROVE_PASS) {
        throw new DefaultClientException("销售出库单已审核通过，不允许继续执行审核！");
      }

      if (sheet.getStatus() == SaleOutSheetStatus.APPROVE_REFUSE) {
        throw new DefaultClientException("销售出库单已审核拒绝，不允许继续执行审核！");
      }

      throw new DefaultClientException("销售出库单无法审核拒绝！");
    }

    sheet.setStatus(SaleOutSheetStatus.APPROVE_REFUSE);

    LambdaUpdateWrapper<SaleOutSheet> updateOrderWrapper = Wrappers.lambdaUpdate(SaleOutSheet.class)
        .set(SaleOutSheet::getApproveBy, SecurityUtil.getCurrentUser().getId())
        .set(SaleOutSheet::getApproveTime, LocalDateTime.now())
        .set(SaleOutSheet::getRefuseReason, vo.getRefuseReason())
        .eq(SaleOutSheet::getId, sheet.getId())
        .eq(SaleOutSheet::getStatus, SaleOutSheetStatus.CREATED);
    if (getBaseMapper().updateAllColumn(sheet, updateOrderWrapper) != 1) {
      throw new DefaultClientException("销售出库单信息已过期，请刷新重试！");
    }

    OpLogUtil.setVariable("code", sheet.getCode());
    OpLogUtil.setExtra(vo);
  }

  @OpLog(type = SaleOpLogType.class, name = "删除销售出库单，单号：{}", params = "#code")
  @OrderTimeLineLog(orderId = "#id", delete = true)
  @Transactional(rollbackFor = Exception.class)
  @Override
  public void deleteById(String id) {

    Assert.notBlank(id);
    SaleOutSheet sheet = getBaseMapper().selectById(id);
    if (sheet == null) {
      throw new InputErrorException("销售出库单不存在！");
    }

    if (sheet.getStatus() != SaleOutSheetStatus.CREATED
        && sheet.getStatus() != SaleOutSheetStatus.APPROVE_REFUSE) {

      if (sheet.getStatus() == SaleOutSheetStatus.APPROVE_PASS) {
        throw new DefaultClientException("“审核通过”的销售出库单不允许执行删除操作！");
      }

      throw new DefaultClientException("销售出库单无法删除！");
    }

    if (logisticsSheetDetailService.getByBizId(sheet.getId(),
        LogisticsSheetDetailBizType.SALE_OUT_SHEET) != null) {
      throw new DefaultClientException("销售出库单已关联物流单，请先删除物流单！");
    }

    Wrapper<SaleOutSheetDetail> queryDetailWrapper = Wrappers.lambdaQuery(SaleOutSheetDetail.class)
        .eq(SaleOutSheetDetail::getSheetId, sheet.getId());
    List<SaleOutSheetDetail> details = saleOutSheetDetailService.list(queryDetailWrapper);

    if (!StringUtil.isBlank(sheet.getSaleOrderId())) {
      for (SaleOutSheetDetail detail : details) {
        if (!StringUtil.isBlank(detail.getSaleOrderDetailId())) {
          saleOrderDetailService.subOutNum(detail.getSaleOrderDetailId(), detail.getOrderNum());
        }
      }
    }

    Wrapper<SaleOutSheetDetail> deleteDetailWrapper = Wrappers.lambdaQuery(SaleOutSheetDetail.class)
        .eq(SaleOutSheetDetail::getSheetId, sheet.getId());
    saleOutSheetDetailService.remove(deleteDetailWrapper);

    Wrapper<SaleOutSheetDetailBundle> deleteDetailBundleWrapper = Wrappers.lambdaQuery(
        SaleOutSheetDetailBundle.class).eq(SaleOutSheetDetailBundle::getSheetId, sheet.getId());
    saleOutSheetDetailBundleService.remove(deleteDetailBundleWrapper);

    Wrapper<SaleOutSheetDetailLot> deleteDetailLotWrapper = Wrappers.lambdaQuery(
        SaleOutSheetDetailLot.class).in(SaleOutSheetDetailLot::getDetailId,
        details.stream().map(SaleOutSheetDetail::getId).collect(Collectors.toList()));
    saleOutSheetDetailLotService.remove(deleteDetailLotWrapper);

    Wrapper<SaleOutSheet> deleteWrapper = Wrappers.lambdaQuery(SaleOutSheet.class)
        .in(SaleOutSheet::getId, id)
        .in(SaleOutSheet::getStatus, SaleOutSheetStatus.CREATED, SaleOutSheetStatus.APPROVE_REFUSE);
    if (!remove(deleteWrapper)) {
      throw new DefaultClientException("销售出库单信息已过期，请刷新重试！");
    }

    OpLogUtil.setVariable("code", sheet.getCode());
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public int setUnSettle(String id) {

    Wrapper<SaleOutSheet> updateWrapper = Wrappers.lambdaUpdate(SaleOutSheet.class)
        .set(SaleOutSheet::getSettleStatus, SettleStatus.UN_SETTLE).eq(SaleOutSheet::getId, id)
        .eq(SaleOutSheet::getSettleStatus, SettleStatus.PART_SETTLE);
    int count = getBaseMapper().update(updateWrapper);

    return count;
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public int setPartSettle(String id) {

    Wrapper<SaleOutSheet> updateWrapper = Wrappers.lambdaUpdate(SaleOutSheet.class)
        .set(SaleOutSheet::getSettleStatus, SettleStatus.PART_SETTLE).eq(SaleOutSheet::getId, id)
        .in(SaleOutSheet::getSettleStatus, SettleStatus.UN_SETTLE, SettleStatus.PART_SETTLE);
    int count = getBaseMapper().update(updateWrapper);

    return count;
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public int setSettled(String id) {

    Wrapper<SaleOutSheet> updateWrapper = Wrappers.lambdaUpdate(SaleOutSheet.class)
        .set(SaleOutSheet::getSettleStatus, SettleStatus.SETTLED).eq(SaleOutSheet::getId, id)
        .in(SaleOutSheet::getSettleStatus, SettleStatus.UN_SETTLE, SettleStatus.PART_SETTLE);
    int count = getBaseMapper().update(updateWrapper);

    return count;
  }

  @Override
  public List<SaleOutSheet> getApprovedList(String customerId, LocalDateTime startTime,
      LocalDateTime endTime, SettleStatus settleStatus) {

    return getBaseMapper().getApprovedList(customerId, startTime, endTime, settleStatus);
  }

  @Override
  public PageResult<SaleOutProductDto> querySaleOutByCondition(Integer pageIndex, Integer pageSize,
      String scId, String condition) {

    Assert.greaterThanZero(pageIndex);
    Assert.greaterThanZero(pageSize);
    Assert.notBlank(scId);

    PageHelperUtil.startPage(pageIndex, pageSize);
    List<SaleOutProductDto> datas = getBaseMapper().querySaleOutByCondition(scId, condition);

    return PageResultUtil.convert(new PageInfo<>(datas));
  }

  @Override
  public PageResult<SaleOutProductDto> querySaleOutList(Integer pageIndex, Integer pageSize,
      QuerySaleOutProductVo vo) {

    Assert.greaterThanZero(pageIndex);
    Assert.greaterThanZero(pageSize);
    Assert.notNull(vo);
    Assert.notBlank(vo.getScId());

    PageHelperUtil.startPage(pageIndex, pageSize);
    List<SaleOutProductDto> datas = getBaseMapper().querySaleOutList(vo);

    return PageResultUtil.convert(new PageInfo<>(datas));
  }

  private void create(SaleOutSheet sheet, CreateSaleOutSheetVo vo, boolean requireSale) {

    StoreCenter sc = storeCenterService.findById(vo.getScId());
    if (sc == null) {
      throw new InputErrorException("仓库不存在！");
    }

    sheet.setScId(vo.getScId());
    sheet.setSaleOrderId(null);

    Customer customer = null;
    if (StringUtil.isNotBlank(vo.getCustomerId())) {
      customer = customerService.findById(vo.getCustomerId());
      if (customer == null) {
        throw new InputErrorException("收货方不存在！");
      }
      sheet.setCustomerId(vo.getCustomerId());
    } else {
      sheet.setCustomerId(null);
    }

    if (!StringUtil.isBlank(vo.getSalerId())) {
      SysUser saler = userService.findById(vo.getSalerId());
      if (saler == null) {
        throw new InputErrorException("销售员不存在！");
      }

      sheet.setSalerId(vo.getSalerId());
    } else {
      sheet.setSalerId(null);
    }

    SaleConfig saleConfig = saleConfigService.get();

    if (requireSale) {

      SaleOrder saleOrder = saleOrderService.getById(vo.getSaleOrderId());
      if (saleOrder == null) {
        throw new DefaultClientException("销售订单不存在！");
      }

      customer = customerService.findById(saleOrder.getCustomerId());
      if (customer == null) {
        throw new InputErrorException("收货方不存在！");
      }

      sheet.setScId(saleOrder.getScId());
      sheet.setCustomerId(saleOrder.getCustomerId());
      sheet.setSaleOrderId(saleOrder.getId());

      if (!saleConfig.getOutStockMultipleRelateSale()) {
        Wrapper<SaleOutSheet> checkWrapper = Wrappers.lambdaQuery(SaleOutSheet.class)
            .eq(SaleOutSheet::getSaleOrderId, saleOrder.getId())
            .ne(SaleOutSheet::getId, sheet.getId());
        if (getBaseMapper().selectCount(checkWrapper) > 0) {
          throw new DefaultClientException("销售订单号：" + saleOrder.getCode()
              + "，已关联其他销售出库单，不允许关联多个销售出库单！");
        }
      }
    }

    GetPaymentDateDto paymentDate = customer == null ? null : this.getPaymentDate(customer.getId());
    if (paymentDate == null) {
      sheet.setPaymentDate(vo.getPaymentDate() == null ? LocalDate.now() : vo.getPaymentDate());
    } else {
      sheet.setPaymentDate(
          vo.getAllowModifyPaymentDate() || paymentDate.getAllowModify() ? vo.getPaymentDate()
              : paymentDate.getPaymentDate());
    }

    if (sheet.getPaymentDate() == null) {
      sheet.setPaymentDate(LocalDate.now());
    }

    BigDecimal purchaseNum = BigDecimal.ZERO;
    BigDecimal giftNum = BigDecimal.ZERO;
    BigDecimal totalAmount = BigDecimal.ZERO;
    int orderNo = 1;
    for (SaleOutProductVo productVo : vo.getProducts()) {
      if (requireSale) {
        if (!StringUtil.isBlank(productVo.getSaleOrderDetailId())) {
          SaleOrderDetail orderDetail = saleOrderDetailService.getById(
              productVo.getSaleOrderDetailId());
          productVo.setOriPrice(orderDetail.getOriPrice());
          productVo.setTaxPrice(orderDetail.getTaxPrice());
          productVo.setDiscountRate(orderDetail.getDiscountRate());
        } else {
          productVo.setTaxPrice(BigDecimal.ZERO);
          productVo.setDiscountRate(BigDecimal.valueOf(100));
        }
      }

      boolean isGift = productVo.getTaxPrice().doubleValue() == 0D;

      if (requireSale) {
        if (StringUtil.isBlank(productVo.getSaleOrderDetailId()) && !isGift) {
          throw new InputErrorException("第" + orderNo + "行药品必须为“赠品”！");
        }
      }

      if (isGift) {
        giftNum = NumberUtil.add(giftNum, productVo.getOrderNum());
      } else {
        purchaseNum = NumberUtil.add(purchaseNum, productVo.getOrderNum());
      }

      totalAmount = NumberUtil.add(totalAmount,
          NumberUtil.getNumber(NumberUtil.mul(productVo.getTaxPrice(), productVo.getOrderNum()),
              2));

      SaleOutSheetDetail detail = new SaleOutSheetDetail();
      detail.setId(IdUtil.getId());
      detail.setSheetId(sheet.getId());

      Product product = productService.findById(productVo.getProductId());
      if (product == null) {
        throw new InputErrorException("第" + orderNo + "行药品不存在！");
      }

      detail.setProductId(productVo.getProductId());
      detail.setOrderNum(productVo.getOrderNum());
      detail.setOriPrice(productVo.getOriPrice());
      detail.setTaxPrice(productVo.getTaxPrice());
      detail.setDiscountRate(productVo.getDiscountRate());
      detail.setIsGift(isGift);
      detail.setTaxRate(product.getSaleTaxRate());
      detail.setDescription(StringUtil.isBlank(productVo.getDescription()) ? StringPool.EMPTY_STR
          : productVo.getDescription());
      detail.setOrderNo(orderNo);
      detail.setSettleStatus(this.getInitSettleStatus(customer));
      detail.setTaxAmount(
          NumberUtil.getNumber(NumberUtil.mul(detail.getTaxPrice(), detail.getOrderNum()), 2));
      if (requireSale && !StringUtil.isBlank(productVo.getSaleOrderDetailId())) {
        detail.setSaleOrderDetailId(productVo.getSaleOrderDetailId());
        saleOrderDetailService.addOutNum(productVo.getSaleOrderDetailId(), detail.getOrderNum());
      }

      saleOutSheetDetailService.save(detail);

      if (product.getProductType() == ProductType.BUNDLE) {
        if (!NumberUtil.isInteger(productVo.getOrderNum())) {
          throw new InputErrorException("第" + orderNo + "行药品出库数量必须是整数！");
        }
        List<ProductBundle> productBundles = productBundleService.getByMainProductId(
            product.getId());
        Map<Object, Number> bundleWeight = new HashMap<>(productBundles.size());
        for (ProductBundle productBundle : productBundles) {
          bundleWeight.put(productBundle.getProductId(),
              NumberUtil.mul(productBundle.getSalePrice(), productBundle.getBundleNum()));
        }
        Map<Object, Number> splitPriceMap = SplitNumberUtil.split(detail.getTaxAmount(),
            bundleWeight, 2);
        List<SaleOutSheetDetailBundle> saleOutSheetDetailBundles = productBundles.stream()
            .map(productBundle -> {
              Product bundle = productService.findById(productBundle.getProductId());
              SaleOutSheetDetailBundle saleOutSheetDetailBundle = new SaleOutSheetDetailBundle();
              saleOutSheetDetailBundle.setId(IdUtil.getId());
              saleOutSheetDetailBundle.setSheetId(sheet.getId());
              saleOutSheetDetailBundle.setDetailId(detail.getId());
              saleOutSheetDetailBundle.setMainProductId(product.getId());
              saleOutSheetDetailBundle.setOrderNum(detail.getOrderNum());
              saleOutSheetDetailBundle.setProductId(productBundle.getProductId());
              saleOutSheetDetailBundle.setProductOrderNum(
                  NumberUtil.mul(detail.getOrderNum(), productBundle.getBundleNum()));
              saleOutSheetDetailBundle.setProductOriPrice(productBundle.getSalePrice());
              saleOutSheetDetailBundle.setProductTaxAmount(BigDecimal.valueOf(
                  splitPriceMap.get(productBundle.getProductId()).doubleValue()));
              saleOutSheetDetailBundle.setProductTaxPrice(NumberUtil.getNumber(NumberUtil.div(
                  saleOutSheetDetailBundle.getProductTaxAmount(),
                  saleOutSheetDetailBundle.getProductOrderNum()), 6));
              saleOutSheetDetailBundle.setProductTaxRate(bundle.getSaleTaxRate());

              return saleOutSheetDetailBundle;
            }).collect(Collectors.toList());

        saleOutSheetDetailBundleService.saveBatch(saleOutSheetDetailBundles);
      }
      orderNo++;
    }
    sheet.setTotalNum(purchaseNum);
    sheet.setTotalGiftNum(giftNum);
    sheet.setTotalAmount(totalAmount);
    sheet.setDescription(
        StringUtil.isBlank(vo.getDescription()) ? StringPool.EMPTY_STR : vo.getDescription());
    sheet.setSettleStatus(this.getInitSettleStatus(customer));
  }

  private SettleStatus getInitSettleStatus(Customer customer) {

    return SettleStatus.UN_SETTLE;
  }
}
