# O1+O2+O4 实施计划

设计：docs/plans/2026-10-09-radar-patrol-pwa-design.md（已签收）
验证策略：验收驱动（无 JUnit，构建 + curl 全链路，用户已确认）

## T1 公共基建
- WxydApplication 加 @EnableScheduling
- pom 加 com.jcraft:jsch:0.1.55
- sql/V12__radar_patrol.sql（radar_subscribe / patrol_asset / patrol_setting）+ DataInitializer 自检建表
- util/DingTalkRobotUtil 加 sendMarkdownMsg（已完成）
- push/PushService：pushDingtalk(userId, robotId, title, md)、pushEmail(to, subject, md)；校验 robot 归属；失败 log.error 不抛

## T2 O1 后端
- mapper：radar_subscribe CRUD（MysqlMapper.xml 追加或独立 RadarMapper.xml → 独立 RadarMapper）
- RadarController（/radar/**，AuthInterceptor 加路径，不实现 CommController）：list/add/delete/toggle/test
- RadarPushJob（@Scheduled 0 10 * * * *）：增量扫描 scan_post → 按通道推送 → 更新 last_post_id
- 验收：add→test 收到推送→job 增量→delete 停推

## T3 O1 前端
- welfare.js 头部「📡 订阅雷达」→ 抽屉列表+表单（PC）
- h5-welfare.js 同功能（H5 sheet）

## T4 O2 后端
- PatrolService：check(url/domain/server) 三探测器 + SM4 加解密 ssh_password
- PatrolController（/patrol/**）：list/add/delete/runNow/setting
- PatrolJob（@Scheduled 每小时）：push_hour 匹配的用户巡检+汇总日报+推送
- 验收：runNow 三类资产、密文落库、日报推送

## T5 O2 前端
- 新菜单 patrol「巡检日报」：api.html 模块块 + patrol.js + index.js 懒加载钩子
- h5.html + h5-patrol.js（registerH5Module）
- CSS 复用既有类，状态色点 ok=绿/warn=黄/fail=红/unknown=灰

## T6 O4 PWA
- static/manifest.json、static/sw.js（壳缓存，API 不缓存）
- h5.html：manifest/apple-touch-icon/SW 注册（相对路径适配 /api）

## T7 全链路回归
- 打包 → 起服务（8099 独立目录）→ 双端 curl 验收清单 → 清理测试数据
- 越权/鉴权抽查（/radar/** /patrol/** 未登录 302；robot_id 归属校验）

## T8 Review + 提交
- 对照设计逐项核对 → memory → commit
