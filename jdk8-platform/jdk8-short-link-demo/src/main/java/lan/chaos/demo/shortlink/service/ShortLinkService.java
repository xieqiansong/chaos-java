package lan.chaos.demo.shortlink.service;

import lan.chaos.demo.shortlink.dto.ShortLinkResponse;

import javax.servlet.http.HttpServletResponse;

public interface ShortLinkService {

    /**
     * 创建短链
     *
     * @param originalUrl 原始长链
     * @param expireTime  过期时间（可选）
     * @return 短链响应（含短链 Key、短链 URL、原始 URL）
     */
    ShortLinkResponse createShortLink(String originalUrl, String expireTime);

    /**
     * 跳转到原始 URL
     *
     * @param shortKey 短链 Key
     * @param response HttpServletResponse，用于 302 重定向
     */
    void redirect(String shortKey, HttpServletResponse response);
}
