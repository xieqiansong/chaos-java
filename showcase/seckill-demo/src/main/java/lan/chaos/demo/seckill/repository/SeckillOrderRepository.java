package lan.chaos.demo.seckill.repository;

import lan.chaos.demo.seckill.entity.SeckillOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 秒杀订单数据访问层
 */
@Repository
public interface SeckillOrderRepository extends JpaRepository<SeckillOrder, Long> {

    /**
     * 根据令牌查询订单
     */
    Optional<SeckillOrder> findByToken(String token);

    /**
     * 统计用户对某商品的购买数量（用于限购检查）
     */
    long countByUserIdAndProductIdAndStatus(String userId, Long productId, String status);
}
