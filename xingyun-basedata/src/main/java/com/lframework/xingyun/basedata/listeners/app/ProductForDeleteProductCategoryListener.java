package com.lframework.xingyun.basedata.listeners.app;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.xingyun.basedata.entity.Product;
import com.lframework.xingyun.basedata.events.DeleteProductCategoryEvent;
import com.lframework.xingyun.basedata.service.product.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ProductForDeleteProductCategoryListener implements
    ApplicationListener<DeleteProductCategoryEvent> {

  @Autowired
  private ProductService productService;

  @Override
  public void onApplicationEvent(DeleteProductCategoryEvent event) {
    Wrapper<Product> queryWrapper = Wrappers.lambdaQuery(Product.class)
        .eq(Product::getCategoryId, event.getEntity().getId())
        .eq(Product::getAvailable, Boolean.TRUE);
    if (productService.count(queryWrapper) > 0) {
      throw new DefaultClientException(
          "商品分类：" + event.getEntity().getName() + "下存在商品信息，无法删除！");
    }
  }
}
