ALTER TABLE `base_data_customer`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `base_data_member`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `base_data_product_brand`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称',
    MODIFY COLUMN `short_name` varchar(40) DEFAULT '' COMMENT '简称';

ALTER TABLE `base_data_product_property`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `base_data_product_property_item`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `base_data_store_center`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `base_data_supplier`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `base_data_pay_type`
    MODIFY COLUMN `name` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称';

ALTER TABLE `base_data_logistics_company`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `base_data_stock_cell`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `dic_city`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `gen_custom_form`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `gen_custom_form_category`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `gen_custom_list`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `gen_custom_list_category`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `gen_custom_selector`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `gen_custom_selector_category`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `gen_data_entity`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `gen_data_entity_category`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `gen_data_obj`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `gen_data_obj_category`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `gen_custom_page_category`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `gen_custom_page`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `settle_in_item`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `settle_out_item`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `sys_data_dic`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `sys_data_dic_category`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `sys_data_dic_item`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `sys_data_permission_model_detail`
    MODIFY COLUMN `name` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称';

ALTER TABLE `sys_dept`
    MODIFY COLUMN `short_name` varchar(40) NOT NULL COMMENT '简称';

ALTER TABLE `sys_generate_code`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `sys_menu`
    MODIFY COLUMN `title` varchar(40) NOT NULL COMMENT '标题';

ALTER TABLE `sys_notify_group`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `sys_role`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `sys_role_category`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `sys_user`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '姓名';

ALTER TABLE `sys_user_group`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `tbl_print_template`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `tbl_shop`
    MODIFY COLUMN `name` varchar(40) NOT NULL COMMENT '名称';

ALTER TABLE `tbl_stock_adjust_reason`
    MODIFY COLUMN `name` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称';
