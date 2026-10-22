package com.cyz.wbgateway.provider;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 免费模型优先级：管理员可手动指定顺序（逗号分隔的模型 id）。
 * <p>
 * 规则：手动列表里仍然存在的模型按手动顺序排在前面，其余自动目录里的免费模型补在后面；
 * 手动列表为空或全部失效时，完全按目录自动顺序。
 */
public final class FreePriority {

    private FreePriority() {
    }

    public static List<String> parse(String csv) {
        List<String> out = new ArrayList<>();
        if (csv == null) return out;
        for (String s : csv.split(",")) {
            String t = s.trim();
            if (!t.isEmpty() && !out.contains(t)) out.add(t);
        }
        return out;
    }

    public static List<String> order(List<String> auto, String manualCsv) {
        List<String> autoList = auto == null ? new ArrayList<>() : auto;
        List<String> manual = parse(manualCsv);
        if (manual.isEmpty()) return autoList;
        List<String> out = new ArrayList<>();
        Set<String> used = new LinkedHashSet<>();
        for (String m : manual) {
            for (String a : autoList) {
                if (a.equalsIgnoreCase(m) && !used.contains(a)) {
                    out.add(a);
                    used.add(a);
                    break;
                }
            }
        }
        for (String a : autoList) if (!used.contains(a)) out.add(a);
        return out;
    }

    /** 手动配置里已失效（目录中不存在）的模型 */
    public static List<String> invalid(List<String> auto, String manualCsv) {
        List<String> bad = new ArrayList<>();
        for (String m : parse(manualCsv)) {
            boolean hit = false;
            for (String a : auto) if (a.equalsIgnoreCase(m)) { hit = true; break; }
            if (!hit) bad.add(m);
        }
        return bad;
    }
}
