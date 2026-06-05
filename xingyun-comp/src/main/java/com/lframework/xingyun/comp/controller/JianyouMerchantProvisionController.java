package com.lframework.xingyun.comp.controller;

import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.web.core.components.resp.InvokeResult;
import com.lframework.starter.web.core.components.resp.InvokeResultBuilder;
import com.lframework.starter.web.core.controller.DefaultBaseController;
import com.lframework.xingyun.comp.bo.JianyouMerchantProvisionBo;
import com.lframework.xingyun.comp.bo.JianyouPlatformProvisionBo;
import com.lframework.xingyun.comp.service.JianyouMerchantProvisionService;
import com.lframework.xingyun.comp.service.JianyouPlatformProvisionService;
import com.lframework.xingyun.comp.vo.JianyouMerchantProvisionVo;
import com.lframework.xingyun.comp.vo.JianyouPlatformProvisionVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Api(tags = "建友星云初始化接口")
@Validated
@RestController
@RequestMapping("/xy/xingyun/jianyou")
public class JianyouMerchantProvisionController extends DefaultBaseController {

  private static final String SECRET_HEADER = "X-Jianyou-Api-Secret";

  @Value("${xingyun.sso.jianyou.provision-secret:}")
  private String provisionSecret;

  @Autowired
  private JianyouMerchantProvisionService jianyouMerchantProvisionService;

  @Autowired
  private JianyouPlatformProvisionService jianyouPlatformProvisionService;

  @ApiOperation("建佑平台商资源初始化")
  @PostMapping("/platform/provision")
  public InvokeResult<JianyouPlatformProvisionBo> provisionPlatform(
      @RequestHeader(value = SECRET_HEADER, required = false) String apiSecret,
      @Valid @RequestBody JianyouPlatformProvisionVo vo) {

    validateSecret(apiSecret);
    return InvokeResultBuilder.success(jianyouPlatformProvisionService.provision(vo));
  }

  @ApiOperation("建佑商户初始化")
  @PostMapping("/merchant/provision")
  public InvokeResult<JianyouMerchantProvisionBo> provisionMerchant(
      @RequestHeader(value = SECRET_HEADER, required = false) String apiSecret,
      @Valid @RequestBody JianyouMerchantProvisionVo vo) {

    validateSecret(apiSecret);
    return InvokeResultBuilder.success(jianyouMerchantProvisionService.provision(vo));
  }

  private void validateSecret(String apiSecret) {
    if (StringUtils.isBlank(provisionSecret) || !StringUtils.equals(provisionSecret, apiSecret)) {
      throw new DefaultClientException("接口认证失败");
    }
  }
}
