UPDATE `sys_module` SET `description` = '维护企业、系统基础数据，包括部门、岗位、角色和参数等。' WHERE `id` = 2;
ALTER TABLE `tenant`
    ADD COLUMN `is_platform` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否为平台管理租户' AFTER `jdbc_password`;
UPDATE `tenant` SET `is_platform` = 1 WHERE `id` = 1000;
ALTER TABLE `sys_module`
    ADD COLUMN `is_platform` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否为平台功能模块' AFTER `description`;
UPDATE `sys_module` SET `is_platform` = 1 WHERE `id` IN (1, 12, 14);