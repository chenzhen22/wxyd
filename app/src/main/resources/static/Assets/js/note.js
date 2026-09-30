/**
 * 记忆笔记前端逻辑（用户级 · 可编辑）
 * 双 tab：我的笔记（可新增/编辑/删除）/ 公共搜索（按标题搜公共笔记，只读）。
 * 编辑器为纯 textarea + renderMarkdown 实时预览；详情排版沿用左侧目录 + 居中文章 + 右侧 TOC。
 * 后端：note/list、note/search、note/get、note/save、note/delete（Result 包裹）。
 */
var Note = (function () {
    var allNotes = [];      // 我的笔记
    var mode = 'mine';      // mine | public
    var myId = null;
    var editing = null;     // {oldName, isNew}
    var currentDetail = null;
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
        if (m === 'mine') {
            loadMyNotes();
        } else {
            showView('welcome');
            $('#noteBreadcrumb').text('记忆笔记 / 公共搜索');
            loadPublic('');
        }
    }

    /* 公共笔记（keyword 为空时返回全部，按标题模糊搜索） */
    function loadPublic(keyword) {
        loading(true);
        $.getJSON('note/search' + (keyword ? '?keyword=' + encodeURIComponent(keyword) : ''), function (res) {
            loading(false);
            renderPublicList((res && res.body) || []);
        }).fail(function () {
            loading(false);
            $('#noteList').html('<div class="note-hint">加载失败</div>');
        });
    }

    function renderPublicList(list) {
        if (!list.length) {
            $('#noteList').html('<div class="note-hint">' + ($('#noteSearch').val().trim() ? '未找到相关公共笔记' : '暂无公共笔记') + '</div>');
            return;
        }
        var html = '';
        list.forEach(function (t) {
            html += '<div class="note-item" data-owner="' + t.ownerId + '" data-name="' + escHtml(t.name) + '">' +
                '<span class="note-item-name">' + escHtml(t.name) + '</span>' +
                '<span class="note-item-owner">' + escHtml(t.ownerName || '') + '</span></div>';
        });
        $('#noteList').html(html);
    }

    /* ---------------- 我的笔记 ---------------- */

    function loadMyNotes() {
        $.getJSON('note/list', function (res) {
            allNotes = (res && res.body) || [];
            renderMyList($('#noteSearch').val());
        }).fail(function () {
            $('#noteList').html('<div class="note-hint">加载失败</div>');
        });
    }

    function renderMyList(keyword) {
        var filtered = allNotes.filter(function (t) {
            if (!keyword) return true;
            var kw = keyword.toLowerCase();
            return (t.name && t.name.toLowerCase().indexOf(kw) !== -1)
                || (t.category && t.category.toLowerCase().indexOf(kw) !== -1);
        });
        if (filtered.length === 0) {
            $('#noteList').html('<div class="note-hint">' + (keyword ? '未找到匹配的笔记' : '暂无笔记，点击「＋ 新增笔记」创建') + '</div>');
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
                var vis = t.visibility === 'private'
                    ? '<span class="note-vis-badge private">私有</span>'
                    : '<span class="note-vis-badge public">公开</span>';
                html += '<div class="note-item" data-name="' + escHtml(t.name) + '">' +
                    '<span class="note-item-name">' + escHtml(t.name) + '</span>' + vis +
                    '<span class="note-item-ops">' +
                    '<button class="note-op" data-op="edit" data-name="' + escHtml(t.name) + '">编辑</button>' +
                    '<button class="note-op del" data-op="del" data-name="' + escHtml(t.name) + '">删除</button>' +
                    '</span></div>';
            });
        });
        $('#noteList').html(html);
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
            renderNoteDetail(res.body, mine);
            showView('content');
        }).fail(function () { loading(false); alterModal('加载失败'); });
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
        $('#noteBreadcrumb').html(mode === 'public'
            ? '记忆笔记<span class="sep">/</span>公共搜索<span class="sep">/</span>' + escHtml(note.ownerName || '')
            : '记忆笔记<span class="sep">/</span>' + escHtml(note.category || '未分类'));
        renderToc(res.toc);
        // 编辑按钮：进入编辑器
        $('#btnEditNote').off('click').on('click', function () {
            openEditor(currentDetail);
        });
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
            oldName: editing.oldName
        };
        if (!body.name) { alterModal('请输入标题'); return; }
        loading(true);
        $.ajax({
            url: 'note/save', type: 'POST', contentType: 'application/json',
            data: JSON.stringify({ body: body }),
            success: function (res) {
                loading(false);
                if (res.errorCode === '000000') {
                    alterModal('保存成功');
                    loadMyNotes();
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
                        loadMyNotes();
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

    /* ---------------- 初始化 ---------------- */

    function init() {
        if (loaded) return;
        loaded = true;

        $.getJSON('user/info', function (res) {
            myId = res && res.body && res.body.id;
            if (myId) $('#noteUserBadge').show();
        });

        $('#noteTabMine').on('click', function () { switchTab('mine'); });
        $('#noteTabPublic').on('click', function () { switchTab('public'); });
        $('#btnNewNote').on('click', function () { openEditor(null); });

        // 搜索框：我的 tab 本地过滤；公共 tab 服务端按标题搜索
        var searchTimer = null;
        $('#noteSearch').on('input', function () {
            var kw = $(this).val().trim();
            if (mode === 'mine') {
                renderMyList(kw);
            } else {
                clearTimeout(searchTimer);
                searchTimer = setTimeout(function () {
                    loadPublic(kw);
                }, 300);
            }
        });

        // 列表点击（动态内容，统一委托）
        $('#noteList').on('click', '.note-item', function (e) {
            if ($(e.target).hasClass('note-op')) return;
            var $t = $(this);
            var ownerId = $t.data('owner');
            openDetail(ownerId != null ? ownerId : myId, $t.data('name'), mode === 'mine');
        });
        $('#noteList').on('click', '.note-op', function () {
            var op = $(this).data('op');
            var name = $(this).data('name');
            if (op === 'edit') {
                var note = allNotes.find(function (t) { return t.name === name; });
                // 列表无正文，拉全量再编辑
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

    return { init: init };
})();
