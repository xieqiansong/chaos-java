package lan.chaos.demo.shortlink.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 创建短链请求
 */
public class ShortLinkCreateRequest {

    @NotBlank(message = "原始 URL 不能为空")
    @Pattern(regexp = "^(http|https)://.*", message = "URL 必须以 http:// 或 https:// 开头")
    private String url;

    /** 过期时间（可选，格式: yyyy-MM-dd HH:mm:ss） */
    private String expireTime;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getExpireTime() { return expireTime; }
    public void setExpireTime(String expireTime) { this.expireTime = expireTime; }
}
