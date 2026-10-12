package com.cyz.service;

import com.cyz.pojo.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface MenuService {

    /** 当前用户在某平台可见的菜单列表（已合并四态权限，按 sortNo 排序） */
    Result list(String platform, Long userId, boolean isAdmin);

    /** 全部菜单及权限配置（仅管理员，菜单管理页用） */
    Result allConfigs(String platform);

    /** 保存某菜单的可见性配置（仅管理员） */
    Result save(Long userId, String platform, String menuKey, Integer visible, String userIds);

    /** 新增/编辑菜单项（仅管理员；存在则更新，否则插入） */
    Result saveItem(Long userId, String platform, String menuKey, String name,
                    String icon, String hashUrl, Integer sortNo, Integer adminOnly);

    /** 删除菜单项（仅管理员；fixed=1 禁止；连带删权限配置） */
    Result deleteItem(Long userId, String platform, String menuKey);

    /**
     * 老接口，前端切换 menu/list 后删除。
     * 返回 desktop 平台可见菜单的 key 字符串列表，保持老前端兼容。
     *
     * @deprecated 使用 {@link #list(String, Long, boolean)}
     */
    @Deprecated
    default Result visibleKeys(Long userId, boolean isAdmin) {
        Result r = list("desktop", userId, isAdmin);
        Object body = r.getBody();
        if (body instanceof List) {
            List<String> keys = new ArrayList<>();
            for (Object o : (List<?>) body) {
                if (o instanceof Map) {
                    Object key = ((Map<?, ?>) o).get("key");
                    if (key != null) {
                        keys.add(String.valueOf(key));
                    }
                }
            }
            Result out = Result.getInstance();
            out.setBody(keys);
            return out;
        }
        return r;
    }
}
