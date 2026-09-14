package lan.chaos.filterasync.service;

import lan.chaos.filterasync.model.StatusReport;
import org.springframework.stereotype.Service;

/**
 * 上报处理服务（fire-and-forget，禁止依赖请求作用域）。
 * 运行在独立线程池，承接「读流 → 反序列化 → 校验」之后的业务处理。
 */
@Service
public class ReportService {

    private final ReportSink sink;

    public ReportService(ReportSink sink) {
        this.sink = sink;
    }

    public void submitReport(StatusReport report, String clientIp) {
        sink.submit(report, clientIp);
    }
}
