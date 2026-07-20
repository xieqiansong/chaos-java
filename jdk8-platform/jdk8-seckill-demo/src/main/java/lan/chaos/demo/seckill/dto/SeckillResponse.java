package lan.chaos.demo.seckill.dto;

/**
 * 秒杀响应
 */
public class SeckillResponse {

    private int code;
    private String message;
    private Data data;

    public SeckillResponse() {
    }

    public SeckillResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public SeckillResponse(int code, String message, Data data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    // ===== 快捷工厂方法 =====

    public static SeckillResponse success(String token, Long orderId) {
        Data data = new Data(token, orderId);
        return new SeckillResponse(200, "抢购成功", data);
    }

    public static SeckillResponse soldOut() {
        return new SeckillResponse(410, "商品已售罄");
    }

    public static SeckillResponse rateLimited() {
        return new SeckillResponse(429, "请求太频繁，请稍后再试");
    }

    public static SeckillResponse duplicate() {
        return new SeckillResponse(409, "您已购买过该商品，限购1件");
    }

    public static SeckillResponse error(String message) {
        return new SeckillResponse(500, message);
    }

    /**
     * 响应数据
     */
    public static class Data {
        private String token;
        private Long orderId;

        public Data() {
        }

        public Data(String token, Long orderId) {
            this.token = token;
            this.orderId = orderId;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }
    }
}
