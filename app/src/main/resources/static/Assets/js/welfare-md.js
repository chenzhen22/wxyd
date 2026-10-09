/* ============================================================
   福利专区 — Markdown 渲染器（PC welfare.js 与 H5 h5-welfare.js 共用）
   window.WelfareMd.render(mdText, urlMap) -> html
   - 先整体 HTML 转义再生成标签，天然防 XSS，无需事后 sanitize
   - 图片 ![](url)：url 在 urlMap 中有本地代理地址则替换，否则整图丢弃
   - 链接 [text](url)：不渲染 <a>，只保留文字（纯展示，不跳外站）
   ============================================================ */
(function () {
    function escHtml(s) {
        return String(s == null ? '' : s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    /* 还原 escHtml 造成的 URL 变形，用于查映射表 */
    function unescapeUrl(u) {
        return u.replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/>/g, '>')
            .replace(/&quot;/g, '"').replace(/&#39;/g, "'");
    }

    /* 行内：图片 / 加粗 / 斜体 / 行内代码；链接只留文字 */
    function inline(text, urlMap) {
        // markdownify 会把 & 等转成实体、把 _ * 等加反斜杠转义；
        // 先还原实体再整体 escHtml（安全等价，杜绝双重转义），最后还原反斜杠转义
        var s = escHtml(String(text == null ? '' : text)
            .replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>')
            .replace(/&quot;/g, '"').replace(/&#39;/g, "'").replace(/&nbsp;/g, ' '));
        s = s.replace(/\\([`_*\[\]()>#+\-.!~])/g, '$1');
        // 图片：![alt](url "title") — 无本地代理地址的图直接丢弃
        s = s.replace(/!\[([^\]]*)\]\(([^)\s]+)(?:\s+&quot;[^)]*&quot;)?\)/g, function (m, alt, rawUrl) {
            var web = urlMap && urlMap[unescapeUrl(rawUrl)];
            if (!web) return '';
            return '<img src="' + escHtml(web) + '" alt="' + alt + '" loading="lazy">';
        });
        // 链接：只保留链接文字
        s = s.replace(/\[([^\]]*)\]\(([^)]*)\)/g, '$1');
        s = s.replace(/`([^`]+)`/g, '<code>$1</code>');
        s = s.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
        s = s.replace(/\*([^*]+)\*/g, '<em>$1</em>');
        return s;
    }

    function render(md, urlMap) {
        if (!md) return '';
        var lines = String(md).replace(/\r\n/g, '\n').split('\n');
        var html = '';
        var i = 0;

        while (i < lines.length) {
            var line = lines[i];

            // 代码块
            if (/^```/.test(line)) {
                var code = [];
                i++;
                while (i < lines.length && !/^```\s*$/.test(lines[i])) { code.push(lines[i]); i++; }
                i++;
                html += '<pre class="wf-code">' + escHtml(code.join('\n')) + '</pre>';
                continue;
            }

            // 标题
            var h = line.match(/^(#{1,4})\s+(.+)$/);
            if (h) {
                var tag = 'h' + Math.min(h[1].length + 1, 5); /* 正文标题从 h2 起 */
                html += '<' + tag + '>' + inline(h[2].trim(), urlMap) + '</' + tag + '>';
                i++;
                continue;
            }

            // 分割线
            if (/^(\-{3,}|\*{3,})\s*$/.test(line)) { html += '<hr>'; i++; continue; }

            // 引用
            if (/^>\s?/.test(line)) {
                var q = [];
                while (i < lines.length && /^>\s?/.test(lines[i])) {
                    q.push(lines[i].replace(/^>\s?/, ''));
                    i++;
                }
                html += '<blockquote>' + inline(q.join('\n'), urlMap).replace(/\n/g, '<br>') + '</blockquote>';
                continue;
            }

            // 无序列表（支持两空格缩进的二级项）
            if (/^\s*[-*]\s+/.test(line)) {
                var ul = [];
                while (i < lines.length && /^\s*[-*]\s+/.test(lines[i])) {
                    var item = lines[i].replace(/^\s*[-*]\s+/, '');
                    ul.push('<li>' + inline(item, urlMap) + '</li>');
                    i++;
                }
                html += '<ul>' + ul.join('') + '</ul>';
                continue;
            }

            // 有序列表
            if (/^\s*\d+\.\s+/.test(line)) {
                var ol = [];
                while (i < lines.length && /^\s*\d+\.\s+/.test(lines[i])) {
                    ol.push('<li>' + inline(lines[i].replace(/^\s*\d+\.\s+/, ''), urlMap) + '</li>');
                    i++;
                }
                html += '<ol>' + ol.join('') + '</ol>';
                continue;
            }

            // 空行
            if (line.trim() === '') { i++; continue; }

            // 独立成行的图片也走 inline 处理
            if (/^!\[[^\]]*\]\(/.test(line)) {
                html += '<p class="wf-img-line">' + inline(line.trim(), urlMap) + '</p>';
                i++;
                continue;
            }

            // 段落（聚合到下一个块级元素为止）
            var para = [];
            while (i < lines.length && lines[i].trim() !== '' &&
                   !/^```/.test(lines[i]) && !/^#{1,4}\s+/.test(lines[i]) &&
                   !/^>\s?/.test(lines[i]) && !/^\s*[-*]\s+/.test(lines[i]) &&
                   !/^\s*\d+\.\s+/.test(lines[i]) && !/^(\-{3,}|\*{3,})\s*$/.test(lines[i]) &&
                   !/^!\[[^\]]*\]\(/.test(lines[i])) {
                para.push(lines[i]);
                i++;
            }
            html += '<p>' + inline(para.join('\n'), urlMap).replace(/\n/g, '<br>') + '</p>';
        }
        return html;
    }

    window.WelfareMd = { render: render };
})();
