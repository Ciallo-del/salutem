package com.lframework.xingyun.basedata.vo.product.info.saleprop;

import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class CreateProductSalePropItemRelationVo implements BaseVo, Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * 药品ID
   */
  @ApiModelProperty(value = "药品ID", required = true)
  @NotBlank(message = "药品ID不能为空！")
  private String productId;

  /**
   * 销售属性值ID
   */
  @ApiModelProperty(value = "销售属性值ID", required = true)
  @NotEmpty(message = "销售属性值ID不能为空！")
  private List<String> salePropItemIds;
}
