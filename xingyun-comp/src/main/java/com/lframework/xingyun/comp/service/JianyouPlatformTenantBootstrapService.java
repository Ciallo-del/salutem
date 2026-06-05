package com.lframework.xingyun.comp.service;

import com.lframework.xingyun.comp.bo.JianyouPlatformTenantInitBo;

import java.util.Set;

public interface JianyouPlatformTenantBootstrapService {

  JianyouPlatformTenantInitBo initializeTenantResources(Integer tenantId, String tenantJdbcUrl,
      String platformOrgCard, String token, Set<Integer> availableModuleIds);
}
