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

    private static final Map<String, String> ROOT_REDIRECT_PREFIX_MAPPING;

    private static final Map<String, String> LEAF_REDIRECT_MAPPING;

    static {
        Map<String, String> rootMappings = new LinkedHashMap<>();
        rootMappings.put(ROOT_MENU_STOCK_MANAGE, "/stock");
        rootMappings.put(ROOT_MENU_TAKE_STOCK, "/stock/take");
        rootMappings.put(ROOT_MENU_STOCK_ADJUST, "/stock/adjust");
        ROOT_REDIRECT_PREFIX_MAPPING = Collections.unmodifiableMap(rootMappings);

        Map<String, String> leafMappings = new LinkedHashMap<>();
        leafMappings.put("TakeStockConfig", "/stock/take/config");
        leafMappings.put("PreTakeStockSheet", "/stock/take/pre");
        leafMappings.put("TakeStockPlan", "/stock/take/plan");
        leafMappings.put("TakeStockSheet", "/stock/take/sheet");
        leafMappings.put("StockAdjustReason", "/stock/adjust/reason");
        leafMappings.put("StockAdjustSheet", "/stock/stock-adjust");
        LEAF_REDIRECT_MAPPING = Collections.unmodifiableMap(leafMappings);
    }

    private static final List<GroupConfig> GROUP_CONFIGS = Collections.unmodifiableList(Arrays.asList(
            new GroupConfig("System", "系统管理", "System", menu -> true),
            new GroupConfig("MsgCenter", "消息中心", "MsgCenter", menu -> true),
            new GroupConfig("BaseData", "基础信息管理", "BaseData", menu -> true),
            new GroupConfig("Product", "商品中心", "Product", menu -> true),
            new GroupConfig("StockManage", "库存管理", ROOT_MENU_STOCK_MANAGE, menu -> true),
            new GroupConfig("TakeStock", "库存盘点", ROOT_MENU_TAKE_STOCK, menu -> true),
            new GroupConfig("StockAdjust", "库存调整", ROOT_MENU_STOCK_ADJUST, menu -> true),
            new GroupConfig("SaleOutSheet", "销售管理-销售出库管理", "Sale",
                    menu -> StringUtils.equals(menu.getName(), "SaleOutSheet"))
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
            group.setGroupName(StringUtils.equals(groupConfig.getGroupKey(), "SaleOutSheet")
                    ? "销售管理"
                    : groupConfig.getGroupName());

            for (MenuRecord menu : visibleLeafMenus) {
                String rootMenuName = findRootMenuName(menu, menuMap);
                if (!StringUtils.equals(rootMenuName, groupConfig.getRootMenuName())) {
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
                item.setMenuName(StringUtils.defaultIfBlank(menu.getTitle(), menu.getName()));
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
        String leafRedirect = LEAF_REDIRECT_MAPPING.get(currentMenu.getName());
        if (StringUtils.isNotBlank(leafRedirect)) {
            return leafRedirect;
        }
        return buildRedirectPath(currentMenu, menuMap, rootMenuName);
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

        private final String rootMenuName;

        private final Predicate<MenuRecord> filter;
    }
}
