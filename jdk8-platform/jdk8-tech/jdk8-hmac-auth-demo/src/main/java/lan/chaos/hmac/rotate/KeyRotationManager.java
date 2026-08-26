package lan.chaos.hmac.rotate;

import lan.chaos.hmac.core.SecretKeyStore;

/**
 * 双密钥过渡轮换：新密钥立即用于签发，旧密钥在宽限期内继续接受验签，
 * 宽限期结束后丢弃旧密钥，实现平滑切换不中断在途设备。
 *
 * <p>生产配套：宽限期结束前应通过集中配置/密钥管理服务把新密钥广播到各节点，
 * 并给客户端预留升级窗口；本 Demo 聚焦轮换机制本身。
 */
public final class KeyRotationManager {

    private final SecretKeyStore keyStore;

    public KeyRotationManager(SecretKeyStore keyStore) {
        this.keyStore = keyStore;
    }

    /** 轮换：新密钥成为 current（立即生效），原 current 降为 previous。 */
    public void rotate(String newKey) {
        keyStore.rotate(newKey);
    }

    /** 宽限期结束：丢弃旧密钥，此后旧钥签名全部拒绝。 */
    public void completeRotation() {
        keyStore.dropPrevious();
    }

    /** 是否仍处于宽限期（存在旧密钥可验签）。 */
    public boolean inGracePeriod() {
        return keyStore.getPrevious() != null;
    }
}
