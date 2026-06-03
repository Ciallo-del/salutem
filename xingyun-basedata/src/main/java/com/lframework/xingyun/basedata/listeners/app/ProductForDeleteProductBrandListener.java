package com.lframework.xingyun.basedata.listeners.app;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.events.DeleteProductBrandEvent;
import com.lframework.xingyun.basedata.service.product.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ProductForDeleteProductBrandListener implements
    ApplicationListener<DeleteProductBrandEvent> {

  @Autowired
  private ProductService productService;

  @Override
  public void onApplicationEvent(DeleteProductBrandEvent event) {
    Wrapper<Product> queryWrapper = Wrappers.lambdaQuery(Product.class)
        .eq(Product::getBrandId, event.getEntity().getId())
        .eq(Product::getAvailable, Boolean.TRUE);
    if (productService.count(queryWrapper) > 0) {
      throw new DefaultClientException(
          "商品品牌：" + event.getEntity().getName() + "下存在商品信息，无法删除！");
    }
  }
}
