package lan.chaos.webflux.reactor;

import org.reactivestreams.Subscription;
import org.springframework.stereotype.Service;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 能力二：背压（Backpressure）——响应式区别于「回调 / 线程池」的核心。
 *
 * <p>WHY：生产者可能比消费者快得多，没有背压会撑爆消费者内存。Reactor 通过 request(n)
 * 让消费者「按需拉取」，而不是生产者硬推。拉不过来时的策略：
 * onBackpressureBuffer（缓存等待）/ onBackpressureDrop（直接丢弃）/ onBackpressureLatest（只留最新）。
 * 关键 API：Subscription.request(n)、BaseSubscriber.hookOnSubscribe/hookOnNext。
 */
@Service
public class BackpressureDemo {

    /** 快生产者：一次性「生产」20 个，模拟生产者远快于消费者。 */
    public Flux<Integer> fastProducer() {
        return Flux.range(1, 20);
    }

    /**
     * 消费者自定义拉取窗口：每次 request 5 个，处理完再要下一批。
     * 用 BaseSubscriber 钩子演示「订阅即请求、按需拉取」的背压机制。
     */
    public String run() {
        StringBuilder sb = new StringBuilder();
        CountDownLatch done = new CountDownLatch(1);

        fastProducer().subscribe(new BaseSubscriber<Integer>() {
            final int window = 5;
            int received = 0;

            @Override
            protected void hookOnSubscribe(Subscription s) {
                sb.append("onSubscribe: 首次 request(").append(window).append(")\n");
                request(window);
            }

            @Override
            protected void hookOnNext(Integer value) {
                received++;
                sb.append("  拉到 ").append(value).append(" (累计 ").append(received).append(")\n");
                // 每处理完一窗，再向生产者请求下一窗——这就是「按需拉取」的背压核心
                if (received % window == 0) {
                    request(window);
                }
            }

            @Override
            protected void hookOnComplete() {
                sb.append("onComplete: 消费者共处理 ").append(received).append(" 个\n");
                done.countDown();
            }
        });

        try {
            done.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return sb.toString();
    }
}
