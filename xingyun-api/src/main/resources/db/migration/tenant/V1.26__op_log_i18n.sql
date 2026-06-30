ALTER TABLE `op_logs`
  ADD COLUMN `name_key` varchar(200) NULL DEFAULT NULL COMMENT '日志名称标识' AFTER `name`,
  ADD COLUMN `name_template` varchar(500) NULL DEFAULT NULL COMMENT '日志名称模板' AFTER `name_key`,
  ADD COLUMN `name_params` longtext NULL COMMENT '日志名称参数(JSON)' AFTER `name_template`;
