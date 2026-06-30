ALTER TABLE `sys_open_domain`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `sys_module`
    MODIFY COLUMN `name` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称';
