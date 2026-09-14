package lan.chaos.hmac.verify;

import lan.chaos.hmac.core.HmacSigner;
import lan.chaos.hmac.core.SecretKeyStore;
import lan.chaos.hmac.model.ReportRequest;
import lan.chaos.hmac.model.VerifyResult;

/**
 * 上报请求完整校验链：时间戳时间窗 → HMAC 签名（current/previous 双钥）→ 防重放。
 * 全部本地计算，0 网络往返。
 *
 * <p>校验顺序说明：先查时间窗（O(1) 且不依赖密钥）再验签，可挡住绝大多数
 * 过期/伪造请求，避免无效签名计算；签名用 current 失败后再尝试 previous，
 * 以支持双密钥轮换宽限期。
 */
public final class RequestVerifier {

    /** 接口路径约定：签名时 method 固定为 POST（Demo 简化，生产按实际方法）。 */
    private static final String METHOD = "POST";

    private final SecretKeyStore keyStore;
    private final ReplayGuard replayGuard;
    private final long skewSeconds;

    /**
     * @param skewSeconds 时间戳允许偏移（秒），超出即拒绝
     */
    public RequestVerifier(SecretKeyStore keyStore, ReplayGuard replayGuard, long skewSeconds) {
        this.keyStore = keyStore;
        this.replayGuard = replayGuard;
        this.skewSeconds = skewSeconds;
    }

    public VerifyResult verify(ReportRequest req, long nowSeconds) {
        long start = System.nanoTime();

        long skew = Math.abs(nowSeconds - req.getTimestamp());
        if (skew > skewSeconds) {
            return VerifyResult.fail("timestamp out of window: skew=" + skew + "s");
        }

        String current = keyStore.getCurrent();
        boolean ok = HmacSigner.verify(current, METHOD, req.getPath(), req.getTimestamp(),
                req.getNonce(), req.getBody(), req.getSign());
        if (!ok && keyStore.getPrevious() != null) {
            // 双密钥宽限期：旧钥签名在轮换过渡期仍可验签
            ok = HmacSigner.verify(keyStore.getPrevious(), METHOD, req.getPath(),
                    req.getTimestamp(), req.getNonce(), req.getBody(), req.getSign());
        }
        if (!ok) {
            return VerifyResult.fail("sign mismatch");
        }

        if (!replayGuard.allow(req.getDeviceId(), nowSeconds * 1000L)) {
            return VerifyResult.fail("too many requests in sliding window");
        }
        if (replayGuard.isDuplicate(req.getDeviceId(), req.getBatchNo())) {
            return VerifyResult.fail("duplicated idempotency key: " + req.getBatchNo());
        }

        return VerifyResult.ok(System.nanoTime() - start);
    }
}
