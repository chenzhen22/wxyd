package com.cyz.controller;

import com.cyz.pojo.Result;
import com.cyz.service.GroupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 群聊控制器（桌面版 + H5 共用）。
 * <p>
 * 路径前缀 group（context-path 为 /api，完整路径 /api/group/**），
 * 鉴权由 AuthInterceptor 的 /group/** 作用域保障（必须登录）。
 * 图片存放在 {@code wxyd.share.dir}/group/yyyyMMdd/ 下，消息中只传相对 URL。
 */
@RestController
@RequestMapping("group")
@Slf4j
public class GroupController implements CommController {

    @Value("${wxyd.share.dir:/apps/shareFile}")
    private String shareDir;

    /** 图片扩展名白名单 */
    private static final String[] IMG_EXTS = {"jpg", "jpeg", "png", "gif", "webp"};
    private static final long MAX_IMG_SIZE = 5L * 1024 * 1024;
    /** 落盘文件名格式：yyyyMMdd/uuid.ext */
    private static final String IMG_NAME_PATTERN = "\\d{8}/[a-f0-9-]{36}\\.(jpg|jpeg|png|gif|webp)";

    @Autowired
    GroupService groupService;

    @PostMapping("create")
    public Result create(@RequestBody(required = false) Result req, HttpSession session) {
        Map<String, Object> body = body(req);
        return groupService.create(userId(session), str(body.get("name")));
    }

    @GetMapping("my")
    public Result my(HttpSession session) {
        return groupService.my(userId(session));
    }

    @GetMapping("search")
    public Result search(@RequestParam(value = "keyword", required = false) String keyword, HttpSession session) {
        return groupService.search(userId(session), keyword);
    }

    @PostMapping("apply")
    public Result apply(@RequestBody(required = false) Result req, HttpSession session) {
        Map<String, Object> body = body(req);
        return groupService.apply(userId(session), longVal(body.get("groupId")), str(body.get("reason")));
    }

    @GetMapping("applies")
    public Result applies(HttpSession session) {
        return groupService.applies(userId(session));
    }

    @PostMapping("handle")
    public Result handle(@RequestBody(required = false) Result req, HttpSession session) {
        Map<String, Object> body = body(req);
        boolean approve = Boolean.parseBoolean(String.valueOf(body.get("approve")));
        return groupService.handle(userId(session), longVal(body.get("applyId")), approve);
    }

    @PostMapping("kick")
    public Result kick(@RequestBody(required = false) Result req, HttpSession session) {
        Map<String, Object> body = body(req);
        return groupService.kick(userId(session), longVal(body.get("groupId")), longVal(body.get("userId")));
    }

    @GetMapping("users")
    public Result users(@RequestParam("groupId") Long groupId,
                        @RequestParam(value = "keyword", required = false) String keyword,
                        HttpSession session) {
        return groupService.users(userId(session), groupId, keyword);
    }

    @PostMapping("invite")
    public Result invite(@RequestBody(required = false) Result req, HttpSession session) {
        Map<String, Object> body = body(req);
        return groupService.invite(userId(session), longVal(body.get("groupId")), longVal(body.get("userId")));
    }

    @GetMapping("members")
    public Result members(@RequestParam("groupId") Long groupId, HttpSession session) {
        return groupService.members(userId(session), groupId);
    }

    @PostMapping("send")
    public Result send(@RequestBody(required = false) Result req, HttpSession session) {
        Map<String, Object> body = body(req);
        return groupService.send(userId(session), longVal(body.get("groupId")), str(body.get("type")), str(body.get("content")));
    }

    @GetMapping("pull")
    public Result pull(HttpSession session) {
        return groupService.pull(userId(session));
    }

    /** 上传群聊图片，返回可访问的相对 URL（group/img/get?name=...） */
    @PostMapping("img")
    public Result uploadImg(@RequestParam("image") MultipartFile image) {
        Result result = Result.getInstance();
        if (image == null || image.isEmpty()) {
            result.setErrorMsg("未选择图片");
            result.setErrorCode("000017");
            return result;
        }
        if (image.getSize() > MAX_IMG_SIZE) {
            result.setErrorMsg("图片超过 5MB 限制");
            result.setErrorCode("000017");
            return result;
        }
        String original = image.getOriginalFilename();
        String ext = extOf(original);
        if (ext == null) {
            result.setErrorMsg("仅支持 jpg/png/gif/webp 图片");
            result.setErrorCode("000017");
            return result;
        }
        String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String name = day + "/" + UUID.randomUUID() + "." + ext;
        File dir = new File(groupImgRoot(), day);
        if (!dir.exists() && !dir.mkdirs()) {
            log.warn("群聊图片目录创建失败：{}", dir.getAbsolutePath());
            result.setErrorMsg("图片保存失败");
            result.setErrorCode("000017");
            return result;
        }
        try {
            image.transferTo(new File(dir, name.substring(name.indexOf('/') + 1)));
        } catch (IOException e) {
            log.error("群聊图片上传失败", e);
            result.setErrorMsg("图片保存失败");
            result.setErrorCode("000017");
            return result;
        }
        result.setBody("group/img/get?name=" + name);
        return result;
    }

    /** 图片访问入口（需登录，<img> 标签同源自带会话 Cookie） */
    @GetMapping("img/get")
    public void getImg(@RequestParam("name") String name, HttpServletResponse response) {
        if (name == null || !name.matches(IMG_NAME_PATTERN)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        File target = new File(groupImgRoot(), name);
        try {
            // 防路径穿越：规范路径必须位于图片根目录内
            if (!target.getCanonicalPath().startsWith(groupImgRoot().getCanonicalPath() + File.separator)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!target.isFile()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setContentType(contentTypeOf(name));
        response.setContentLengthLong(target.length());
        try (InputStream in = new BufferedInputStream(Files.newInputStream(target.toPath()));
             OutputStream out = response.getOutputStream()) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            out.flush();
        } catch (IOException e) {
            log.error("群聊图片读取失败：{}", name, e);
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    private File groupImgRoot() {
        File dir = new File(shareDir, "group");
        if (!dir.exists() && !dir.mkdirs()) {
            log.warn("群聊图片根目录创建失败：{}", dir.getAbsolutePath());
        }
        return dir;
    }

    private String extOf(String name) {
        if (name == null) {
            return null;
        }
        String f = new File(name).getName();
        int dot = f.lastIndexOf('.');
        if (dot < 0 || dot == f.length() - 1) {
            return null;
        }
        String ext = f.substring(dot + 1).toLowerCase();
        for (String allowed : IMG_EXTS) {
            if (allowed.equals(ext)) {
                return "jpeg".equals(ext) ? "jpg" : ext;
            }
        }
        return null;
    }

    private String contentTypeOf(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    private Map<String, Object> body(Result req) {
        if (req == null || !(req.getBody() instanceof Map)) {
            return new HashMap<>();
        }
        return (Map<String, Object>) req.getBody();
    }

    private static Long userId(HttpSession session) {
        return (Long) session.getAttribute("userId");
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Long longVal(Object o) {
        if (o == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
