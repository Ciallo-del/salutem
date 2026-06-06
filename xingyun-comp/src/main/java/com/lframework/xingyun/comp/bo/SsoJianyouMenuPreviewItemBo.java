package com.lframework.xingyun.comp.bo;

import lombok.Data;

import java.io.Serializable;

@Data
public class SsoJianyouMenuPreviewItemBo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String menuKey;

    private String menuName;

    private String redirect;
}
