# Remove 6 Features Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fully remove six features (文档管理, 证书管理, 短信服务, 清单生成, 查询日志, 接口文档) from the merged `app` module — frontend HTML/JS, Java controllers/services/pojos/util, mock JSON, and MyBatis mapper methods/XML — while keeping 首页/用户管理(白名单)/免密登录/留言板 and all shared infrastructure.

**Architecture:** Whole-file delete for feature-only files; surgical per-method removal on mixed files (MessageController/Service/Impl, TbpService/Impl, TbpController, ApiController/Service/Impl, MysqlMapper + XML, ActionDispatcher); delete removed-feature mock JSON. This mirrors the proven module-merge approach: a sequence of tasks where some intermediate commits do not yet compile, ending in a green compile → boot → smoke verify.

**Tech Stack:** Java 8, Spring Boot 2.2.5.RELEASE, MyBatis 3.0.0, MySQL 8.0.30, fastjson/fastjson2, POI/easyexcel, JSch/Hutool. Single `app` module (jar), `WxydApplication`, context-path `/api`, port 8090, mock-first (`wxyd.mock.enabled=true`).

## Global Constraints

- **No unit-test harness exists.** Verification = `mvn -pl app -am clean compile` → boot → smoke-test the mock `tbphx.do` endpoint. Steps adapted from TDD to "compile → boot → smoke."
- **NEVER stage or commit `app/src/main/resources/env.properties`** — it holds real DB credentials (`mysql5.sqlpub.com:3310/wxyd_123`, user `wxydprd`, password `hnBMr5VBIzx8SZ2i`). The env.properties step only edits the local file; do not `git add` it.
- Branch: `V3.0`. Every task ends with a commit. End git commit messages with `Co-Authored-By: Claude <noreply@anthropic.com>`.
- **Kept features (do not touch their working code):** 首页 (index welcome), 用户管理/白名单 (addWhite/deleteWhite/updateWhite/queryWhiteInfo), 免密登录 (mmLogin/ngLogin — `LoginService`/`LoginServiceImpl`/`HttpRequestCPR09003`), 留言板 (addMessage/delMessage/queryMessage).
- **Kept shared infrastructure (do not delete):** `HttpRequestCPR09003` (used by LoginService), `SocketMessage` pojo (used by HttpRequestCPR09003 + LoginService), the `ESBPojo`/`OSBData`/`OSBDataListener`/`ReqSvcHeader`/`SvcBody`/`MsgBean`/`UUS1007T`/`UUS1009T` cluster (all used by LoginService), AOP request-log (`ControllerAspect`, `LogPojo`, `MysqlMapper.addLog`, `requestlog` table), `Result`/`Doc`(deleted)/`Message` pojos as needed.
- **Removed features' actions must NOT crash:** when `mock=false`, removed actions fall to `ActionDispatcher.default` → empty `Result` (no NPE). When `mock=true`, removed-action mock files are gone → `MockAspect` logs "no mock found" and returns empty Result (verify it returns gracefully, not 500).

---

## File Structure Map

### Whole-file DELETE (feature-only — confirmed no kept-code reference)
- `app/src/main/java/com/chenzhen/controller/DocController.java` (文档管理)
- `app/src/main/java/com/chenzhen/controller/ManifestController.java` (清单生成)
- `app/src/main/java/com/chenzhen/controller/UkeyController.java` (证书管理)
- `app/src/main/java/com/chenzhen/service/DocService.java` (接口文档/文档管理 Excel helpers)
- `app/src/main/java/com/chenzhen/service/ManifestService.java` + `ManifestServiceImpl.java` (清单生成)
- `app/src/main/java/com/chenzhen/service/UkeyService.java` + `UkeyServiceImpl.java` (证书管理)
- `app/src/main/java/com/chenzhen/config/CFCAConfig.java` (证书管理 — dead @Component; only a javadoc @link in WxydApplication references it)
- `app/src/main/java/com/chenzhen/util/SSHUtil.java` (查询日志 — sole caller is TbpController.start, removed in Task 4)
- Pojos (feature-only, grep-gated in Task 3): `pojo/Doc.java`, `pojo/DocumentFile.java`, `pojo/UdInfo.java`, `pojo/CprUser.java`, `pojo/OperInfo.java`, `pojo/Sshbean.java`

### Surgical EDIT (mixed files)
- `controller/MessageController.java` — remove `queryMsgCode`, `queryUdOper`
- `service/MessageService.java` + `MessageServiceImpl.java` — remove `queryMsgCode`, `queryOperInfo`, `addOperInfo`
- `service/TbpService.java` + `TbpServiceImpl.java` — remove all doc/Excel methods, keep `dec`
- `controller/TbpController.java` — remove `start` + all doc routes, keep `dec`, `getParamValue`
- `controller/ApiController.java` + `service/ApiService.java` + `ApiServiceImpl.java` — remove `docQry`/`docQryAll`/`uploadDoc`/`downExcel`, keep `tbphx`
- `mapper/mysqlMapper/MysqlMapper.java` + `mapper/MysqlMapper.xml` — remove feature statements
- `dispatcher/ActionDispatcher.java` — drop deleted-service autowires
- `WxydApplication.java` — fix javadoc @link to deleted CFCAConfig

### Frontend EDIT
- `app/src/main/resources/static/index.html` — remove 6 menu items + 6 module sections + `#previewModal`
- `app/src/main/resources/static/Assets/js/index.js` — remove feature JS + dead `bookmarks()`

### Mock DELETE — `app/src/main/resources/mock/tbphx.do/`
Remove 12: `action=queryMsgCode.json`, `queryUdInfo.json`, `queryUdOper.json`, `udOper.json`, `cfcaInfoQry.json`, `unBindUkey.json`, `queryCprUser.json`, `orderCreate.json`, `datadict.json`, `requestData.json`, `responseData.json`, `start.json`. Keep 9: `addMessage`, `delMessage`, `queryMessage`, `addWhite`, `deleteWhite`, `updateWhite`, `queryWhiteInfo`, `mmLogin`, `ngLogin`.

### Config EDIT (local only — DO NOT COMMIT)
- `app/src/main/resources/env.properties` — remove the 4 `cfca.*` keys

---

## Task 1: Frontend — index.html

**Files:**
- Modify: `app/src/main/resources/static/index.html`

**Interfaces:** none (static resource).

- [ ] **Step 1: Read the file and locate the 6 sidebar menu items**

Run: read `app/src/main/resources/static/index.html`. Find the `<li class="menu-item">` blocks whose id/onclick target: `docManage`, `certManage`, `smsService`, `listGenerate`, `queryLog`, `apiDoc`.

- [ ] **Step 2: Delete the 6 sidebar `<li class="menu-item">` entries**

Delete exactly these six `<li>...</li>` blocks (the ones for certManage, smsService, listGenerate, queryLog, apiDoc, docManage). Keep the 4 kept menu items: 首页 (welcome), 用户管理 (userManage), 免密登录 (noPwdLogin), 留言板 (messageBoard).

- [ ] **Step 3: Delete the 6 `<div class="module-box">` sections**

Delete the six module-box divs with ids: `docManage`, `certManage`, `smsService`, `listGenerate`, `queryLog`, `apiDoc`. Keep `welcome`, `userManage`, `noPwdLogin`, `messageBoard` module-box divs.

- [ ] **Step 4: Delete `#previewModal`**

`#previewModal` is used only by docManage (document preview). Delete the entire `<div id="previewModal">...</div>` block. Keep `#userModal` (userManage), `#msgModal` (messageBoard), `#loading`, `#alterModal`.

- [ ] **Step 5: Verify no dangling references in HTML**

Run: `grep -n "docManage\|certManage\|smsService\|listGenerate\|queryLog\|apiDoc\|previewModal" app/src/main/resources/static/index.html`
Expected: no output (all removed).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/resources/static/index.html
git commit -m "refactor: remove 6 feature modules from index.html UI

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 2: Frontend — index.js

**Files:**
- Modify: `app/src/main/resources/static/Assets/js/index.js`

**Interfaces:** none.

- [ ] **Step 1: Read the file**

Run: read `app/src/main/resources/static/Assets/js/index.js` (1048 lines).

- [ ] **Step 2: Delete cert functions + handlers**

Delete: `queryCertOper`, `unbindCert`, `queryCFCAUdInfo`, `updateCertStatus`, `queryUdInfo`, `updateUdInfo`, `rendercert`, the `CERT_KEY` constant, and the `#btnRefreshCert / #btnChangeUDn / #btnSubmitCertStatus / #btnUnbindCert / #btnCfcaCert` click-handler bindings.

- [ ] **Step 3: Delete sms function**

Delete: `queryMsgCode` and its `#btnSendMsg`/sms handler binding.

- [ ] **Step 4: Delete manifest function**

Delete: `orderCreate` and its handler binding.

- [ ] **Step 5: Delete queryLog functions**

Delete: `nowQuery`, `historyQuery`, and their `#btnQueryLog*` handler bindings.

- [ ] **Step 6: Delete apiDoc functions**

Delete: `webDown`, `docQry`, and the `#btnApiQuery / #btnWebDownload / #btnDubboDownload / #btnApiSubmit` handler bindings.

- [ ] **Step 7: Delete 文档管理 (docManage) block**

Delete: `DOC_KEY`, `queryDocList`, `renderDocTable`, `fileUpload`, the doc delete/download/preview handler bindings, and `#searchDocBtn*`, `#docPag` pagination binding.

- [ ] **Step 8: Remove auto-call lines for removed features**

Delete the `queryUdInfo()` auto-call (~line 257) and the `queryCertOper()` auto-call (~line 290) from the ready/init block.

- [ ] **Step 9: Delete dead `bookmarks()` function**

`bookmarks()` (~lines 915-916) references the already-deleted `bookmarks.html`. Delete the function and any call to it.

- [ ] **Step 10: Verify no dangling references to removed selectors/functions**

Run: `grep -n "queryCertOper\|unbindCert\|queryCFCAUdInfo\|CERT_KEY\|queryMsgCode\|orderCreate\|nowQuery\|historyQuery\|webDown\|docQry\|DOC_KEY\|queryDocList\|renderDocTable\|fileUpload\|bookmarks(" app/src/main/resources/static/Assets/js/index.js`
Expected: no output.

- [ ] **Step 11: Commit**

```bash
git add app/src/main/resources/static/Assets/js/index.js
git commit -m "refactor: remove 6 feature JS handlers from index.js

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 3: Java — whole-file deletes (feature-only)

**Files:**
- Delete: the 8 feature-only Java files + `SSHUtil.java` + `CFCAConfig.java` listed below
- Delete (grep-gated): the 6 feature-only pojos

**Interfaces:** Produces: a tree missing feature-only files. Compile WILL BREAK here (mixed files still reference some deleted symbols); Tasks 4–8 repair them. This matches the merge-plan precedent of committing intermediate broken-compile structural state.

- [ ] **Step 1: Delete feature-only controllers/services**

```bash
git rm app/src/main/java/com/chenzhen/controller/DocController.java
git rm app/src/main/java/com/chenzhen/controller/ManifestController.java
git rm app/src/main/java/com/chenzhen/controller/UkeyController.java
git rm app/src/main/java/com/chenzhen/service/DocService.java
git rm app/src/main/java/com/chenzhen/service/ManifestService.java
git rm app/src/main/java/com/chenzhen/service/ManifestServiceImpl.java
git rm app/src/main/java/com/chenzhen/service/UkeyService.java
git rm app/src/main/java/com/chenzhen/service/UkeyServiceImpl.java
```

- [ ] **Step 2: Delete SSHUtil (查询日志) and CFCAConfig (证书管理)**

```bash
git rm app/src/main/java/com/chenzhen/util/SSHUtil.java
git rm app/src/main/java/com/chenzhen/config/CFCAConfig.java
```

- [ ] **Step 3: Grep-gate each feature-only pojo before deletion**

For EACH pojo below, run a reference grep across the whole `app/src/main/java` tree EXCLUDING the pojo's own file. Only delete if the grep returns ZERO references in non-deleted (kept) code. (After Tasks 4–8 the mixed-file references will be gone; re-run the gate at the end of Task 8 to confirm before this commit, OR delete now and rely on Task 8's final compile.)

Pojos to gate: `pojo/Doc.java`, `pojo/DocumentFile.java`, `pojo/UdInfo.java`, `pojo/CprUser.java`, `pojo/OperInfo.java`, `pojo/Sshbean.java`.

Grep pattern per pojo (example for `Doc`):
```bash
grep -rln '\bDoc\b' app/src/main/java --include=*.java | grep -v '/pojo/Doc.java'
```
**CAUTION — `Doc` is a common substring.** Use word-boundary `\b` and manually confirm each remaining hit is in a file being deleted/edited this plan, not in kept code. `UdInfo`, `CprUser`, `OperInfo`, `DocumentFile`, `Sshbean` are distinctive — safe.

- [ ] **Step 4: Delete the 6 pojos (after gate passes)**

```bash
git rm app/src/main/java/com/chenzhen/pojo/Doc.java
git rm app/src/main/java/com/chenzhen/pojo/DocumentFile.java
git rm app/src/main/java/com/chenzhen/pojo/UdInfo.java
git rm app/src/main/java/com/chenzhen/pojo/CprUser.java
git rm app/src/main/java/com/chenzhen/pojo/OperInfo.java
git rm app/src/main/java/com/chenzhen/pojo/Sshbean.java
```

- [ ] **Step 5: Confirm the KEEP set is untouched**

Run:
```bash
ls app/src/main/java/com/chenzhen/service/HttpRequestCPR09003.java app/src/main/java/com/chenzhen/pojo/SocketMessage.java app/src/main/java/com/chenzhen/pojo/ESBPojo.java app/src/main/java/com/chenzhen/listen/OSBDataListener.java
```
Expected: all four paths exist (these are kept — shared with login).

- [ ] **Step 6: Commit (compile will fail — expected; fixed in Tasks 4–8)**

```bash
git add -A
git commit -m "refactor: delete 6 feature-only Java files, pojos, SSHUtil, CFCAConfig

Compile breaks (mixed files still reference deleted symbols); repaired in
follow-up surgical tasks. Matches merge-plan intermediate-state precedent.

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 4: Surgical — ActionDispatcher + TbpController

**Files:**
- Modify: `app/src/main/java/com/chenzhen/dispatcher/ActionDispatcher.java`
- Modify: `app/src/main/java/com/chenzhen/controller/TbpController.java`

**Interfaces:**
- Consumes: `MessageService`, `LoginService`, `UserService`, `TbpService.dec` (kept).
- Produces: `ActionDispatcher.dispatch` (kept, signature unchanged); `TbpController.dec` + `TbpController.getParamValue` (kept).

- [ ] **Step 1: ActionDispatcher — drop deleted-service autowires**

In `ActionDispatcher.java`, replace the six `@Autowired` lines (26–31) with the three kept ones:

```java
    @Autowired private MessageService messageService;
    @Autowired private LoginService loginService;
    @Autowired private UserService userService;
```

Delete the lines for `UkeyService`, `TbpService`, `ManifestService` (all deleted in Task 3). The `switch` body is unchanged (it only ever wired message + getParamValue; the deleted services were never referenced in the switch).

- [ ] **Step 2: ActionDispatcher — update class javadoc**

Replace the class javadoc block (lines 11–21) with:

```java
/**
 * In-process replacement for the former TbpApplyFeign + ./msg/*.send file queue.
 * Reads the {@code action} field from {@code result.getBody()} and calls the
 * corresponding service directly.
 * <p>
 * Only reached when {@code wxyd.mock.enabled=false}; in mock mode {@code MockAspect}
 * short-circuits every {@code tbphx.do} action before it gets here. The MySQL-backed
 * message actions and {@code getParamValue} are wired below; all other actions are
 * mock-only under this design and fall through to {@code default}, returning an empty
 * {@link Result}.
 */
```

- [ ] **Step 3: TbpController — delete the `start` method (查询日志)**

Delete the entire `start` method (lines 46–110), i.e. from:
```java
    @ResponseBody
    @RequestMapping("start")
    public Result start(@RequestBody Result result) throws Exception {
```
through its closing `}` (the `}` at line 110).

- [ ] **Step 4: TbpController — delete the doc/接口文档 routes**

Delete these methods entirely (lines 127–191):
- `createDoc` (127–130)
- `queryDocAll` (135–140)
- `addDocFile` (142–147)
- `uploadDocumentFile` (149–155)
- `queryDocumentFileList` (157–168)
- `deleteDocumentFile` (170–191)

Also delete the now-obsolete comments at lines 132–133 (`// uploadDoc removed...`).

- [ ] **Step 5: TbpController — remove the `MysqlMapper` field**

`mysqlMapper` was used only by `start` (now gone). Delete:
```java
    @Resource
    MysqlMapper mysqlMapper;
```
(lines 43–44).

- [ ] **Step 6: TbpController — clean imports to only what `dec` + `getParamValue` need**

After Step 4–5, `dec` and `getParamValue` need only: `Result`, `TbpService`, `CommUtils`, `@RestController`, `@GetMapping`, `@RequestMapping`, `@ResponseBody`, `@RequestBody`, `@Param`, `lombok.extern.slf4j.Slf4j`, `org.springframework.beans.factory.annotation.Autowired`, `java.util.Map`.

Delete ALL of these now-unused imports (currently at lines 3–34):
- `cn.hutool.core.bean.BeanUtil`
- `cn.hutool.core.util.ObjectUtil`
- `com.alibaba.fastjson.JSONObject`
- `com.chenzhen.config.ProperConfig`
- `com.chenzhen.mapper.mysqlMapper.MysqlMapper`
- `com.chenzhen.pojo.*`  (the `.*` wildcard — `Result` is referenced via `com.chenzhen.pojo.Result`? NO: the file uses `Result` unqualified, so it comes from the `.*`. Replace `import com.chenzhen.pojo.*;` with `import com.chenzhen.pojo.Result;`.)
- `com.chenzhen.util.SSHUtil`
- `com.jcraft.jsch.ChannelSftp`
- `com.jcraft.jsch.Session`
- `org.apache.ibatis.annotations.Param`  — KEEP (used by `dec`)
- `org.apache.poi.ss.usermodel.Workbook`
- `org.springframework.util.StringUtils`
- `org.springframework.web.bind.annotation.*`  (wildcard — keep; provides the web annotations)
- `org.springframework.web.multipart.MultipartFile`
- `javax.annotation.Resource`
- `javax.servlet.ServletOutputStream`
- `javax.servlet.http.HttpServletRequest`
- `javax.servlet.http.HttpServletResponse`
- `java.io.BufferedReader`
- `java.io.IOException`
- `java.io.InputStream`
- `java.io.InputStreamReader`
- `java.nio.charset.Charset`
- `java.util.List`
- `static com.microsoft.schemas.office.x2006.encryption.CTKeyEncryptor.Uri.type`  (unused static import — delete)

Final TbpController should contain only: package, imports (Result, TbpService, CommUtils, Autowired, RestController, RequestMapping/GetMapping/ResponseBody wildcard, RequestBody, Param, Slf4j, Map), class decl with `@RestController @Slf4j`, `@Autowired TbpService tbpService;`, `dec(...)`, `getParamValue(...)`.

- [ ] **Step 7: Commit (compile still broken — ApiServiceImpl/MessageServiceImpl/MysqlMapper refs remain; fixed in Tasks 5–8)**

```bash
git add app/src/main/java/com/chenzhen/dispatcher/ActionDispatcher.java app/src/main/java/com/chenzhen/controller/TbpController.java
git commit -m "refactor: drop deleted-service autowires from ActionDispatcher; remove start + doc routes from TbpController

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 5: Surgical — TbpService + TbpServiceImpl

**Files:**
- Modify: `app/src/main/java/com/chenzhen/service/TbpService.java`
- Modify: `app/src/main/java/com/chenzhen/service/TbpServiceImpl.java`

**Interfaces:**
- Produces: `TbpService.dec(String,String):Result` (sole kept method).

- [ ] **Step 1: TbpService interface — keep only `dec`**

Replace the entire interface body (methods 12–34) with just:
```java
    Result dec(String type, String password);
```
Delete `docQry`, `createDoc`, `getExcelTemplate`, `uploadDoc`, `uploadDocumentFile`, `docQryAll`, `queryDocAll`, `addDocFile`, `queryDocumentFileList`, `queryDocumentFile`, `deleteDocumentFile`.

Remove now-unused imports: `com.chenzhen.pojo.Doc`, `com.chenzhen.pojo.DocumentFile`, `org.apache.poi.ss.usermodel.Workbook`, `java.util.List`. Keep `com.chenzhen.pojo.Result`.

- [ ] **Step 2: TbpServiceImpl — keep only `dec`**

In `TbpServiceImpl.java`, delete every method EXCEPT `dec(String type, String password)` (lines ~307–318). Specifically delete:
- static `getExcelTemplate(String templatePath, String templateName)` (73)
- static `getBLXML(...)` (107)
- static `saveDataList(...)` (205)
- static `writeCell(...)` (221)
- static `writeList(...)` (251)
- static `saveExcel(...)` (283)
- `docQry(String docname)` (320)
- `createDoc(String type)` (328)
- `getExcelTemplate(String path)` (660)
- `uploadDoc(Doc doc)` (685)
- `uploadDocumentFile(DocumentFile docFile)` (695)
- `docQryAll(String type)` (706)
- `queryDocAll(String type)` (717)
- `addDocFile(Doc doc)` (725)
- `queryDocumentFileList(...)` (730)
- `queryDocumentFile(...)` (738)
- `deleteDocumentFile(...)` (743)

- [ ] **Step 3: Grep-gate the static Excel helpers before relying on their removal**

After Step 2, confirm the deleted static helpers are not referenced elsewhere:
```bash
grep -rn 'TbpServiceImpl.getExcelTemplate\|TbpServiceImpl.getBLXML\|TbpServiceImpl.saveExcel\|TbpServiceImpl.writeCell\|TbpServiceImpl.writeList\|TbpServiceImpl.saveDataList' app/src/main/java --include=*.java
```
Expected: no output. (ApiServiceImpl uses `DocService.*`, not `TbpServiceImpl.*` — DocService is deleted in Task 3, and ApiServiceImpl's doc methods are removed in Task 7.)

- [ ] **Step 4: TbpServiceImpl — clean imports**

Remove now-unused imports: `Doc`, `DocumentFile`, `Workbook`, `org.apache.poi.*`, `org.dom4j.*`, `java.io.*`, easyexcel `ReadListener`/`ListUtils` if present, `MysqlMapper` field if it held one, etc. **Keep:** `Result`, `CommUtils`, `DesUtil` (used by `dec`), `lombok.extern.slf4j.Slf4j`, `org.springframework.stereotype.Service`, and whatever `@Autowired`/`@Resource` `dec`'s body needs (dec uses only `CommUtils.getParamValue` + `DesUtil` + `Result` — no mapper). If `TbpServiceImpl` had an `@Autowired MysqlMapper` field used only by doc methods, delete that field too.

**Verify `dec` body has no deleted-symbol refs** — `dec` (lines 307–318) uses `CommUtils.getParamValue("passworkKey")`, `DesUtil.encode/decode`, `Result.getInstance()` — all kept. No change to `dec` itself.

- [ ] **Step 5: Commit (compile still broken — MessageServiceImpl/ApiServiceImpl/MysqlMapper remain)**

```bash
git add app/src/main/java/com/chenzhen/service/TbpService.java app/src/main/java/com/chenzhen/service/TbpServiceImpl.java
git commit -m "refactor: TbpService keeps only dec(); drop all doc/Excel methods

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 6: Surgical — MessageService + MessageServiceImpl + MessageController

**Files:**
- Modify: `app/src/main/java/com/chenzhen/service/MessageService.java`
- Modify: `app/src/main/java/com/chenzhen/service/MessageServiceImpl.java`
- Modify: `app/src/main/java/com/chenzhen/controller/MessageController.java`

**Interfaces:**
- Produces: `MessageService.addMessage/queryMessage/queryMessageById/delMessage` (kept).

- [ ] **Step 1: MessageService interface — remove cert/sms methods**

Delete these two method declarations:
```java
    Result queryMsgCode(String mobilePhone, String type) throws Exception;

    Result queryOperInfo();

    Result addOperInfo(OperInfo operInfo);
```
Keep `addMessage`, `queryMessage`, `queryMessageById`, `delMessage`. Remove the `import com.chenzhen.pojo.OperInfo;` line.

- [ ] **Step 2: MessageServiceImpl — remove cert/sms method bodies**

Read `MessageServiceImpl.java`. Delete the method bodies for `queryMsgCode(String,String)`, `queryOperInfo()`, and `addOperInfo(OperInfo)`. Keep `addMessage`, `queryMessage`, `queryMessageById`, `delMessage`.

Remove now-unused imports: `OperInfo`, and any `HttpRequestCPR09003`/`cfca.*`/`UkeyService`/`UdInfo`/`CprUser` imports that were only used by the removed methods. **CAUTION:** `MessageServiceImpl` imports `cfca.yuzhi.vo.util.StringUtil` (the cfca library) — only remove it if no kept method uses it; if `queryMessage`/`addMessage` use `StringUtil`, keep it. Check each kept method's body before removing an import.

- [ ] **Step 3: MessageController — remove cert/sms routes**

Delete these two methods (lines 69–82):
```java
    @ResponseBody
    @RequestMapping("queryMsgCode")
    public Result queryMsgCode(@RequestBody Result result) throws Exception {
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        String mobilePhone = (String) map.get("mobilePhone");
        String type = (String) map.get("Ostype");
        return messageService.queryMsgCode(mobilePhone, type);
    }

    @ResponseBody
    @RequestMapping("queryUdOper")
    public Result queryUdOper(@RequestBody Result result) throws IOException {
        return messageService.queryOperInfo();
    }
```
Keep `addMessage`, `delMessage`, `queryMessage`.

- [ ] **Step 4: MessageController — clean imports**

After removing `queryUdOper` (the only `throws IOException`), `java.io.IOException` (line 13) is unused — delete it. `java.util.List` (14) is still used by `delMessage` — keep. `java.util.Map` (15) still used — keep.

- [ ] **Step 5: Commit (compile still broken — ApiServiceImpl + MysqlMapper remain)**

```bash
git add app/src/main/java/com/chenzhen/service/MessageService.java app/src/main/java/com/chenzhen/service/MessageServiceImpl.java app/src/main/java/com/chenzhen/controller/MessageController.java
git commit -m "refactor: remove cert/sms methods from MessageService/Impl + MessageController

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 7: Surgical — ApiService + ApiServiceImpl + ApiController

**Files:**
- Modify: `app/src/main/java/com/chenzhen/service/ApiService.java`
- Modify: `app/src/main/java/com/chenzhen/service/ApiServiceImpl.java`
- Modify: `app/src/main/java/com/chenzhen/controller/ApiController.java`

**Interfaces:**
- Produces: `ApiService.tbphx(String,HttpServletRequest):Object` (sole kept method).

- [ ] **Step 1: ApiService interface — keep only `tbphx`**

Replace interface body with just:
```java
    Object tbphx(String transData, HttpServletRequest request);
```
Delete `docQryAll`, `docQry`, `uploadDoc`, `uploadDocumentFile`, `queryDocumentFileList`, `deleteDocumentFile`. Remove imports `com.chenzhen.pojo.Doc`, `com.chenzhen.pojo.DocumentFile`, `java.util.List`. Keep `javax.servlet.http.HttpServletRequest`.

- [ ] **Step 2: ApiServiceImpl — keep only `tbphx`**

Read `ApiServiceImpl.java`. Delete every method except `tbphx(String transData, HttpServletRequest request)` (lines 36–68). Specifically delete:
- `docQryAll(String type)` (70–82)
- `docQry(String docname)` (84–126)
- `createDoc(String type)` (140–241)  [note: not in interface but present as public]
- `uploadDocumentFile(DocumentFile docFile)` (243–248)
- `queryDocumentFileList(...)` (250–258)
- `deleteDocumentFile(...)` (259–end)

**Verify `tbphx` body** — confirm it does NOT call any deleted `DocService.*`/`Doc`/`Workbook` symbol. `tbphx` delegates to the action-dispatch/mock path; if it references `DocService` or `Doc`, that reference must be removed (read the body and adjust). If `tbphx` is clean, no body change.

- [ ] **Step 3: ApiServiceImpl — clean imports**

Remove now-unused: `Doc`, `DocumentFile`, `Workbook`, `org.apache.poi.*`, `DocService`, `java.io.*` file helpers, `CommUtils.getExcelTemplate` usage (if only in deleted methods). Keep what `tbphx` uses (likely `Result`, `BtoAAtoB`/`MsgService`/`ActionDispatcher`, `HttpServletRequest`, fastjson).

- [ ] **Step 4: ApiController — keep only `tbphx`**

In `ApiController.java`, delete methods `docQryAll` (36–43), `docQry` (45–50), `uploadDoc` (52–126), and the static `downExcel` (128–151). Keep only `tbphx` (30–34) and the `ApiService`/`MsgService` autowires that `tbphx` needs.

**`tbphx` uses `apiService` only** (delegates to `apiService.tbphx`). If `msgService` was used only by `uploadDoc` (it was — `msgService.getParamValue("uploadDocIP")`), then after removing `uploadDoc`, `msgService` is unused → delete the `@Autowired MsgService msgService;` field (27–28) AND the `import com.chenzhen.service.MsgService;` (9). If `tbphx` path uses `msgService`, keep it — read `tbphx`/`ApiServiceImpl.tbphx` to confirm before deleting.

- [ ] **Step 5: ApiController — clean imports**

Remove now-unused: `com.alibaba.fastjson.JSONObject`, `com.chenzhen.constant.ErrorEnum`, `com.chenzhen.pojo.Doc`, `com.chenzhen.util.CommUtils` (if only used by deleted `uploadDoc`), `org.apache.poi.ss.usermodel.Workbook`, `org.springframework.web.multipart.MultipartFile`, `javax.servlet.ServletOutputStream`, `javax.servlet.http.HttpServletResponse`, `java.io.*`. Keep: `Result`, `ApiService` (and `MsgService` only if `tbphx` needs it), `HttpServletRequest`, `org.springframework.web.bind.annotation.*`, `ResponseBody`/`RestController`, `Autowired`.

- [ ] **Step 6: Commit (compile still broken — MysqlMapper remains, the last repair)**

```bash
git add app/src/main/java/com/chenzhen/service/ApiService.java app/src/main/java/com/chenzhen/service/ApiServiceImpl.java app/src/main/java/com/chenzhen/controller/ApiController.java
git commit -m "refactor: ApiService/ApiController keep only tbphx; drop doc/excel methods

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 8: Surgical — MysqlMapper + XML (final repair → COMPILE GREEN)

**Files:**
- Modify: `app/src/main/java/com/chenzhen/mapper/mysqlMapper/MysqlMapper.java`
- Modify: `app/src/main/resources/mapper/MysqlMapper.xml`

**Interfaces:**
- Produces: the shared mapper with only kept methods/statements.

- [ ] **Step 1: Read both files**

Read `MysqlMapper.java` and `MysqlMapper.xml`.

- [ ] **Step 2: MysqlMapper.java — delete feature-only method declarations**

Delete these methods (cert + queryLog + 接口文档 + 文档管理):
- `addOperInfo(...)`, `queryOperInfo(...)` (cert)
- `addSocketMessage(...)`, `querySocketRevice(...)`, `updateSocketMsgStatus(...)` (queryLog)
- `queryDoc(...)`, `queryDocAll(...)`, `addDoc(...)`, `updateDoc(...)`, `addDocFile(...)`, `queryDocFile(...)` (接口文档)
- `addDocumentFile(...)`, `queryDocumentFileList(...)`, `queryDocumentFile(...)`, `deleteDocumentFile(...)` (文档管理)

**KEEP:** `addMessage`, `delMessage`, `queryMessage`, `queryMessageCount` (if present), `getUserName`, `getUserNameByStatus`, `queryClientInfo`, `queryWhiteUrl`, `queryWhiteInfo`, `addWhite`, `updateWhiteByUsername`, `updateWhiteByIp`, `deleteWhite`, `addLog`, and any `getParamValue`/system-param methods.

- [ ] **Step 3: MysqlMapper.xml — delete the matching `<select>/<insert>/<update>` statements**

For each method deleted in Step 2, delete its corresponding XML statement (matched by `id="..."`). Delete the `resultType`/`parameterType` references to deleted pojos (`OperInfo`, `Doc`, `DocumentFile`, `SocketMessage` is KEPT — but `addSocketMessage`/`querySocketRevice`/`updateSocketMsgStatus` use `SocketMessage` as param; since those mapper methods are deleted, the statements go too; `SocketMessage` pojo itself stays because `HttpRequestCPR09003` still uses it — do NOT delete the pojo).

- [ ] **Step 4: Grep-gate — no kept Java calls a deleted mapper method**

```bash
grep -rn 'addOperInfo\|queryOperInfo\|addSocketMessage\|querySocketRevice\|updateSocketMsgStatus\|queryDoc\b\|queryDocAll\|addDoc\b\|updateDoc\|addDocFile\|queryDocFile\|addDocumentFile\|queryDocumentFileList\|queryDocumentFile\|deleteDocumentFile' app/src/main/java --include=*.java
```
Expected: no output (all callers removed in Tasks 4–7). If any hit remains in kept code, stop and remove that caller first.

- [ ] **Step 5: COMPILE — must be GREEN now**

```bash
mvn -pl app -am clean compile -DskipTests
```
Expected: `BUILD SUCCESS`. If it fails, read each error: a missing symbol = a reference to a deleted file/method you missed; fix by removing that reference (or, for a kept file genuinely needing a deleted symbol, re-evaluate whether that pojo/service should have been kept). Common misses: an import of `Doc`/`DocumentFile`/`OperInfo`/`Sshbean` left in a kept file, or `TbpServiceImpl` still referencing `MysqlMapper`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/chenzhen/mapper/mysqlMapper/MysqlMapper.java app/src/main/resources/mapper/MysqlMapper.xml
git commit -m "refactor: MysqlMapper keeps only message/white/client/log methods; drop feature statements

Compile green.

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Task 9: Mock delete + env.properties cfca + WxydApplication javadoc

**Files:**
- Delete: 12 mock JSON files under `app/src/main/resources/mock/tbphx.do/`
- Modify (local, DO NOT COMMIT): `app/src/main/resources/env.properties`
- Modify: `app/src/main/java/com/chenzhen/WxydApplication.java`

**Interfaces:** none.

- [ ] **Step 1: Delete the 12 removed-feature mock files**

```bash
git rm "app/src/main/resources/mock/tbphx.do/action=queryMsgCode.json"
git rm "app/src/main/resources/mock/tbphx.do/action=queryUdInfo.json"
git rm "app/src/main/resources/mock/tbphx.do/action=queryUdOper.json"
git rm "app/src/main/resources/mock/tbphx.do/action=udOper.json"
git rm "app/src/main/resources/mock/tbphx.do/action=cfcaInfoQry.json"
git rm "app/src/main/resources/mock/tbphx.do/action=unBindUkey.json"
git rm "app/src/main/resources/mock/tbphx.do/action=queryCprUser.json"
git rm "app/src/main/resources/mock/tbphx.do/action=orderCreate.json"
git rm "app/src/main/resources/mock/tbphx.do/action=datadict.json"
git rm "app/src/main/resources/mock/tbphx.do/action=requestData.json"
git rm "app/src/main/resources/mock/tbphx.do/action=responseData.json"
git rm "app/src/main/resources/mock/tbphx.do/action=start.json"
```

(If a filename has spaces or differs, `ls app/src/main/resources/mock/tbphx.do/` first and match exactly.)

- [ ] **Step 2: Confirm the 9 kept mock files remain**

```bash
ls app/src/main/resources/mock/tbphx.do/
```
Expected: `action=addMessage.json`, `delMessage.json`, `queryMessage.json`, `addWhite.json`, `deleteWhite.json`, `updateWhite.json`, `queryWhiteInfo.json`, `mmLogin.json`, `ngLogin.json`.

- [ ] **Step 3: WxydApplication — fix the broken javadoc @link to CFCAConfig**

In `WxydApplication.java`, find the class javadoc line referencing `{@link com.chenzhen.config.CFCAConfig}` (line ~12) and remove that `{@link ...}` token (CFCAConfig was deleted in Task 3). Replace the sentence to not reference it, e.g.:
```
 * application.yml and env.properties reference datasource.* / cfca.* placeholders.
```
(Do not change the `@PropertySource` annotation — it stays so `datasource.*` resolves; only the comment text changes.)

- [ ] **Step 4: env.properties — remove the 4 cfca.* keys (LOCAL ONLY — DO NOT git add)**

Edit `app/src/main/resources/env.properties`; delete these 4 lines:
```
cfca.socketServerIP=...
cfca.socketServerPort=...
cfca.connectTimeout=...
cfca.readTimeout=...
```
Keep `datasource.*` and any other keys. **Do NOT run `git add env.properties`.** Confirm:
```bash
git status app/src/main/resources/env.properties
```
Expected: not staged / untracked (it was never committed — keep it that way).

- [ ] **Step 5: Recompile to confirm javadoc change is clean**

```bash
mvn -pl app -am clean compile -DskipTests
```
Expected: `BUILD SUCCESS` (no broken @link — javadoc links are warnings, not errors, but confirm no new compile error).

- [ ] **Step 6: Commit (mock + WxydApplication only — NOT env.properties)**

```bash
git add app/src/main/java/com/chenzhen/WxydApplication.java
git add -A app/src/main/resources/mock/
git commit -m "refactor: delete 12 removed-feature mock JSON; fix WxydApplication javadoc

Co-Authored-By: Claude <noreply@anthropic.com>"
```
Then verify env.properties stayed out:
```bash
git show --stat HEAD
```
Expected: no `env.properties` in the committed file list.

---

## Task 10: Verify — boot + smoke

**Files:** none (verification only).

- [ ] **Step 1: Full clean package**

```bash
mvn -pl app -am clean package -DskipTests
```
Expected: `BUILD SUCCESS`, produces `app/target/app-1.0.0.jar`.

- [ ] **Step 2: Boot the app**

```bash
java -jar app/target/app-1.0.0.jar > /tmp/wxyd-boot.log 2>&1 &
```
Wait ~5s. Grep the log:
```bash
grep "Started WxydApplication" /tmp/wxyd-boot.log
```
Expected: `Started WxydApplication in X seconds`. Also confirm `mockEnabled=true` and `ProperConfig loaded common.properties`.

- [ ] **Step 3: Smoke kept features (mock path)**

Generate transData for a kept action (e.g. queryWhiteInfo) using the `Gen.java` helper pattern: `btoa(URLEncoder.encode({"action":"queryWhiteInfo"} || <timestamp>))`. POST:
```bash
curl -s -X POST "http://localhost:8090/api/tbphx.do?transData=<encoded>"
```
Expected: the `action=queryWhiteInfo.json` mock body (errorCode 000000). Repeat for `mmLogin` and `queryMessage` → all return their mock bodies.

- [ ] **Step 4: Smoke bare endpoint (no transData)**

```bash
curl -s -o /dev/null -w "%{http_code}" "http://localhost:8090/api/tbphx.do"
```
Expected: `200` (MockAspect bare-call guide JSON).

- [ ] **Step 5: Smoke removed action → graceful empty (no 500)**

POST a removed action (e.g. `queryMsgCode`) with mock=true. Since its mock file is deleted, MockAspect should log "no mock found" and return an empty Result (HTTP 200), NOT a 500. Confirm:
```bash
curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:8090/api/tbphx.do?transData=<encoded queryMsgCode>"
```
Expected: `200`. If `500`/NPE, inspect `MockAspect` — it must handle a missing mock file by returning empty Result, not throwing. Fix MockAspect's missing-file branch if needed and re-smoke.

- [ ] **Step 6: Frontend loads with only 4 menus**

```bash
curl -s "http://localhost:8090/api/" | grep -o 'menu-item'
```
Open `http://localhost:8090/api/` in a browser; confirm only 4 menu items render (首页, 用户管理, 免密登录, 留言板) and the browser console shows no 404 for removed endpoints.

- [ ] **Step 7: Stop the app and final commit (if any boot-log fixups were needed)**

Kill the boot JVM. If Step 5 required a MockAspect fix, commit it:
```bash
git add app/src/main/java/com/chenzhen/mock/MockAspect.java
git commit -m "fix: MockAspect returns empty Result for removed actions with no mock file

Co-Authored-By: Claude <noreply@anthropic.com>"
```

- [ ] **Step 8: Update progress ledger**

Append to `.superpowers/sdd/progress.md` (or the active ledger) a summary line: "Remove-6-features: complete — 6 features removed from FE/Java/mock/mapper; compile green; boot green; smoke green (kept + removed-action fallthrough + bare 200)."

---

## Self-Review Notes (resolved during planning)

- **Spec correction:** the spec listed `HttpRequestCPR09003`, `SocketMessage`, and the `ESBPojo`/`OSBData`/`UUS*`/`MsgBean` family under "delete if confirmed manifest/sms-only." Cross-ref grep confirmed they are used by `LoginServiceImpl` (kept 免密登录) and `OSBDataListener`. **They are all KEPT.** Only `CFCAConfig` (dead @Component, sole ref = a javadoc @link) is deleted among the cert-side non-controller files.
- `dec` (kept) depends only on `CommUtils.getParamValue` + `DesUtil` + `Result` — no deleted symbols. Verified.
- `MysqlMapper` is one shared interface → method-level surgical (Task 8), not file delete. `BatchMapper` (also references `SocketMessage`) is left untouched — `SocketMessage` pojo is kept, so no break.
- Intermediate tasks (3–7) commit with broken compile by design; Task 8 restores green. This matches the module-merge plan's Task 4 precedent.
- `env.properties` real-creds file is never staged — Task 9 Step 4 edits it locally only and Task 9 Step 6 verifies its absence from the commit.
