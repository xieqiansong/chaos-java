package lan.chaos.demo.shortlink.repository;

import lan.chaos.demo.shortlink.entity.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    /**
     * 根据短链 Key 查询原始 URL
     */
    Optional<ShortUrl> findByShortKey(String shortKey);

    /**
     * 根据原始 URL 查询已存在的短链
     */
    Optional<ShortUrl> findByOriginalUrl(String originalUrl);
}
