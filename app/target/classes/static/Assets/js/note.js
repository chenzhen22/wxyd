/**
 * 记忆笔记前端逻辑
 * 排版参考文档站点（左侧目录导航 + 居中文章 + 右侧“本页目录”），适配主题变量 + jQuery 1.8.3
 * 笔记为整篇 markdown 正文，纯只读展示；支持 #/##/### 标题、> 引用（提示框）、--- 分割线、列表、代码与加粗
 */
var Note = (function () {
    var allNotes = [];
    var currentNote = null;
    var loaded = false;

    function escHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    // 行内格式：先转义，再处理 `code` 与 **bold**（转义后安全）
    function inline(s) {
        return escHtml(s)
            .replace(/`([^`]+)`/g, '<code>$1</code>')
            .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    }

    // 将 markdown 正文渲染为 HTML，并抽取标题生成“本页目录”
    function renderMarkdown(md) {
        if (!md) return { html: '', toc: [] };
        var toc = [];
        var hIndex = 0;
        var lines = String(md).replace(/\r\n/g, '\n').split('\n');
        var html = '';
        var i = 0;

        while (i < lines.length) {
            var line = lines[i];

            // 代码块 ```
            var fence = line.match(/^```(\w*)\s*$/);
            if (fence) {
                var code = [];
                i++;
                while (i < lines.length && !/^```\s*$/.test(lines[i])) { code.push(lines[i]); i++; }
                i++;
                html += '<pre><code>' + escHtml(code.join('\n')) + '</code></pre>';
                continue;
            }

            // 标题 # / ## / ###（标题由笔记名单独渲染为 h1，正文标题从 h2 起）
            var h = line.match(/^(#{1,3})\s+(.+)$/);
            if (h) {
                var lvl = h[1].length;
                var tag = lvl === 1 ? 'h2' : (lvl === 2 ? 'h3' : 'h4');
                var text = h[2].trim();
                var id = 'note-h-' + (hIndex++);
                toc.push({ level: lvl, text: text, id: id });
                html += '<' + tag + ' id="' + id + '">' + inline(text) + '</' + tag + '>';
                i++;
                continue;
            }

            // 分割线
            if (/^(\-{3,}|\*{3,})\s*$/.test(line)) { html += '<hr>'; i++; continue; }

            // 引用（提示框）
            if (/^>\s?/.test(line)) {
                var q = [];
                while (i < lines.length && /^>\s?/.test(lines[i])) {
                    q.push(lines[i].replace(/^>\s?/, ''));
                    i++;
                }
                html += '<blockquote>' + inline(q.join('\n')).replace(/\n/g, '<br>') + '</blockquote>';
                continue;
            }

            // 无序列表
            if (/^[-*]\s+/.test(line)) {
                var items = [];
                while (i < lines.length && /^[-*]\s+/.test(lines[i])) {
                    items.push('<li>' + inline(lines[i].replace(/^[-*]\s+/, '')) + '</li>');
                    i++;
                }
                html += '<ul>' + items.join('') + '</ul>';
                continue;
            }

            // 空行
            if (line.trim() === '') { i++; continue; }

            // 段落（聚合到下一个块级元素为止）
            var para = [];
            while (i < lines.length && lines[i].trim() !== '' &&
                   !/^```/.test(lines[i]) && !/^(#{1,3})\s+/.test(lines[i]) &&
                   !/^>\s?/.test(lines[i]) && !/^[-*]\s+/.test(lines[i]) &&
                   !/^(\-{3,}|\*{3,})\s*$/.test(lines[i])) {
                para.push(lines[i]);
                i++;
            }
            html += '<p>' + inline(para.join('\n')).replace(/\n/g, '<br>') + '</p>';
        }
        return { html: html, toc: toc };
    }

    function loadNoteList() {
        $.ajax({
            url: 'note', type: 'GET',
            success: function (data) {
                allNotes = data || [];
                renderNoteList(allNotes, '');
            },
            error: function () {
                $('#noteList').html('<div style="padding:20px;text-align:center;color:var(--text-9)">加载失败</div>');
            }
        });
    }

    function renderNoteList(notes, keyword) {
        var filtered = notes.filter(function (t) {
            if (!keyword) return true;
            var kw = keyword.toLowerCase();
            return (t.name && t.name.toLowerCase().indexOf(kw) !== -1)
                || (t.category && t.category.toLowerCase().indexOf(kw) !== -1)
                || (t.content && t.content.toLowerCase().indexOf(kw) !== -1);
        });

        if (filtered.length === 0) {
            $('#noteList').html('<div style="padding:20px;text-align:center;color:var(--text-9)">未找到匹配的笔记</div>');
            return;
        }

        var groups = {};
        filtered.forEach(function (t) {
            var cat = t.category || '未分类';
            if (!groups[cat]) groups[cat] = [];
            groups[cat].push(t);
        });

        var sortedCats = Object.keys(groups).sort();
        var html = '';
        sortedCats.forEach(function (cat) {
            html += '<div class="note-cat">' + escHtml(cat) + '</div>';
            groups[cat].sort(function (a, b) { return a.name.localeCompare(b.name); });
            groups[cat].forEach(function (t) {
                var activeClass = (currentNote && currentNote.name === t.name) ? ' active' : '';
                html += '<div class="note-item' + activeClass + '" data-name="' + escHtml(t.name) + '">' + escHtml(t.name) + '</div>';
            });
        });
        $('#noteList').html(html);

        $('#noteList .note-item').on('click', function () {
            var name = $(this).data('name');
            loadNoteDetail(name);
        });
    }

    function loadNoteDetail(name) {
        $('#noteList .note-item').removeClass('active');
        $('#noteList .note-item[data-name="' + name + '"]').addClass('active');
        $('#noteToc').hide();
        $('.note-welcome').hide();
        $('.note-content').hide();

        $.ajax({
            url: 'note/' + encodeURIComponent(name), type: 'GET',
            success: function (data) {
                currentNote = data;
                renderNoteDetail(data);
                $('.note-content').show();
            },
            error: function () {
                $('.note-welcome').show();
                $('#noteBreadcrumb').text('记忆笔记');
            }
        });
    }

    function renderNoteDetail(note) {
        var res = renderMarkdown(note.content || '');
        $('#noteTitle').text(note.name);
        $('#noteCategory').text(note.category || '');
        $('#noteBody').html(res.html);

        var cat = note.category || '未分类';
        $('#noteBreadcrumb').html('记忆笔记<span class="sep">/</span>' + escHtml(cat));

        renderToc(res.toc);
    }

    function renderToc(toc) {
        if (!toc || toc.length === 0) {
            $('#noteToc').hide();
            $('#noteTocList').empty();
            return;
        }
        $('#noteToc').show();
        var html = '';
        toc.forEach(function (t) {
            var cls = t.level >= 3 ? ' class="lvl-3"' : '';
            html += '<li' + cls + '><a data-target="' + t.id + '">' + escHtml(t.text) + '</a></li>';
        });
        $('#noteTocList').html(html);
        $('#noteTocList a').off('click').on('click', function () {
            var el = document.getElementById($(this).data('target'));
            if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
        });
    }

    function init() {
        if (loaded) return;
        loaded = true;
        loadNoteList();

        $('#noteSearch').on('input', function () {
            var keyword = $(this).val().trim().toLowerCase();
            renderNoteList(allNotes, keyword);
        });
    }

    return { init: init };
})();
