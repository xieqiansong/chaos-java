package lan.chaos.demo.shortlink.dto;

/**
 * 短链响应
 */
public class ShortLinkResponse {

    private String shortKey;
    private String shortUrl;
    private String originalUrl;
    private String expireTime;

    public ShortLinkResponse() {}

    public ShortLinkResponse(String shortKey, String shortUrl, String originalUrl) {
        this.shortKey = shortKey;
        this.shortUrl = shortUrl;
        this.originalUrl = originalUrl;
    }

    public String getShortKey() { return shortKey; }
    public void setShortKey(String shortKey) { this.shortKey = shortKey; }

    public String getShortUrl() { return shortUrl; }
    public void setShortUrl(String shortUrl) { this.shortUrl = shortUrl; }

    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }

    public String getExpireTime() { return expireTime; }
    public void setExpireTime(String expireTime) { this.expireTime = expireTime; }
}
