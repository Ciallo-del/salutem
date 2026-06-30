UPDATE `sys_menu`
SET `title` = REPLACE(`title`, '商品', '药品'),
    `update_by` = '系统管理员',
    `update_by_id` = '1',
    `update_time` = NOW()
WHERE `title` LIKE '%商品%';

UPDATE `sys_generate_code`
SET `name` = REPLACE(`name`, '商品', '药品')
WHERE `name` LIKE '%商品%';

UPDATE `tbl_print_template`
SET `template_json` = REPLACE(`template_json`, '商品', '药品'),
    `demo_data` = REPLACE(`demo_data`, '商品', '药品'),
    `update_by` = '系统管理员',
    `update_by_id` = '1',
    `update_time` = NOW()
WHERE `template_json` LIKE '%商品%'
   OR `demo_data` LIKE '%商品%';

UPDATE `tbl_print_template_comp`
SET `comp_json` = REPLACE(`comp_json`, '商品', '药品')
WHERE `comp_json` LIKE '%商品%';

ALTER TABLE `base_data_product`
    MODIFY COLUMN `product_type` tinyint(3) NOT NULL DEFAULT 1 COMMENT '药品类型',
    COMMENT = '药品';

ALTER TABLE `base_data_product_code`
    COMMENT = '药品编号';

ALTER TABLE `base_data_product_bundle`
    MODIFY COLUMN `main_product_id` varchar(32) NOT NULL COMMENT '主药品ID',
    COMMENT = '组合药品';

ALTER TABLE `base_data_stock_cell_product`
    MODIFY COLUMN `product_id` varchar(32) NOT NULL COMMENT '药品ID',
    COMMENT = '仓位药品';

ALTER TABLE `tbl_product_stock_warning`
    MODIFY COLUMN `product_id` varchar(20) NOT NULL COMMENT '药品ID';

ALTER TABLE `tbl_purchase_order`
    MODIFY COLUMN `total_num` decimal(24, 8) NOT NULL DEFAULT 0.00000000 COMMENT '药品数量';

ALTER TABLE `tbl_purchase_order_detail_bundle`
    MODIFY COLUMN `main_product_id` varchar(32) NOT NULL COMMENT '组合药品ID',
    MODIFY COLUMN `order_num` decimal(24, 8) NOT NULL DEFAULT 0.00000000 COMMENT '组合药品数量',
    COMMENT = '采购单组合药品明细';

ALTER TABLE `tbl_purchase_order_detail_bundle_form`
    MODIFY COLUMN `main_product_id` varchar(32) NOT NULL COMMENT '组合药品ID',
    MODIFY COLUMN `order_num` decimal(24, 8) NOT NULL DEFAULT 0.00000000 COMMENT '组合药品数量',
    COMMENT = '采购单组合药品明细';

ALTER TABLE `tbl_purchase_return`
    MODIFY COLUMN `total_num` decimal(24, 8) NOT NULL DEFAULT 0.00000000 COMMENT '药品数量';

ALTER TABLE `tbl_receive_sheet`
    MODIFY COLUMN `total_num` decimal(24, 8) NOT NULL DEFAULT 0.00000000 COMMENT '药品数量';

ALTER TABLE `tbl_receive_sheet_detail_bundle`
    MODIFY COLUMN `main_product_id` varchar(32) NOT NULL COMMENT '组合药品ID',
    MODIFY COLUMN `order_num` decimal(24, 8) NOT NULL DEFAULT 0.00000000 COMMENT '组合药品数量',
    COMMENT = '采购收货单组合药品明细';

ALTER TABLE `tbl_retail_out_sheet`
    MODIFY COLUMN `total_num` decimal(24, 8) NOT NULL DEFAULT 0.00000000 COMMENT '药品数量';

ALTER TABLE `tbl_retail_out_sheet_detail_bundle`
    MODIFY COLUMN `main_product_id` varchar(32) NOT NULL COMMENT '组合药品ID',
    MODIFY COLUMN `order_num` decimal(24, 8) NOT NULL DEFAULT 0.00000000 COMMENT '组合药品数量',
    COMMENT = '零售出库单组合药品明细';

ALTER TABLE `tbl_retail_return`
    MODIFY COLUMN `total_num` decimal(24, 8) NOT NULL DEFAULT 0.00000000 COMMENT '药品数量';

ALTER TABLE `tbl_sale_order`
    MODIFY COLUMN `total_num` decimal(24, 8) NOT NULL DEFAULT 0.00000000 COMMENT '药品数量';

ALTER TABLE `tbl_sale_order_detail_bundle`
    MODIFY COLUMN `main_product_id` varchar(32) NOT NULL COMMENT '组合药品ID',
    MODIFY COLUMN `order_num` decimal(24, 8) NOT NULL DEFAULT 0.00000000 COMMENT '组合药品数量',
    COMMENT = '销售单组合药品明细';

ALTER TABLE `tbl_sale_out_sheet`
    MODIFY COLUMN `total_num` decimal(24, 8) NOT NULL DEFAULT 0.00000000 COMMENT '药品数量';

ALTER TABLE `tbl_sale_out_sheet_detail_bundle`
    MODIFY COLUMN `main_product_id` varchar(32) NOT NULL COMMENT '组合药品ID',
    MODIFY COLUMN `order_num` decimal(24, 8) NOT NULL DEFAULT 0.00000000 COMMENT '组合药品数量',
    COMMENT = '销售出库单组合药品明细';

ALTER TABLE `tbl_sale_return`
    MODIFY COLUMN `total_num` decimal(24, 8) NOT NULL DEFAULT 0.00000000 COMMENT '药品数量';

ALTER TABLE `tbl_stock_adjust_sheet_detail`
    MODIFY COLUMN `product_id` varchar(32) NOT NULL COMMENT '药品ID';

ALTER TABLE `tbl_sc_transfer_order_detail`
    MODIFY COLUMN `product_id` varchar(32) NOT NULL COMMENT '药品ID';
