-- H5 工作台改版（V4.0）：留言板/群聊从底部 tab 改为工作台卡片，新增薄荷账菜单
-- 存量库执行一次（DataInitializer 的 seed 是 INSERT IGNORE，不会更新已有行；新库跳过）。
UPDATE `menu_item` SET `hash_url`='#/message', `sort_no`=15 WHERE `menu_key`='message' AND `platform`='h5' AND `deleted`=0;
UPDATE `menu_item` SET `hash_url`='#/group',   `sort_no`=25 WHERE `menu_key`='group'   AND `platform`='h5' AND `deleted`=0;
INSERT IGNORE INTO `menu_item` (`menu_key`,`platform`,`name`,`icon`,`hash_url`,`sort_no`,`admin_only`,`fixed`)
VALUES
 ('bohe','h5','薄荷账','💰','#/bohe',18,0,1),
 ('boheStats','h5','薄荷统计','📊','#/boheStats',19,0,0),
 ('boheFriends','h5','薄荷关注','🤝','#/boheFriends',20,0,0);
