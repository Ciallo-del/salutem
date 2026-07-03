package com.lframework.xingyun.comp.impl;

import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.web.core.utils.ApplicationUtil;
import com.lframework.xingyun.comp.bo.SsoJianyouMenuPreviewGroupBo;
import com.lframework.xingyun.comp.bo.SsoJianyouMenuPreviewItemBo;
import com.lframework.xingyun.comp.service.SsoJianyouMenuPreviewService;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class SsoJianyouMenuPreviewServiceImpl implements SsoJianyouMenuPreviewService {

    private static final String ROOT_MENU_STOCK_MANAGE = "StockManage";
    private static final String ROOT_MENU_TAKE_STOCK = "TakeStock";
    private static final String ROOT_MENU_STOCK_ADJUST = "StockAdjust";
    private static final String ROOT_MENU_SALE = "Sale";

    private static final String GROUP_KEY_IN_OUT_MANAGE = "SaleOutSheet";

    private static final Map<String, String> ROOT_REDIRECT_PREFIX_MAPPING;

    private static final Map<String, String> LEAF_REDIRECT_MAPPING;

    private static final Map<String, String> PREVIEW_MENU_NAME_MAPPING;

    static {
        Map<String, String> rootMappings = new LinkedHashMap<>();
        rootMappings.put(ROOT_MENU_STOCK_MANAGE, "/stock");
        rootMappings.put(ROOT_MENU_TAKE_STOCK, "/stock/take");
        rootMappings.put(ROOT_MENU_STOCK_ADJUST, "/stock/adjust");
        ROOT_REDIRECT_PREFIX_MAPPING = Collections.unmodifiableMap(rootMappings);

        // 叶子菜单 redirect 优先使用 sys_menu.component；此处仅保留历史兜底（正常叶子均有 component）
        LEAF_REDIRECT_MAPPING = Collections.emptyMap();

        Map<String, String> previewMenuNames = new LinkedHashMap<>();
        previewMenuNames.put("ProductStock", "药品库存");
        previewMenuNames.put("ProductStockLog", "药品库存变动记录");
        previewMenuNames.put("ScTransferOrder", "仓库调拨单");
        previewMenuNames.put("StockWarning", "库存预警");
        previewMenuNames.put("StockCellProduct", "仓位药品管理");
        previewMenuNames.put("TakeStockConfig", "盘点参数设置");
        previewMenuNames.put("PreTakeStockSheet", "预先盘点单管理");
        previewMenuNames.put("TakeStockPlan", "盘点任务管理");
        previewMenuNames.put("TakeStockSheet", "盘点单管理");
        previewMenuNames.put("StockAdjustReason", "库存调整原因");
        previewMenuNames.put("StockAdjustSheet", "库存调整单管理");
        previewMenuNames.put("SaleInSheet", "入库管理");
        previewMenuNames.put("SaleOutSheet", "出库管理");
        previewMenuNames.put("StoreCenterInfo", "仓库信息");
        previewMenuNames.put("Customer", "收货方信息");
        previewMenuNames.put("Supplier", "供应商信息");
        previewMenuNames.put("Member", "会员信息");
        previewMenuNames.put("Shop", "门店信息");
        previewMenuNames.put("PayType", "支付方式");
        previewMenuNames.put("Address", "地址簿");
        previewMenuNames.put("LogisticsCompany", "物流公司");
        previewMenuNames.put("PrintTemplate", "打印模板");
        previewMenuNames.put("ProductCategory", "药品分类");
        previewMenuNames.put("ProductBrand", "药品品牌");
        previewMenuNames.put("ProductProperty", "药品属性");
        previewMenuNames.put("ProductInfo", "药品管理");
        previewMenuNames.put("Menu", "菜单管理");
        previewMenuNames.put("Dept", "部门管理");
        previewMenuNames.put("Role", "角色管理");
        previewMenuNames.put("User", "用户管理");
        previewMenuNames.put("Oplog", "操作日志");
        previewMenuNames.put("SysParameter", "系统参数");
        previewMenuNames.put("SysDataDic", "数据字典");
        previewMenuNames.put("SysTenant", "租户管理");
        previewMenuNames.put("OpenDomain", "开放域");
        previewMenuNames.put("SysGenerateCode", "编号规则");
        previewMenuNames.put("SysNotifyGroup", "消息通知组");
        previewMenuNames.put("UserGroup", "用户组");
        previewMenuNames.put("Qrtz", "定时任务管理");
        previewMenuNames.put("FileBox", "文件箱");
        previewMenuNames.put("Platform", "平台管理");
        previewMenuNames.put("PublishSysNotice", "发布系统通知");
        previewMenuNames.put("MySysNotice", "我的系统通知");
        previewMenuNames.put("SiteMessage", "站内信");
        previewMenuNames.put("MySiteMessage", "我的站内信");
        previewMenuNames.put("MailMessage", "邮件消息");
        PREVIEW_MENU_NAME_MAPPING = Collections.unmodifiableMap(previewMenuNames);
    }

    private static final List<GroupConfig> GROUP_CONFIGS = Collections.unmodifiableList(Arrays.asList(
            new GroupConfig("System", "系统管理", Collections.singleton("System"), menu -> true),
            new GroupConfig("MsgCenter", "消息中心", Collections.singleton("MsgCenter"), menu -> true),
            new GroupConfig("BaseData", "基础信息管理", Collections.singleton("BaseData"), menu -> true),
            new GroupConfig("Product", "药品中心", Collections.singleton("Product"), menu -> true),
            new GroupConfig("StockManage", "库存管理", Collections.singleton(ROOT_MENU_STOCK_MANAGE), menu -> true),
            new GroupConfig("TakeStock", "库存盘点", Collections.singleton(ROOT_MENU_TAKE_STOCK), menu -> true),
            new GroupConfig("StockAdjust", "库存调整", Collections.singleton(ROOT_MENU_STOCK_ADJUST), menu -> true),
            new GroupConfig(
                    GROUP_KEY_IN_OUT_MANAGE,
                    "入库/出库管理",
                    Collections.singleton(ROOT_MENU_SALE),
                    menu -> StringUtils.equalsAny(menu.getName(), "SaleInSheet", "SaleOutSheet")
            )
    ));

    @Override
    public List<SsoJianyouMenuPreviewGroupBo> getMenuPreview(String targetUserId) {
        if (StringUtils.isBlank(targetUserId)) {
            throw new DefaultClientException("星云账号不存在！");
        }

        JdbcTemplate jdbcTemplate = currentTenantJdbcTemplate();
        List<MenuRecord> allMenus = loadAllAvailableMenus(jdbcTemplate);
        if (allMenus.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, MenuRecord> menuMap = allMenus.stream()
                .collect(Collectors.toMap(MenuRecord::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        Set<String> grantedMenuIds = new LinkedHashSet<>(loadGrantedMenuIds(jdbcTemplate, targetUserId));
        if (grantedMenuIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<MenuRecord> visibleLeafMenus = allMenus.stream()
                .filter(item -> grantedMenuIds.contains(item.getId()))
                .filter(this::isVisibleLeafMenu)
                .collect(Collectors.toList());
        if (visibleLeafMenus.isEmpty()) {
            return Collections.emptyList();
        }

        List<SsoJianyouMenuPreviewGroupBo> groups = new ArrayList<>();
        for (GroupConfig groupConfig : GROUP_CONFIGS) {
            SsoJianyouMenuPreviewGroupBo group = new SsoJianyouMenuPreviewGroupBo();
            group.setGroupKey(groupConfig.getGroupKey());
            group.setGroupName(groupConfig.getGroupName());

            for (MenuRecord menu : visibleLeafMenus) {
                String rootMenuName = findRootMenuName(menu, menuMap);
                if (!groupConfig.getRootMenuNames().contains(rootMenuName)) {
                    continue;
                }
                if (!groupConfig.getFilter().test(menu)) {
                    continue;
                }
                String redirect = resolveRedirect(menu, menuMap, rootMenuName);
                if (StringUtils.isBlank(redirect)) {
                    continue;
                }
                SsoJianyouMenuPreviewItemBo item = new SsoJianyouMenuPreviewItemBo();
                item.setMenuKey(menu.getName());
                item.setMenuName(resolvePreviewMenuName(menu));
                item.setRedirect(redirect);
                group.getItems().add(item);
            }

            if (!group.getItems().isEmpty()) {
                groups.add(group);
            }
        }
        return groups;
    }

    private JdbcTemplate currentTenantJdbcTemplate() {
        DataSource dataSource = ApplicationUtil.safeGetBean(DataSource.class);
        if (dataSource == null) {
            throw new DefaultClientException("星云数据源未初始化完成，请稍后重试！");
        }
        return new JdbcTemplate(dataSource);
    }

    private List<MenuRecord> loadAllAvailableMenus(JdbcTemplate jdbcTemplate) {
        String sql = "SELECT id, name, title, parent_id, path, component, display, hidden "
                + "FROM sys_menu WHERE available = 1 ORDER BY code ASC, id ASC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            MenuRecord item = new MenuRecord();
            item.setId(rs.getString("id"));
            item.setName(rs.getString("name"));
            item.setTitle(rs.getString("title"));
            item.setParentId(rs.getString("parent_id"));
            item.setPath(rs.getString("path"));
            item.setComponent(rs.getString("component"));
            item.setDisplay(rs.getInt("display"));
            item.setHidden(rs.getInt("hidden"));
            return item;
        });
    }

    private List<String> loadGrantedMenuIds(JdbcTemplate jdbcTemplate, String targetUserId) {
        String sql = "SELECT DISTINCT rm.menu_id FROM sys_user_role ur "
                + "INNER JOIN sys_role_menu rm ON ur.role_id = rm.role_id "
                + "INNER JOIN sys_menu m ON rm.menu_id = m.id "
                + "WHERE ur.user_id = ? AND m.available = 1 ORDER BY rm.menu_id ASC";
        return jdbcTemplate.query(sql, rs -> {
            List<String> results = new ArrayList<>();
            while (rs.next()) {
                results.add(rs.getString("menu_id"));
            }
            return results;
        }, targetUserId);
    }

    private boolean isVisibleLeafMenu(MenuRecord menu) {
        return StringUtils.isNotBlank(menu.getName())
                && StringUtils.isNotBlank(menu.getComponent())
                && menu.getDisplay() == 1
                && menu.getHidden() == 0;
    }

    private String resolveRedirect(MenuRecord currentMenu, Map<String, MenuRecord> menuMap, String rootMenuName) {
        String pathRedirect = buildRedirectPath(currentMenu, menuMap, rootMenuName);
        String component = normalizeComponentRedirect(currentMenu.getComponent());

        if (StringUtils.isBlank(pathRedirect)) {
            if (StringUtils.isNotBlank(component)) {
                return component;
            }
            String leafRedirect = LEAF_REDIRECT_MAPPING.get(currentMenu.getName());
            if (StringUtils.isNotBlank(leafRedirect)) {
                return leafRedirect;
            }
            return null;
        }
        if (StringUtils.isBlank(component)) {
            return pathRedirect;
        }

        // System / base-data 等：component 与 path 前缀一致，直接用 component（含 /index）
        if (component.equals(pathRedirect) || component.equals(pathRedirect + "/index")) {
            return component;
        }
        if (component.startsWith(pathRedirect + "/")) {
            return component;
        }

        // 库存/销售等 /sc/* component：退回 path 拼接，并按 component 是否带 /index 决定是否追加
        if (component.endsWith("/index")) {
            return pathRedirect + "/index";
        }
        return pathRedirect;
    }

    private String normalizeComponentRedirect(String component) {
        String value = StringUtils.trimToEmpty(component);
        if (StringUtils.isBlank(value)) {
            return null;
        }
        if (!value.startsWith("/")) {
            value = "/" + value;
        }
        return StringUtils.removeEnd(value, "/");
    }

    private String resolvePreviewMenuName(MenuRecord menu) {
        String previewMenuName = PREVIEW_MENU_NAME_MAPPING.get(menu.getName());
        if (StringUtils.isNotBlank(previewMenuName)) {
            return previewMenuName;
        }
        return StringUtils.defaultIfBlank(menu.getTitle(), menu.getName());
    }

    private String buildRedirectPath(MenuRecord currentMenu, Map<String, MenuRecord> menuMap, String rootMenuName) {
        if (currentMenu == null || StringUtils.isBlank(currentMenu.getPath())) {
            return null;
        }

        List<String> segments = new ArrayList<>();
        MenuRecord cursor = currentMenu;
        Set<String> visited = new LinkedHashSet<>();
        while (cursor != null && StringUtils.isNotBlank(cursor.getId()) && visited.add(cursor.getId())) {
            String path = StringUtils.trimToEmpty(cursor.getPath());
            if (StringUtils.isNotBlank(path)) {
                if (StringUtils.equals(cursor.getName(), rootMenuName)) {
                    String normalizedRootPrefix = ROOT_REDIRECT_PREFIX_MAPPING.get(rootMenuName);
                    if (StringUtils.isNotBlank(normalizedRootPrefix)) {
                        segments.add(normalizedRootPrefix);
                    } else {
                        segments.add(normalizePathSegment(path));
                    }
                } else {
                    segments.add(normalizePathSegment(path));
                }
            }
            if (StringUtils.isBlank(cursor.getParentId())) {
                break;
            }
            cursor = menuMap.get(cursor.getParentId());
        }

        if (segments.isEmpty()) {
            return null;
        }

        Collections.reverse(segments);
        String redirect = String.join("", segments).replaceAll("/+", "/");
        if (!redirect.startsWith("/")) {
            redirect = "/" + redirect;
        }
        return StringUtils.removeEnd(redirect, "/");
    }

    private String normalizePathSegment(String path) {
        String value = StringUtils.trimToEmpty(path);
        if (StringUtils.isBlank(value)) {
            return "";
        }
        if (!value.startsWith("/")) {
            value = "/" + value;
        }
        return StringUtils.removeEnd(value, "/");
    }

    private String findRootMenuName(MenuRecord currentMenu, Map<String, MenuRecord> menuMap) {
        MenuRecord cursor = currentMenu;
        Set<String> visited = new LinkedHashSet<>();
        while (cursor != null && StringUtils.isNotBlank(cursor.getId()) && visited.add(cursor.getId())) {
            if (StringUtils.isBlank(cursor.getParentId())) {
                return cursor.getName();
            }
            MenuRecord parent = menuMap.get(cursor.getParentId());
            if (parent == null) {
                return cursor.getName();
            }
            cursor = parent;
        }
        return currentMenu.getName();
    }

    @Data
    private static class MenuRecord {

        private String id;

        private String name;

        private String title;

        private String parentId;

        private String path;

        private String component;

        private Integer display;

        private Integer hidden;
    }

    @Data
    private static class GroupConfig {

        private final String groupKey;

        private final String groupName;

        private final Set<String> rootMenuNames;

        private final Predicate<MenuRecord> filter;
    }
}
