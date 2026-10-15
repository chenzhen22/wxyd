# -*- coding: utf-8 -*-
"""忠实模拟 ShellScriptDataService 的解析逻辑，校验 data-shell/*.md。

逐行翻译自 Java：
  parseDocument : rawBody.split("(?m)^## ") -> 每节首行为标题
  parseSyntaxes : body.split("(?m)^### ") -> 每节首行为名称，
                  代码块内首行作 signature，块外 "- 描述" 行累积为描述
  parseExamples : 同切分，描述取最后一条 "- 描述"，代码取代码块全部行
另查：BOM / CRLF / 围栏配平 / front-matter 必填三项 / 全库重名。
"""
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
BASE = os.path.join(os.path.dirname(HERE), "app", "src", "main", "resources", "data-shell")
TARGETS = sys.argv[1:] or ["redis.md"]


def parse_syntaxes(body):
    out = []
    for section in re.split(r"(?m)^### ", body):
        section = section.strip()
        if not section:
            continue
        lines = section.split("\n", 1)
        name = lines[0].strip()
        rest = lines[1].strip() if len(lines) > 1 else ""
        signature, desc_parts = "", []
        in_code = False
        for line in rest.split("\n"):
            t = line.strip()
            if t.startswith("```bash") or t.startswith("```sh") or t.startswith("```shell"):
                in_code = True
                continue
            if in_code and t.startswith("```"):
                in_code = False
                continue
            if in_code:
                if not signature:
                    signature = line.strip()
            elif t.startswith("- 描述") or t.startswith("- 描述:"):
                desc_parts.append(re.sub(r"^- 描述:?\s*", "", t))
        out.append((name, signature, " ".join(desc_parts).strip()))
    return out


def parse_examples(body):
    out = []
    for section in re.split(r"(?m)^### ", body):
        section = section.strip()
        if not section:
            continue
        lines = section.split("\n", 1)
        name = lines[0].strip()
        rest = lines[1].strip() if len(lines) > 1 else ""
        desc, code_lines = "", []
        in_code = False
        for line in rest.split("\n"):
            t = line.strip()
            if t.startswith("```bash") or t.startswith("```sh") or t.startswith("```shell"):
                in_code = True
                continue
            if in_code and t.startswith("```"):
                in_code = False
                continue
            if in_code:
                code_lines.append(line)
            if t.startswith("- 描述") or t.startswith("- 描述:"):
                desc = re.sub(r"^- 描述:?\s*", "", t)
        out.append((name, desc, "\n".join(code_lines).strip()))
    return out


def parse_intro(body):
    lines, out, in_code = body.split("\n"), [], False
    for line in lines:
        if line.strip().startswith("```"):
            in_code = not in_code
            continue
        if not in_code:
            out.append(line)
    return "\n".join(out).strip()


def parse_file(path):
    raw = open(path, "rb").read()
    text = raw.decode("utf-8")
    r = {"file": os.path.basename(path), "errors": [],
         "bom": raw[:3] == b"\xef\xbb\xbf", "crlf": text.count("\r\n"),
         "fences": text.count("```")}
    if r["bom"]:
        r["errors"].append("有 BOM")
    if r["crlf"]:
        r["errors"].append("存在 CRLF 换行")
    if r["fences"] % 2:
        r["errors"].append("代码围栏不闭合（%d 个 ```）" % r["fences"])

    front, body = "", text
    if text.startswith("---"):
        end = text.index("---", 3)
        if end != -1:
            front, body = text[3:end].strip(), text[end + 3:].strip()
    r["fm"] = dict(l.split(":", 1) for l in front.splitlines() if ":" in l)
    for k in ("name", "category", "order"):
        if not r["fm"].get(k, "").strip():
            r["errors"].append("front-matter 缺 %s" % k)

    intro = ""
    syntaxes, examples = [], []
    for section in re.split(r"(?m)^## ", body):
        section = section.strip()
        if not section:
            continue
        lines = section.split("\n", 1)
        title, sbody = lines[0].strip(), (lines[1].strip() if len(lines) > 1 else "")
        if title == "介绍":
            intro = parse_intro(sbody)
        elif title == "语法":
            syntaxes = parse_syntaxes(sbody)
        elif title == "示例":
            examples = parse_examples(sbody)
    r["intro_len"] = len(intro)
    r["syntaxes"] = syntaxes
    r["examples"] = examples
    if not intro:
        r["errors"].append("介绍段为空")
    if not syntaxes:
        r["errors"].append("语法段为空")
    if not examples:
        r["errors"].append("示例段为空")
    for i, (n, sig, d) in enumerate(syntaxes, 1):
        if not n:
            r["errors"].append("语法 %d 缺名称" % i)
        if not sig:
            r["errors"].append("语法 %d「%s」签名（代码块首行）为空" % (i, n))
        if not d:
            r["errors"].append("语法 %d「%s」缺描述" % (i, n))
    for i, (n, d, c) in enumerate(examples, 1):
        if not n:
            r["errors"].append("示例 %d 缺名称" % i)
        if not d:
            r["errors"].append("示例 %d「%s」缺描述" % (i, n))
        if not c:
            r["errors"].append("示例 %d「%s」代码为空" % (i, n))
    return r


def main():
    print("BASE: %s" % BASE)
    print("目标: %s\n" % ", ".join(TARGETS))
    ok = True
    for fn in TARGETS:
        p = os.path.join(BASE, fn)
        if not os.path.exists(p):
            print("!! 文件不存在: %s\n" % p)
            ok = False
            continue
        r = parse_file(p)
        print("== %s  (%s / %s / order=%s)" % (fn, r["fm"].get("name"), r["fm"].get("category"), r["fm"].get("order")))
        print("   BOM=%s | CRLF=%d | 围栏=%d (%s) | 介绍 %d 字" % (
            "有" if r["bom"] else "无", r["crlf"], r["fences"],
            "闭合" if r["fences"] % 2 == 0 else "未闭合", r["intro_len"]))
        print("   语法 %d 条:" % len(r["syntaxes"]))
        for n, sig, d in r["syntaxes"]:
            print("     - %-14s | %-38s | %s" % (n, sig[:38], d[:40]))
        print("   示例 %d 个:" % len(r["examples"]))
        for n, d, c in r["examples"]:
            print("     - %-24s | %2d 行 | %s" % (n[:24], c.count("\n") + 1 if c else 0, d[:36]))
        if r["errors"]:
            ok = False
            for e in r["errors"]:
                print("   !! %s" % e)
        else:
            print("   => 通过")
        print()

    names, cats, total = {}, {}, 0
    for fn in sorted(os.listdir(BASE)):
        if not fn.endswith(".md"):
            continue
        total += 1
        t = open(os.path.join(BASE, fn), encoding="utf-8").read()
        nm = re.search(r"^name:\s*(.+)$", t, re.M)
        cm = re.search(r"^category:\s*(.+)$", t, re.M)
        if nm:
            names.setdefault(nm.group(1).strip(), []).append(fn)
        if cm:
            cats[cm.group(1).strip()] = cats.get(cm.group(1).strip(), 0) + 1
    dup = {k: v for k, v in names.items() if len(v) > 1}
    print("全库 md=%d | 唯一 name=%d | 重名=%s" % (total, len(names), dup or "无"))
    print("分类分布: %s" % dict(sorted(cats.items(), key=lambda x: -x[1])))
    tgt = os.path.join(BASE, TARGETS[0])
    if os.path.exists(tgt):
        t = open(tgt, encoding="utf-8").read()
        c = re.search(r"^category:\s*(.+)$", t, re.M).group(1).strip()
        print("「%s」分类现有主题数: %d" % (c, cats.get(c, 0)))
    print("\n总体: %s" % ("通过" if ok and not dup else "有问题"))
    return 0 if ok and not dup else 1


if __name__ == "__main__":
    sys.exit(main())
