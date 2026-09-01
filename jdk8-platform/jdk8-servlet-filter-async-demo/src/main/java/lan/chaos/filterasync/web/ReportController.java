package lan.chaos.filterasync.web;

import lan.chaos.filterasync.model.StatusReport;
import lan.chaos.filterasync.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 基线：完整走 DispatcherServlet → HandlerMapping → @RequestBody 反序列化 → 本方法 → 异步提交即返回。
 * 压测模式下与 {@link EarlyReportFilter} 形成对照：二者下游处理完全一致（异步提交即返回），
 * 唯一差异是被 DispatcherServlet 链路包裹与否 —— 这正是「链路能省多少 CPU」的度量点。
 */
@RestController
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/api/report")
    public ResponseEntity<Void> report(@RequestBody StatusReport report, HttpServletRequest request) {
        if (report == null || !org.springframework.util.StringUtils.hasText(report.getId())) {
            return ResponseEntity.badRequest().build();
        }
        reportService.submitReport(report, request.getRemoteAddr());
        return ResponseEntity.ok().build();
    }
}
