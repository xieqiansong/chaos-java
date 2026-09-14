package lan.chaos.excel.common.model;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 样例数据模型：订单。全模块共用，POI / EasyExcel / Hutool 三种实现都读写它，便于横向对比。
 *
 * <p>自带 {@link #samples(int)} 工厂，调用方无需自己准备输入。
 * 字段全部是虚构数据，不含任何真实业务信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @ExcelProperty("订单号")
    private String orderNo;

    @ExcelProperty("客户")
    private String customer;

    @ExcelProperty("商品")
    private String product;

    @ExcelProperty("数量")
    private Integer quantity;

    @ExcelProperty("金额")
    private BigDecimal amount;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("下单时间")
    private Date createdAt;

    /** 样例工厂：造 1 条默认订单。 */
    public static Order sample(long i) {
        return new Order(
                "SO-0000" + i,
                "客户-" + (i % 7),
                "商品-" + (i % 13),
                (int) (1 + i % 9),
                new BigDecimal(99).multiply(BigDecimal.valueOf(1 + i % 50)).setScale(2, BigDecimal.ROUND_HALF_UP),
                i % 11 == 0 ? "CANCELLED" : "PAID",
                new Date()
        );
    }

    /** 样例工厂：造 n 条订单（序号连续，便于断言行数与顺序）。 */
    public static java.util.List<Order> samples(int n) {
        java.util.List<Order> list = new java.util.ArrayList<>(n);
        for (int i = 1; i <= n; i++) {
            list.add(sample(i));
        }
        return list;
    }
}
