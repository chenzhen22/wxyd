/* ============================================================
   金融日报（H5 移动端）
   数据：/api/dayinfo/list、/api/dayinfo/detail、/api/dayinfo/subscribe/*
   注册：registerH5Module('dayinfo', renderDayInfo)
   依赖：h5.js 的 esc / api / postJSON / toast / loading
         Assets/js/welfare-md.js 的 window.WelfareMd
   ============================================================ */
(function () {

    var PAGE_SIZE = 50;

    function renderDayInfo($page) {
        $page.html(
            '<div class="h5-section-title">金融日报</div>' +
            '<div style="display:flex;gap:8px;align-items:center;margin-bottom:10px;">' +
            '<select id="h5DayinfoDateSel" style="flex:1;padding:10px;border-radius:10px;background:var(--glass-3);color:var(--text-1);border:1px solid var(--glass-5);"></select>' +
            '<span class="h5-btn-ghost h5-btn-sm" id="h5DayinfoSubBtn">🔔 订阅</span>' +
            '</div>' +
            '<div id="h5DayinfoSub" style="display:none;margin-bottom:12px;padding:12px;border-radius:12px;background:var(--glass-2);border:1px solid var(--glass-5);">' +
            '<div style="display:flex;gap:8px;align-items:center;margin-bottom:8px;">' +
            '<select id="h5DayinfoSubChannel" style="flex:1;padding:8px;border-radius:8px;background:var(--glass-3);color:var(--text-1);border:1px solid var(--glass-5);">' +
            '<option value="dingtalk">钉钉机器人</option><option value="email">邮件</option></select>' +
            '<select id="h5DayinfoSubRobot" style="flex:1;display:none;padding:8px;border-radius:8px;background:var(--glass-3);color:var(--text-1);border:1px solid var(--glass-5);"></select>' +
            '<input id="h5DayinfoSubEmail" placeholder="接收邮箱" style="flex:1;display:none;padding:8px;border-radius:8px;background:var(--glass-3);color:var(--text-1);border:1px solid var(--glass-5);">' +
            '</div>' +
            '<div style="display:flex;gap:8px;">' +
            '<span class="h5-btn-ghost h5-btn-sm" id="h5DayinfoSubSave">保存订阅</span>' +
            '<span class="h5-btn-ghost h5-btn-sm" id="h5DayinfoSubOff">暂停提醒</span>' +
            '</div></div>' +
            '<div class="wf-body" id="h5DayinfoArticle" style="padding:12px;border-radius:12px;background:var(--glass-2);border:1px solid var(--glass-5);"><div class="h5-empty">加载中…</div></div>' +
            '<div class="h5-empty" id="h5DayinfoEmpty" style="display:none">暂无日报</div>');

        bindChannelToggle();
        bindSubscribe();

        $.getJSON('dayinfo/list', { pageNum: 1, pageSize: PAGE_SIZE }).done(function (res) {
            var b = (res.errorCode === '000000' && res.body) || {};
            var list = b.list || [];
            if (!list.length) {
                $('#h5DayinfoArticle').hide();
                $('#h5DayinfoEmpty').show();
                return;
            }
            var $sel = $('#h5DayinfoDateSel').html('');
            list.forEach(function (d) {
                $sel.append('<option value="' + d.id + '">' + esc(d.infoDate) + '</option>');
            });
            $sel.val(String(list[0].id)); // 倒序，首条即最新
            loadDetail(list[0].id);
            $sel.off('change').on('change', function () {
                if (this.value) loadDetail(this.value);
            });
        }).fail(function () {
            $('#h5DayinfoArticle').html('<div class="h5-empty">加载失败</div>');
        });

        $('#h5DayinfoSubBtn').off('click').on('click', function () {
            var $s = $('#h5DayinfoSub');
            $s.toggle();
            if ($s.is(':visible')) loadSubscribe();
        });
    }

    function loadDetail(id) {
        loading(true);
        $.getJSON('dayinfo/detail', { id: id }).done(function (res) {
            loading(false);
            if (res.errorCode !== '000000' || !res.body) {
                $('#h5DayinfoArticle').html('<div class="h5-empty">' + esc(res.errorMsg || '加载失败') + '</div>');
                return;
            }
            var d = res.body;
            $('#h5DayinfoEmpty').hide();
            $('#h5DayinfoArticle').show().html(
                '<h2 style="margin:0 0 6px;color:var(--title);word-break:break-all;">' + esc(d.title) + '</h2>' +
                '<div style="font-size:12px;color:var(--text-5);margin-bottom:12px;">' + esc(d.infoDate || '') + '</div>' +
                window.WelfareMd.render(d.content));
        }).fail(function () {
            loading(false);
            $('#h5DayinfoArticle').html('<div class="h5-empty">加载失败</div>');
        });
    }

    function bindChannelToggle() {
        $('#h5DayinfoSubChannel').off('change').on('change', function () {
            var isDing = $(this).val() === 'dingtalk';
            $('#h5DayinfoSubRobot').toggle(isDing);
            $('#h5DayinfoSubEmail').toggle(!isDing);
        });
    }

    function fillSubForm(s) {
        var has = s && s.id;
        $('#h5DayinfoSubChannel').val(has ? (s.channel || 'dingtalk') : 'dingtalk');
        $('#h5DayinfoSubEmail').val(has ? (s.email || '') : '');
        var fillRobot = function () {
            if (has && s.robotId) $('#h5DayinfoSubRobot').val(String(s.robotId));
        };
        if (!$('#h5DayinfoSubRobot option').length) {
            api({ url: 'robot/list', type: 'GET' }).done(function (r) {
                var list = (r && r.body) || [];
                var html = '';
                list.forEach(function (rb) {
                    html += '<option value="' + rb.id + '">' + esc(rb.name || ('机器人 #' + rb.id)) + '</option>';
                });
                $('#h5DayinfoSubRobot').html(html);
                fillRobot();
            });
        } else {
            fillRobot();
        }
        $('#h5DayinfoSubChannel').change();
        $('#h5DayinfoSubOff').text(has && s.enabled == 1 ? '暂停提醒' : '恢复提醒');
    }

    function loadSubscribe() {
        api({ url: 'dayinfo/subscribe/my', type: 'GET' }).done(function (res) {
            fillSubForm(res.errorCode === '000000' ? res.body : null);
        });
    }

    function bindSubscribe() {
        $('#h5DayinfoSubSave').off('click').on('click', function () {
            var payload = { channel: $('#h5DayinfoSubChannel').val(), enabled: true };
            if (payload.channel === 'dingtalk') {
                payload.robotId = $('#h5DayinfoSubRobot').val();
                if (!payload.robotId) { toast('请先添加钉钉机器人'); return; }
            } else {
                payload.email = $('#h5DayinfoSubEmail').val().trim();
                if (!payload.email) { toast('请填写接收邮箱'); return; }
            }
            loading(true);
            postJSON('dayinfo/subscribe/save', payload).done(function (res) {
                loading(false);
                if (res.errorCode === '000000') {
                    toast('订阅成功');
                    fillSubForm(res.body);
                } else {
                    toast(res.errorMsg || '保存失败');
                }
            }).fail(function () { loading(false); });
        });

        $('#h5DayinfoSubOff').off('click').on('click', function () {
            var resume = $(this).text() === '恢复提醒';
            loading(true);
            postJSON('dayinfo/subscribe/toggle', { enabled: resume }).done(function (res) {
                loading(false);
                if (res.errorCode === '000000') {
                    toast(resume ? '已恢复提醒' : '已暂停提醒');
                    $('#h5DayinfoSubOff').text(resume ? '暂停提醒' : '恢复提醒');
                } else {
                    toast(res.errorMsg || '操作失败');
                }
            }).fail(function () { loading(false); });
        });
    }

    window.registerH5Module('dayinfo', renderDayInfo);
})();
