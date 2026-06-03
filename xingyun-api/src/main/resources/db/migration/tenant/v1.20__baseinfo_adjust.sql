ALTER TABLE `base_data_product`
    MODIFY COLUMN `sku_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT 'SKU' AFTER `short_name`,
    MODIFY COLUMN `brand_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '品牌ID' AFTER `category_id`;
INSERT INTO `sys_menu` (`id`, `code`, `name`, `title`, `icon`, `component_type`, `component`, `request_param`, `parent_id`, `sys_module_id`, `path`, `no_cache`, `display`, `hidden`, `permission`, `is_special`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('1000002004', '1000002004', '', '删除部门', NULL, 0, '', NULL, '1000002', '2', '', 0, 2, 0, 'system:dept:delete', 1, 1, '', '系统管理员', '1', '2021-06-27 01:33:47', '系统管理员', '1', '2025-01-19 18:36:07');
INSERT INTO `sys_menu` (`id`, `code`, `name`, `title`, `icon`, `component_type`, `component`, `request_param`, `parent_id`, `sys_module_id`, `path`, `no_cache`, `display`, `hidden`, `permission`, `is_special`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('1000004004', '1000004004', '', '删除角色', NULL, 0, '', NULL, '1000004', '2', '', 0, 2, 0, 'system:role:delete', 1, 1, '', '系统管理员', '1', '2021-06-30 00:32:45', '系统管理员', '1', '2025-01-19 18:36:08');

ALTER TABLE `sys_role`
DROP INDEX `code`,
DROP INDEX `name`,
ADD INDEX `code`(`code`) USING BTREE,
ADD INDEX `name`(`name`) USING BTREE;
INSERT INTO `sys_menu` (`id`, `code`, `name`, `title`, `icon`, `component_type`, `component`, `request_param`, `parent_id`, `sys_module_id`, `path`, `no_cache`, `display`, `hidden`, `permission`, `is_special`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('1000005004', '1000005004', '', '删除用户', NULL, 0, '', NULL, '1000005', '2', '', 0, 2, 0, 'system:user:delete', 1, 1, '', '系统管理员', '1', '2021-06-30 00:32:45', '系统管理员', '1', '2025-01-19 18:36:09');
ALTER TABLE `sys_user`
DROP INDEX `code`,
DROP INDEX `username`,
ADD INDEX `code`(`code`) USING BTREE,
ADD INDEX `username`(`username`) USING BTREE;

ALTER TABLE `sys_notify_group`
DROP INDEX `name`,
ADD INDEX `name`(`name`) USING BTREE;
INSERT INTO `sys_menu` (`id`, `code`, `name`, `title`, `icon`, `component_type`, `component`, `request_param`, `parent_id`, `sys_module_id`, `path`, `no_cache`, `display`, `hidden`, `permission`, `is_special`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('1000014003', '1000014003', '', '删除消息通知组', NULL, 0, '', NULL, '1000014', '8', '', 0, 2, 0, 'system:notify-group:delete', 1, 1, '', '系统管理员', '1', '2021-05-12 22:50:27', '系统管理员', '1', '2025-01-19 18:36:11');
INSERT INTO `sys_menu` (`id`, `code`, `name`, `title`, `icon`, `component_type`, `component`, `request_param`, `parent_id`, `sys_module_id`, `path`, `no_cache`, `display`, `hidden`, `permission`, `is_special`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('1000015003', '1000015003', '', '删除用户分组', NULL, 0, '', NULL, '1000015', '8', '', 0, 2, 0, 'system:user-group:delete', 1, 1, '', '系统管理员', '1', '2021-05-12 22:50:27', '系统管理员', '1', '2025-01-19 18:36:11');
ALTER TABLE `sys_user_group`
DROP INDEX `name`,
DROP INDEX `code`,
ADD INDEX `name`(`name`) USING BTREE,
ADD INDEX `code`(`code`) USING BTREE;
INSERT INTO `sys_menu` (`id`, `code`, `name`, `title`, `icon`, `component_type`, `component`, `request_param`, `parent_id`, `sys_module_id`, `path`, `no_cache`, `display`, `hidden`, `permission`, `is_special`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2000002004', '2000002004', '', '删除仓库', NULL, 0, '', NULL, '2000002', '3', '', 0, 2, 0, 'base-data:store-center:delete', 1, 1, '', '系统管理员', '1', '2021-05-12 23:23:33', '系统管理员', '1', '2021-07-04 00:34:23');
ALTER TABLE `base_data_store_center`
DROP INDEX `code`,
ADD INDEX `code`(`code`) USING BTREE;
ALTER TABLE `base_data_customer`
DROP INDEX `code`,
ADD INDEX `code`(`code`) USING BTREE;
INSERT INTO `sys_menu` (`id`, `code`, `name`, `title`, `icon`, `component_type`, `component`, `request_param`, `parent_id`, `sys_module_id`, `path`, `no_cache`, `display`, `hidden`, `permission`, `is_special`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2000004004', '2000004004', '', '删除客户', NULL, 0, '', NULL, '2000004', '3', '', 0, 2, 0, 'base-data:customer:delete', 1, 1, '', '系统管理员', '1', '2021-05-12 23:23:33', '系统管理员', '1', '2021-07-04 00:34:23');
INSERT INTO `sys_menu` (`id`, `code`, `name`, `title`, `icon`, `component_type`, `component`, `request_param`, `parent_id`, `sys_module_id`, `path`, `no_cache`, `display`, `hidden`, `permission`, `is_special`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2000005004', '2000005004', '', '删除供应商', NULL, 0, '', NULL, '2000005', '3', '', 0, 2, 0, 'base-data:supplier:delete', 1, 1, '', '系统管理员', '1', '2021-05-12 23:23:33', '系统管理员', '1', '2021-07-04 00:34:23');
ALTER TABLE `base_data_supplier`
DROP INDEX `code`,
ADD INDEX `code`(`code`) USING BTREE;
INSERT INTO `sys_menu` (`id`, `code`, `name`, `title`, `icon`, `component_type`, `component`, `request_param`, `parent_id`, `sys_module_id`, `path`, `no_cache`, `display`, `hidden`, `permission`, `is_special`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2000006004', '2000006004', '', '删除会员', NULL, 0, '', NULL, '2000006', '3', '', 0, 2, 0, 'base-data:member:delete', 1, 1, '', '系统管理员', '1', '2021-05-12 23:23:33', '系统管理员', '1', '2021-07-04 00:34:23');
ALTER TABLE `base_data_member`
DROP INDEX `code`,
DROP INDEX `telephone`,
ADD INDEX `code`(`code`) USING BTREE,
ADD INDEX `telephone`(`telephone`) USING BTREE;
INSERT INTO `sys_menu` (`id`, `code`, `name`, `title`, `icon`, `component_type`, `component`, `request_param`, `parent_id`, `sys_module_id`, `path`, `no_cache`, `display`, `hidden`, `permission`, `is_special`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2000007004', '2000007004', '', '删除门店', NULL, 0, '', NULL, '2000007', '3', '', 0, 2, 0, 'base-data:shop:delete', 1, 1, '', '系统管理员', '1', '2021-05-12 23:23:33', '系统管理员', '1', '2021-07-04 00:34:23');
ALTER TABLE `tbl_shop`
DROP INDEX `code`,
ADD INDEX `code`(`code`) USING BTREE;
INSERT INTO `sys_menu` (`id`, `code`, `name`, `title`, `icon`, `component_type`, `component`, `request_param`, `parent_id`, `sys_module_id`, `path`, `no_cache`, `display`, `hidden`, `permission`, `is_special`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2000008003', '2000008003', '', '删除支付方式', NULL, 0, '', NULL, '2000008', '3', '', 0, 2, 0, 'base-data:pay-type:delete', 1, 1, '', '系统管理员', '1', '2021-05-12 23:23:33', '系统管理员', '1', '2021-07-04 00:34:23');
ALTER TABLE `base_data_pay_type`
DROP INDEX `code`,
ADD INDEX `code`(`code`) USING BTREE;
INSERT INTO `sys_menu` (`id`, `code`, `name`, `title`, `icon`, `component_type`, `component`, `request_param`, `parent_id`, `sys_module_id`, `path`, `no_cache`, `display`, `hidden`, `permission`, `is_special`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2000009005', '2000009005', '', '删除地址', NULL, 0, '', NULL, '2000009', '15', '', 0, 2, 0, 'base-data:address:delete', 1, 1, '', '系统管理员', '1', '2021-05-12 23:23:33', '系统管理员', '1', '2021-07-04 00:34:23');
ALTER TABLE `base_data_logistics_company`
DROP INDEX `code`,
ADD INDEX `code`(`code`) USING BTREE;
INSERT INTO `sys_menu` (`id`, `code`, `name`, `title`, `icon`, `component_type`, `component`, `request_param`, `parent_id`, `sys_module_id`, `path`, `no_cache`, `display`, `hidden`, `permission`, `is_special`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2000010003', '2000010003', '', '删除物流公司', NULL, 0, '', NULL, '2000010', '15', '', 0, 2, 0, 'base-data:logistics-company:delete', 1, 1, '', '系统管理员', '1', '2021-05-12 23:23:33', '系统管理员', '1', '2021-07-04 00:34:23');
ALTER TABLE `base_data_product_category`
DROP INDEX `code`,
ADD INDEX `code`(`code`) USING BTREE,
ADD INDEX `name`(`name`) USING BTREE;
INSERT INTO `sys_menu` (`id`, `code`, `name`, `title`, `icon`, `component_type`, `component`, `request_param`, `parent_id`, `sys_module_id`, `path`, `no_cache`, `display`, `hidden`, `permission`, `is_special`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2001001004', '2001001004', '', '删除分类', NULL, 0, '', NULL, '2001001', '4', '', 0, 2, 0, 'base-data:product:category:delete', 1, 1, '', '系统管理员', '1', '2021-05-12 23:23:33', '系统管理员', '1', '2021-07-04 00:34:23');
ALTER TABLE `base_data_product_brand`
DROP INDEX `code`,
ADD INDEX `code`(`code`) USING BTREE,
ADD INDEX `name`(`name`) USING BTREE;
INSERT INTO `sys_menu` (`id`, `code`, `name`, `title`, `icon`, `component_type`, `component`, `request_param`, `parent_id`, `sys_module_id`, `path`, `no_cache`, `display`, `hidden`, `permission`, `is_special`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2001002004', '2001002004', '', '删除品牌', NULL, 0, '', NULL, '2001002', '4', '', 0, 2, 0, 'base-data:product:brand:delete', 1, 1, '', '系统管理员', '1', '2021-05-12 23:23:33', '系统管理员', '1', '2021-07-04 00:34:23');
ALTER TABLE `base_data_product_property`
    ADD INDEX `code`(`code`) USING BTREE,
ADD INDEX `name`(`name`) USING BTREE;
ALTER TABLE `base_data_product_property_item`
DROP INDEX `property_id`,
ADD INDEX `property_id`(`property_id`, `code`) USING BTREE,
ADD INDEX `property_id2`(`name`, `property_id`) USING BTREE;

INSERT INTO `sys_menu` (`id`, `code`, `name`, `title`, `icon`, `component_type`, `component`, `request_param`, `parent_id`, `sys_module_id`, `path`, `no_cache`, `display`, `hidden`, `permission`, `is_special`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2001004006', '2001004006', '', '删除属性', NULL, 0, '', NULL, '2001004', '4', '', 0, 2, 0, 'base-data:product:property:delete', 1, 1, '', '系统管理员', '1', '2021-05-12 23:23:33', '系统管理员', '1', '2021-07-04 00:34:23');
INSERT INTO `sys_menu` (`id`, `code`, `name`, `title`, `icon`, `component_type`, `component`, `request_param`, `parent_id`, `sys_module_id`, `path`, `no_cache`, `display`, `hidden`, `permission`, `is_special`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2001004007', '2001004007', '', '删除属性值', NULL, 0, '', NULL, '2001004', '4', '', 0, 2, 0, 'base-data:product:property-item:delete', 1, 1, '', '系统管理员', '1', '2021-05-12 23:23:33', '系统管理员', '1', '2021-07-04 00:34:23');
ALTER TABLE `base_data_product`
DROP INDEX `code`,
DROP INDEX `sku_code`,
ADD INDEX `code`(`code`) USING BTREE,
ADD INDEX `sku_code`(`sku_code`) USING BTREE;
INSERT INTO `sys_menu` (`id`, `code`, `name`, `title`, `icon`, `component_type`, `component`, `request_param`, `parent_id`, `sys_module_id`, `path`, `no_cache`, `display`, `hidden`, `permission`, `is_special`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('2001005004', '2001005004', '', '删除商品', NULL, 0, '', NULL, '2001005', '4', '', 0, 2, 0, 'base-data:product:info:delete', 1, 1, '', '系统管理员', '1', '2021-05-12 23:23:33', '系统管理员', '1', '2021-07-04 00:34:23');
ALTER TABLE `tbl_stock_adjust_reason`
DROP INDEX `code`,
ADD INDEX `code`(`code`) USING BTREE;
INSERT INTO `sys_menu` (`id`, `code`, `name`, `title`, `icon`, `component_type`, `component`, `request_param`, `parent_id`, `sys_module_id`, `path`, `no_cache`, `display`, `hidden`, `permission`, `is_special`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('3000005002003', '3000005002003', '', '删除库存调整原因', NULL, 0, '', NULL, '3000005002', '10', '', 0, 2, 0, 'stock:adjust:reason:delete', 1, 1, '', '系统管理员', '1', '2021-05-12 22:50:27', '系统管理员', '1', '2021-07-04 00:34:23');
ALTER TABLE `settle_in_item`
DROP INDEX `code`,
ADD INDEX `code`(`code`) USING BTREE;
ALTER TABLE `settle_out_item`
DROP INDEX `code`,
ADD INDEX `code`(`code`) USING BTREE;
INSERT INTO `sys_menu` (`id`, `code`, `name`, `title`, `icon`, `component_type`, `component`, `request_param`, `parent_id`, `sys_module_id`, `path`, `no_cache`, `display`, `hidden`, `permission`, `is_special`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('4000001004', '4000001004', '', '删除收入项目', NULL, 0, '', NULL, '4000001', '11', '', 0, 2, 0, 'settle:in-item:delete', 1, 1, '', '系统管理员', '1', '2021-05-12 10:53:45', '系统管理员', '1', '2021-07-04 00:34:23');
INSERT INTO `sys_menu` (`id`, `code`, `name`, `title`, `icon`, `component_type`, `component`, `request_param`, `parent_id`, `sys_module_id`, `path`, `no_cache`, `display`, `hidden`, `permission`, `is_special`, `available`, `description`, `create_by`, `create_by_id`, `create_time`, `update_by`, `update_by_id`, `update_time`) VALUES ('4000002004', '4000002004', '', '删除支出项目', NULL, 0, '', NULL, '4000002', '11', '', 0, 2, 0, 'settle:out-item:delete', 1, 1, '', '系统管理员', '1', '2021-05-12 10:53:45', '系统管理员', '1', '2021-07-04 00:34:23');
UPDATE `sys_menu` SET `parent_id` = '1001' WHERE `id` = '9000';
UPDATE `sys_menu` SET `icon` = NULL WHERE `id` = '9000';
UPDATE `sys_menu` SET `parent_id` = '1001' WHERE `id` = '1000001';

UPDATE `sys_menu` SET `sys_module_id` = '1' WHERE `id` IN (
    '1000001',
    '1000001001',
    '1000001002',
    '1000001003',
    '9000',
    '9000001',
    '9000002',
    '9000003',
    '9000004',
    '9000005',
    '9000008'
);

UPDATE `sys_menu` SET `sys_module_id` = '12' WHERE `id` IN (
    '9000',
    '9000001',
    '9000002',
    '9000003',
    '9000004',
    '9000005',
    '9000008'
);

UPDATE `sys_menu` SET `parent_id` = '1001' WHERE `id` = '1000007';
UPDATE `sys_menu` SET `parent_id` = '1001' WHERE `id` = '1000010';
DELETE FROM `sys_menu` WHERE `id` IN (
    '9000002',
    '9000003',
    '9000004',
    '9000005',
    '9000008'
);