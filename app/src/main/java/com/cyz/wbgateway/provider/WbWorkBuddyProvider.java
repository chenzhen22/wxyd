package com.cyz.wbgateway.provider;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.cyz.wbgateway.WbCredential;
import com.cyz.wbgateway.WbModelCatalog;
import com.cyz.wbgateway.WbUpstreamException;
import com.cyz.wbgateway.WbUpstreamSseReader;
import com.cyz.wbgateway.WbWorkBuddyClient;
import com.cyz.wbgateway.pool.WbPoolAccount;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * WorkBuddy 适配器：把现有账号池 + 模型目录 + SSE 客户端包装成通用 provider。
 * <p>
 * 账号级容错（令牌刷新、429 模型级冷却、402 账号级冷却、换号）在本类内部完成，
 * 候选链（模型降级）也在这里推进，网关只需拿最终的流。
 */
@Component
@Order(10)
@Slf4j
public class WbWorkBuddyProvider implements UpstreamProvider {

    @Autowired
    private WbWorkBuddyClient client;

    @Override
    public String id() {
        return "workbuddy";
    }

    @Override
    public String displayName() {
        return "WorkBuddy";
    }

    @Override
    public ModelCatalog catalog() {
        return client.models();
    }

    @Override
    public String defaultModel() {
        return client.getDefaultModel();
    }

    @Override
    public Map<String, Object> status() {
        return client.bridgeStatus();
    }

    @Override
    public int usableAccountCount() {
        try {
            Object h = client.bridgeStatus().get("healthy");
            return h instanceof Number ? ((Number) h).intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public long remainingCredits() {
        try {
            Object accounts = client.bridgeStatus().get("accounts");
            if (!(accounts instanceof List)) return -1;
            long sum = 0;
            boolean any = false;
            for (Object o : (List<?>) accounts) {
                if (!(o instanceof Map)) continue;
                Object cr = ((Map<?, ?>) o).get("credits");
                if (cr instanceof Number) {
                    any = true;
                    sum += ((Number) cr).longValue();
                }
            }
            return any ? sum : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    /** 账号积分是否过期由上游账号体系决定，这里无法得知，视为不过期 */
    @Override
    public long creditsExpireAtMs() {
        return Long.MAX_VALUE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> accountRows() {
        try {
            Object accounts = client.bridgeStatus().get("accounts");
            if (accounts instanceof List) return (List<Map<String, Object>>) accounts;
        } catch (Exception ignore) {
        }
        return new java.util.ArrayList<>();
    }

    @Override
    public ProviderStream open(List<String> chain, String openAiBodyJson) throws WbUpstreamException {
        JSONObject openaiReq;
        try {
            openaiReq = JSON.parseObject(openAiBodyJson);
        } catch (Exception e) {
            throw new WbUpstreamException(WbUpstreamException.KIND_CLIENT, 400, "请求体不是合法 JSON");
        }
        if (openaiReq == null) throw new WbUpstreamException(WbUpstreamException.KIND_CLIENT, 400, "请求体为空");

        Response upstream = null;
        WbUpstreamException failure = null;
        int chainIdx = 0;
        WbPoolAccount acc = null;
        WbCredential cred = null;
        boolean refreshedOnce = false;
        int switched = 0;
        int maxSwitch = 8;

        while (true) {
            openaiReq.put("model", chain.get(chainIdx));
            String bodyJson = client.prepareBody(openaiReq.toJSONString());
            try {
                if (acc == null) {
                    try {
                        acc = client.acquireAccount(chain.get(chainIdx));
                    } catch (IllegalStateException ne) {
                        // 账号池暂时无可用号（全部冷却/判死）属于可重试，交给上层换 provider
                        failure = new WbUpstreamException(WbUpstreamException.KIND_SOFT_RATE, 429, ne.getMessage());
                        break;
                    }
                    cred = acc.getCredential().toWbCredential();
                    refreshedOnce = false;
                }
                upstream = client.chatStream(cred, bodyJson);
                client.onSuccess(acc.getId());
                break;
            } catch (WbUpstreamException e) {
                boolean canChain = chainIdx < chain.size() - 1 && WbWorkBuddyClient.isModelFallbackRetryable(e);
                if (WbUpstreamException.KIND_SESSION_DEAD.equals(e.getKind()) && !refreshedOnce && acc != null) {
                    refreshedOnce = true;
                    try {
                        client.refreshAccount(acc.getId());
                        cred = acc.getCredential().toWbCredential();
                        log.warn("[WBGATEWAY] 账号 {} 令牌被上游判死，已刷新重试", acc.getLabel());
                        continue;
                    } catch (Exception re) {
                        client.onCredentialDead(acc.getId(), re.getMessage());
                        acc = null;
                        continue;
                    }
                }
                if (WbUpstreamException.KIND_SOFT_RATE.equals(e.getKind()) && acc != null) {
                    client.onModelRateLimited(acc.getId(), chain.get(chainIdx));
                    acc = null;
                    if (switched++ < maxSwitch) continue;
                    if (canChain) { chainIdx++; continue; }
                    failure = e;
                    break;
                }
                if (WbUpstreamException.KIND_HARD_CREDIT.equals(e.getKind()) && acc != null) {
                    client.onHardCredit(acc.getId());
                    acc = null;
                    if (switched++ < maxSwitch) continue;
                    if (canChain) { chainIdx++; continue; }
                    failure = e;
                    break;
                }
                if (canChain) {
                    log.warn("[WBGATEWAY] 模型 {} 失败（{}），切换候选链下一个 {}",
                            chain.get(chainIdx), e.getKind(), chain.get(chainIdx + 1));
                    chainIdx++;
                    acc = null;
                    continue;
                }
                failure = e;
                break;
            }
        }
        if (failure != null) {
            if (WbUpstreamException.KIND_SESSION_DEAD.equals(failure.getKind()) && acc != null) {
                client.onCredentialDead(acc.getId(), failure.getMessage());
            }
            throw failure;
        }

        final Response resp = upstream;
        final String usedModel = chain.get(chainIdx);
        return new ProviderStream() {
            @Override
            public String usedModel() {
                return usedModel;
            }

            @Override
            public void read(Consumer<JSONObject> onChunk) throws IOException {
                WbUpstreamSseReader.read(resp, onChunk);
            }

            @Override
            public void close() {
                try {
                    resp.close();
                } catch (Exception ignore) {
                }
            }
        };
    }
}
