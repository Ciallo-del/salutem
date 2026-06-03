package com.lframework.xingyun.basedata.controller;

import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.common.utils.CollectionUtil;
import com.lframework.starter.web.core.annotations.security.HasPermission;
import com.lframework.starter.web.core.components.resp.InvokeResult;
import com.lframework.starter.web.core.components.resp.InvokeResultBuilder;
import com.lframework.starter.web.core.controller.DefaultBaseController;
import com.lframework.starter.web.core.utils.ExcelUtil;
import com.lframework.starter.web.inner.service.RecursionMappingService;
import com.lframework.xingyun.basedata.bo.product.category.GetProductCategoryBo;
import com.lframework.xingyun.basedata.bo.product.category.ProductCategoryTreeBo;
import com.lframework.xingyun.basedata.entity.ProductCategory;
import com.lframework.xingyun.basedata.excel.product.category.ProductCategoryImportListener;
import com.lframework.xingyun.basedata.excel.product.category.ProductCategoryImportModel;
import com.lframework.xingyun.basedata.service.product.ProductCategoryService;
import com.lframework.xingyun.basedata.vo.product.category.CreateProductCategoryVo;
import com.lframework.xingyun.basedata.vo.product.category.UpdateProductCategoryVo;
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
 * 分类管理
 *
 * @author zmj
 */
@Api(tags = "分类管理")
@Validated
@RestController
@RequestMapping("/basedata/product/category")
public class ProductCategoryController extends DefaultBaseController {

  @Autowired
  private ProductCategoryService productCategoryService;

  @Autowired
  private RecursionMappingService recursionMappingService;

  /**
   * 分类列表
   */
  @ApiOperation("分类列表")
  @HasPermission({"base-data:product:category:query", "base-data:product:category:add",
      "base-data:product:category:modify"})
  @GetMapping("/query")
  public InvokeResult<List<ProductCategoryTreeBo>> query() {

    List<ProductCategory> datas = productCategoryService.getAllProductCategories();
    if (CollectionUtil.isEmpty(datas)) {
      return InvokeResultBuilder.success(CollectionUtil.emptyList());
    }

    List<ProductCategoryTreeBo> results = datas.stream().map(ProductCategoryTreeBo::new)
        .collect(Collectors.toList());

    return InvokeResultBuilder.success(results);
  }

  /**
   * 查询分类
   */
  @ApiOperation("查询分类")
  @ApiImplicitParam(value = "ID", name = "id", paramType = "query", required = true)
  @HasPermission({"base-data:product:category:query", "base-data:product:category:add",
      "base-data:product:category:modify"})
  @GetMapping
  public InvokeResult<GetProductCategoryBo> get(@NotBlank(message = "ID不能为空！") String id) {

    ProductCategory data = productCategoryService.findById(id);
    if (data == null) {
      throw new DefaultClientException("分类不存在！");
    }

    GetProductCategoryBo result = new GetProductCategoryBo(data);

    return InvokeResultBuilder.success(result);
  }

  /**
   * 根据ID删除
   */
  @ApiOperation("根据ID删除")
  @HasPermission({"base-data:product:category:delete"})
  @DeleteMapping
  public InvokeResult<Void> deleteById(
      @ApiParam(value = "ID", required = true) @NotEmpty(message = "分类ID不能为空！") String id) {

    productCategoryService.deleteById(id);
    productCategoryService.cleanCacheByKey(id);

    return InvokeResultBuilder.success();
  }

  /**
   * 新增分类
   */
  @ApiOperation("新增分类")
  @HasPermission({"base-data:product:category:add"})
  @PostMapping
  public InvokeResult<Void> create(@Valid CreateProductCategoryVo vo) {

    productCategoryService.create(vo);

    return InvokeResultBuilder.success();
  }

  /**
   * 修改分类
   */
  @ApiOperation("修改分类")
  @HasPermission({"base-data:product:category:modify"})
  @PutMapping
  public InvokeResult<Void> update(@Valid UpdateProductCategoryVo vo) {

    productCategoryService.update(vo);

    productCategoryService.cleanCacheByKey(vo.getId());

    return InvokeResultBuilder.success();
  }

  @ApiOperation("下载导入模板")
  @HasPermission({"base-data:product:category:import"})
  @GetMapping("/import/template")
  public void downloadImportTemplate() {
    ExcelUtil.exportXls("分类导入模板", ProductCategoryImportModel.class);
  }

  @ApiOperation("导入")
  @HasPermission({"base-data:product:category:import"})
  @PostMapping("/import")
  public InvokeResult<Void> importExcel(@NotBlank(message = "ID不能为空") String id,
      @NotNull(message = "请上传文件") MultipartFile file) {

    ProductCategoryImportListener listener = new ProductCategoryImportListener();
    listener.setTaskId(id);
    ExcelUtil.read(file, ProductCategoryImportModel.class, listener).sheet().doRead();

    return InvokeResultBuilder.success();
  }
}
