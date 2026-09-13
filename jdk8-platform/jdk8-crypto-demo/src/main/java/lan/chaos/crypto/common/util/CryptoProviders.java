package lan.chaos.crypto.common.util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

/**
 * BouncyCastle 提供者（BC）的统一注册入口。
 *
 * <p>WHY：JDK8 原生 JCE 只带「过时基线 + 常用商用算法」——AES/RSA/SHA-2/HMAC/DH/ECDSA 有，
 * 但 <b>SM2/SM3/SM4、SHA-3、ChaCha20-Poly1305、Ed25519/X25519、BLAKE2</b> 都没有。
 * 这些算法由 BouncyCastle 以 JCE Provider 的方式补上；注册一次，全局可用。
 *
 * <p>注册后仍必须在 {@code getInstance} 时显式指定 {@code "BC"}，
 * 因为同一算法名可能被多个 Provider 实现（如 {@code SHA3-256}、{@code ChaCha20-Poly1305}），
 * 不指定就落到默认 Provider 上、直接报 {@code NoSuchAlgorithmException}。
 */
public final class CryptoProviders {

    /** BouncyCastle 的 Provider 名称，getInstance 时用它指定提供者。 */
    public static final String BC = BouncyCastleProvider.PROVIDER_NAME;

    private CryptoProviders() {
    }

    /** 幂等注册 BC（多次调用只有第一次生效），一般放在静态块里调用。 */
    public static synchronized void ensureBouncyCastle() {
        if (Security.getProvider(BC) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
}
