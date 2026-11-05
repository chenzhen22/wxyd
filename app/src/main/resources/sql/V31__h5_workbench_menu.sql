-- H5 工作台改版（V4.0）：留言板/群聊从底部 tab 改为工作台卡片
-- 存量库执行一次（DataInitializer 的 seed 是 INSERT IGNORE，不会更新已有行；新库跳过）。
-- 薄荷账仅保留底部 tab 入口，不在工作台放卡片（bohe 系菜单项已从 seed 移除；存量库如已插入请手工软删）。
UPDATE `menu_item` SET `hash_url`='#/message', `sort_no`=15 WHERE `menu_key`='message' AND `platform`='h5' AND `deleted`=0;
UPDATE `menu_item` SET `hash_url`='#/group',   `sort_no`=25 WHERE `menu_key`='group'   AND `platform`='h5' AND `deleted`=0;
-- 若之前执行过含 bohe 三行的旧版 V31，这里幂等清理（软删，保留行占位防 seed 复活——seed 已无这三行）
UPDATE `menu_item` SET `deleted`=1 WHERE `menu_key` IN ('bohe','boheStats','boheFriends') AND `platform`='h5';
