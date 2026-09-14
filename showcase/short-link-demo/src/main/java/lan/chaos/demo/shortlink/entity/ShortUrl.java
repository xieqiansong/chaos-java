package lan.chaos.demo.shortlink.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 短链实体
 */
@Entity
@Table(name = "t_short_url", indexes = {
        @Index(name = "idx_short_key", columnList = "shortKey", unique = true)
})
public class ShortUrl {

    @Id
    private Long id;

    /** Base62 短链 Key（如 "a3d9Kj"） */
    @Column(nullable = false, length = 10, unique = true)
    private String shortKey;

    /** 原始长链接 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    /** 创建时间 */
    @Column(nullable = false)
    private LocalDateTime createTime;

    /** 过期时间（可选） */
    private LocalDateTime expireTime;

    public ShortUrl() {}

    public ShortUrl(Long id, String shortKey, String originalUrl) {
        this.id = id;
        this.shortKey = shortKey;
        this.originalUrl = originalUrl;
        this.createTime = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getShortKey() { return shortKey; }
    public void setShortKey(String shortKey) { this.shortKey = shortKey; }

    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
}
