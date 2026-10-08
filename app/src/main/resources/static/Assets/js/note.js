/**
 * 记忆笔记前端逻辑（用户级 · 可编辑）—— 个人知识库 v2
 * 检索走服务端（note/search?keyword&scope&field），支持标题+正文全文命中。
 * 双 tab：我的笔记（可新增/编辑/删除）/ 公共搜索（他人公开笔记，只读）。
 * 编辑器为纯 textarea + renderMarkdown 实时预览；详情排版沿用左侧目录 + 居中文章 + 右侧 TOC。
 * 后端：note/list、note/search、note/get、note/save、note/delete、
 *       note/saveFromSource、note/categories、note/tags、note/rebuildIndex。
 *
 * ⚠️ Q1 决策（产品）：他人公开笔记的正文不入库，只能按标题搜索。
 *    前端在 scope 包含他人时必须给出明确文案，避免用户误以为功能失效。
 */
var Note = (function () {
    var allNotes = [];      // 当前列表
    var mode = 'mine';      // mine | public
    var myId = null;
    var editing = null;     // {oldName, isNew}
    var currentDetail = null;
    var loaded = false;
    var activeTag = null;   // 当前标签筛选（null=不限）

    function escHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    // 行内格式：先转义，再处理 `code`、**bold** 与 [[wiki 链接]]（转义后安全）
    function inline(s) {
        return escHtml(s)
            .replace(/`([^`]+)`/g, '<code>$1</code>')
            .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
            .replace(/\[\[([^\[\]]+)\]\]/g, function (m, title) {
                // R7 双向链接：点击后经 note/resolve 解析再跳转（服务端保证只解析可见笔记）
                return '<a href="javascript:;" class="note-wikilink" data-wiki="' + title.trim() + '">' + title.trim() + '</a>';
            });
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

    /** 搜索结果预览：把命中的关键词高亮（先转义再插 mark，避免 XSS） */
    function highlight(text, keyword) {
        var safe = escHtml(text || '');
        if (!keyword) return safe;
        var kw = escHtml(keyword);
        if (!kw) return safe;
        try {
            var re = new RegExp(kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'gi');
            return safe.replace(re, function (m) { return '<mark>' + m + '</mark>'; });
        } catch (e) {
            return safe;
        }
    }

    /** sourceKey → 资料页跳转 URL（正向关联 R3）；dubbo 无独立资料页返回 null */
    function sourceUrl(key) {
        if (!key) return null;
        var parts = String(key).split('/');
        var kind = parts[0];
        var name = parts[1];
        if (!name) return null;
        if (kind === 'javaapi') return 'javaapi.html?topic=' + encodeURIComponent(name);
        if (kind === 'shell') return 'shellscript.html?topic=' + encodeURIComponent(name);
        return null;
    }

    function sourceLabel(key) {
        if (!key) return '';
        var parts = String(key).split('/');
        var kind = parts[0] === 'javaapi' ? 'Java API'
            : parts[0] === 'shell' ? 'Shell 脚本'
            : parts[0] === 'dubbo' ? 'Dubbo 调用' : '资料';
        return kind + ' / ' + (parts[1] || '');
    }

    /* ---------------- 视图切换 ---------------- */

    function showView(view) {
        $('.note-welcome').toggle(view === 'welcome');
        $('#noteContent').toggle(view === 'content');
        $('#noteEditor').toggle(view === 'editor');
        if (view !== 'content') $('#noteToc').hide();
    }

    function switchTab(m) {
        mode = m;
        $('#noteTabMine').toggleClass('active', m === 'mine');
        $('#noteTabPublic').toggleClass('active', m === 'public');
        $('#btnNewNote').toggle(m === 'mine');
        $('#noteSearch').val('');
        $('#noteScope').val(m === 'public' ? 'public' : 'mine');
        activeTag = null;
        loadTags();
        if (m === 'public') {
            showView('welcome');
            $('#noteBreadcrumb').text('记忆笔记 / 公共搜索');
        }
        refreshScopeHint();
        reload();
    }

    /** 范围提示：他人笔记只能按标题搜（Q1 决策），必须如实告知用户 */
    function refreshScopeHint() {
        var scope = $('#noteScope').val();
        var field = $('#noteField').val();
        var $h = $('#noteScopeHint');
        if (scope !== 'mine' && field !== 'title') {
            $h.attr('class', 'note-hint-line warn')
               .text('他人笔记仅支持按标题搜索（正文不入库）');
        } else {
            $h.attr('class', 'note-hint-line').text('');
        }
    }

    /** 统一按当前 scope/field/keyword 走服务端检索 */
    function reload() {
        var kw = ($('#noteSearch').val() || '').trim();
        var scope = $('#noteScope').val();
        var field = $('#noteField').val();
        loading(true);
        var url = 'note/search?field=' + encodeURIComponent(field)
                + '&scope=' + encodeURIComponent(scope)
                + (kw ? '&keyword=' + encodeURIComponent(kw) : '');
        $.getJSON(url, function (res) {
            loading(false);
            allNotes = (res && res.body) || [];
            renderList(allNotes, kw);
        }).fail(function () {
            loading(false);
            $('#noteList').html('<div class="note-hint">加载失败</div>');
        });
    }

    function renderList(list, keyword) {
        if (activeTag) {
            list = list.filter(function (n) {
                return (n.tags || []).some(function (t) { return t === activeTag; });
            });
        }
        if (!list.length) {
            $('#noteList').html('<div class="note-hint">' + (keyword ? '未找到相关笔记' : '暂无笔记') + '</div>');
            return;
        }
        var mine = $('#noteScope').val() === 'mine';
        var groups = {};
        var order = [];
        list.forEach(function (n) {
            var g = (n.category || '未分类') + '\u0000' + (mine ? '' : (n.ownerName || ''));
            if (!groups[g]) { groups[g] = []; order.push({ key: g, cat: n.category || '未分类', owner: n.ownerName || '' }); }
            groups[g].push(n);
        });
        order.sort(function (a, b) {
            return a.cat.localeCompare(b.cat) || a.owner.localeCompare(b.owner);
        });
        var html = '';
        order.forEach(function (g) {
            html += '<div class="note-cat">' + escHtml(g.cat)
                    + (g.owner ? '<span class="note-item-owner"> · ' + escHtml(g.owner) + '</span>' : '')
                    + '</div>';
            groups[g.key].forEach(function (n) {
                var isMine = mine || (myId != null && n.ownerId === myId);
                var vis = n.visibility === 'private'
                    ? '<span class="note-vis-badge private">私有</span>'
                    : '<span class="note-vis-badge public">公开</span>';
                var badge = '';
                if (n.titleMatched) badge += '<span class="note-item-badge title">标题命中</span>';
                else if (n.contentMatched) badge += '<span class="note-item-badge">正文命中</span>';
                // Q1：他人的正文不入库，明确提示"仅按标题匹配"，避免误以为搜不到是 bug
                if (n.contentSearchable === false) badge += '<span class="note-item-badge">仅标题</span>';
                var tags = (n.tags || []).slice(0, 3).map(function (t) {
                    return '<span class="note-item-tag">' + escHtml(t) + '</span>';
                }).join('');
                var ops = isMine
                    ? '<span class="note-item-ops">'
                      + '<button class="note-op" data-op="edit" data-name="' + escHtml(n.name) + '">编辑</button>'
                      + '<button class="note-op del" data-op="del" data-name="' + escHtml(n.name) + '">删除</button>'
                      + '</span>'
                    : '';
                html += '<div class="note-item" data-owner="' + (n.ownerId != null ? n.ownerId : myId)
                        + '" data-name="' + escHtml(n.name) + '">'
                    + '<div class="note-item-main">'
                    + '  <div style="display:flex;align-items:center;gap:6px;">'
                    + '    <span class="note-item-name">' + highlight(n.name, keyword) + '</span>'
                    + (isMine ? vis : '') + badge
                    + '  </div>'
                    + (n.preview ? '<div class="note-preview">' + highlight(n.preview, keyword) + '</div>' : '')
                    + (tags ? '<div class="note-item-tags" style="margin-top:3px;">' + tags + '</div>' : '')
                    + '</div>'
                    + ops
                    + '</div>';
            });
        });
        $('#noteList').html(html);
    }

    /** 标签筛选条（R5） */
    function loadTags() {
        $.getJSON('note/tags', function (res) {
            var list = (res && res.body) || [];
            if (!list.length) { $('#noteTagFilter').empty(); return; }
            var html = '<span class="note-tag-chip' + (activeTag ? '' : ' active') + '" data-tag="">全部</span>';
            list.forEach(function (t) {
                html += '<span class="note-tag-chip' + (activeTag === t.name ? ' active' : '')
                        + '" data-tag="' + escHtml(t.name) + '">' + escHtml(t.name)
                        + '<span class="cnt">' + t.cnt + '</span></span>';
            });
            $('#noteTagFilter').html(html);
        });
    }

    /** 分类联想数据源（R4） */
    function loadCategories() {
        $.getJSON('note/categories', function (res) {
            var body = (res && res.body) || {};
            var names = [];
            (body.preset || []).forEach(function (c) { names.push(c.name); });
            (body.custom || []).forEach(function (c) { names.push(c.name); });
            $('#noteCategoryList').html(names.map(function (n) {
                return '<option value="' + escHtml(n) + '">';
            }).join(''));
        });
    }

    /* ---------------- 详情 ---------------- */

    function openDetail(ownerId, name, mine) {
        loading(true);
        $.getJSON('note/get?ownerId=' + ownerId + '&name=' + encodeURIComponent(name), function (res) {
            loading(false);
            if (!res || res.errorCode !== '000000') {
                alterModal((res && res.errorMsg) || '笔记不存在');
                return;
            }
            currentDetail = res.body;
            pushRecent(ownerId, name);
            renderNoteDetail(res.body, mine);
            renderBacklinks(name);
            // R9 单篇导出：原生 <a> 下载（jQuery ajax 不便处理 blob 下载）
            $('#btnExportNote').attr('href', 'note/export?ownerId=' + ownerId
                + '&name=' + encodeURIComponent(name));
            $('#noteExportWrap').show();
            showView('content');
        }).fail(function () { loading(false); alterModal('加载失败'); });
    }

    /* ---------------- 最近浏览（R8，localStorage，仅本机本浏览器） ---------------- */

    var RECENT_KEY = 'wxydNoteRecent';
    var RECENT_MAX = 10;

    function getRecent() {
        try {
            var raw = localStorage.getItem(RECENT_KEY);
            var arr = raw ? JSON.parse(raw) : [];
            return Array.isArray(arr) ? arr : [];
        } catch (e) { return []; }
    }

    function pushRecent(ownerId, name) {
        var list = getRecent().filter(function (r) {
            return !(String(r.ownerId) === String(ownerId) && r.name === name);
        });
        list.unshift({ ownerId: ownerId, name: name, time: new Date().getTime() });
        if (list.length > RECENT_MAX) list = list.slice(0, RECENT_MAX);
        try { localStorage.setItem(RECENT_KEY, JSON.stringify(list)); } catch (e) { /* 存储满则忽略 */ }
        renderRecent();
    }

    function renderRecent() {
        var list = getRecent();
        var $box = $('#noteRecent');
        if (!list.length) { $box.hide(); return; }
        var html = '';
        list.forEach(function (r) {
            html += '<div class="note-item note-recent-item" data-owner="' + escHtml(r.ownerId)
                + '" data-name="' + escHtml(r.name) + '">'
                + '<span class="note-item-name">' + escHtml(r.name) + '</span></div>';
        });
        $('#noteRecentList').html(html);
        $box.show();
    }

    /* ---------------- 反向链接（R7） ---------------- */

    function renderBacklinks(name) {
        $.getJSON('note/backlinks?name=' + encodeURIComponent(name), function (res) {
            var list = (res && res.body) || [];
            var $b = $('#noteBacklinks');
            if (!list.length) { $b.hide().empty(); return; }
            var html = '<div class="note-backlinks-title">🔗 被以下笔记引用</div>';
            list.forEach(function (n) {
                html += '<a href="javascript:;" class="note-backlink" data-owner="' + n.ownerId
                    + '" data-name="' + escHtml(n.name) + '">'
                    + escHtml(n.name)
                    + (n.ownerId === myId ? '' : ' <span class="note-item-owner">' + escHtml(n.ownerName || '') + '</span>')
                    + '</a>';
            });
            $b.html(html).show();
        });
    }

    function renderNoteDetail(note, mine) {
        var res = renderMarkdown(note.content || '');
        $('#noteTitle').text(note.name);
        $('#noteCategory').text((note.ownerName ? escHtml(note.ownerName) + ' · ' : '') + (note.category || ''));
        $('#noteCategory').html(escHtml(note.ownerName || '') + (note.category ? ' · ' + escHtml(note.category) : ''));
        var visBadge = $('#noteVisBadge');
        if (mine) {
            visBadge.text(note.visibility === 'private' ? '私有' : '公开')
                .attr('class', 'note-vis-badge ' + (note.visibility === 'private' ? 'private' : 'public'));
            visBadge.show();
            $('#noteEditWrap').show();
        } else {
            visBadge.hide();
            $('#noteEditWrap').hide();
        }
        $('#noteBody').html(res.html);
        renderNoteTags(note);
        renderNoteSource(note);
        $('#noteBreadcrumb').html(mode === 'public'
            ? '记忆笔记<span class="sep">/</span>公共搜索<span class="sep">/</span>' + escHtml(note.ownerName || '')
            : '记忆笔记<span class="sep">/</span>' + escHtml(note.category || '未分类'));
        renderToc(res.toc);
        // 编辑按钮：进入编辑器
        $('#btnEditNote').off('click').on('click', function () {
            openEditor(currentDetail);
        });
    }

    /** 详情页标签行（R5）：点击标签可筛选 */
    function renderNoteTags(note) {
        var tags = note.tags || [];
        var $t = $('#noteTags');
        if (!tags.length) { $t.hide().empty(); return; }
        $t.show().html(tags.map(function (x) {
            return '<span class="note-tag-view" data-tag="' + escHtml(x) + '">' + escHtml(x) + '</span>';
        }).join(''));
    }

    /** 来源跳转条（R3 正向关联）：笔记 → 资料页 */
    function renderNoteSource(note) {
        var url = sourceUrl(note.source);
        var bar = $('#noteSourceBar');
        if (!url) { bar.hide(); return; }
        $('#noteSourceName').text(sourceLabel(note.source));
        $('#noteSourceLink').attr('href', url);
        bar.show();
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

    /* ---------------- 编辑器 ---------------- */

    function openEditor(note) {
        editing = note
            ? { oldName: note.name, isNew: false }
            : { oldName: '', isNew: true };
        $('#noteEditTitle').val(note ? note.name : '');
        $('#noteEditCategory').val(note ? (note.category || '') : '');
        $('#noteEditVis').val(note && note.visibility === 'private' ? 'private' : 'public');
        $('#noteEditTags').val(note && note.tags ? note.tags.join(', ') : '');
        $('#noteEditBody').val(note ? (note.content || '') : '');
        $('#noteEditPreview').hide();
        $('#btnNotePreview').text('预览');
        showView('editor');
        $('#noteBreadcrumb').text(editing.isNew ? '记忆笔记 / 新增' : '记忆笔记 / 编辑');
        $('#noteToc').hide();
    }

    function saveNote() {
        var body = {
            name: $('#noteEditTitle').val().trim(),
            category: $('#noteEditCategory').val().trim(),
            visibility: $('#noteEditVis').val(),
            content: $('#noteEditBody').val(),
            tags: $('#noteEditTags').val().split(/[,，]/).map(function (s) { return s.trim(); })
                     .filter(function (s) { return !!s; }),
            oldName: editing.oldName
        };
        // 摘录笔记保留来源，避免编辑一次就把 source 丢了（R3 断链）
        if (editing && editing.isNew === false && currentDetail && currentDetail.source) {
            body.source = currentDetail.source;
        }
        if (!body.name) { alterModal('请输入标题'); return; }
        loading(true);
        $.ajax({
            url: 'note/save', type: 'POST', contentType: 'application/json',
            data: JSON.stringify({ body: body }),
            success: function (res) {
                loading(false);
                if (res.errorCode === '000000') {
                    alterModal('保存成功');
                    loadTags();
                    reload();
                    openDetail(myId, res.body, true);
                } else {
                    alterModal(res.errorMsg || '保存失败');
                }
            },
            error: function () { loading(false); alterModal('保存失败'); }
        });
    }

    function deleteNote(name) {
        confirmModal('确定删除笔记「' + escHtml(name) + '」？不可恢复。', function () {
            loading(true);
            $.ajax({
                url: 'note/delete', type: 'POST', contentType: 'application/json',
                data: JSON.stringify({ body: { name: name } }),
                success: function (res) {
                    loading(false);
                    if (res.errorCode === '000000') {
                        alterModal('已删除');
                        loadTags();
                        reload();
                        showView('welcome');
                        $('#noteBreadcrumb').text('记忆笔记');
                    } else {
                        alterModal(res.errorMsg || '删除失败');
                    }
                },
                error: function () { loading(false); alterModal('删除失败'); }
            });
        });
    }

    /* ---------------- 一键沉淀（R2，供资料页调用） ---------------- */

    /**
     * 把资料页条目存为我的私有笔记。
     * @param {string} sourceKey   javaapi/{name} | shell/{name} | dubbo/{configId}
     * @param {string} sourceTitle 条目标题（用于生成「xxx（摘录）」）
     * @param {string} sourceCategory 来源分类
     * @param {string} content     条目 markdown 正文
     * @param {function} cb        回调 (ok, message)
     */
    function saveFromSource(sourceKey, sourceTitle, sourceCategory, content, cb) {
        if (!content || !content.trim()) {
            cb && cb(false, '摘录内容为空');
            return;
        }
        $.ajax({
            url: 'note/saveFromSource', type: 'POST', contentType: 'application/json',
            data: JSON.stringify({
                body: {
                    sourceKey: sourceKey,
                    sourceTitle: sourceTitle,
                    sourceCategory: sourceCategory,
                    content: content
                }
            }),
            success: function (res) {
                if (res && res.errorCode === '000000') {
                    cb && cb(true, (res.body && res.body.name) || '');
                } else {
                    cb && cb(false, (res && res.errorMsg) || '保存失败');
                }
            },
            error: function () { cb && cb(false, '保存失败'); }
        });
    }

    /** 已保存过该来源的笔记数（资料页展示「关于此条目的笔记」） */
    function listSourceNotes(sourceKey, cb) {
        $.getJSON('note/source/notes?sourceKey=' + encodeURIComponent(sourceKey), function (res) {
            cb && cb((res && res.body) || []);
        }).fail(function () { cb && cb([]); });
    }

    /* ---------------- 初始化 ---------------- */

    function init() {
        if (loaded) return;
        loaded = true;

        $.getJSON('user/info', function (res) {
            myId = res && res.body && res.body.id;
            if (myId) $('#noteUserBadge').show();
            loadTags();
            loadCategories();
            renderRecent();
        });

        $('#noteTabMine').on('click', function () { switchTab('mine'); });
        $('#noteTabPublic').on('click', function () { switchTab('public'); });
        $('#btnNewNote').on('click', function () { openEditor(null); });

        // 搜索：防抖 300ms 走后端全文检索
        var searchTimer = null;
        $('#noteSearch').on('input', function () {
            clearTimeout(searchTimer);
            searchTimer = setTimeout(reload, 300);
        });
        $('#noteScope, #noteField').on('change', function () {
            refreshScopeHint();
            reload();
        });

        // 标签筛选条
        $('#noteTagFilter').on('click', '.note-tag-chip', function () {
            var t = $(this).data('tag') || null;
            activeTag = t;
            loadTags();   // 刷新高亮
            reload();
        });
        // 详情页标签点击 → 回到列表并按该标签筛选
        $('#noteTags').on('click', '.note-tag-view', function () {
            activeTag = $(this).data('tag');
            showView('welcome');
            $('#noteSearch').val('');
            loadTags();
            reload();
            $('#noteBreadcrumb').html('记忆笔记<span class="sep">/</span>标签：' + escHtml(activeTag));
        });

        // 列表点击（动态内容，统一委托）
        $('#noteList').on('click', '.note-item', function (e) {
            if ($(e.target).hasClass('note-op')) return;
            var $t = $(this);
            var ownerId = $t.data('owner');
            var isMine = ownerId == null || (myId != null && ownerId === myId);
            openDetail(ownerId != null ? ownerId : myId, $t.data('name'), isMine);
        });

        // R7 wiki 链接：先 resolve 再跳（服务端只解析可见笔记）
        $('#noteBody').on('click', '.note-wikilink', function () {
            var title = $(this).data('wiki');
            $.getJSON('note/resolve?name=' + encodeURIComponent(title), function (res) {
                var n = res && res.body;
                if (n && n.ownerId != null) {
                    openDetail(n.ownerId, n.name, myId != null && n.ownerId === myId);
                } else {
                    alterModal('未找到笔记「' + title + '」（可能不存在或对您不可见）');
                }
            }).fail(function () { alterModal('解析失败'); });
        });

        // R7 反向链接 / R8 最近浏览：直接跳转
        $('#noteBacklinks').on('click', '.note-backlink', function () {
            var ownerId = $(this).data('owner');
            openDetail(ownerId, $(this).data('name'), myId != null && ownerId === myId);
        });
        $('#noteRecentList').on('click', '.note-recent-item', function () {
            var ownerId = $(this).data('owner');
            openDetail(ownerId, $(this).data('name'), myId != null && ownerId === myId);
        });

        // R10 标签管理：重命名 / 合并 / 删除
        $('#btnTagManage').on('click', function () {
            var from = window.prompt('要修改的标签名：');
            if (!from || !from.trim()) return;
            var to = window.prompt('新标签名（留空 = 从所有笔记中删除该标签）：');
            if (to === null) return;
            $.ajax({
                url: 'note/tags/rename', type: 'POST', contentType: 'application/json',
                data: JSON.stringify({ body: { from: from.trim(), to: to.trim() } }),
                success: function (res) {
                    if (res && res.errorCode === '000000') {
                        alterModal('已更新 ' + (res.body || 0) + ' 篇笔记的标签');
                        loadTags();
                        reload();
                    } else {
                        alterModal((res && res.errorMsg) || '操作失败');
                    }
                },
                error: function () { alterModal('操作失败'); }
            });
        });
        $('#noteList').on('click', '.note-op', function () {
            var op = $(this).data('op');
            var name = $(this).data('name');
            if (op === 'edit') {
                $.getJSON('note/get?ownerId=' + myId + '&name=' + encodeURIComponent(name), function (res) {
                    if (res && res.errorCode === '000000') openEditor(res.body);
                    else alterModal('加载失败');
                });
            } else if (op === 'del') {
                deleteNote(name);
            }
        });

        $('#btnNoteSave').on('click', saveNote);
        $('#btnNoteCancel').on('click', function () {
            if (editing && !editing.isNew && currentDetail) {
                renderNoteDetail(currentDetail, true);
                showView('content');
            } else {
                showView('welcome');
                $('#noteBreadcrumb').text('记忆笔记');
            }
        });
        $('#btnNotePreview').on('click', function () {
            var $p = $('#noteEditPreview');
            if ($p.is(':visible')) {
                $p.hide();
                $('#noteEditBody').show();
                $(this).text('预览');
            } else {
                $p.html(renderMarkdown($('#noteEditBody').val()).html);
                $p.show();
                $('#noteEditBody').hide();
                $(this).text('编辑');
            }
        });

        switchTab('mine');
    }

    return {
        init: init,
        saveFromSource: saveFromSource,
        listSourceNotes: listSourceNotes
    };
})();
