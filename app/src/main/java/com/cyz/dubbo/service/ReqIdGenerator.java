package com.cyz.dubbo.service;

import com.cyz.dubbo.util.StringUtil;

import java.util.Map;

/**
 * 生成请求标识 reqId，并在参数 Map 中替换占位符 {@value #PLACEHOLDER}。
 *
 * <p>生成规则：{@code IDC001-{客户端ip}-{13位时间戳}{7位随机}}，
 * 即 {@code IDC001-} + 客户端 ip + {@code -} + {@code String.valueOf(System.currentTimeMillis())}
 * + {@link StringUtil#generateRandomString(int) StringUtil.generateRandomString(7)}。</p>
 *
 * <p>当 paramJson 中 {@code reqId} 取值为 {@code $reqId} 时，由后端按上述规则自动生成实际值。</p>
 */
public final class ReqIdGenerator {

    /** reqId 占位符：前端/参数里写此值，后端调用前替换为生成的实际 reqId。 */
    public static final String PLACEHOLDER = "$reqId";

    private static final String PREFIX = "IDC001-";
    private static final String UNKNOWN_IP = "unknown";

    private ReqIdGenerator() {
    }

    /**
     * 按规则生成 reqId。
     *
     * @param clientIp 客户端 ip，允许 null（按 unknown 处理）
     * @return 形如 {@code IDC001-127.0.0.1-1770000000000AbC1234} 的 reqId
     */
    public static String generate(String clientIp) {
        StringBuilder sb = new StringBuilder(PREFIX);
        sb.append(clientIp == null || clientIp.isEmpty() ? UNKNOWN_IP : clientIp);
        sb.append("-");
        sb.append(String.valueOf(System.currentTimeMillis()));
        sb.append(StringUtil.generateRandomString(7));
        return sb.toString();
    }

    /**
     * 若 paramMap 中 {@code reqId} 为占位符 {@link #PLACEHOLDER}，则替换为生成的实际值；
     * 否则原样保留（包括无 reqId 键、或值为非字符串的情况）。
     *
     * @param paramMap  请求参数 Map（会被原地修改）
     * @param clientIp 客户端 ip
     */
    @SuppressWarnings("unchecked")
    public static void resolvePlaceholder(Map<String, Object> paramMap, String clientIp) {
        if (paramMap == null) {
            return;
        }
        Object value = paramMap.get("reqId");
        if (PLACEHOLDER.equals(value)) {
            paramMap.put("reqId", generate(clientIp));
        }
    }
}
