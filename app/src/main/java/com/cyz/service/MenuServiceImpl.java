package com.cyz.service;

import com.cyz.constant.ErrorEnum;
import com.cyz.mapper.mysqlMapper.MysqlMapper;
import com.cyz.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
 * 菜单注册表由 menu_item 表驱动（platform 区分 desktop / h5 两端），
 * 菜单项增改删见 saveItem / deleteItem。四态模型见 V6__menu_config.sql：
 * visible × user_ids(空/非空) 组合出
 * 全部可见 / 全部隐藏 / 白名单可见 / 黑名单不可见。
 * 管理员(role=0)不受配置影响，始终可见全部菜单。
 */
@Service
@Slf4j
public class MenuServiceImpl implements MenuService {

    /** 支持的平台 */
    private static final Set<String> PLATFORMS = new HashSet<>(Arrays.asList("h5", "desktop"));

    @Resource
    MysqlMapper mysqlMapper;

    @Override
    public Result list(String platform, Long userId, boolean isAdmin) {
        Result result = Result.getInstance();
        if (!validPlatform(platform)) {
            return result.setErrorEnum(ErrorEnum.ERROR000015);
        }
        List<Map<String, Object>> items = loadItems(platform);
        if (!isAdmin) {
            Map<String, Map<String, Object>> cfgMap = loadConfigMap();
            items.removeIf(item -> {
                if (((Number) item.get("adminOnly")).intValue() == 1) {
                    return true; // 仅管理员可见
                }
                Map<String, Object> cfg = cfgMap.get(item.get("key"));
                if (cfg == null) return false; // 未配置默认可见
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
                return !show;
            });
        }
        items.sort(Comparator.comparingInt(m -> ((Number) m.get("sortNo")).intValue()));
        List<Map<String, Object>> body = new ArrayList<>(items.size());
        for (Map<String, Object> item : items) {
            Map<String, Object> row = new LinkedHashMap<>(8);
            row.put("key", item.get("key"));
            row.put("name", item.get("name"));
            row.put("icon", item.get("icon"));
            row.put("hashUrl", item.get("hashUrl"));
            row.put("adminOnly", item.get("adminOnly"));
            row.put("sortNo", item.get("sortNo"));
            body.add(row);
        }
        result.setBody(body);
        return result;
    }

    @Override
    public Result allConfigs(String platform) {
        Result result = Result.getInstance();
        if (!validPlatform(platform)) {
            return result.setErrorEnum(ErrorEnum.ERROR000015);
        }
        Map<String, Map<String, Object>> cfgMap = loadConfigMap();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> item : loadItems(platform)) {
            Map<String, Object> row = new LinkedHashMap<>(item);
            Map<String, Object> cfg = cfgMap.get(item.get("key"));
            row.put("visible", cfg == null ? 1 : ((Number) cfg.get("visible")).intValue());
            row.put("userIds", cfg == null ? "" : String.valueOf(cfg.get("userIds")));
            list.add(row);
        }
        list.sort(Comparator.comparingInt(m -> ((Number) m.get("sortNo")).intValue()));
        result.setBody(list);
        return result;
    }

    @Override
    public Result save(Long userId, String platform, String menuKey, Integer visible, String userIds) {
        Result result = Result.getInstance();
        if (!validPlatform(platform) || menuKey == null || findItem(platform, menuKey) == null
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

    @Override
    public Result saveItem(Long userId, String platform, String menuKey, String name,
                           String icon, String hashUrl, Integer sortNo, Integer adminOnly) {
        Result result = Result.getInstance();
        if (!validPlatform(platform) || menuKey == null || !menuKey.matches("\\w{1,50}")) {
            return result.setErrorEnum(ErrorEnum.ERROR000015);
        }
        if (name == null || name.trim().isEmpty() || name.length() > 50) {
            return result.setErrorEnum(ErrorEnum.ERROR000015);
        }
        String iconVal = icon == null ? "" : icon;
        if (iconVal.length() > 20) {
            return result.setErrorEnum(ErrorEnum.ERROR000015);
        }
        String hashUrlVal = hashUrl == null ? "" : hashUrl;
        if (!hashUrlVal.isEmpty() && !hashUrlVal.startsWith("#/")) {
            return result.setErrorEnum(ErrorEnum.ERROR000015);
        }
        int sortNoVal = sortNo == null ? 0 : sortNo;
        if (sortNoVal < 0 || sortNoVal > 9999) {
            return result.setErrorEnum(ErrorEnum.ERROR000015);
        }
        int adminOnlyVal = adminOnly == null ? 0 : adminOnly;
        if (adminOnlyVal != 0 && adminOnlyVal != 1) {
            return result.setErrorEnum(ErrorEnum.ERROR000015);
        }
        if (findItem(platform, menuKey) != null) {
            mysqlMapper.updateMenuItem(menuKey, platform, name, iconVal, hashUrlVal, sortNoVal, adminOnlyVal);
        } else {
            mysqlMapper.insertMenuItem(menuKey, platform, name, iconVal, hashUrlVal, sortNoVal, adminOnlyVal, 0);
        }
        log.info("菜单项保存：user={} platform={} key={}", userId, platform, menuKey);
        return result;
    }

    @Override
    public Result deleteItem(Long userId, String platform, String menuKey) {
        Result result = Result.getInstance();
        if (!validPlatform(platform) || menuKey == null) {
            return result.setErrorEnum(ErrorEnum.ERROR000015);
        }
        Map<String, Object> item = findItem(platform, menuKey);
        if (item == null) {
            return result.setErrorEnum(ErrorEnum.ERROR000015);
        }
        if (((Number) item.get("fixed")).intValue() == 1) {
            // 结构性入口禁止删除
            return result.setErrorEnum(ErrorEnum.ERROR000015);
        }
        mysqlMapper.deleteMenuConfig(menuKey);
        mysqlMapper.deleteMenuItem(menuKey, platform);
        log.info("菜单项删除：user={} platform={} key={}", userId, platform, menuKey);
        return result;
    }

    private boolean validPlatform(String platform) {
        return platform != null && PLATFORMS.contains(platform);
    }

    /** 读取指定平台的菜单项并规范化字段（key/name/icon/hashUrl/sortNo/adminOnly/fixed） */
    private List<Map<String, Object>> loadItems(String platform) {
        List<Map<String, Object>> items = new ArrayList<>();
        List<Map<String, Object>> rows = mysqlMapper.listMenuItems();
        if (rows == null) {
            return items;
        }
        for (Map<String, Object> row : rows) {
            if (!platform.equals(String.valueOf(row.get("platform")))) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>(8);
            item.put("key", String.valueOf(row.get("menuKey")));
            item.put("name", String.valueOf(row.get("name")));
            item.put("icon", row.get("icon") == null ? "" : String.valueOf(row.get("icon")));
            item.put("hashUrl", row.get("hashUrl") == null ? "" : String.valueOf(row.get("hashUrl")));
            item.put("sortNo", row.get("sortNo") instanceof Number ? row.get("sortNo") : 0);
            item.put("adminOnly", row.get("adminOnly") instanceof Number ? row.get("adminOnly") : 0);
            item.put("fixed", row.get("fixed") instanceof Number ? row.get("fixed") : 0);
            items.add(item);
        }
        return items;
    }

    /** 按平台 + key 查找菜单项；不存在返回 null */
    private Map<String, Object> findItem(String platform, String menuKey) {
        for (Map<String, Object> item : loadItems(platform)) {
            if (item.get("key").equals(menuKey)) {
                return item;
            }
        }
        return null;
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
