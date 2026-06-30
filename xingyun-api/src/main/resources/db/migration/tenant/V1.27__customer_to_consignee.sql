UPDATE `sys_menu`
SET `title` = CASE `id`
                 WHEN '2000004' THEN '收货方信息'
                 WHEN '2000004001' THEN '新增收货方'
                 WHEN '2000004002' THEN '修改收货方'
                 WHEN '2000004003' THEN '导入收货方'
                 WHEN '2000004004' THEN '删除收货方'
                 WHEN '4000008' THEN '收货方结算'
                 WHEN '4000009' THEN '收货方费用'
                 WHEN '4000009001' THEN '新增收货方费用单'
                 WHEN '4000009002' THEN '修改收货方费用单'
                 WHEN '4000009003' THEN '删除收货方费用单'
                 WHEN '4000009004' THEN '审核收货方费用单'
                 WHEN '4000009005' THEN '导出收货方费用单'
                 WHEN '4000010' THEN '收货方预收款'
                 WHEN '4000010001' THEN '新增收货方预收款单'
                 WHEN '4000010002' THEN '修改收货方预收款单'
                 WHEN '4000010003' THEN '删除收货方预收款单'
                 WHEN '4000010004' THEN '审核收货方预收款单'
                 WHEN '4000010005' THEN '导出收货方预收款单'
                 WHEN '4000011' THEN '收货方对账'
                 WHEN '4000011001' THEN '新增收货方对账单'
                 WHEN '4000011002' THEN '修改收货方对账单'
                 WHEN '4000011003' THEN '删除收货方对账单'
                 WHEN '4000011004' THEN '审核收货方对账单'
                 WHEN '4000011005' THEN '导出收货方对账单'
                 WHEN '4000012' THEN '收货方结算'
                 WHEN '4000012001' THEN '新增收货方结算单'
                 WHEN '4000012002' THEN '修改收货方结算单'
                 WHEN '4000012003' THEN '删除收货方结算单'
                 WHEN '4000012004' THEN '审核收货方结算单'
                 WHEN '4000012005' THEN '导出收货方结算单'
                 ELSE `title`
    END,
    `update_by` = '系统管理员',
    `update_by_id` = '1',
    `update_time` = NOW()
WHERE `id` IN (
               '2000004', '2000004001', '2000004002', '2000004003', '2000004004',
               '4000008', '4000009', '4000009001', '4000009002', '4000009003', '4000009004', '4000009005',
               '4000010', '4000010001', '4000010002', '4000010003', '4000010004', '4000010005',
               '4000011', '4000011001', '4000011002', '4000011003', '4000011004', '4000011005',
               '4000012', '4000012001', '4000012002', '4000012003', '4000012004', '4000012005'
    );

UPDATE `sys_generate_code`
SET `name` = CASE `id`
                WHEN 5 THEN '收货方编号'
                WHEN 304 THEN '收货方对账单号'
                WHEN 305 THEN '收货方费用单号'
                WHEN 306 THEN '收货方预付款单号'
                WHEN 307 THEN '收货方结算单号'
                ELSE `name`
    END
WHERE `id` IN (5, 304, 305, 306, 307);

UPDATE `tbl_print_template`
SET `template_json` = REPLACE(`template_json`, '客户', '收货方'),
    `demo_data` = REPLACE(`demo_data`, '客户', '收货方'),
    `update_by` = '系统管理员',
    `update_by_id` = '1',
    `update_time` = NOW()
WHERE `template_json` LIKE '%客户%'
   OR `demo_data` LIKE '%客户%';

ALTER TABLE `base_data_customer`
    COMMENT = '收货方';

ALTER TABLE `customer_settle_check_sheet`
    MODIFY COLUMN `customer_id` varchar(32) NOT NULL COMMENT '收货方ID',
    COMMENT = '收货方对账单';

ALTER TABLE `customer_settle_check_sheet_detail`
    COMMENT = '收货方对账单明细';

ALTER TABLE `customer_settle_fee_sheet`
    MODIFY COLUMN `customer_id` varchar(32) NOT NULL COMMENT '收货方ID',
    COMMENT = '收货方费用单';

ALTER TABLE `customer_settle_fee_sheet_detail`
    COMMENT = '收货方费用单明细';

ALTER TABLE `customer_settle_pre_sheet`
    MODIFY COLUMN `customer_id` varchar(32) NOT NULL COMMENT '收货方ID',
    COMMENT = '收货方预付款单';

ALTER TABLE `customer_settle_pre_sheet_detail`
    COMMENT = '收货方预付款单明细';

ALTER TABLE `customer_settle_sheet`
    MODIFY COLUMN `customer_id` varchar(32) NOT NULL COMMENT '收货方ID',
    COMMENT = '收货方结算单';

ALTER TABLE `customer_settle_sheet_detail`
    COMMENT = '收货方结算单明细';

ALTER TABLE `tbl_sale_order`
    MODIFY COLUMN `customer_id` varchar(32) NOT NULL COMMENT '收货方ID';

ALTER TABLE `tbl_sale_out_sheet`
    MODIFY COLUMN `customer_id` varchar(32) NULL DEFAULT NULL COMMENT '收货方ID';

ALTER TABLE `tbl_sale_return`
    MODIFY COLUMN `customer_id` varchar(32) NOT NULL COMMENT '收货方ID';
