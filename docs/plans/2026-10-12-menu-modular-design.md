# 菜单与卡片模块化设计（H5 + 桌面）

日期：2026-10-12
状态：已确认

## 背景与目标

- 桌面版 `api.html`（691 行单页）：侧栏菜单硬编码在 HTML，`index.js` 按角色显隐部分菜单。
- H5 `h5.html`：SPA 单页，首页卡片硬编码在 `h5.js` 的 `MODULE_ITEMS` 数组。
- 数据库已有 `V6__menu_config`（visible × user_ids 四态权限），但菜单注册表（key→名称）硬编码在 `MenuServiceImpl.REGISTRY`，`ADMIN_ONLY` 集合硬编码。
- H5 与桌面 key 命名不一致（`user` vs `userManage` 等），历史上共用一份 `menu_config`。

目标：
1. 菜单注册表全入库（名称/图标/URL/排序/管理员专属），新增菜单不改代码（功能内容仍需代码）。
2. H5 与桌面按平台独立配置菜单与权限（同一功能两行记录，各自四态权限）。
3. 每个菜单一个 hash URL（前端路由），可直达/刷新/收藏。
4. H5 首页卡片与桌面侧栏全部由接口返回渲染。

## 关键决策

| 决策点 | 结论 |
|--------|------|
| URL 形式 | 前端 hash 路由（`/api/h5.html#/welfare`、`/api/api.html#/userManage`），改动小、可刷新/收藏/分享 |
| 菜单入库范围 | 注册表全入库（menu_item 表），权限沿用 menu_config 四态模型 |
| 权限粒度 | 按平台独立配置：同一功能 H5/桌面两行，各自 visible × user_ids |

## 数据模型

新表 `menu_item`（V13__menu_item.sql）：

```sql
CREATE TABLE IF NOT EXISTS `menu_item` (
  `id`         BIGINT AUTO_INCREMENT PRIMARY KEY,
  `menu_key`   VARCHAR(50)  NOT NULL,   -- 前端模块名，如 welfare
  `platform`   VARCHAR(10)  NOT NULL,   -- h5 / desktop
  `name`       VARCHAR(50)  NOT NULL,   -- 显示名，如 福利专区
  `icon`       VARCHAR(20)  DEFAULT '', -- emoji 图标
  `hash_url`   VARCHAR(100) DEFAULT '', -- 如 #/welfare；空=无独立URL（固定tab）
  `sort_no`    INT          DEFAULT 0,
  `admin_only` TINYINT      DEFAULT 0,
  UNIQUE KEY `uk_key_platform` (`menu_key`, `platform`)
);
```

- `menu_config` 表保留不动，继续承担四态权限；其 menu_key 现唯一指向 (menu_key, platform) 行。
- `MenuServiceImpl.REGISTRY` 与 `ADMIN_ONLY` 删除，全部读库。
- seed 放 `DataInitializer` 自检（项目惯例，幂等），SQL 文件只建表。
- 存量 menu_config 迁移：desktop key 直接对应；H5 侧把桌面 key 的权限按映射表复制到对应 H5 key。

## 后端接口

`MenuController`（前缀 `menu/`，AuthInterceptor 鉴权）：

| 接口 | 方法 | 说明 |
|------|------|------|
| `menu/list?platform=h5\|desktop` | GET | 当前用户在该平台可见菜单（合并注册表+四态权限），按 sort_no 排序。body 项：`{key, name, icon, hashUrl, adminOnly, sortNo}` |
| `menu/all?platform=` | GET | 管理员：该平台全部菜单（含不可见） |
| `menu/save` | POST | 保留：权限四态保存，校验 menuKey+platform 存在 |
| `menu/item/save` | POST | 管理员：新增/编辑菜单项（name/icon/hashUrl/sortNo/adminOnly） |
| `menu/item/delete` | POST | 管理员：删除菜单项（连带删 menu_config 行） |

- 四态算法不变，遍历对象从 REGISTRY 改为 menu_item 行；`admin_only=1` 非管理员不可见。
- 结构性入口（H5 `home`/`profile`、桌面 `welcome`、两端 `menuManage`）代码特殊处理，不参与删改。
- `menu/visible` 老接口保留一个版本周期，前端切换后删除。

## 前端

**H5（h5.html + h5.js）**
- hash 路由：`#/模块名`；启动按 hash 直达（不可见回首页），切换模块同步 hash。
- `MODULE_ITEMS` 删除，首页卡片由 `menu/list?platform=h5` 渲染，点击跳 hash。
- tabbar 骨架保留，message/group 显隐由 menu/list 控制。
- `registerH5Module` 机制不变。

**桌面（api.html + index.js）**
- 侧栏硬编码删除，由 `menu/list?platform=desktop` 渲染。
- index.js 按角色隐藏菜单的代码删除。
- module-box 显隐切换逻辑不变，入口由 hash 路由驱动。
- `?welfare=id` 分享链接逻辑不动。

**菜单管理页（menu-manage.js）**
- 双平台 Tab，除权限四态编辑外增加菜单项编辑（名称/图标/排序/adminOnly/hashUrl）与新增/删除。
- 新增菜单无对应渲染器时显示「模块开发中」占位。

**兼容**：无 hash 老链接进首页。

## 验证点

- diff 复查：REGISTRY/ADMIN_ONLY/MODULE_ITEMS/侧栏硬编码无残留引用。
- 四态权限：admin / 普通用户 / 白名单 / 黑名单 四种会话核对 menu/list。
- hash 路由：直达、隐藏回首页、后退/刷新、`?welfare=id` 不受影响。
- 菜单管理页双平台编辑、新增占位。

## 风险

- 改动横跨后端 + 两端前端，用 git worktree 隔离开发。
- H5/桌面 key 映射迁移需写死在 seed/迁移逻辑中，注意幂等。
