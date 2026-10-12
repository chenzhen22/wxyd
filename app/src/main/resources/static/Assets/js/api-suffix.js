/* ============================================================
   统一 API 后缀：所有 $.ajax/$.get/$.post 发出的后端请求自动追加 .do
   （后端 WebMvcConfig 已开启后缀模式匹配，/xxx.do 等价 /xxx）。
   排除：绝对/协议相对 URL、data:、已带 .do、以及静态资源扩展名。
   fetch() 调用不经过 jQuery，需各自手写 .do。
   ============================================================ */
(function () {
    if (!window.jQuery || !$.ajaxPrefilter) return;
    $.ajaxPrefilter(function (options) {
        var url = options.url || '';
        if (!url) return;
        if (/^(https?:)?\/\//i.test(url) || /^data:/i.test(url)) return;
        if (/\.do(\?|#|$)/i.test(url)) return;
        if (/\.(html?|js|css|json|md|svg|png|jpe?g|gif|ico|woff2?|ttf|webp)(\?|#|$)/i.test(url)) return;
        var q = url.indexOf('?');
        options.url = q === -1 ? url + '.do' : url.slice(0, q) + '.do' + url.slice(q);
    });
})();
