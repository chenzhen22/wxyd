-- V27: wb_gateway_usage 补 account 列（账号/来源）
-- 调用明细大字段（request_data/response_data）不再入库，改存 /apps/shareFile/gateway/{id}.json；
-- DB 旧列保留用于存量记录回退，新记录不再写入。
-- 全新安装由 DataInitializer 启动自检自动补列（本文件为手工执行备份）

ALTER TABLE `wb_gateway_usage`
  ADD COLUMN `account` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '服务本请求的账号/来源';
