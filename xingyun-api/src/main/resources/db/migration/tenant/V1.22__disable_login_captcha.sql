UPDATE `sys_parameter`
SET `pm_value` = 'false',
    `update_by` = 'SYSTEM',
    `update_by_id` = '1',
    `update_time` = NOW()
WHERE `pm_key` = 'login-captcha.enabled';

INSERT INTO `sys_parameter` (
    `id`, `pm_key`, `pm_value`, `description`,
    `create_by`, `create_by_id`, `create_time`,
    `update_by`, `update_by_id`, `update_time`
)
SELECT
    13, 'login-captcha.enabled', 'false',
    'Disable login captcha by default.',
    'SYSTEM', '1', NOW(),
    'SYSTEM', '1', NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM `sys_parameter`
    WHERE `pm_key` = 'login-captcha.enabled'
);
