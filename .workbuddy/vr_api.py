# -*- coding: utf-8 -*-
"""端到端验证：登录 -> 主题列表 -> Redis 命令行详情。"""
import json
import re
import time
import urllib.error
import urllib.request

BASE = "http://localhost:8090/api"
OUT = r"D:\GIT\wxyd\.workbuddy\vr.txt"
USER, PWD = "admin", "Admin@2026"
TARGET = "Redis 命令行"


def req(path, method="GET", data=None, headers=None, timeout=30):
    r = urllib.request.Request(BASE + path, method=method)
    r.add_header("X-Requested-With", "XMLHttpRequest")
    r.add_header("Accept", "application/json")
    if headers:
        for k, v in headers.items():
            r.add_header(k, v)
    body = None
    if data is not None:
        body = json.dumps(data).encode("utf-8")
        r.add_header("Content-Type", "application/json;charset=UTF-8")
    try:
        with urllib.request.urlopen(r, data=body, timeout=timeout) as resp:
            return resp.status, resp.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")
    except Exception as e:
        return 0, "ERR: %s" % e


lines = []


def w(s=""):
    lines.append(str(s))
    print(s)


# 1. 等待启动
ok = False
for i in range(40):
    try:
        with urllib.request.urlopen(BASE + "/actuator/health", timeout=5) as resp:
            if resp.status == 200:
                w("health: 200 (第 %d 次探测，约 %ds)" % (i + 1, (i + 1) * 2))
                ok = True
                break
    except Exception:
        pass
    time.sleep(2)
if not ok:
    w("!! 应用未在预期时间内启动")
    open(OUT, "w", encoding="utf-8").write("\n".join(lines))
    raise SystemExit(1)

# 2. 登录
st, txt = req("/login", "POST", {"body": {"username": USER, "password": PWD}})
try:
    d = json.loads(txt)
    w("login: HTTP %s | errorCode=%s | user=%s" % (
        st, d.get("errorCode"), (d.get("body") or {}).get("username")))
except Exception:
    w("login: HTTP %s | %s" % (st, txt[:200]))

# 3. 主题列表
st, txt = req("/shellscript/topics")
ts = json.loads(txt)
w("topics: HTTP %s | 总数 %d" % (st, len(ts)))
cat = [t for t in ts if t.get("category") == "数据库"]
w("数据库分类 (%d): %s" % (len(cat), " / ".join(t["name"] for t in cat)))
hit = [t for t in ts if t.get("name") == TARGET]
if hit:
    w("命中: %s | category=%s" % (hit[0]["name"], hit[0]["category"]))
    w("intro: %s" % (hit[0].get("intro") or "")[:120])
else:
    w("!! 未命中 %s" % TARGET)

# 4. 详情
import urllib.parse
st, txt = req("/shellscript/topics/" + urllib.parse.quote(TARGET))
d = json.loads(txt)
w("detail: HTTP %s | 语法 %d | 示例 %d" % (st, len(d.get("syntaxes", [])), len(d.get("examples", []))))
w("语法签名:")
for s in d.get("syntaxes", []):
    w("  - %-14s | %s" % (s.get("name", ""), s.get("signature", "")))
w("示例:")
for e in d.get("examples", []):
    code = e.get("code", "")
    w("  - %-24s | %2d 行 | %s" % (e.get("name", "")[:24], code.count("\n") + 1 if code else 0,
                                    (e.get("description", "") or "")[:30]))

# 5. 页面与静态资源
for p in ("/index.html", "/api.html"):
    try:
        with urllib.request.urlopen(BASE.replace("/api", "") + p, timeout=10) as resp:
            w("GET %s -> HTTP %s (%d bytes)" % (p, resp.status, len(resp.read())))
    except Exception as e:
        w("GET %s -> ERR %s" % (p, e))

# 6. 反面：未登录时 topics 应跳登录页（302 -> 200 login html）
try:
    req2 = urllib.request.Request(BASE + "/shellscript/topics")
    req2.add_header("X-Requested-With", "XMLHttpRequest")
    with urllib.request.urlopen(req2, timeout=10) as resp:
        body = resp.read().decode("utf-8", "replace")
        w("未登录 topics: HTTP %s | 是否登录页: %s" % (resp.status, "login" in body.lower()))
except Exception as e:
    w("未登录 topics: ERR %s" % e)

open(OUT, "w", encoding="utf-8").write("\n".join(lines))
w("\n结果已写入 %s" % OUT)
