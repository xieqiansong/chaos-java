package lan.chaos.rocketmq.acl;

import lan.chaos.rocketmq.common.constant.MqConstant;
import lan.chaos.rocketmq.common.util.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * ACL 权限控制：生产/消费鉴权，多租户隔离、防止越权订阅。
 * <p>
 * 做法是用 {@link AclClientRPCHook}（携带 accessKey/secretKey）构造生产者，
 * 每次请求都会由 Hook 做签名，Broker 端开启 aclEnable=true 后按 plaintext 账号配置校验。
 * <p>
 * 注意：要让它真正跑通，Broker 必须打开 ACL 并在 {@code plain_acl.yml} 配好对应的 accessKey/secretKey；
 * 否则会收到"No permission"之类的鉴权失败。本 demo 仅演示客户端接入写法。
 * <p>
 * accessKey/secretKey 建议放配置中心/环境变量，不要硬编码到代码里。
 */
@Slf4j
@Service
public class AclProducer {

    @Value("${rocketmq.name-server}")
    private String nameServer;
    @Value("${rocketmq.acl.access-key:rocketmq2}")
    private String accessKey;
    @Value("${rocketmq.acl.secret-key:12345678}")
    private String secretKey;

    private DefaultMQProducer producer;

    @PostConstruct
    public void init() throws Exception {
        SessionCredentials credentials = new SessionCredentials(accessKey, secretKey);
        // 用 RPCHook 注入签名逻辑
        producer = new DefaultMQProducer(MqConstant.GROUP_ACL, new AclClientRPCHook(credentials));
        producer.setNamesrvAddr(nameServer);
        producer.start();
    }

    public String send(String body) {
        try {
            SendResult result = producer.send(new Message(MqConstant.TOPIC_ACL, MessageUtils.pack(body).getBytes()));
            log.info("【ACL】发送完成 | msgId={}", result.getMsgId());
            return result.getMsgId();
        } catch (Exception e) {
            log.error("【ACL】发送失败（请确认 Broker 已开启 ACL 且账号匹配）", e);
            return null;
        }
    }

    @PreDestroy
    public void destroy() {
        if (producer != null) {
            producer.shutdown();
        }
    }
}
