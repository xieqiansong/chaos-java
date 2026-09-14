package lan.chaos.webflux.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 演示实体：自带 sample() 工厂，调用方无需自己准备输入数据。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private Long id;
    private String name;
    private double price;
    private int stock;

    /** 样例工厂：按 id 造一个确定性商品。 */
    public static Product sample(long id) {
        return new Product(id, "product-" + id, 10.0 * id, (int) (id * 5));
    }

    /** 造 n 个样例商品。 */
    public static List<Product> samples(int n) {
        List<Product> list = new ArrayList<>();
        for (long i = 1; i <= n; i++) {
            list.add(sample(i));
        }
        return list;
    }
}
