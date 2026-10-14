/* ============================================================
   金融日报（PC 桌面版）
   数据：/api/dayinfo/list、/api/dayinfo/latest、/api/dayinfo/detail（登录可看）
         /api/dayinfo/subscribe/*（每用户一条订阅提醒）
   纯展示：按日期选择查看，md 渲染复用 welfare-md.js（window.WelfareMd）
   注册：index.js showPage 懒加载 window.DayInfo.init()
   ============================================================ */
(function () {

    var state = { initialized: false, list: [], currentId: null, subLoaded: false };

    function esc(s) {
        return s == null ? '' : String(s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function postJSON(url, body) {
        return $.ajax({ url: url, type: 'POST', contentType: 'application/json',
            data: JSON.stringify({ body: body || {} }) });
    }

    function showArticle(info) {
        if (!info || !info.id) {
            $('#dayinfoArticle').hide();
            $('#dayinfoEmpty').show();
            return;
        }
        $('#dayinfoEmpty').hide();
        $('#dayinfoArticle').show().html(
            '<h1 class="wf-title">' + esc(info.title) + '</h1>' +
            '<div class="wf-meta">' + esc(info.infoDate || '') +
            (info.updateTime ? ' · 更新于 ' + esc(info.updateTime) : '') + '</div>' +
            '<div class="wf-body">' + window.WelfareMd.render(info.content) + '</div>');
    }

    function loadDetail(id) {
        state.currentId = id;
        $('#dayinfoArticle').html('<div class="welfare-empty">加载中…</div>').show();
        $.getJSON('dayinfo/detail', { id: id }).done(function (res) {
            if (res.errorCode !== '000000' || !res.body) {
                $('#dayinfoArticle').html('<div class="welfare-empty">' + esc(res.errorMsg || '加载失败') + '</div>');
                return;
            }
            showArticle(res.body);
        }).fail(function () {
            $('#dayinfoArticle').html('<div class="welfare-empty">加载失败，请稍后重试</div>');
        });
    }

    function loadList(preferId) {
        $.getJSON('dayinfo/list', { pageNum: 1, pageSize: 50 }).done(function (res) {
            var b = (res.errorCode === '000000' && res.body) || {};
            state.list = b.list || [];
            var $sel = $('#dayinfoDateSel').html('');
            if (!state.list.length) {
                $('#dayinfoArticle').hide();
                $('#dayinfoEmpty').show();
                return;
            }
            state.list.forEach(function (d) {
                $sel.append('<option value="' + d.id + '">' + esc(d.infoDate) + '</option>');
            });
            var target = preferId;
            if (!target) {
                // 默认最新一期（列表按日期倒序，首条即最新）
                target = state.list[0].id;
            }
            $sel.val(String(target));
            loadDetail(target);
        }).fail(function () {
            $('#dayinfoArticle').html('<div class="welfare-empty">加载失败，请稍后重试</div>').show();
        });
    }

    /* ================= 订阅提醒 ================= */

    function fillSubForm(s) {
        var has = s && s.id;
        $('#dayinfoSubChannel').val(has ? (s.channel || 'dingtalk') : 'dingtalk');
        $('#dayinfoSubEmail').val(has ? (s.email || '') : '');
        var fillRobot = function () {
            if (has && s.robotId) $('#dayinfoSubRobot').val(String(s.robotId));
        };
        if (!$('#dayinfoSubRobot option').length) {
            $.getJSON('robot/list').done(function (r) {
                var list = (r && r.body) || [];
                var html = '';
                list.forEach(function (rb) {
                    html += '<option value="' + rb.id + '">' + esc(rb.name || ('机器人 #' + rb.id)) + '</option>';
                });
                $('#dayinfoSubRobot').html(html);
                fillRobot();
            });
        } else {
            fillRobot();
        }
        $('#dayinfoSubChannel').change();
        $('#dayinfoSubOff').text(has && s.enabled == 1 ? '暂停提醒' : '恢复提醒');
    }

    function loadSubscribe() {
        $.getJSON('dayinfo/subscribe/my').done(function (res) {
            fillSubForm(res.errorCode === '000000' ? res.body : null);
        });
    }

    function init() {
        if (state.initialized) return;
        state.initialized = true;

        // 日期切换
        $('#dayinfoDateSel').on('change', function () {
            var id = $(this).val();
            if (id) loadDetail(id);
        });

        // 订阅卡片显隐
        $('#dayinfoSubBtn').on('click', function () {
            var $c = $('#dayinfoSubCard');
            $c.toggle();
            if ($c.is(':visible')) loadSubscribe();
        });

        // 渠道切换：钉钉选机器人 / 邮件填邮箱
        $('#dayinfoSubChannel').on('change', function () {
            var isDing = $(this).val() === 'dingtalk';
            $('#dayinfoSubRobot').toggle(isDing);
            $('#dayinfoSubEmail').toggle(!isDing);
        });

        // 保存订阅
        $('#dayinfoSubSave').on('click', function () {
            var payload = { channel: $('#dayinfoSubChannel').val(), enabled: true };
            if (payload.channel === 'dingtalk') {
                payload.robotId = $('#dayinfoSubRobot').val();
                if (!payload.robotId) { alterModal('请先在「钉钉机器人」中添加机器人'); return; }
            } else {
                payload.email = $('#dayinfoSubEmail').val().trim();
                if (!payload.email) { alterModal('请填写接收邮箱'); return; }
            }
            loading(true);
            postJSON('dayinfo/subscribe/save', payload).done(function (res) {
                loading(false);
                if (res.errorCode === '000000') {
                    alterModal('订阅成功，新日报入库时会提醒你');
                    fillSubForm(res.body);
                } else {
                    alterModal(res.errorMsg || '保存失败');
                }
            }).fail(function () { loading(false); });
        });

        // 暂停 / 恢复
        $('#dayinfoSubOff').on('click', function () {
            var resume = $(this).text() === '恢复提醒';
            loading(true);
            postJSON('dayinfo/subscribe/toggle', { enabled: resume }).done(function (res) {
                loading(false);
                if (res.errorCode === '000000') {
                    alterModal(resume ? '已恢复提醒' : '已暂停提醒');
                    $('#dayinfoSubOff').text(resume ? '暂停提醒' : '恢复提醒');
                } else {
                    alterModal(res.errorMsg || '操作失败');
                }
            }).fail(function () { loading(false); });
        });

        loadList();
    }

    window.DayInfo = { init: init };
})();
