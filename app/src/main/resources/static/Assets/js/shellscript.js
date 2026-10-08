/**
 * Shell 脚本学习平台前端逻辑
 * 参考 javaapi.js，适配暗色玻璃风格 + jQuery 1.8.3
 * 与 Java API 不同：纯只读展示，无代码编辑器和运行按钮
 */
var ShellScript = (function () {
    var allTopics = [];
    var currentTopic = null;
    var loaded = false;

    function escHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function renderMarkdown(md) {
        if (!md) return '';
        var html = md
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/```(\w*)\n?([\s\S]*?)```/g, function (match, lang, code) {
                return '<pre><code>' + escHtml(code.trim()) + '</code></pre>';
            })
            .replace(/`([^`]+)`/g, '<code>$1</code>')
            .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
            .split(/\n\n+/)
            .map(function (p) {
                p = p.trim();
                if (!p) return '';
                if (p.match(/^- /)) {
                    var items = p.split('\n').filter(function (l) { return l.trim(); });
                    var listHtml = '<ul>';
                    items.forEach(function (item) { listHtml += '<li>' + item.replace(/^- /, '') + '</li>'; });
                    listHtml += '</ul>';
                    return listHtml;
                }
                return '<p>' + p.replace(/\n/g, '<br>') + '</p>';
            })
            .join('');
        return html;
    }

    function loadTopicList() {
        $.ajax({
            url: 'shellscript/topics', type: 'GET',
            success: function (data) {
                allTopics = data || [];
                renderTopicList(allTopics, '');
            },
            error: function () {
                $('#ssTopicList').html('<div style="padding:20px;text-align:center;color:rgba(255,255,255,.4)">加载失败</div>');
            }
        });
    }

    function renderTopicList(topics, keyword) {
        var filtered = topics.filter(function (t) {
            if (!keyword) return true;
            var kw = keyword.toLowerCase();
            return t.name.toLowerCase().indexOf(kw) !== -1
                || (t.category && t.category.toLowerCase().indexOf(kw) !== -1)
                || (t.intro && t.intro.toLowerCase().indexOf(kw) !== -1);
        });

        if (filtered.length === 0) {
            $('#ssTopicList').html('<div style="padding:20px;text-align:center;color:rgba(255,255,255,.4)">未找到匹配的主题</div>');
            return;
        }

        var groups = {};
        filtered.forEach(function (t) {
            var letter = t.name.charAt(0).toUpperCase();
            if (!groups[letter]) groups[letter] = [];
            groups[letter].push(t);
        });

        var sortedLetters = Object.keys(groups).sort();
        var html = '';
        sortedLetters.forEach(function (letter) {
            html += '<div class="ss-letter">' + escHtml(letter) + '</div>';
            groups[letter].sort(function (a, b) { return a.name.localeCompare(b.name); });
            groups[letter].forEach(function (t) {
                var activeClass = (currentTopic && currentTopic.name === t.name) ? ' active' : '';
                html += '<div class="ss-item' + activeClass + '" data-name="' + escHtml(t.name) + '">' + escHtml(t.name) + '</div>';
            });
        });
        $('#ssTopicList').html(html);

        $('#ssTopicList .ss-item').on('click', function () {
            var name = $(this).data('name');
            loadTopicDetail(name);
        });
    }

    function loadTopicDetail(name) {
        $('#ssTopicList .ss-item').removeClass('active');
        $('#ssTopicList .ss-item[data-name="' + name + '"]').addClass('active');
        $('.shellscript-welcome').hide();
        $('.shellscript-content').hide();
        loading(true);
        $("#loadingMsg").text("正在加载脚本主题详情…");

        $.ajax({
            url: 'shellscript/topics/' + encodeURIComponent(name), type: 'GET',
            success: function (data) {
                loading(false);
                currentTopic = data;
                renderTopicDetail(data);
                $('.shellscript-content').show();
            },
            error: function () {
                loading(false);
                $('.shellscript-welcome').show();
            }
        });
    }

    function renderTopicDetail(topic) {
        $('#ssTitle').text(topic.name);
        $('#ssCategory').text(topic.category || '');
        $('#ssIntro').html(renderMarkdown(topic.intro || ''));
        renderSyntaxes(topic.syntaxes || []);
        renderExamples(topic.examples || []);
        renderSourceNotes(topic.name, topic.category);
    }

    /**
     * 一键沉淀（R2）：把当前条目完整存成我的私有笔记。
     * 内容按资料页的原有结构拼装（介绍 / 语法 / 示例），保证笔记可独立阅读。
     */
    function saveCurrentTopic() {
        if (!currentTopic) return;
        var t = currentTopic;
        var md = ['## 介绍', t.intro || '', '## 语法'];
        (t.syntaxes || []).forEach(function (s) {
            md.push('### ' + s.name);
            if (s.signature) md.push('```bash', s.signature, '```');
            if (s.description) md.push(s.description);
        });
        md.push('## 示例');
        (t.examples || []).forEach(function (ex) {
            md.push('### ' + ex.name);
            if (ex.description) md.push(ex.description);
            if (ex.code) md.push('```bash', ex.code, '```');
        });
        if (typeof Note !== 'undefined' && Note.saveFromSource) {
            Note.saveFromSource('shell/' + t.name, t.name, t.category, md.join('\n'), function (ok, msg) {
                var $b = $('#btnSaveNote');
                if (ok) {
                    $b.text('已存入').addClass('done');
                    setTimeout(function () { $b.text('存入笔记').removeClass('done'); }, 2000);
                    alterModal('已存为私有笔记：' + msg);
                } else {
                    alterModal(msg || '保存失败');
                }
            });
        } else {
            alterModal('笔记模块未就绪');
        }
    }

    /** 反向关联（R3）：展示有多少公开笔记引用了当前条目 */
    function renderSourceNotes(name, category) {
        if (typeof Note === 'undefined' || !Note.listSourceNotes) { return; }
        Note.listSourceNotes('shell/' + name, function (list) {
            if (!list || !list.length) { $('#ssSourceNotes').remove(); return; }
            var html = '<div class="source-note-bar">关于此条目的笔记（' + list.length + '）：';
            list.forEach(function (n, i) {
                html += (i > 0 ? '、' : '')
                    + '<a data-owner="' + n.ownerId + '" data-name="' + escHtml(n.name) + '">'
                    + escHtml(n.name) + '</a>';
            });
            html += '</div>';
            $('#ssSourceNotes').remove();
            $('#ssExamples').after(html);
            $('#ssSourceNotes a').off('click').on('click', function () {
                openNote($(this).data('owner'), $(this).data('name'));
            });
        });
    }

    /** 跳到笔记模块并打开该篇（详情在 api.html 的笔记页） */
    function openNote(ownerId, name) {
        var u = 'note.html?ownerId=' + encodeURIComponent(ownerId)
              + '&name=' + encodeURIComponent(name);
        window.open(u, '_blank');
    }

    function renderSyntaxes(syntaxes) {
        if (!syntaxes || syntaxes.length === 0) {
            $('#ssSyntaxes').html('<p style="color:rgba(255,255,255,.4)">暂无语法信息</p>');
            return;
        }
        var html = '<table class="ss-syntaxes-table"><thead><tr><th style="width:30%">语法</th><th style="width:70%">说明</th></tr></thead><tbody>';
        syntaxes.forEach(function (s) {
            var sigHtml = s.signature ? '<div class="ss-syntax-sig">' + escHtml(s.signature) + '</div>' : '';
            html += '<tr><td><div class="ss-syntax-name">' + escHtml(s.name) + '</div>' + sigHtml + '</td><td>' + escHtml(s.description) + '</td></tr>';
        });
        html += '</tbody></table>';
        $('#ssSyntaxes').html(html);
    }

    function renderExamples(examples) {
        if (!examples || examples.length === 0) {
            $('#ssExamples').html('<p style="color:rgba(255,255,255,.4)">暂无示例</p>');
            return;
        }
        var html = '';
        examples.forEach(function (ex) {
            html += '<div class="ss-example">';
            html += '<div class="ss-example-title">' + escHtml(ex.name) + '</div>';
            if (ex.description) {
                html += '<div class="ss-example-desc">' + escHtml(ex.description) + '</div>';
            }
            if (ex.code) {
                html += '<pre class="ss-example-code"><code>' + escHtml(ex.code) + '</code></pre>';
            }
            html += '</div>';
        });
        $('#ssExamples').html(html);
    }

    function init() {
        if (loaded) return;
        loaded = true;
        loadTopicList();

        $('#ssSearch').on('input', function () {
            var keyword = $(this).val().trim().toLowerCase();
            renderTopicList(allTopics, keyword);
        });

        var $btn = $('#btnSaveNote');
        if ($btn.length) {
            $btn.on('click', function () { saveCurrentTopic(); });
        }
    }

    return { init: init };
})();