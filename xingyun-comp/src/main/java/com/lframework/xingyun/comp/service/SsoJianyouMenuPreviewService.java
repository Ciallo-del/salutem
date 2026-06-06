package com.lframework.xingyun.comp.service;

import com.lframework.xingyun.comp.bo.SsoJianyouMenuPreviewGroupBo;

import java.util.List;

public interface SsoJianyouMenuPreviewService {

    List<SsoJianyouMenuPreviewGroupBo> getMenuPreview(String targetUserId);
}
