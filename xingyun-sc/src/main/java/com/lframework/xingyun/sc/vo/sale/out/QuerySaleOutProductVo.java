package com.lframework.xingyun.sc.vo.sale.out;

import com.lframework.starter.web.core.vo.PageVo;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QuerySaleOutProductVo extends PageVo {

  private static final long serialVersionUID = 1L;

  @ApiModelProperty(value = "仓库ID", required = true)
  @NotBlank(message = "仓库ID不能为空！")
  private String scId;

  @ApiModelProperty("搜索关键字")
  private String condition;

  @ApiModelProperty("分类ID")
  private String categoryId;

  @ApiModelProperty("品牌ID")
  private String brandId;
}
