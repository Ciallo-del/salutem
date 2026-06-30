ALTER TABLE base_data_product
  ADD COLUMN production_time date NULL COMMENT '生产时间' AFTER volume,
  ADD COLUMN deadline_time date NULL COMMENT '截止时间' AFTER production_time;

UPDATE base_data_product
SET production_time = CURDATE(), deadline_time = CURDATE()
WHERE production_time IS NULL;

ALTER TABLE base_data_product
  MODIFY COLUMN production_time date NOT NULL COMMENT '生产时间',
  MODIFY COLUMN deadline_time date NOT NULL COMMENT '截止时间';
