package lan.chaos.demo.seckill.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/**
 * 秒杀请求
 */
public class SeckillRequest {

    @NotBlank(message = "用户ID不能为空")
    private String userId;

    @Min(value = 1, message = "购买数量至少为1")
    private Integer quantity = 1;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
