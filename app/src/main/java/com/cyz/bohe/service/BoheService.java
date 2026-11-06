package com.cyz.bohe.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cyz.bohe.mapper.BoheMapper;
import com.cyz.bohe.model.BoheBill;
import com.cyz.bohe.model.BoheFriend;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 薄荷账业务：账单查询/记账/删除、统计、好友关系、云导出 JSON 导入。
 * 金额一律整数分。导入为快照语义：按 cloud_id upsert，重复导入覆盖。
 */
@Service
public class BoheService {

    @Autowired
    private BoheMapper boheMapper;

    public List<BoheBill> listBills(long userId, String month, String period) {
        return boheMapper.listBills(userId, emptyToNull(month), emptyToNull(period));
    }

    /** 统计：month 值随 period 语义变化（yyyy / yyyy-MM / yyyy-MM-dd）；period 空 = 月。 */
    public Map<String, Object> stats(long userId, String month, String period) {
        String m = emptyToNull(month);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", boheMapper.sumMonth(userId, m, emptyToNull(period)));
        r.put("byCategory", boheMapper.sumByCategory(userId, m, emptyToNull(period)));
        return r;
    }

    /** viewer==target 短路；否则要求任一方向 friend 记录 status=1，且对方未隐藏薄荷账
     *  （users.bohe_hidden 全局开关 V30 + bohe_friend.hide_my_bills 好友粒度开关 V32，任一生效即不可见）。 */
    public boolean canView(long viewerId, long targetId) {
        if (viewerId == targetId) {
            return true;
        }
        Integer hidden = boheMapper.boheHidden(targetId);
        if (hidden != null && hidden == 1) {
            return false;
        }
        BoheFriend f = boheMapper.findFriend(viewerId, targetId);
        if (f == null || f.getStatus() == null || f.getStatus() != 1) {
            return false;
        }
        /* 好友粒度：target 对 viewer 单独隐藏 */
        Integer hideOne = boheMapper.hideMyBills(targetId, viewerId);
        return hideOne == null || hideOne != 1;
    }

    /** 我对单个好友隐藏/恢复我的薄荷账（V32；双向记录均存在，按发起方方向更新）。 */
    public void setHideMyBills(long userId, long friendId, boolean hidden) {
        boheMapper.setHideMyBills(userId, friendId, hidden ? 1 : 0);
    }

    /** 双向查好友记录（供 controller 判断关系/读取标记）。 */
    public BoheFriend findFriendBoth(long a, long b) {
        return boheMapper.findFriend(a, b);
    }

    /** 我对单个好友的隐藏标记（V32；无记录或未标记返回 0）。 */
    public int hideMyBillsFlag(long userId, long friendId) {
        Integer v = boheMapper.hideMyBills(userId, friendId);
        return v != null && v == 1 ? 1 : 0;
    }

    /**
     * 记账/编辑。id 为空 = 新增（返回新 id，离线同步回填用）；
     * id 非空 = 编辑（仅属主，updateBillById 行数 0 即无权/不存在）。
     */
    public Long saveBill(long userId, Long id, Long amount, String category, String note, LocalDate billDate) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("金额无效");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("分类不能为空");
        }
        if (billDate == null) {
            billDate = LocalDate.now();
        }
        if (id != null) {
            BoheBill b = new BoheBill();
            b.setId(id);
            b.setUserId(userId);
            b.setAmount(amount);
            b.setCategory(category.trim());
            b.setNote(note);
            b.setBillDate(billDate);
            int updated = boheMapper.updateBillById(b);
            if (updated == 0) {
                throw new IllegalArgumentException("账单不存在或无权编辑");
            }
            return id;
        }
        BoheBill b = new BoheBill();
        b.setUserId(userId);
        b.setAmount(amount);
        b.setCategory(category.trim());
        b.setNote(note);
        b.setBillDate(billDate);
        boheMapper.insertBill(b);
        return b.getId();
    }

    /** 查询用户薄荷账对好友是否隐藏（0/1）。 */
    public int boheHidden(long userId) {
        Integer h = boheMapper.boheHidden(userId);
        return h != null ? h : 0;
    }

    /** 设置薄荷账对好友可见性（0=可见 1=隐藏）。 */
    public void setBoheHidden(long userId, boolean hidden) {
        boheMapper.setBoheHidden(userId, hidden ? 1 : 0);
    }

    /** 仅本人可删；物理删除（cloud_id 随行消失，重复导入不会复活）。 */
    public void deleteBill(long userId, long billId) {
        boheMapper.deleteBill(billId, userId);
    }

    public List<Map<String, Object>> searchUsers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return boheMapper.searchUsers(keyword.trim());
    }

    public Long findIdByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        return boheMapper.findIdByUsername(username.trim());
    }

    public List<BoheFriend> listFriends(long userId) {
        // 双侧列表（我发起的 + 别人对我发起的），由 controller 组装对方信息
        return boheMapper.listFriendsByUser(userId, null);
    }

    public void requestFriend(long userId, long friendId) {
        if (userId == friendId) {
            throw new IllegalArgumentException("不能添加自己");
        }
        BoheFriend exist = boheMapper.findFriend(userId, friendId);
        if (exist != null) {
            if (exist.getStatus() != null && exist.getStatus() == 0) {
                throw new IllegalArgumentException("已提交申请，等待对方处理");
            }
            if (exist.getStatus() != null && exist.getStatus() == 1) {
                throw new IllegalArgumentException("已经是好友");
            }
            // status=2（拒绝/删除）→ 重置回待通过，方向统一为本次发起方
            boheMapper.updateFriendStatus(exist.getUserId(), exist.getFriendId(), 0);
            return;
        }
        BoheFriend f = new BoheFriend();
        f.setUserId(userId);
        f.setFriendId(friendId);
        f.setStatus(0);
        boheMapper.insertFriend(f);
    }

    /** 只处理"对方发起、自己接收"方向的申请。 */
    public void handleFriend(long userId, long friendId, boolean accept) {
        BoheFriend f = boheMapper.findFriend(userId, friendId);
        if (f == null || f.getFriendId() == null || f.getFriendId() != userId) {
            throw new IllegalArgumentException("申请不存在");
        }
        boheMapper.updateFriendStatus(f.getUserId(), f.getFriendId(), accept ? 1 : 2);
    }

    public void removeFriend(long userId, long friendId) {
        boheMapper.updateFriendStatus(userId, friendId, 2);
        boheMapper.updateFriendStatus(friendId, userId, 2);
    }

    /**
     * 导入云数据库导出的 bills JSON（快照语义，cloud_id upsert）。
     * 返回 {inserted, updated, skipped}。
     */
    public Map<String, Integer> importBills(long targetUserId, String json) {
        List<BoheBill> bills = parseExport(json);
        int inserted = 0, updated = 0, skipped = 0;
        for (BoheBill b : bills) {
            if (b.getAmount() == null || b.getBillDate() == null) {
                skipped++;
                continue;
            }
            b.setUserId(targetUserId);
            BoheBill exist = b.getCloudId() != null ? boheMapper.findByCloudId(b.getCloudId()) : null;
            if (exist != null) {
                boheMapper.updateBillByCloudId(b);
                updated++;
            } else {
                boheMapper.insertBill(b);
                inserted++;
            }
        }
        Map<String, Integer> r = new LinkedHashMap<>();
        r.put("inserted", inserted);
        r.put("updated", updated);
        r.put("skipped", skipped);
        return r;
    }

    /** 解析云导出数组：字段 _id/amount/category/note/date/createTime；坏行跳过不打断整批。 */
    private List<BoheBill> parseExport(String json) {
        List<BoheBill> out = new ArrayList<>();
        JSONArray arr;
        try {
            arr = JSON.parseArray(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON 格式错误: " + e.getMessage());
        }
        if (arr == null) {
            throw new IllegalArgumentException("JSON 不是数组");
        }
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 0; i < arr.size(); i++) {
            JSONObject o = arr.getJSONObject(i);
            if (o == null) {
                continue;
            }
            BoheBill b = new BoheBill();
            String id = o.getString("_id");
            b.setCloudId(id);
            try {
                String amt = String.valueOf(o.get("amount"));
                b.setAmount(amt == null || "null".equals(amt) ? null : Long.parseLong(amt.trim()));
            } catch (Exception e) {
                b.setAmount(null);
            }
            b.setCategory(o.getString("category"));
            b.setNote(o.getString("note"));
            String date = o.getString("date");
            if (date != null) {
                try {
                    b.setBillDate(LocalDate.parse(date.substring(0, 10), dateFmt));
                } catch (Exception e) {
                    b.setBillDate(null);
                }
            }
            String ct = o.getString("createTime");
            if (ct != null) {
                try {
                    b.setCreateTime(new java.util.Date(Long.parseLong(ct)));
                } catch (Exception e) {
                    // createTime 解析失败不影响导入
                }
            }
            out.add(b);
        }
        return out;
    }

    private String emptyToNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }
}
