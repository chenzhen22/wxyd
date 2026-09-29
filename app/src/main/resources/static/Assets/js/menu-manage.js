/* ============================================================
   菜单管理（桌面版 + H5 共用）— 仅管理员
   四态模型：全部可见 / 全部隐藏 / 仅指定用户可见(白名单) / 指定用户不可见(黑名单)
   管理员始终可见全部菜单（服务端保障）。
   桌面挂载：MenuManage.mount(container)；H5：goPage('menuManage') 自动挂载。
   ============================================================ */
(function () {

    var state = { menus: [], users: [], pending: {} };
    // pending[key] = { mode: 'all'|'none'|'whitelist'|'blacklist', ids: Set(userId) }

    function esc(s) {
        return s == null ? '' : String(s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function mmMsg(msg) {
        if (typeof window.toast === 'function') window.toast(msg);
        else if (typeof window.alterModal === 'function') window.alterModal(msg);
        else alert(msg);
    }

    function modeOf(m) {
        var hasIds = m.userIds && String(m.userIds).length > 0;
        if (Number(m.visible) === 1) return hasIds ? 'whitelist' : 'all';
        return hasIds ? 'blacklist' : 'none';
    }

    var MODE_LABEL = { all: '全部可见', none: '全部隐藏', whitelist: '仅指定用户可见', blacklist: '指定用户不可见' };

    function initPending(m) {
        var key = m.key;
        if (!state.pending[key]) {
            var ids = new Set();
            if (m.userIds) String(m.userIds).split(',').forEach(function (p) {
                if (p.trim()) ids.add(p.trim());
            });
            state.pending[key] = { mode: modeOf(m), ids: ids };
        }
        return state.pending[key];
    }

    function userName(id) {
        for (var i = 0; i < state.users.length; i++) {
            if (Number(state.users[i].id) === Number(id)) {
                return state.users[i].displayName || state.users[i].username || id;
            }
        }
        return id;
    }

    function summaryText(key) {
        var p = state.pending[key];
        if (!p) return '';
        if (p.mode === 'all') return '所有人';
        if (p.mode === 'none') return '—';
        if (!p.ids.size) return p.mode === 'whitelist' ? '（未选择用户）' : '（未选择用户，等同全部可见）';
        var names = Array.from(p.ids).map(function (id) { return esc(userName(id)); });
        var s = names.join('、');
        return s.length > 60 ? s.substring(0, 60) + '… 等' + p.ids.size + '人' : s;
    }

    function load(done) {
        var left = 2;
        var menus = null, users = null;
        function check() { if (--left === 0 && done) done(menus, users); }
        $.getJSON('menu/all', function (res) {
            menus = (res && res.errorCode === '000000' && res.body) || [];
            check();
        }).fail(function () { menus = []; check(); });
        $.getJSON('user/list', function (res) {
            users = (res && res.errorCode === '000000' && res.body) || [];
            check();
        }).fail(function () { users = []; check(); });
    }

    function save(key, onDone) {
        var p = state.pending[key];
        var m = state.menus.find(function (x) { return x.key === key; });
        if (!p || !m) return;
        if ((p.mode === 'whitelist' || p.mode === 'blacklist') && p.ids.size === 0) {
            mmMsg('请至少选择一名用户'); return;
        }
        var visible = (p.mode === 'all' || p.mode === 'whitelist') ? 1 : 0;
        var userIds = Array.from(p.ids).join(',');
        $.ajax({
            url: 'menu/save', type: 'POST', contentType: 'application/json',
            data: JSON.stringify({ body: { menuKey: key, visible: visible, userIds: userIds } }),
            success: function (res) {
                if (res && res.errorCode === '000000') {
                    // 同步本地状态
                    m.visible = visible; m.userIds = userIds;
                    if (onDone) onDone();
                    mmMsg('已保存');
                } else {
                    mmMsg((res && res.errorMsg) || '保存失败');
                }
            },
            error: function () { mmMsg('保存失败'); }
        });
    }

    /* ---------------- 桌面版 ---------------- */

    function mount(container) {
        var $c = $(container);
        if (!$c.length) return;
        if (!$c.data('mmMounted')) {
            $c.data('mmMounted', true);
            $c.html('<div style="color:var(--text-8);font-size:13px;margin-bottom:12px;">' +
                '说明：管理员始终可见全部菜单；未配置的菜单默认所有人可见。改动立即对所有用户生效（刷新页面后可见）。</div>' +
                '<table class="data-table"><thead><tr>' +
                '<th width="16%">菜单</th><th width="14%">标识</th><th width="24%">可见性</th>' +
                '<th width="26%">适用用户</th><th width="20%">操作</th>' +
                '</tr></thead><tbody id="mmTableBody"><tr><td colspan="5" style="text-align:center;color:#999">加载中…</td></tr></tbody></table>');
            bindDesktop($c);
        }
        load(function (menus, users) {
            state.menus = menus;
            state.users = users;
            state.pending = {};
            renderDesktop($c);
        });
    }

    function renderDesktop($c) {
        var html = '';
        state.menus.forEach(function (m) {
            initPending(m);
            html += '<tr>' +
                '<td>' + esc(m.name) + '</td>' +
                '<td style="color:var(--text-8)">' + esc(m.key) + '</td>' +
                '<td><select class="mm-mode" data-key="' + esc(m.key) + '">' +
                Object.keys(MODE_LABEL).map(function (k) {
                    return '<option value="' + k + '"' + (state.pending[m.key].mode === k ? ' selected' : '') + '>' + MODE_LABEL[k] + '</option>';
                }).join('') +
                '</select></td>' +
                '<td class="mm-usersum" data-key="' + esc(m.key) + '">' + summaryText(m.key) + '</td>' +
                '<td><button class="btn-main" data-mm="pick" data-key="' + esc(m.key) + '" style="padding:3px 10px;font-size:12px;">选择用户</button> ' +
                '<button class="btn-main" data-mm="save" data-key="' + esc(m.key) + '" style="padding:3px 10px;font-size:12px;">保存</button></td>' +
                '</tr>' +
                '<tr class="mm-picker" data-key="' + esc(m.key) + '" style="display:none"><td colspan="5" style="background:var(--glass-1)">' +
                '<div style="display:flex;flex-wrap:wrap;gap:10px;padding:8px;">' +
                state.users.map(function (u) {
                    var checked = state.pending[m.key].ids.has(String(u.id)) ? ' checked' : '';
                    return '<label style="cursor:pointer;font-size:13px;color:var(--text-2)">' +
                        '<input type="checkbox" class="mm-user-cb" data-key="' + esc(m.key) + '" value="' + u.id + '"' + checked + '> ' +
                        esc(u.displayName || u.username || u.id) + '</label>';
                }).join('') +
                '</div></td></tr>';
        });
        $c.find('#mmTableBody').html(html || '<tr><td colspan="5" style="text-align:center;color:#999">加载失败</td></tr>');
    }

    function bindDesktop($c) {
        $c.on('change', '.mm-mode', function () {
            var key = $(this).data('key');
            state.pending[key].mode = $(this).val();
            $c.find('.mm-usersum[data-key="' + key + '"]').html(summaryText(key));
            // 名单模式变化时展开/收起选择器
            var need = state.pending[key].mode === 'whitelist' || state.pending[key].mode === 'blacklist';
            $c.find('.mm-picker[data-key="' + key + '"]').toggle(need);
        });
        $c.on('click', '[data-mm="pick"]', function () {
            var key = $(this).data('key');
            $c.find('.mm-picker[data-key="' + key + '"]').toggle();
        });
        $c.on('change', '.mm-user-cb', function () {
            var key = $(this).data('key');
            var id = String($(this).val());
            if (this.checked) state.pending[key].ids.add(id);
            else state.pending[key].ids.delete(id);
            $c.find('.mm-usersum[data-key="' + key + '"]').html(summaryText(key));
        });
        $c.on('click', '[data-mm="save"]', function () {
            var $btn = $(this);
            save($btn.data('key'), function () { renderDesktop($c); });
        });
    }

    /* ---------------- H5 ---------------- */

    function renderH5($page) {
        $page.html('<div class="h5-section-title">菜单管理</div>' +
            '<div class="h5-alert h5-alert-info">管理员始终可见全部菜单；未配置的菜单默认所有人可见</div>' +
            '<div id="mmList"><div class="h5-empty">加载中…</div></div>');
        load(function (menus, users) {
            state.menus = menus;
            state.users = users;
            state.pending = {};
            renderH5List();
        });
    }

    function renderH5List() {
        var html = '';
        state.menus.forEach(function (m) {
            initPending(m);
            html += '<div class="h5-card mm-card-h5" data-key="' + esc(m.key) + '" style="cursor:pointer">' +
                '<div class="h5-card-row"><div style="flex:1;min-width:0">' +
                '<div class="h5-card-title">' + esc(m.name) + '</div>' +
                '<div class="h5-card-sub">' + MODE_LABEL[state.pending[m.key].mode] +
                (state.pending[m.key].ids.size ? ' · ' + state.pending[m.key].ids.size + ' 名用户' : '') + '</div>' +
                '</div><span style="color:var(--text-6)">›</span></div></div>';
        });
        $('#mmList').html(html || '<div class="h5-empty">加载失败</div>');
    }

    function openEditorH5(key) {
        var m = state.menus.find(function (x) { return x.key === key; });
        if (!m) return;
        var p = initPending(m);
        openSheet('<div class="h5-sheet-head"><h3>' + esc(m.name) + '</h3></div>' +
            '<div class="h5-field"><label>可见性</label><select class="h5-select" id="mmMode">' +
            Object.keys(MODE_LABEL).map(function (k) {
                return '<option value="' + k + '"' + (p.mode === k ? ' selected' : '') + '>' + MODE_LABEL[k] + '</option>';
            }).join('') + '</select></div>' +
            '<div class="h5-field"><label>适用用户（白名单/黑名单时生效）</label>' +
            '<div style="max-height:200px;overflow-y:auto;border:1px solid var(--border-1);border-radius:8px;padding:8px">' +
            state.users.map(function (u) {
                var checked = p.ids.has(String(u.id)) ? ' checked' : '';
                return '<label style="display:block;padding:4px 0;cursor:pointer;color:var(--text-2);font-size:13.5px">' +
                    '<input type="checkbox" class="mm-user-cb-h5" value="' + u.id + '"' + checked + '> ' +
                    esc(u.displayName || u.username || u.id) + '</label>';
            }).join('') + '</div></div>' +
            '<button class="h5-btn-primary h5-btn-block" id="mmSave">保存</button>');
        $('#mmMode').on('change', function () { p.mode = $(this).val(); });
        $('.mm-user-cb-h5').on('change', function () {
            var id = String($(this).val());
            if (this.checked) p.ids.add(id); else p.ids.delete(id);
        });
        $('#mmSave').on('click', function () {
            save(key, function () { closeSheet(); renderH5List(); });
        });
    }

    /* ---------------- 对外接口 ---------------- */

    window.MenuManage = { mount: mount };

    if (window.registerH5Module) {
        window.registerH5Module('menuManage', function ($page) {
            window.MenuManage.mount($page[0]);
            $page.off('click', '.mm-card-h5').on('click', '.mm-card-h5', function () {
                openEditorH5($(this).data('key'));
            });
        });
    }
})();
