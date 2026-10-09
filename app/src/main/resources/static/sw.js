/*
 * 小泽助手 H5 Service Worker（O4 PWA）
 * 策略：
 *  - 壳资源（h5.html / manifest / css / js / 字体）：cache-first + 版本化 cache 名，发版后旧 cache 清理
 *  - 页面导航（navigate 请求）：network-first，离线时回退缓存壳
 *  - 其余（API、图片代理等一切 /api/ 动态请求）：一律不缓存，直连网络 —— 登录态与实时数据绝不走 SW
 * 注意：SW 仅在 localhost 或 HTTPS 下被浏览器允许注册；注册处已 try-catch 静默降级。
 */
var CACHE_NAME = 'wxyd-shell-v1';

var SHELL_ASSETS = [
    'h5.html',
    'manifest.json',
    'favicon.png',
    'Assets/js/jquery-3.7.1.min.js',
    'Assets/js/h5.js',
    'Assets/css/h5.css'
];

self.addEventListener('install', function (event) {
    event.waitUntil(
        caches.open(CACHE_NAME).then(function (cache) {
            // 逐个 addAll 容错：单个 404 不阻断安装（如某资源改名）
            return Promise.all(SHELL_ASSETS.map(function (url) {
                return cache.add(url).catch(function () { /* 忽略缺失资源 */ });
            }));
        }).then(function () { return self.skipWaiting(); })
    );
});

self.addEventListener('activate', function (event) {
    event.waitUntil(
        caches.keys().then(function (names) {
            return Promise.all(names.map(function (name) {
                if (name !== CACHE_NAME) return caches.delete(name);
            }));
        }).then(function () { return self.clients.claim(); })
    );
});

self.addEventListener('fetch', function (event) {
    var req = event.request;
    if (req.method !== 'GET') return;

    var url = new URL(req.url);

    /* API 与动态请求绝不缓存（含 welfare-img 图片代理、note/export 下载等） */
    if (url.pathname.indexOf('/api/') !== -1 && !/\.(css|js|png|svg|json|woff2?)$/.test(url.pathname)) {
        return;
    }

    /* 页面导航：network-first，离线回退缓存壳 */
    if (req.mode === 'navigate') {
        event.respondWith(
            fetch(req).then(function (resp) {
                var copy = resp.clone();
                caches.open(CACHE_NAME).then(function (c) { c.put(req, copy); });
                return resp;
            }).catch(function () {
                return caches.match(req).then(function (hit) {
                    return hit || caches.match('h5.html');
                });
            })
        );
        return;
    }

    /* 壳资源：cache-first，后台刷新 */
    event.respondWith(
        caches.match(req).then(function (hit) {
            var fetchPromise = fetch(req).then(function (resp) {
                if (resp && resp.status === 200) {
                    var copy = resp.clone();
                    caches.open(CACHE_NAME).then(function (c) { c.put(req, copy); });
                }
                return resp;
            }).catch(function () { return hit; });
            return hit || fetchPromise;
        })
    );
});
