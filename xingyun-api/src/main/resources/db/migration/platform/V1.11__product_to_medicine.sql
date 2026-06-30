UPDATE `sys_module`
SET `name` = REPLACE(`name`, '商品', '药品'),
    `description` = REPLACE(`description`, '商品', '药品'),
    `update_by` = '系统管理员',
    `update_by_id` = '1',
    `update_time` = NOW()
WHERE `name` LIKE '%商品%'
   OR `description` LIKE '%商品%';
