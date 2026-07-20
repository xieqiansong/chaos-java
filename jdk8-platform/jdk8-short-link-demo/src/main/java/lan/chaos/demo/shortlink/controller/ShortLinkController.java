package lan.chaos.demo.shortlink.controller;

import lan.chaos.demo.shortlink.dto.ShortLinkCreateRequest;
import lan.chaos.demo.shortlink.dto.ShortLinkResponse;
import lan.chaos.demo.shortlink.service.ShortLinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

@RestController
public class ShortLinkController {

    private static final Logger log = LoggerFactory.getLogger(ShortLinkController.class);

    private final ShortLinkService shortLinkService;

    public ShortLinkController(ShortLinkService shortLinkService) {
        this.shortLinkService = shortLinkService;
    }

    /**
     * 生成短链
     * POST /api/short-link
     * Body: { "url": "https://example.com/very-long-url", "expireTime": "2025-12-31 23:59:59" }
     */
    @PostMapping("/api/short-link")
    public ResponseEntity<ShortLinkResponse> createShortLink(
            @Valid @RequestBody ShortLinkCreateRequest request) {
        log.info("Create short link request: url={}", request.getUrl());
        ShortLinkResponse response = shortLinkService.createShortLink(
                request.getUrl(), request.getExpireTime());
        return ResponseEntity.ok(response);
    }

    /**
     * 短链跳转
     * GET /{shortKey} -> 302 重定向
     */
    @GetMapping("/{shortKey}")
    public void redirect(@PathVariable String shortKey, HttpServletResponse response) {
        log.debug("Redirect request: key={}", shortKey);
        shortLinkService.redirect(shortKey, response);
    }

    /**
     * 健康检查
     */
    @GetMapping("/api/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
