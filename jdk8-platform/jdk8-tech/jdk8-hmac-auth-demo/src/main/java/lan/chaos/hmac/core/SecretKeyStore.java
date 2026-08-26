package lan.chaos.hmac.core;

/**
 * 密钥双槽存储：{@code current}（当前签发密钥）+ {@code previous}（轮换前的旧密钥，
 * 宽限期内仍可验签）。支持滚动轮换不中断业务。
 */
public final class SecretKeyStore {

    private volatile String current;
    private volatile String previous;

    public SecretKeyStore(String current) {
        this.current = current;
    }

    public String getCurrent() {
        return current;
    }

    public String getPrevious() {
        return previous;
    }

    /** 轮换：原 current 降级为 previous，新密钥成为 current。 */
    public void rotate(String newKey) {
        this.previous = this.current;
        this.current = newKey;
    }

    /** 宽限期结束：丢弃旧密钥，此后旧钥签名全部拒绝。 */
    public void dropPrevious() {
        this.previous = null;
    }
}
