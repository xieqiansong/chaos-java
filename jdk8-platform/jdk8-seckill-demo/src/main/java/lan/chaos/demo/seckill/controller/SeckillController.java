package lan.chaos.demo.seckill.controller;

import lan.chaos.demo.seckill.dto.SeckillRequest;
import lan.chaos.demo.seckill.dto.SeckillResponse;
import lan.chaos.demo.seckill.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 秒杀控制器
 * <p>
 * POST /api/seckill/{productId} — 秒杀入口
 */
@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    private static final Logger log = LoggerFactory.getLogger(SeckillController.class);

    private final SeckillService seckillService;

    public SeckillController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    /**
     * 秒杀接口
     *
     * @param productId 商品ID
     * @param request   请求体（userId, quantity）
     * @return 秒杀结果
     */
    @PostMapping("/{productId}")
    public SeckillResponse seckill(@PathVariable Long productId,
                                   @Valid @RequestBody SeckillRequest request) {
        log.info("秒杀请求: userId={}, productId={}", request.getUserId(), productId);
        return seckillService.seckill(productId, request);
    }
}
