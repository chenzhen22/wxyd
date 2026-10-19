-- V22: 令牌消耗统计补充请求/应答数据（详情弹框用）
-- 全新安装由 DataInitializer 启动自检自动补列（本文件为手工执行备份）

ALTER TABLE `wb_gateway_usage`
  ADD COLUMN `request_data` MEDIUMTEXT NULL COMMENT '客户端请求数据 JSON（超长截断）',
  ADD COLUMN `response_data` MEDIUMTEXT NULL COMMENT '应答数据 JSON（聚合后的内容/工具调用/usage，超长截断）';
