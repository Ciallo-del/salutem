package com.lframework.xingyun.basedata.vo.stockcell.product;

import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class CreateStockCellProductVo implements BaseVo, Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * 仓位ID
   */
  @ApiModelProperty(value = "仓位ID", required = true)
  @NotBlank(message = "仓位ID不能为空！")
  private String stockCellId;

  /**
   * 药品ID
   */
  @ApiModelProperty("药品ID")
  @NotEmpty(message = "请选择药品！")
  private List<String> productIds;
}
