package com.lframework.xingyun.basedata.controller;

import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.web.core.annotations.security.HasPermission;
import com.lframework.starter.web.core.controller.DefaultBaseController;
import com.lframework.starter.web.core.components.resp.InvokeResult;
import com.lframework.starter.web.core.components.resp.InvokeResultBuilder;
import com.lframework.starter.web.core.components.resp.PageResult;
import com.lframework.starter.web.core.utils.ExcelUtil;
import com.lframework.starter.web.core.utils.PageResultUtil;
import com.lframework.xingyun.basedata.bo.customer.GetCustomerBo;
import com.lframework.xingyun.basedata.bo.customer.QueryCustomerBo;
import com.lframework.xingyun.basedata.entity.Customer;
import com.lframework.xingyun.basedata.excel.customer.CustomerImportListener;
import com.lframework.xingyun.basedata.excel.customer.CustomerImportModel;
import com.lframework.xingyun.basedata.service.customer.CustomerService;
import com.lframework.xingyun.basedata.vo.customer.CreateCustomerVo;
import com.lframework.xingyun.basedata.vo.customer.QueryCustomerVo;
import com.lframework.xingyun.basedata.vo.customer.UpdateCustomerVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 收货方管理
 *
 * @author zmj
 */
@Api(tags = "收货方管理")
@Validated
@RestController
@RequestMapping("/basedata/customer")
public class CustomerController extends DefaultBaseController {

  @Autowired
  private CustomerService customerService;

  /**
   * 收货方列表
   */
  @ApiOperation("收货方列表")
  @HasPermission({"base-data:customer:query", "base-data:customer:add",
      "base-data:customer:modify"})
  @GetMapping("/query")
  public InvokeResult<PageResult<QueryCustomerBo>> query(@Valid QueryCustomerVo vo) {

    PageResult<Customer> pageResult = customerService.query(getPageIndex(vo), getPageSize(vo), vo);

    List<Customer> datas = pageResult.getDatas();
    List<QueryCustomerBo> results = null;

    if (!CollectionUtil.isEmpty(datas)) {
      results = datas.stream().map(QueryCustomerBo::new).collect(Collectors.toList());
    }

    return InvokeResultBuilder.success(PageResultUtil.rebuild(pageResult, results));
  }

  /**
   * 查询收货方
   */
  @ApiOperation("查询收货方")
  @ApiImplicitParam(value = "ID", name = "id", paramType = "query", required = true)
  @HasPermission({"base-data:customer:query", "base-data:customer:add",
      "base-data:customer:modify"})
  @GetMapping
  public InvokeResult<GetCustomerBo> get(@NotBlank(message = "ID不能为空！") String id) {

    Customer data = customerService.findById(id);
    if (data == null) {
      throw new DefaultClientException("收货方不存在！");
    }

    GetCustomerBo result = new GetCustomerBo(data);

    return InvokeResultBuilder.success(result);
  }

  /**
   * 删除收货方
   */
  @ApiOperation("删除收货方")
  @HasPermission({"base-data:customer:delete"})
  @DeleteMapping
  public InvokeResult<Void> deleteById(
      @ApiParam(value = "ID", required = true) @NotEmpty(message = "收货方ID不能为空！") String id) {

    customerService.deleteById(id);

    customerService.cleanCacheByKey(id);

    return InvokeResultBuilder.success();
  }

  /**
   * 新增收货方
   */
  @ApiOperation("新增收货方")
  @HasPermission({"base-data:customer:add"})
  @PostMapping
  public InvokeResult<Void> create(@Valid CreateCustomerVo vo) {

    customerService.create(vo);

    return InvokeResultBuilder.success();
  }

  /**
   * 修改收货方
   */
  @ApiOperation("修改收货方")
  @HasPermission({"base-data:customer:modify"})
  @PutMapping
  public InvokeResult<Void> update(@Valid UpdateCustomerVo vo) {

    customerService.update(vo);

    customerService.cleanCacheByKey(vo.getId());

    return InvokeResultBuilder.success();
  }

  @ApiOperation("下载导入模板")
  @HasPermission({"base-data:customer:import"})
  @GetMapping("/import/template")
  public void downloadImportTemplate() {
    ExcelUtil.exportXls("收货方导入模板", CustomerImportModel.class);
  }

  @ApiOperation("导入")
  @HasPermission({"base-data:customer:import"})
  @PostMapping("/import")
  public InvokeResult<Void> importExcel(@NotBlank(message = "ID不能为空") String id,
      @NotNull(message = "请上传文件") MultipartFile file) {

    CustomerImportListener listener = new CustomerImportListener();
    listener.setTaskId(id);
    ExcelUtil.read(file, CustomerImportModel.class, listener).sheet().doRead();

    return InvokeResultBuilder.success();
  }
}
