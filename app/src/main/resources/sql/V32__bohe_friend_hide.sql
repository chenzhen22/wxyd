-- 薄荷账好友粒度隐藏：bohe_friend 加 hide_my_bills 列
-- 需求：好友列表点击好友弹操作框，「不让他看我的薄荷账」按单个好友生效；
--       与 users.bohe_hidden（V30，对全部好友隐藏）并存，任一生效即不可见。
ALTER TABLE `bohe_friend`
    ADD COLUMN `hide_my_bills` TINYINT NOT NULL DEFAULT 0 COMMENT '发起方对好友隐藏自己的薄荷账：0=可见 1=隐藏';
