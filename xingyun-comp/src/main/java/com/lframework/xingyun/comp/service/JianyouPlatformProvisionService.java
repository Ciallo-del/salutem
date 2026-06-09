package com.lframework.xingyun.comp.service;

import com.lframework.xingyun.comp.bo.JianyouPlatformProvisionBo;
import com.lframework.xingyun.comp.bo.JianyouPlatformRepairBo;
import com.lframework.xingyun.comp.vo.JianyouPlatformProvisionVo;
import com.lframework.xingyun.comp.vo.JianyouPlatformRepairVo;

public interface JianyouPlatformProvisionService {

  JianyouPlatformProvisionBo provision(JianyouPlatformProvisionVo vo);

  JianyouPlatformRepairBo repairExisting(JianyouPlatformRepairVo vo);
}
