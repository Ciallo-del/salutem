package com.lframework.xingyun.comp.service;

import com.lframework.xingyun.comp.bo.JianyouMerchantProvisionBo;
import com.lframework.xingyun.comp.bo.JianyouMerchantRepairBo;
import com.lframework.xingyun.comp.vo.JianyouMerchantProvisionVo;
import com.lframework.xingyun.comp.vo.JianyouMerchantRepairVo;

public interface JianyouMerchantProvisionService {

  JianyouMerchantProvisionBo provision(JianyouMerchantProvisionVo vo);

  JianyouMerchantRepairBo repairExisting(JianyouMerchantRepairVo vo);
}
