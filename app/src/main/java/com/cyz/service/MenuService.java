package com.cyz.service;

import com.cyz.pojo.Result;

public interface MenuService {

    /** 全部菜单及配置（仅管理员，菜单管理界面用） */
    Result allConfigs();

    /** 当前用户可见的菜单 key 列表（管理员返回全部；非管理员不含 menuManage/sqlTool） */
    Result visibleKeys(Long userId, boolean isAdmin);

    /** 保存某菜单的可见性配置（仅管理员） */
    Result save(Long userId, String menuKey, Integer visible, String userIds);
}
