/**
 * Java8 API 交互式学习平台前端逻辑
 * 改编自 java-api 项目的 app.js，适配 wxyd 暗色玻璃风格 + jQuery 1.8.3
 */
var JavaApi = (function () {
    var allClasses = [];
    var currentApi = null;
    var currentTestCases = [];
    var isRunning = false;
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

    function loadApiList() {
        $.ajax({
            url: 'javaapi/classes', type: 'GET',
            success: function (data) {
                allClasses = data || [];
                renderApiList(allClasses, '');
            },
            error: function () {
                $('#jaApiList').html('<div style="padding:20px;text-align:center;color:rgba(255,255,255,.4)">加载失败</div>');
            }
        });
    }

    function renderApiList(classes, keyword) {
        var filtered = classes.filter(function (ac) {
            if (!keyword) return true;
            var kw = keyword.toLowerCase();
            return ac.name.toLowerCase().indexOf(kw) !== -1
                || (ac.packageName && ac.packageName.toLowerCase().indexOf(kw) !== -1)
                || (ac.intro && ac.intro.toLowerCase().indexOf(kw) !== -1);
        });

        if (filtered.length === 0) {
            $('#jaApiList').html('<div style="padding:20px;text-align:center;color:rgba(255,255,255,.4)">未找到匹配的 API</div>');
            return;
        }

        var groups = {};
        filtered.forEach(function (ac) {
            var letter = ac.name.charAt(0).toUpperCase();
            if (!groups[letter]) groups[letter] = [];
            groups[letter].push(ac);
        });

        var sortedLetters = Object.keys(groups).sort();
        var html = '';
        sortedLetters.forEach(function (letter) {
            html += '<div class="ja-letter">' + letter + '</div>';
            groups[letter].sort(function (a, b) { return a.name.localeCompare(b.name); });
            groups[letter].forEach(function (ac) {
                var activeClass = (currentApi && currentApi.name === ac.name) ? ' active' : '';
                html += '<div class="ja-item' + activeClass + '" data-name="' + escHtml(ac.name) + '">' + escHtml(ac.name) + '</div>';
            });
        });
        $('#jaApiList').html(html);

        $('#jaApiList .ja-item').on('click', function () {
            var name = $(this).data('name');
            loadApiDetail(name);
        });
    }

    function loadApiDetail(name) {
        $('#jaApiList .ja-item').removeClass('active');
        $('#jaApiList .ja-item[data-name="' + name + '"]').addClass('active');
        $('.javaapi-welcome').hide();
        $('.javaapi-content').hide();

        $.ajax({
            url: 'javaapi/classes/' + encodeURIComponent(name), type: 'GET',
            success: function (data) {
                currentApi = data;
                renderApiDetail(data);
                $('.javaapi-content').show();
            },
            error: function () {
                $('.javaapi-welcome').show();
            }
        });
    }

    function renderApiDetail(api) {
        $('#jaTitle').text(api.name);
        $('#jaPackage').text(api.packageName || '');
        $('#jaIntro').html(renderMarkdown(api.intro || ''));

        var sortedMethods = (api.methods || []).slice().sort(function (a, b) {
            return a.name.localeCompare(b.name);
        });
        renderMethods(sortedMethods);

        currentTestCases = (api.testCases || []).slice().sort(function (a, b) {
            return a.name.localeCompare(b.name);
        });
        renderTestCases(currentTestCases);

        $('#jaResultSection').hide();
        $('#jaResultError').hide();
        isRunning = false;
        $('#jaRunBtn').text('▶ 运行测试').prop('disabled', false);
    }

    function renderMethods(methods) {
        if (!methods || methods.length === 0) {
            $('#jaMethods').html('<p style="color:rgba(255,255,255,.4)">暂无方法信息</p>');
            return;
        }
        var html = '<table class="ja-methods-table"><thead><tr><th style="width:30%">方法名</th><th style="width:70%">说明</th></tr></thead><tbody>';
        methods.forEach(function (m) {
            var sigHtml = m.signature ? '<div class="ja-method-sig">' + escHtml(m.signature) + '</div>' : '';
            html += '<tr><td><div class="ja-method-name" data-method="' + escHtml(m.name) + '">' + escHtml(m.name) + '</div>' + sigHtml + '</td><td>' + escHtml(m.description) + '</td></tr>';
        });
        html += '</tbody></table>';
        $('#jaMethods').html(html);
    }

    function renderTestCases(testCases) {
        if (!testCases || testCases.length === 0) {
            $('#jaTestSection').hide();
            return;
        }
        $('#jaTestSection').show();
        var $sel = $('#jaTestSelector');
        $sel.empty();
        testCases.forEach(function (tc, index) {
            $sel.append('<option value="' + index + '">' + escHtml(tc.name) + '</option>');
        });
        $sel.val('0');
        var firstCase = testCases[0];
        $('#jaCodeEditor').val(firstCase.code);
        updateTestDescription(firstCase);
    }

    function updateTestDescription(testCase) {
        var parts = [];
        if (testCase.description) parts.push('描述: ' + testCase.description);
        if (testCase.assertion) parts.push('断言: ' + testCase.assertion);
        $('#jaTestDesc').text(parts.join(' ｜ '));
    }

    function jumpToTestCase(methodName) {
        var baseName = methodName.replace(/\(.*\)$/, '').trim();
        var hasParams = methodName.indexOf('(') !== -1;
        for (var i = 0; i < currentTestCases.length; i++) {
            if (currentTestCases[i].name === methodName || currentTestCases[i].name === baseName) {
                selectTestCase(i); return;
            }
        }
        if (hasParams) {
            for (var i = 0; i < currentTestCases.length; i++) {
                if (currentTestCases[i].name.indexOf(baseName) === 0 && currentTestCases[i].name.indexOf('(') !== -1) {
                    selectTestCase(i); return;
                }
            }
        }
        for (var i = 0; i < currentTestCases.length; i++) {
            var tcName = currentTestCases[i].name;
            if (tcName.indexOf(baseName) === 0) {
                var nextChar = tcName.charAt(baseName.length);
                if (nextChar === '' || nextChar === '(') { selectTestCase(i); return; }
            }
        }
        for (var i = 0; i < currentTestCases.length; i++) {
            if (baseName.indexOf(currentTestCases[i].name) === 0) { selectTestCase(i); return; }
        }
    }

    function selectTestCase(index) {
        $('#jaTestSelector').val(index);
        $('#jaTestSelector').trigger('change');
        $('.javaapi-detail').animate({ scrollTop: $('#jaTestSection').offset().top - 40 }, 400);
        $('#jaCodeEditor').focus();
    }

    function runTest() {
        if (!currentApi) return;
        var testName = '';
        var selectedIndex = $('#jaTestSelector').val();
        if (selectedIndex && currentTestCases[selectedIndex]) {
            testName = currentTestCases[selectedIndex].name;
        }
        var code = $('#jaCodeEditor').val();
        if (!code.trim()) { showError('代码不能为空'); return; }

        isRunning = true;
        $('#jaRunBtn').text('⏳ 运行中...').prop('disabled', true);
        $('#jaResultSection').hide();
        $('#jaResultError').hide();

        $.ajax({
            url: 'javaapi/classes/' + encodeURIComponent(currentApi.name) + '/test',
            type: 'POST', contentType: 'application/json',
            data: JSON.stringify({ testName: testName, code: code }),
            success: function (result) { displayResult(result); },
            error: function () { showError('请求失败，请检查网络连接后重试'); },
            complete: function () {
                isRunning = false;
                $('#jaRunBtn').text('▶ 运行测试').prop('disabled', false);
            }
        });
    }

    function displayResult(result) {
        if (!result.success) { showError(result.error || '执行失败'); return; }
        $('#jaResultSection').show();
        $('#jaResultError').hide();
        if (result.passed) {
            $('#jaResultStatus').removeClass('failed').addClass('passed').text('✅ 测试通过 (' + result.executionTime + 'ms)');
        } else {
            $('#jaResultStatus').removeClass('passed').addClass('failed').text('❌ 测试失败 (' + result.executionTime + 'ms)');
        }
        var outputText = '';
        if (result.output) outputText += result.output + '\n';
        if (result.error) outputText += '\n--- 错误信息 ---\n' + result.error;
        $('#jaResultOutput').text(outputText || '(无输出)');
    }

    function showError(msg) {
        $('#jaResultSection').hide();
        if ($('#jaResultError').length === 0) {
            $('#jaTestSection').append('<div id="jaResultError"></div>');
        }
        $('#jaResultError').show().text(msg);
    }

    /** 初始化（仅当首次切换到 java8-api 菜单时触发） */
    function init() {
        if (loaded) return;
        loaded = true;
        loadApiList();

        $('#jaSearch').on('input', function () {
            var keyword = $(this).val().trim().toLowerCase();
            renderApiList(allClasses, keyword);
        });

        $('#jaRunBtn').on('click', function () {
            if (isRunning) return;
            runTest();
        });

        $('#jaTestSelector').on('change', function () {
            var selectedIndex = $(this).val();
            if (selectedIndex && currentTestCases[selectedIndex]) {
                var testCase = currentTestCases[selectedIndex];
                $('#jaCodeEditor').val(testCase.code);
                updateTestDescription(testCase);
            }
        });

        $('#jaMethods').on('click', '.ja-method-name', function () {
            var methodName = $(this).data('method');
            jumpToTestCase(methodName);
        });
    }

    return { init: init };
})();
