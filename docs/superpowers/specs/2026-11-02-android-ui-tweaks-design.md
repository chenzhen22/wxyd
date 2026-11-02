# 2026-11-02 Android UI 微调：紧凑顶栏 + 登录页优化

## 意图
Android 端二级页顶栏（Material3 默认 64dp TopAppBar）太高占空间；登录页有多余文案、缺少 H5 同款背景图。

## 改动
1. **通用紧凑顶栏**：新建 `ui/common/CompactTopBar.kt`，高 48dp，返回箭头 + 标题。替换 12 处二级页（AuthWizard、JavaApi、DayInfo、Gateway、NoteEdit、Notes、Placeholder、Robot、Share、Shell、WelfareDetail、Welfare）的 TopAppBar。福利专区的 "←" 文本按钮换成标准箭头。
2. **登录页删文案**：删除 `LoginScreen.kt` 中 "支持内网 HTTP 明文与自签证书部署"。
3. **登录页背景图**：拷贝 H5 的 `static/Assets/images/login-bg.jpg` 到 `res/drawable-nodpi/`，背景 = 图片铺满 + 深色压暗层（rgba(10,8,24,.55→.68) 渐变），文字加阴影保证可读。

## 方案取舍
- 48dp：紧凑但保留舒适点击区（用户已确认，全部二级页统一改）。
- 背景图直接复用 H5 同一张 jpg（95KB），不新增资源体积。
