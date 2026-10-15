-- 金融日报表增加简洁版字段：外部推送时可传 data.brief（今日要点/摘要），
-- 订阅推送（钉钉/邮件）优先取该字段；为空则后端自动截取正文前 100 字
ALTER TABLE `day_info`
  ADD COLUMN `brief` TEXT NULL COMMENT '简洁版内容，订阅推送用；空则后端自动截取正文' AFTER `content`;
