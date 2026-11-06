package com.cyz.bohe.controller;

import com.cyz.bohe.model.BoheFriend;
import com.cyz.bohe.service.BoheService;
import com.cyz.controller.CommController;
import com.cyz.pojo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 薄荷账接口：账单/统计（好友互看需 status=1）、好友、用户搜索、超管导入。
 * userId 一律取 session，不信任客户端。
 */
@RestController
public class BoheController implements CommController {

    @Autowired
    private BoheService boheService;

    @Autowired
    private com.cyz.mapper.mysqlMapper.MysqlMapper mysqlMapper;

    private long me(HttpSession session) {
        Object uid = session.getAttribute("userId");
        return ((Number) uid).longValue();
    }

    private Result err(String msg) {
        Result r = Result.getInstance();
        r.setErrorCode("000003");
        r.setErrorMsg(msg);
        return r;
    }

    @ResponseBody
    @GetMapping("bohe/bill/list")
    public Result billList(@RequestParam(required = false) String month,
                           @RequestParam(required = false) String period,
                           @RequestParam(required = false) Long friendId,
                           HttpSession session) {
        Result r = Result.getInstance();
        try {
            long me = me(session);
            long target = friendId != null ? friendId : me;
            if (!boheService.canView(me, target)) {
                return err("无权查看该用户账单");
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("bills", boheService.listBills(target, month, period));
            body.put("self", target == me);
            r.setBody(body);
        } catch (IllegalArgumentException e) {
            return err(e.getMessage());
        } catch (Exception e) {
            return err("获取账单失败: " + e.getMessage());
        }
        return r;
    }

    @ResponseBody
    @PostMapping("bohe/bill/save")
    public Result billSave(@RequestBody Map<String, Object> body, HttpSession session) {
        Result r = Result.getInstance();
        try {
            long me = me(session);
            Long id = body.get("id") != null ? Long.parseLong(String.valueOf(body.get("id"))) : null;
            Long amount = body.get("amount") != null ? Long.parseLong(String.valueOf(body.get("amount"))) : null;
            String category = (String) body.get("category");
            String note = (String) body.get("note");
            String date = (String) body.get("date");
            LocalDate billDate = date != null && !date.isEmpty()
                    ? LocalDate.parse(date.substring(0, 10), DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    : null;
            Long savedId = boheService.saveBill(me, id, amount, category, note, billDate);
            r.setBody(Collections.singletonMap("id", savedId));
        } catch (IllegalArgumentException e) {
            return err(e.getMessage());
        } catch (Exception e) {
            return err("记账失败: " + e.getMessage());
        }
        return r;
    }

    @ResponseBody
    @PostMapping("bohe/bill/delete")
    public Result billDelete(@RequestBody Map<String, Object> body, HttpSession session) {
        Result r = Result.getInstance();
        try {
            long me = me(session);
            long id = Long.parseLong(String.valueOf(body.get("id")));
            boheService.deleteBill(me, id);
        } catch (IllegalArgumentException e) {
            return err(e.getMessage());
        } catch (Exception e) {
            return err("删除失败: " + e.getMessage());
        }
        return r;
    }

    @ResponseBody
    @GetMapping("bohe/stats")
    public Result stats(@RequestParam(required = false) String month,
                        @RequestParam(required = false) String period,
                        @RequestParam(required = false) Long friendId,
                        HttpSession session) {
        Result r = Result.getInstance();
        try {
            long me = me(session);
            long target = friendId != null ? friendId : me;
            if (!boheService.canView(me, target)) {
                return err("无权查看该用户统计");
            }
            r.setBody(boheService.stats(target, month, period));
        } catch (IllegalArgumentException e) {
            return err(e.getMessage());
        } catch (Exception e) {
            return err("获取统计失败: " + e.getMessage());
        }
        return r;
    }

    @ResponseBody
    @GetMapping("bohe/friend/list")
    public Result friendList(HttpSession session) {
        Result r = Result.getInstance();
        try {
            long me = me(session);
            List<Map<String, Object>> out = new ArrayList<>();
            for (BoheFriend f : boheService.listFriends(me)) {
                boolean outgoing = f.getUserId() == me;
                long otherId = outgoing ? f.getFriendId() : f.getUserId();
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("userId", otherId);
                item.put("outgoing", outgoing);
                item.put("status", f.getStatus());
                /* 按 id 精确查用户（原 LIKE '%id%' 会误匹配，如 28 命中 128，显示成别人的名字） */
                com.cyz.pojo.User u = mysqlMapper.queryUserById(otherId);
                if (u != null) {
                    item.put("username", u.getUsername());
                    item.put("displayName", u.getDisplayName());
                }
                /* 我对该好友的薄荷账隐藏标记（V32，好友弹窗「不让他看我的薄荷账」勾选态） */
                item.put("hideMyBills", boheService.hideMyBillsFlag(me, otherId));
                out.add(item);
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("friends", out);
            r.setBody(body);
        } catch (IllegalArgumentException e) {
            return err(e.getMessage());
        } catch (Exception e) {
            return err("获取好友列表失败: " + e.getMessage());
        }
        return r;
    }

    @ResponseBody
    @PostMapping("bohe/friend/request")
    public Result friendRequest(@RequestBody Map<String, Object> body, HttpSession session) {
        Result r = Result.getInstance();
        try {
            long me = me(session);
            String username = (String) body.get("username");
            if (username == null || username.trim().isEmpty()) {
                return err("用户名不能为空");
            }
            List<Map<String, Object>> hit = boheService.searchUsers(username.trim());
            Long targetId = null;
            for (Map<String, Object> u : hit) {
                if (username.trim().equals(u.get("username"))) {
                    targetId = ((Number) u.get("id")).longValue();
                    break;
                }
            }
            if (targetId == null) {
                return err("用户不存在: " + username);
            }
            boheService.requestFriend(me, targetId);
        } catch (IllegalArgumentException e) {
            return err(e.getMessage());
        } catch (Exception e) {
            return err("申请失败: " + e.getMessage());
        }
        return r;
    }

    @ResponseBody
    @PostMapping("bohe/friend/handle")
    public Result friendHandle(@RequestBody Map<String, Object> body, HttpSession session) {
        Result r = Result.getInstance();
        try {
            long me = me(session);
            long otherId = Long.parseLong(String.valueOf(body.get("userId")));
            boolean accept = Boolean.parseBoolean(String.valueOf(body.get("accept")));
            boheService.handleFriend(me, otherId, accept);
        } catch (IllegalArgumentException e) {
            return err(e.getMessage());
        } catch (Exception e) {
            return err("处理失败: " + e.getMessage());
        }
        return r;
    }

    @ResponseBody
    @PostMapping("bohe/friend/remove")
    public Result friendRemove(@RequestBody Map<String, Object> body, HttpSession session) {
        Result r = Result.getInstance();
        try {
            long me = me(session);
            long otherId = Long.parseLong(String.valueOf(body.get("userId")));
            boheService.removeFriend(me, otherId);
        } catch (IllegalArgumentException e) {
            return err(e.getMessage());
        } catch (Exception e) {
            return err("删除好友失败: " + e.getMessage());
        }
        return r;
    }

    /** 好友粒度隐藏我的薄荷账（V32）：body {userId, hidden}；仅 status=1 好友有效。 */
    @ResponseBody
    @PostMapping("bohe/friend/hide")
    public Result friendHide(@RequestBody Map<String, Object> body, HttpSession session) {
        Result r = Result.getInstance();
        try {
            long me = me(session);
            long otherId = Long.parseLong(String.valueOf(body.get("userId")));
            boolean hidden = Boolean.parseBoolean(String.valueOf(body.get("hidden")));
            BoheFriend f = boheService.findFriendBoth(me, otherId);
            if (f == null || f.getStatus() == null || f.getStatus() != 1) {
                return err("仅好友可设置");
            }
            boheService.setHideMyBills(me, otherId, hidden);
            r.setBody(Collections.singletonMap("hidden", hidden ? 1 : 0));
        } catch (IllegalArgumentException e) {
            return err(e.getMessage());
        } catch (Exception e) {
            return err("设置失败: " + e.getMessage());
        }
        return r;
    }

    @ResponseBody
    @GetMapping("bohe/user/search")
    public Result userSearch(@RequestParam String keyword, HttpSession session) {
        Result r = Result.getInstance();
        try {
            me(session); // 仅要求登录
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("users", boheService.searchUsers(keyword));
            r.setBody(body);
        } catch (IllegalArgumentException e) {
            return err(e.getMessage());
        } catch (Exception e) {
            return err("搜索失败: " + e.getMessage());
        }
        return r;
    }

    /** 薄荷账对好友的可见开关（需求 4）：GET 查询 / POST 设置。 */
    @ResponseBody
    @GetMapping("bohe/visibility")
    public Result visibility(HttpSession session) {
        Result r = Result.getInstance();
        try {
            long me = me(session);
            r.setBody(Collections.singletonMap("hidden", boheService.boheHidden(me)));
        } catch (Exception e) {
            return err("查询失败: " + e.getMessage());
        }
        return r;
    }

    @ResponseBody
    @PostMapping("bohe/visibility")
    public Result setVisibility(@RequestBody Map<String, Object> body, HttpSession session) {
        Result r = Result.getInstance();
        try {
            long me = me(session);
            boolean hidden = Boolean.parseBoolean(String.valueOf(body.get("hidden")));
            boheService.setBoheHidden(me, hidden);
            r.setBody(Collections.singletonMap("hidden", hidden ? 1 : 0));
        } catch (Exception e) {
            return err("设置失败: " + e.getMessage());
        }
        return r;
    }

    @ResponseBody
    @PostMapping("bohe/import")
    public Result importBills(@RequestBody Map<String, Object> body, HttpSession session) {
        Result r = Result.getInstance();
        Object role = session.getAttribute("role");
        if (role == null || ((Number) role).intValue() != 0) {
            return err("权限不足，仅超级管理员可导入");
        }
        try {
            String targetUsername = (String) body.get("targetUsername");
            Long targetUserId = boheService.findIdByUsername(targetUsername);
            if (targetUserId == null) {
                return err("目标用户不存在: " + targetUsername);
            }
            @SuppressWarnings("unchecked")
            List<Object> bills = (List<Object>) body.get("bills");
            if (bills == null) {
                return err("bills 不能为空");
            }
            r.setBody(boheService.importBills(targetUserId, com.alibaba.fastjson2.JSON.toJSONString(bills)));
        } catch (IllegalArgumentException e) {
            return err(e.getMessage());
        } catch (Exception e) {
            return err("导入失败: " + e.getMessage());
        }
        return r;
    }
}
