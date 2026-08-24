# Remove 6 Features — Design Spec

**Date:** 2026-08-24
**Branch:** V3.0
**Goal:** Remove six features (文档管理, 证书管理, 短信服务, 清单生成, 查询日志, 接口文档) from HTML, JS, and Java — full removal including mock files and MyBatis mapper methods/XML. DB tables/SQL untouched.

## Kept features
首页 (welcome), 用户管理 (userManage / 白名单), 免密登录 (noPwdLogin — mmLogin/ngLogin), 留言板 (messageBoard — addMessage/delMessage/queryMessage).

## Removed features and their action/endpoint surface
| Feature | tbphx.do actions | REST endpoints | Java |
|---|---|---|---|
| 文档管理 | uploadDocumentFile, queryDocumentFileList, deleteDocumentFile | doc/list, doc/upload, doc/delete, doc/download (DocController) | DocController, DocService, DocumentFile pojo |
| 证书管理 | queryUdInfo, queryUdOper, udOper, cfcaInfoQry, unBindUkey, queryCprUser | (via tbphx.do) | UkeyController, UkeyService/Impl, HttpRequestCPR09003, CFCAConfig; pojos UdInfo, CprUser, OperInfo, UUS1007T, UUS1009T |
| 短信服务 | queryMsgCode | MessageController#queryMsgCode | MessageService#queryMsgCode |
| 清单生成 | orderCreate, datadict, requestData, responseData | (via tbphx.do) | ManifestController, ManifestService/Impl |
| 查询日志 | start | (via tbphx.do) | TbpController#start, TbpService#start, SSHUtil, Sshbean, SocketMessage |
| 接口文档 | datadict (shared with 清单生成) | ApiController#docQry/docQryAll/uploadDoc, downExcel | ApiService#docQry/docQryAll/uploadDoc, Doc pojo |

`datadict` is shared by 清单生成 and 接口文档 — both removed, so it is removed entirely.

## Approach
Surgical per-method removal on mixed files; whole-file delete for feature-only files; delete removed-feature mock JSON and mapper XML statements. Preserves all shared infrastructure. No dead code.

## Frontend changes
### `app/src/main/resources/static/index.html`
- Delete the 6 sidebar `<li class="menu-item">` entries: certManage, smsService, listGenerate, queryLog, apiDoc, docManage.
- Delete the 6 `<div class="module-box">` sections with the same ids.
- Delete `#previewModal` (only docManage used it). Delete `#userModal`? No — userManage uses it (keep). Keep `#msgModal` (messageBoard). Keep `#loading`, `#alterModal`.
### `app/src/main/resources/static/Assets/js/index.js`
- Delete cert functions: `queryCertOper, unbindCert, queryCFCAUdInfo, updateCertStatus, queryUdInfo, updateUdInfo, rendercert`; the `#btnRefreshCert/#btnChangeUDn/#btnSubmitCertStatus/#btnUnbindCert/#btnCfcaCert` handlers; `CERT_KEY`.
- Delete sms: `queryMsgCode`.
- Delete manifest: `orderCreate`.
- Delete queryLog: `nowQuery, historyQuery`.
- Delete apiDoc: `webDown, docQry` + `#btnApiQuery/#btnWebDownload/#btnDubboDownload/#btnApiSubmit` handlers.
- Delete 文档管理 block: `DOC_KEY, queryDocList, renderDocTable, fileUpload`, doc delete/download/preview handlers, `#searchDocBtn*`, `#docPag`.
- Keep: white/user functions, `loginWy/NGLogin/NGLoginChange`, message-board (`loadMsg/renderMsg/addMsg/delMsg`), `commonPagination, loading, alterModal, sbtoa, bookmarks`.
- Remove the `queryUdInfo()` auto-call at line 257 and `queryCertOper()` call at line 290.

## Java — whole-file delete (feature-only)
- `controller/DocController.java`
- `service/DocService.java`
- `controller/ManifestController.java`, `service/ManifestService.java`, `service/ManifestServiceImpl.java`
- `controller/UkeyController.java`, `service/UkeyService.java`, `service/UkeyServiceImpl.java`, `service/HttpRequestCPR09003.java`, `config/CFCAConfig.java`
- pojos: `UdInfo, CprUser, OperInfo, UUS1007T, UUS1009T, DocumentFile, Doc, SocketMessage, Sshbean` (and ESBPojo/OSBData/ReqSvcHeader/SvcBody/Number/MsgBean if confirmed manifest/sms-only)
- `util/SSHUtil.java` (only TbpController.start uses SSH — plan verifies)

## Java — surgical edit (mixed files)
### `controller/MessageController.java`
Remove `queryMsgCode`, `queryUdOper`. Keep `addMessage, delMessage, queryMessage`.
### `service/MessageService.java` + `MessageServiceImpl.java`
Remove `queryMsgCode(String,String)`, `queryOperInfo()`, `addOperInfo(OperInfo)`. Keep `addMessage, delMessage, queryMessage, queryMessageById`. Remove `OperInfo` import once deleted.
### `service/TbpService.java` + `TbpServiceImpl.java`
Remove `docQry, createDoc, uploadDoc, uploadDocumentFile, docQryAll, queryDocAll, addDocFile, queryDocumentFileList, queryDocumentFile, deleteDocumentFile, getExcelTemplate, start`. Keep `dec(String,String)`. Remove `Doc/DocumentFile` imports.
### `controller/TbpController.java`
Remove `start` + all doc/接口文档 routes (`createDoc, queryDocAll, addDocFile, uploadDocumentFile, queryDocumentFileList, deleteDocumentFile`). Keep `dec, getParamValue`. Remove now-unused imports (`MysqlMapper, SSHUtil, ChannelSftp, Session, SocketMessage, Sshbean, Workbook`, etc.).
### `controller/ApiController.java` + `service/ApiService.java` + `ApiServiceImpl.java`
Remove `docQry, docQryAll, uploadDoc, downExcel` (接口文档). Keep `tbphx` (entry + bare-call guide). Remove `Doc`/`Workbook`/`CommUtils.getExcelTemplate` usage as it becomes unused.
### `mapper/mysqlMapper/MysqlMapper.java` + `mapper/MysqlMapper.xml`
Remove: `addOperInfo, queryOperInfo` (cert); `addSocketMessage, querySocketRevice, updateSocketMsgStatus` (queryLog); `queryDoc, queryDocAll, addDoc, updateDoc, addDocFile, queryDocFile` (接口文档); `addDocumentFile, queryDocumentFileList, queryDocumentFile, deleteDocumentFile` (文档管理). Keep: `addMessage, delMessage, queryMessage, queryMessageCount, getUserName, getUserNameByStatus, queryClientInfo, queryWhiteUrl, queryWhiteInfo, addWhite, updateWhiteByUsername, updateWhiteByIp, deleteWhite, addLog`.
### `dispatcher/ActionDispatcher.java`
Drop `@Autowired UkeyService, ManifestService, TbpService` (deleted). Keep `MessageService, LoginService, UserService`. Switch body unchanged. Update the class comment (remove references to deleted services).
### `config` / resources
- `env.properties`: remove the 4 `cfca.*` keys (CFCAConfig deleted). (File stays local/untracked per real creds; just drop cfca lines.)
- `application.yml`: no cfca refs (none present) — no change.

## Mock files — delete under `app/src/main/resources/mock/tbphx.do/`
Remove: `action=queryMsgCode.json, queryUdInfo.json, queryUdOper.json, udOper.json, cfcaInfoQry.json, unBindUkey.json, queryCprUser.json, orderCreate.json, datadict.json, requestData.json, responseData.json, start.json`.
Keep: `action=addMessage.json, delMessage.json, queryMessage.json, addWhite.json, deleteWhite.json, updateWhite.json, queryWhiteInfo.json, mmLogin.json, ngLogin.json`.

## Verification
1. `mvn -pl app -am clean compile` → green.
2. Boot, `mockEnabled=true`.
3. Smoke kept features: `tbphx.do` action=queryWhiteInfo / action=mmLogin / action=queryMessage → mock returned; bare `tbphx.do` → HTTP 200 guide.
4. Removed-action mock files gone; e.g. action=queryMsgCode → MockAspect "no mock found" → ActionDispatcher default → empty Result (no crash).
5. Static index page loads, only 4 kept menu items render, no console 404s for removed endpoints.

## Entanglement warnings (resolved during implementation)
- `MysqlMapper` is one shared interface → method-level surgical, not file delete.
- `dec` and `getParamValue` are kept (not in removal list); plan confirms they don't depend on deleted code.
- `SSHUtil`/`Sshbean`/`SocketMessage` deleted only after confirming `TbpController.start` was the sole caller.
- Pojos `ESBPojo/OSBData/ReqSvcHeader/SvcBody/Number/MsgBean` deleted only after confirming manifest/sms-only ownership.
- `Doc` pojo used by 接口文档 mapper + DocService — removed together.
- `OperInfo` pojo used by `MessageServiceImpl.addOperInfo/queryOperInfo` (cert操作记录) — removed with those methods.
