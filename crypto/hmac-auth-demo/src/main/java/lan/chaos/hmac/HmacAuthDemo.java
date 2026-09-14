package lan.chaos.hmac;

import lan.chaos.hmac.bench.ThroughputBenchmark;
import lan.chaos.hmac.core.HmacSigner;
import lan.chaos.hmac.core.SecretKeyStore;
import lan.chaos.hmac.model.ReportRequest;
import lan.chaos.hmac.model.VerifyResult;
import lan.chaos.hmac.rotate.KeyRotationManager;
import lan.chaos.hmac.verify.ReplayGuard;
import lan.chaos.hmac.verify.RequestVerifier;

/**
 * HMAC 无状态签名鉴权 Demo 入口：串行演示四个场景。
 *
 * <pre>
 *   场景 A：签名 / 验签（篡改、错误密钥被拒）
 *   场景 B：防重放（时间窗外、超频、幂等键重复被拒）
 *   场景 C：双密钥轮换（宽限期旧钥可验，宽限期后拒绝）
 *   场景 D：吞吐对比（本地验签 0 往返 vs Token+Redis 读 1 往返）
 * </pre>
 */
public final class HmacAuthDemo {

    private static final String SECRET = "demo-secret";
    private static final String PATH = "/v1/report";
    private static final String METHOD = "POST";

    public static void main(String[] args) {
        System.out.println("== HMAC 无状态签名鉴权 Demo（纯 JDK8，零运行时依赖） ==");

        sceneA();
        sceneB();
        sceneC();
        sceneD();

        System.out.println();
        System.out.println("== 全部场景演示完成 ==");
    }

    /** 场景 A：签名 / 验签。 */
    private static void sceneA() {
        System.out.println();
        System.out.println("== 场景 A：签名 / 验签 ==");
        SecretKeyStore keyStore = new SecretKeyStore(SECRET);
        RequestVerifier verifier = new RequestVerifier(keyStore,
                new ReplayGuard(100, 60_000L), 300);

        long now = System.currentTimeMillis() / 1000L;
        String nonce = HmacSigner.newNonce();
        String body = "{\"online\":1,\"cpu\":42}";
        String sign = HmacSigner.sign(SECRET, METHOD, PATH, now, nonce, body);

        ReportRequest ok = request("dev-00001", now, nonce, "batch-001", body, sign);
        VerifyResult pass = verifier.verify(ok, now);
        System.out.println("合法请求（正确签名）      -> " + pass);

        // 篡改 body 后验签
        ReportRequest tampered = request("dev-00001", now, nonce, "batch-001",
                "{\"online\":1,\"cpu\":99}", sign);
        System.out.println("篡改请求体后的原签名      -> " + verifier.verify(tampered, now));

        // 错误密钥签名
        String wrongSign = HmacSigner.sign("wrong-secret", METHOD, PATH, now, nonce, body);
        ReportRequest wrongKey = request("dev-00001", now, nonce, "batch-001", body, wrongSign);
        System.out.println("错误密钥签名              -> " + verifier.verify(wrongKey, now));
    }

    /** 场景 B：防重放。 */
    private static void sceneB() {
        System.out.println();
        System.out.println("== 场景 B：防重放（时间戳时间窗 + 滑动窗口频率 + 业务幂等） ==");
        SecretKeyStore keyStore = new SecretKeyStore(SECRET);
        // 窗口 60s 内每设备最多 5 次
        RequestVerifier verifier = new RequestVerifier(keyStore,
                new ReplayGuard(5, 60_000L), 300);
        long now = System.currentTimeMillis() / 1000L;

        // 1. 时间戳超窗（5 分钟前）
        long oldTs = now - 301;
        String staleNonce = HmacSigner.newNonce();
        String oldSign = HmacSigner.sign(SECRET, METHOD, PATH, oldTs, staleNonce, "{}");
        ReportRequest stale = request("dev-00002", oldTs, staleNonce, "batch-002",
                "{}", oldSign);
        System.out.println("时间戳超出时间窗(301s前)  -> " + verifier.verify(stale, now));

        // 2. 窗口内连续高频（第 6 次被限流）
        String lastResult = "";
        for (int i = 1; i <= 6; i++) {
            long ts = now;
            String n = HmacSigner.newNonce();
            String s = HmacSigner.sign(SECRET, METHOD, PATH, ts, n, "{}");
            ReportRequest burst = request("dev-00003", ts, n, "batch-00" + i, "{}", s);
            lastResult = verifier.verify(burst, now).toString();
        }
        System.out.println("滑动窗口内第 6 次请求     -> " + lastResult);

        // 3. 幂等键重复（同 deviceId+batchNo 重放：换新 nonce 新签名，仍被幂等键拦截）
        long ts = now;
        String n = HmacSigner.newNonce();
        String s = HmacSigner.sign(SECRET, METHOD, PATH, ts, n, "{}");
        ReportRequest first = request("dev-00004", ts, n, "batch-004", "{}", s);
        System.out.println("首次上报(batch-004)        -> " + verifier.verify(first, now));
        String dupNonce = HmacSigner.newNonce();
        String dupSign = HmacSigner.sign(SECRET, METHOD, PATH, ts, dupNonce, "{}");
        ReportRequest dup = request("dev-00004", ts, dupNonce, "batch-004", "{}", dupSign);
        System.out.println("同批次号重放(batch-004)    -> " + verifier.verify(dup, now));
    }

    /** 场景 C：双密钥轮换。 */
    private static void sceneC() {
        System.out.println();
        System.out.println("== 场景 C：双密钥过渡轮换 ==");
        SecretKeyStore keyStore = new SecretKeyStore(SECRET);
        KeyRotationManager rotation = new KeyRotationManager(keyStore);
        RequestVerifier verifier = new RequestVerifier(keyStore,
                new ReplayGuard(100, 60_000L), 300);
        long now = System.currentTimeMillis() / 1000L;

        // 轮换：SECRET -> SECRET_V2，原密钥降为 previous（宽限期）
        String secretV2 = "demo-secret-v2";
        rotation.rotate(secretV2);
        System.out.println("轮换后仍处宽限期: " + rotation.inGracePeriod());

        // 新密钥签名立即生效
        String n = HmacSigner.newNonce();
        String newSign = HmacSigner.sign(secretV2, METHOD, PATH, now, n, "{}");
        System.out.println("新密钥签发（立即生效）    -> "
                + verifier.verify(request("dev-00005", now, n, "batch-005", "{}", newSign), now));

        // 旧密钥在宽限期内仍可验签（在途设备未升级）
        String oldNonce = HmacSigner.newNonce();
        String oldSign = HmacSigner.sign(SECRET, METHOD, PATH, now, oldNonce, "{}");
        System.out.println("旧密钥宽限期内（在途设备）-> "
                + verifier.verify(request("dev-00006", now, oldNonce, "batch-006",
                "{}", oldSign), now));

        // 宽限期结束：旧密钥被拒
        rotation.completeRotation();
        System.out.println("宽限期结束, 仍处宽限期: " + rotation.inGracePeriod());
        String staleNonce2 = HmacSigner.newNonce();
        String staleOldSign = HmacSigner.sign(SECRET, METHOD, PATH, now, staleNonce2, "{}");
        System.out.println("旧密钥宽限期后            -> "
                + verifier.verify(request("dev-00007", now, staleNonce2, "batch-007",
                "{}", staleOldSign), now));
    }

    /** 场景 D：吞吐对比。 */
    private static void sceneD() {
        new ThroughputBenchmark(100_000, 8, 1000).run();
    }

    private static ReportRequest request(String deviceId, long ts, String nonce,
                                         String batchNo, String body, String sign) {
        ReportRequest req = new ReportRequest();
        req.setDeviceId(deviceId);
        req.setPath(PATH);
        req.setTimestamp(ts);
        req.setNonce(nonce);
        req.setBatchNo(batchNo);
        req.setBody(body);
        req.setSign(sign);
        return req;
    }
}
