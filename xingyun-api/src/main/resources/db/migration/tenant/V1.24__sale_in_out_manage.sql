UPDATE `sys_menu`
SET `title` = '入库/出库管理'
WHERE `id` = '2003';

UPDATE `sys_menu`
SET `title` = '出库管理'
WHERE `id` = '2003003';

UPDATE `sys_menu`
SET `code` = '2003005',
    `name` = 'SaleInSheet',
    `title` = '入库管理',
    `icon` = NULL,
    `component_type` = 0,
    `component` = '/sc/purchase/receive/index',
    `request_param` = NULL,
    `parent_id` = '2003',
    `sys_module_id` = '6',
    `path` = '/in',
    `no_cache` = 0,
    `display` = 1,
    `hidden` = 0,
    `permission` = 'purchase:receive:query',
    `is_special` = 1,
    `available` = 1,
    `description` = '',
    `update_by` = '系统管理员',
    `update_by_id` = '1',
    `update_time` = NOW()
WHERE `id` = '2003005';

INSERT INTO `sys_menu` (
    `id`, `code`, `name`, `title`, `icon`, `component_type`, `component`, `request_param`,
    `parent_id`, `sys_module_id`, `path`, `no_cache`, `display`, `hidden`, `permission`,
    `is_special`, `available`, `description`, `create_by`, `create_by_id`, `create_time`,
    `update_by`, `update_by_id`, `update_time`
)
SELECT
    '2003005', '2003005', 'SaleInSheet', '入库管理', NULL, 0, '/sc/purchase/receive/index', NULL,
    '2003', '6', '/in', 0, 1, 0, 'purchase:receive:query',
    1, 1, '', '系统管理员', '1', NOW(),
    '系统管理员', '1', NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM `sys_menu`
    WHERE `id` = '2003005'
);

INSERT IGNORE INTO `sys_role_menu` (`id`, `role_id`, `menu_id`)
SELECT REPLACE(UUID(), '-', ''), `role_id`, '2003005'
FROM `sys_role_menu`
WHERE `menu_id` = '2002003';
