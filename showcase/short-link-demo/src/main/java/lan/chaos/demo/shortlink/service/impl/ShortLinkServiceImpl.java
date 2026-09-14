package lan.chaos.demo.shortlink.service.impl;

import lan.chaos.demo.shortlink.dto.ShortLinkResponse;
import lan.chaos.demo.shortlink.entity.ShortUrl;
import lan.chaos.demo.shortlink.idgen.SnowflakeIdGenerator;
import lan.chaos.demo.shortlink.repository.ShortUrlRepository;
import lan.chaos.demo.shortlink.service.ShortLinkService;
import lan.chaos.demo.shortlink.util.Base62Util;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class ShortLinkServiceImpl implements ShortLinkService {

    private static final Logger log = LoggerFactory.getLogger(ShortLinkServiceImpl.class);

    /** 短链在 Redis 中的过期时间（秒）- 7 天 */
    private static final long REDIS_TTL_SECONDS = 7 * 24 * 3600;

    private final SnowflakeIdGenerator idGenerator;
    private final ShortUrlRepository shortUrlRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheManager caffeineCacheManager;
    private final RBloomFilter<String> bloomFilter;
    private final RedissonClient redissonClient;

    @Value("${short-link.domain:http://localhost:8080}")
    private String domain;

    public ShortLinkServiceImpl(ShortUrlRepository shortUrlRepository,
                                RedisTemplate<String, Object> redisTemplate,
                                CacheManager caffeineCacheManager,
                                RBloomFilter<String> bloomFilter,
                                RedissonClient redissonClient) {
        this.idGenerator = new SnowflakeIdGenerator();
        this.shortUrlRepository = shortUrlRepository;
        this.redisTemplate = redisTemplate;
        this.caffeineCacheManager = caffeineCacheManager;
        this.bloomFilter = bloomFilter;
        this.redissonClient = redissonClient;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShortLinkResponse createShortLink(String originalUrl, String expireTime) {
        // 1. 检查是否已存在该 URL 的短链
        Optional<ShortUrl> existing = shortUrlRepository.findByOriginalUrl(originalUrl);
        if (existing.isPresent()) {
            return buildResponse(existing.get());
        }

        // 2. 发号器生成唯一 ID
        long id = idGenerator.nextId();

        // 3. Base62 编码为短 Key
        String shortKey = Base62Util.encode(id);

        // 4. 构建实体
        ShortUrl shortUrl = new ShortUrl(id, shortKey, originalUrl);
        if (expireTime != null && !expireTime.isEmpty()) {
            try {
                shortUrl.setExpireTime(LocalDateTime.parse(expireTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } catch (DateTimeParseException e) {
                log.warn("Invalid expire time format: {}", expireTime);
            }
        }

        // 5. 写入数据库
        shortUrlRepository.save(shortUrl);

        // 6. 写入 Redis 缓存 + 布隆过滤器
        String redisKey = "short:" + shortKey;
        redisTemplate.opsForValue().set(redisKey, originalUrl, REDIS_TTL_SECONDS, TimeUnit.SECONDS);
        bloomFilter.add(shortKey);

        log.info("Short link created: key={}, original={}", shortKey, originalUrl);
        return buildResponse(shortUrl);
    }

    @Override
    public void redirect(String shortKey, HttpServletResponse response) {
        // 1. 布隆过滤器快速过滤无效 Key
        if (!bloomFilter.contains(shortKey)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 2. 查询原始 URL（多级缓存）
        String originalUrl = getOriginalUrl(shortKey);

        if (originalUrl == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 3. 302 重定向
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", originalUrl);
    }

    /**
     * 多级缓存查询原始 URL:
     * 1. Caffeine 本地缓存（最快）
     * 2. Redis 远程缓存
     * 3. 数据库兜底
     */
    private String getOriginalUrl(String shortKey) {
        // Level 1: Caffeine 本地缓存
        Cache caffeineCache = caffeineCacheManager.getCache("short-link");
        if (caffeineCache != null) {
            Cache.ValueWrapper wrapper = caffeineCache.get(shortKey);
            if (wrapper != null) {
                String url = (String) wrapper.get();
                if (url != null) {
                    log.debug("Cache L1 hit: key={}", shortKey);
                    return url;
                }
            }
        }

        // Level 2: Redis 缓存
        String redisKey = "short:" + shortKey;
        String url = (String) redisTemplate.opsForValue().get(redisKey);
        if (url != null) {
            log.debug("Cache L2 hit: key={}", shortKey);
            // 回填 L1 缓存
            if (caffeineCache != null) {
                caffeineCache.put(shortKey, url);
            }
            return url;
        }

        // Level 3: 数据库查询
        Optional<ShortUrl> shortUrlOpt = shortUrlRepository.findByShortKey(shortKey);
        if (shortUrlOpt.isPresent()) {
            ShortUrl shortUrl = shortUrlOpt.get();

            // 检查是否过期
            if (shortUrl.getExpireTime() != null && shortUrl.getExpireTime().isBefore(LocalDateTime.now())) {
                log.warn("Short link expired: key={}", shortKey);
                return null;
            }

            url = shortUrl.getOriginalUrl();

            // 回填 Redis 缓存
            redisTemplate.opsForValue().set(redisKey, url, REDIS_TTL_SECONDS, TimeUnit.SECONDS);
            // 回填 L1 缓存
            if (caffeineCache != null) {
                caffeineCache.put(shortKey, url);
            }

            log.debug("Cache L3 hit: key={}", shortKey);
            return url;
        }

        log.warn("Short link not found: key={}", shortKey);
        return null;
    }

    private ShortLinkResponse buildResponse(ShortUrl shortUrl) {
        ShortLinkResponse resp = new ShortLinkResponse(
                shortUrl.getShortKey(),
                domain + "/" + shortUrl.getShortKey(),
                shortUrl.getOriginalUrl()
        );
        if (shortUrl.getExpireTime() != null) {
            resp.setExpireTime(shortUrl.getExpireTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        return resp;
    }
}
