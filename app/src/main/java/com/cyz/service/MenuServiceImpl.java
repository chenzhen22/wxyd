package com.cyz.service;

import com.cyz.constant.ErrorEnum;
import com.cyz.mapper.mysqlMapper.MysqlMapper;
import com.cyz.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 菜单管理服务。
 * <p>
 * 可配置菜单注册表（key 与前端 data-target/H5 模块名对应，由前端完成映射）。
 * 四态模型见 V6__menu_config.sql：visible × user_ids(空/非空) 组合出
 * 全部可见 / 全部隐藏 / 白名单可见 / 黑名单不可见。
 * 管理员(role=0)不受配置影响，始终可见全部菜单。
 */
@Service
@Slf4j
public class MenuServiceImpl implements MenuService {

    /** 可配置菜单注册表（key → 名称）；menuManage 为管理入口本身，固定管理员可见，不参与配置 */
    private static final Map<String, String> REGISTRY = new LinkedHashMap<>();
    /** 仅管理员可见的菜单 */
    private static final Set<String> ADMIN_ONLY = new HashSet<>(Arrays.asList("sqlTool", "menuManage"));

    static {
        REGISTRY.put("userManage", "用户管理");
        REGISTRY.put("messageBoard", "留言板");
        REGISTRY.put("robotManage", "钉钉机器人");
        REGISTRY.put("groupChat", "群聊");
        REGISTRY.put("javaApi", "Java8-API");
        REGISTRY.put("shellScript", "Shell脚本");
        REGISTRY.put("note", "记忆笔记");
        REGISTRY.put("shareFile", "文件共享");
        REGISTRY.put("sqlTool", "SQL查询");
        REGISTRY.put("dubboCall", "Dubbo调用");
        REGISTRY.put("archiveTool", "归档下载");
    }

    @Resource
    MysqlMapper mysqlMapper;

    @Override
    public Result allConfigs() {
        Result result = Result.getInstance();
        Map<String, Map<String, Object>> cfgMap = loadConfigMap();
        List<Map<String, Object>> list = new ArrayList<>();
        REGISTRY.forEach((key, name) -> {
            Map<String, Object> row = new HashMap<>(4);
            row.put("key", key);
            row.put("name", name);
            Map<String, Object> cfg = cfgMap.get(key);
            row.put("visible", cfg == null ? 1 : ((Number) cfg.get("visible")).intValue());
            row.put("userIds", cfg == null ? "" : String.valueOf(cfg.get("userIds")));
            list.add(row);
        });
        result.setBody(list);
        return result;
    }

    @Override
    public Result visibleKeys(Long userId, boolean isAdmin) {
        Result result = Result.getInstance();
        Set<String> keys = new HashSet<>();
        if (isAdmin) {
            keys.addAll(REGISTRY.keySet());
            keys.add("menuManage");
        } else {
            keys.addAll(REGISTRY.keySet());
            keys.removeAll(ADMIN_ONLY);
            Map<String, Map<String, Object>> cfgMap = loadConfigMap();
            REGISTRY.forEach((key, name) -> {
                Map<String, Object> cfg = cfgMap.get(key);
                if (cfg == null) return; // 未配置默认可见
                int visible = ((Number) cfg.get("visible")).intValue();
                Set<Long> ids = parseIds(String.valueOf(cfg.get("userIds")));
                boolean inList = userId != null && ids.contains(userId);
                boolean show;
                if (ids.isEmpty()) {
                    show = visible == 1;               // 全部可见 / 全部隐藏
                } else if (visible == 1) {
                    show = inList;                     // 白名单
                } else {
                    show = !inList;                    // 黑名单
                }
                if (!show) keys.remove(key);
            });
        }
        result.setBody(new ArrayList<>(keys));
        return result;
    }

    @Override
    public Result save(Long userId, String menuKey, Integer visible, String userIds) {
        Result result = Result.getInstance();
        if (menuKey == null || !REGISTRY.containsKey(menuKey)
                || visible == null || (visible != 0 && visible != 1)) {
            return result.setErrorEnum(ErrorEnum.ERROR000015);
        }
        String ids = normalizeIds(userIds);
        if (ids == null) {
            return result.setErrorEnum(ErrorEnum.ERROR000015);
        }
        mysqlMapper.upsertMenuConfig(menuKey, visible, ids);
        log.info("菜单配置更新：user={} menu={} visible={} userIds={}", userId, menuKey, visible, ids);
        return result;
    }

    private Map<String, Map<String, Object>> loadConfigMap() {
        Map<String, Map<String, Object>> map = new HashMap<>();
        List<Map<String, Object>> rows = mysqlMapper.listMenuConfigs();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                map.put(String.valueOf(row.get("menuKey")), row);
            }
        }
        return map;
    }

    /** 解析逗号分隔用户 id；非法字符返回 null，空串返回空集合 */
    private Set<Long> parseIds(String s) {
        Set<Long> ids = new HashSet<>();
        if (s == null || s.trim().isEmpty()) return ids;
        for (String part : s.split(",")) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            try {
                ids.add(Long.valueOf(p));
            } catch (NumberFormatException e) {
                return new HashSet<>();
            }
        }
        return ids;
    }

    /** 规范化保存的 userIds：仅允许数字与逗号，去重去空；非法返回 null */
    private String normalizeIds(String s) {
        if (s == null || s.trim().isEmpty()) return "";
        Set<String> uniq = new LinkedHashSet<>();
        for (String part : s.split(",")) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            if (!p.matches("\\d{1,18}")) return null;
            uniq.add(p);
        }
        return String.join(",", uniq);
    }
}
