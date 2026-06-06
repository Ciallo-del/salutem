package com.lframework.xingyun.comp.bo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class SsoJianyouMenuPreviewGroupBo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String groupKey;

    private String groupName;

    private List<SsoJianyouMenuPreviewItemBo> items = new ArrayList<>();
}
