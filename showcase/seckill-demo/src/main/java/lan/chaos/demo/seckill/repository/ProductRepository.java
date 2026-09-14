package lan.chaos.demo.seckill.repository;

import lan.chaos.demo.seckill.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 商品数据访问层
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * 查询所有秒杀中的商品
     */
    List<Product> findByStatus(String status);
}
