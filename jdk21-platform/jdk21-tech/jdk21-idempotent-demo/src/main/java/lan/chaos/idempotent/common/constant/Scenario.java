package lan.chaos.idempotent.common.constant;

/**
 * 幂等防护的三个分层场景：
 * REQUEST  —— 请求级：前端/网关重复提交，用幂等号（requestId）防重。
 * CONSUME  —— 消费级：MQ 至少一次投递，用 messageId 防重复消费。
 * STATE    —— 状态机级：流程回调重复到达，已终态则忽略。
 */
public enum Scenario {
    REQUEST("请求级幂等：幂等号 + 并发双发事务首检"),
    CONSUME("消费级幂等：messageId 去重"),
    STATE("状态机级幂等：终态回调忽略");

    private final String desc;

    Scenario(String desc) {
        this.desc = desc;
    }

    public String desc() {
        return desc;
    }
}
