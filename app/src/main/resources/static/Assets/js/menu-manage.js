/* ============================================================
   菜单管理（桌面版 + H5 共用）— 仅管理员
   双平台：桌面版(desktop) / H5 版(h5) 菜单相互独立，可切换管理。
   四态模型：全部可见 / 全部隐藏 / 仅指定用户可见(白名单) / 指定用户不可见(黑名单)
   管理员始终可见全部菜单（服务端保障）。
   桌面挂载：MenuManage.mount(container)；H5：goPage('menuManage') 自动挂载。
   ============================================================ */
(function () {

    var state = { menus: [], users: [], pending: {}, platform: 'desktop' };
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
        $.getJSON('menu/all?platform=' + state.platform, function (res) {
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
            data: JSON.stringify({ body: { platform: state.platform, menuKey: key, visible: visible, userIds: userIds } }),
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

    /* ---------------- 菜单项（图标/名称/URL/排序/仅管理员） ---------------- */

    function saveItem(payload, onDone) {
        $.ajax({
            url: 'menu/item/save', type: 'POST', contentType: 'application/json',
            data: JSON.stringify({ body: $.extend({ platform: state.platform }, payload) }),
            success: function (res) {
                if (res && res.errorCode === '000000') {
                    if (onDone) onDone();
                } else {
                    mmMsg((res && res.errorMsg) || '保存失败');
                }
            },
            error: function () { mmMsg('保存失败'); }
        });
    }

    function deleteItem(key, onDone) {
        var m = state.menus.find(function (x) { return x.key === key; });
        if (!confirm('确认删除菜单「' + (m ? m.name : key) + '」？')) return;
        $.ajax({
            url: 'menu/item/delete', type: 'POST', contentType: 'application/json',
            data: JSON.stringify({ body: { platform: state.platform, menuKey: key } }),
            success: function (res) {
                if (res && res.errorCode === '000000') {
                    if (onDone) onDone();
                    mmMsg('已删除');
                } else {
                    mmMsg((res && res.errorMsg) || '删除失败');
                }
            },
            error: function () { mmMsg('删除失败'); }
        });
    }

    // 从编辑区收集菜单项字段（客户端只做最基本校验，服务端为权威）
    function readFields($scope) {
        return {
            name: $.trim($scope.find('.mm-f-name').val() || ''),
            icon: $.trim($scope.find('.mm-f-icon').val() || ''),
            hashUrl: $.trim($scope.find('.mm-f-url').val() || ''),
            sortNo: parseInt($scope.find('.mm-f-sort').val(), 10) || 0,
            adminOnly: $scope.find('.mm-f-admin').prop('checked') ? 1 : 0
        };
    }

    function validateFields(f, needKey, menuKey) {
        if (needKey && !/^\w{1,50}$/.test(menuKey || '')) {
            mmMsg('菜单标识需为 1-50 位英文/数字/下划线'); return false;
        }
        if (!f.name) { mmMsg('请填写菜单名称'); return false; }
        return true;
    }

    /* ---------------- 平台切换（桌面版 + H5 共用） ---------------- */

    function platBtnHtml() {
        return '<button class="btn-main mm-plat" data-plat="desktop">桌面版</button> ' +
            '<button class="btn-main mm-plat" data-plat="h5">H5 版</button>';
    }

    function applyPlatBtns($scope) {
        $scope.find('.mm-plat').each(function () {
            var active = $(this).data('plat') === state.platform;
            $(this).css({
                background: active ? '#a78bfa' : '',
                color: active ? '#fff' : '',
                borderColor: active ? 'transparent' : ''
            });
        });
    }

    function switchPlatform(plat, reload) {
        if (plat === state.platform) return;
        state.platform = plat;
        state.pending = {};
        reload();
    }

    /* ---------------- 桌面版 ---------------- */

    function mount(container) {
        var $c = $(container);
        if (!$c.length) return;
        if (!$c.data('mmMounted')) {
            $c.data('mmMounted', true);
            $c.html('<div style="color:var(--text-8);font-size:13px;margin-bottom:12px;">' +
                '说明：管理员始终可见全部菜单；未配置的菜单默认所有人可见。改动立即对所有用户生效（刷新页面后可见）。' +
                '桌面版与 H5 版菜单相互独立，请分别配置。</div>' +
                '<div style="display:flex;align-items:center;gap:8px;margin-bottom:10px;">' +
                platBtnHtml() +
                '<span style="flex:1"></span>' +
                '<button class="btn-main" data-mm="add" style="padding:3px 10px;font-size:12px;">＋ 新增菜单</button>' +
                '</div>' +
                '<table class="data-table"><thead><tr>' +
                '<th width="6%">图标</th><th width="12%">名称</th><th width="12%">标识</th><th width="16%">URL</th>' +
                '<th width="15%">可见性</th><th width="19%">适用用户</th><th width="20%">操作</th>' +
                '</tr></thead><tbody id="mmTableBody">' +
                '<tr id="mmAddRow" style="display:none"><td colspan="7" style="background:var(--glass-1)">' +
                '<div style="display:flex;flex-wrap:wrap;gap:10px;padding:8px;align-items:center;font-size:13px;">' +
                '标识 <input class="mm-f-key" maxlength="50" placeholder="英文/数字/下划线" style="width:9em"> ' +
                '图标 <input class="mm-f-icon" maxlength="20" style="width:3.5em;text-align:center"> ' +
                '名称 <input class="mm-f-name" maxlength="50" style="width:7em"> ' +
                'URL <input class="mm-f-url" maxlength="100" placeholder="如 #/welfare，固定tab留空" style="width:13em"> ' +
                '排序 <input type="number" class="mm-f-sort" min="0" max="9999" value="0" style="width:4.5em"> ' +
                '<label style="cursor:pointer"><input type="checkbox" class="mm-f-admin"> 仅管理员</label>' +
                '<button class="btn-main" data-mm="addSave" style="padding:3px 10px;font-size:12px;">保存</button>' +
                '</div></td></tr>' +
                '<tr><td colspan="7" style="text-align:center;color:#999">加载中…</td></tr></tbody></table>');
            bindDesktop($c);
        }
        applyPlatBtns($c);
        reloadDesktop($c);
    }

    function reloadDesktop($c) {
        load(function (menus, users) {
            state.menus = menus;
            state.users = users;
            state.pending = {};
            renderDesktop($c);
        });
    }

    function itemEditorRowHtml(m) {
        return '<tr class="mm-edit" data-key="' + esc(m.key) + '" style="display:none"><td colspan="7" style="background:var(--glass-1)">' +
            '<div style="display:flex;flex-wrap:wrap;gap:10px;padding:8px;align-items:center;font-size:13px;">' +
            '图标 <input class="mm-f-icon" maxlength="20" value="' + esc(m.icon || '') + '" style="width:3.5em;text-align:center"> ' +
            '名称 <input class="mm-f-name" maxlength="50" value="' + esc(m.name) + '" style="width:7em"> ' +
            'URL <input class="mm-f-url" maxlength="100" value="' + esc(m.hashUrl || '') + '" placeholder="如 #/welfare，固定tab留空" style="width:13em"> ' +
            '排序 <input type="number" class="mm-f-sort" min="0" max="9999" value="' + Number(m.sortNo || 0) + '" style="width:4.5em"> ' +
            '<label style="cursor:pointer"><input type="checkbox" class="mm-f-admin"' + (Number(m.adminOnly) === 1 ? ' checked' : '') + '> 仅管理员</label>' +
            '<button class="btn-main" data-mm="editSave" data-key="' + esc(m.key) + '" style="padding:3px 10px;font-size:12px;">保存</button>' +
            '</div></td></tr>';
    }

    function renderDesktop($c) {
        var html = '';
        state.menus.forEach(function (m) {
            initPending(m);
            var fixed = Number(m.fixed) === 1;
            html += '<tr>' +
                '<td style="text-align:center">' + esc(m.icon || '—') + '</td>' +
                '<td>' + esc(m.name) + '</td>' +
                '<td style="color:var(--text-8)">' + esc(m.key) + '</td>' +
                '<td style="color:var(--text-8);font-size:12px">' + esc(m.hashUrl || '—') + '</td>' +
                '<td><select class="mm-mode" data-key="' + esc(m.key) + '">' +
                Object.keys(MODE_LABEL).map(function (k) {
                    return '<option value="' + k + '"' + (state.pending[m.key].mode === k ? ' selected' : '') + '>' + MODE_LABEL[k] + '</option>';
                }).join('') +
                '</select></td>' +
                '<td class="mm-usersum" data-key="' + esc(m.key) + '">' + summaryText(m.key) + '</td>' +
                '<td>' +
                '<button class="btn-main" data-mm="pick" data-key="' + esc(m.key) + '" style="padding:3px 10px;font-size:12px;">选择用户</button> ' +
                '<button class="btn-main" data-mm="edit" data-key="' + esc(m.key) + '" style="padding:3px 10px;font-size:12px;">编辑</button> ' +
                '<button class="btn-main" data-mm="save" data-key="' + esc(m.key) + '" style="padding:3px 10px;font-size:12px;">保存权限</button> ' +
                (fixed ? '' : '<button class="btn-main" data-mm="del" data-key="' + esc(m.key) + '" style="padding:3px 10px;font-size:12px;">删除</button>') +
                '</td>' +
                '</tr>' +
                '<tr class="mm-picker" data-key="' + esc(m.key) + '" style="display:none"><td colspan="7" style="background:var(--glass-1)">' +
                '<div style="display:flex;flex-wrap:wrap;gap:10px;padding:8px;">' +
                state.users.map(function (u) {
                    var checked = state.pending[m.key].ids.has(String(u.id)) ? ' checked' : '';
                    return '<label style="cursor:pointer;font-size:13px;color:var(--text-2)">' +
                        '<input type="checkbox" class="mm-user-cb" data-key="' + esc(m.key) + '" value="' + u.id + '"' + checked + '> ' +
                        esc(u.displayName || u.username || u.id) + '</label>';
                }).join('') +
                '</div></td></tr>' +
                itemEditorRowHtml(m);
        });
        $c.find('#mmTableBody tr:not(#mmAddRow)').remove();
        if (html) $c.find('#mmAddRow').after(html);
        else $c.find('#mmAddRow').after('<tr><td colspan="7" style="text-align:center;color:#999">加载失败</td></tr>');
    }

    function bindDesktop($c) {
        $c.on('click', '.mm-plat', function () {
            switchPlatform($(this).data('plat'), function () {
                $c.find('#mmAddRow').hide();
                applyPlatBtns($c);
                reloadDesktop($c);
            });
        });
        $c.on('click', '[data-mm="add"]', function () {
            $c.find('#mmAddRow').toggle();
        });
        $c.on('click', '[data-mm="addSave"]', function () {
            var $row = $c.find('#mmAddRow');
            var key = $.trim($row.find('.mm-f-key').val() || '');
            var f = readFields($row);
            if (!validateFields(f, true, key)) return;
            saveItem($.extend({ menuKey: key }, f), function () {
                $row.hide().find('input').not('[type=checkbox]').val('');
                $row.find('.mm-f-sort').val('0');
                $row.find('.mm-f-admin').prop('checked', false);
                mmMsg('已保存');
                reloadDesktop($c);
            });
        });
        $c.on('click', '[data-mm="edit"]', function () {
            var key = $(this).data('key');
            $c.find('.mm-picker[data-key="' + key + '"]').hide();
            $c.find('.mm-edit[data-key="' + key + '"]').toggle();
        });
        $c.on('click', '[data-mm="editSave"]', function () {
            var key = $(this).data('key');
            var $row = $c.find('.mm-edit[data-key="' + key + '"]');
            var f = readFields($row);
            if (!validateFields(f, false)) return;
            saveItem($.extend({ menuKey: key }, f), function () {
                mmMsg('已保存');
                reloadDesktop($c);
            });
        });
        $c.on('click', '[data-mm="del"]', function () {
            var key = $(this).data('key');
            deleteItem(key, function () { reloadDesktop($c); });
        });
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
            '<div style="display:flex;gap:8px;margin-bottom:10px">' + platBtnHtml() + '</div>' +
            '<div class="h5-alert h5-alert-info">管理员始终可见全部菜单；未配置的菜单默认所有人可见。桌面版与 H5 版菜单相互独立。</div>' +
            '<div id="mmList"><div class="h5-empty">加载中…</div></div>');
        applyPlatBtns($page);
        reloadH5();
    }

    function reloadH5() {
        load(function (menus, users) {
            state.menus = menus;
            state.users = users;
            state.pending = {};
            renderH5List();
        });
    }

    function renderH5List() {
        var html = '<div class="h5-card mm-add-h5" style="cursor:pointer;text-align:center;color:#a78bfa">＋ 新增菜单</div>';
        state.menus.forEach(function (m) {
            initPending(m);
            html += '<div class="h5-card mm-card-h5" data-key="' + esc(m.key) + '" style="cursor:pointer">' +
                '<div class="h5-card-row"><div style="flex:1;min-width:0">' +
                '<div class="h5-card-title">' + esc(m.icon ? m.icon + ' ' + m.name : m.name) + '</div>' +
                '<div class="h5-card-sub">' + MODE_LABEL[state.pending[m.key].mode] +
                (state.pending[m.key].ids.size ? ' · ' + state.pending[m.key].ids.size + ' 名用户' : '') + '</div>' +
                '</div><span style="color:var(--text-6)">›</span></div></div>';
        });
        $('#mmList').html(html || '<div class="h5-empty">加载失败</div>');
    }

    // H5 编辑弹层：m 为 null 表示新增
    function openEditorH5(m) {
        var isNew = !m;
        var key = m ? m.key : '';
        var p = isNew ? null : initPending(m);
        var html = '<div class="h5-sheet-head"><h3>' + (isNew ? '新增菜单' : esc(m.name)) + '</h3></div>' +
            '<div class="h5-field"><label>菜单标识' + (isNew ? '' : '（不可修改）') + '</label>' +
            '<input class="h5-input" id="mmFKey" maxlength="50" placeholder="英文/数字/下划线" value="' + esc(key) + '"' + (isNew ? '' : ' readonly') + '></div>' +
            '<div class="h5-field"><label>图标（≤20 字符）</label>' +
            '<input class="h5-input" id="mmFIcon" maxlength="20" value="' + esc(m ? m.icon || '' : '') + '"></div>' +
            '<div class="h5-field"><label>名称</label>' +
            '<input class="h5-input" id="mmFName" maxlength="50" value="' + esc(m ? m.name : '') + '"></div>' +
            '<div class="h5-field"><label>URL（固定 tab 留空）</label>' +
            '<input class="h5-input" id="mmFUrl" maxlength="100" placeholder="如 #/welfare" value="' + esc(m ? m.hashUrl || '' : '') + '"></div>' +
            '<div class="h5-field"><label>排序（0-9999）</label>' +
            '<input class="h5-input" id="mmFSort" type="number" min="0" max="9999" value="' + (m ? Number(m.sortNo || 0) : 0) + '"></div>' +
            '<div class="h5-field"><label><input type="checkbox" id="mmFAdmin"' + (m && Number(m.adminOnly) === 1 ? ' checked' : '') + '> 仅管理员可见</label></div>';
        if (!isNew) {
            html += '<div class="h5-field"><label>可见性</label><select class="h5-select" id="mmMode">' +
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
                }).join('') + '</div></div>';
        }
        html += '<button class="h5-btn-primary h5-btn-block" id="mmSave">保存</button>';
        openSheet(html);
        $('#mmMode').on('change', function () { p.mode = $(this).val(); });
        $('.mm-user-cb-h5').on('change', function () {
            var id = String($(this).val());
            if (this.checked) p.ids.add(id); else p.ids.delete(id);
        });
        $('#mmSave').on('click', function () {
            var f = {
                name: $.trim($('#mmFName').val() || ''),
                icon: $.trim($('#mmFIcon').val() || ''),
                hashUrl: $.trim($('#mmFUrl').val() || ''),
                sortNo: parseInt($('#mmFSort').val(), 10) || 0,
                adminOnly: $('#mmFAdmin').prop('checked') ? 1 : 0
            };
            var itemKey = isNew ? $.trim($('#mmFKey').val() || '') : key;
            if (!validateFields(f, isNew, itemKey)) return;
            // 先保存菜单项，再保存权限（新增菜单无权限配置，默认所有人可见）
            saveItem($.extend({ menuKey: itemKey }, f), function () {
                if (isNew) {
                    mmMsg('已保存');
                    closeSheet();
                    reloadH5();
                } else {
                    save(key, function () { closeSheet(); renderH5List(); });
                }
            });
        });
    }

    /* ---------------- 对外接口 ---------------- */

    window.MenuManage = { mount: mount };

    if (window.registerH5Module) {
        window.registerH5Module('menuManage', function ($page) {
            renderH5($page);
            $page.off('click', '.mm-card-h5').on('click', '.mm-card-h5', function () {
                var key = $(this).data('key');
                var m = state.menus.find(function (x) { return x.key === key; });
                if (m) openEditorH5(m);
            });
            $page.off('click', '.mm-add-h5').on('click', '.mm-add-h5', function () {
                openEditorH5(null);
            });
            $page.off('click', '.mm-plat').on('click', '.mm-plat', function () {
                var $btn = $(this);
                switchPlatform($btn.data('plat'), function () {
                    applyPlatBtns($page);
                    reloadH5();
                });
            });
        });
    }
})();
