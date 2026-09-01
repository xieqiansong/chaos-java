package lan.chaos.filterasync.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import lan.chaos.filterasync.model.StatusReport;
import lan.chaos.filterasync.service.ReportService;
import org.springframework.util.StringUtils;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 热路径前置异步 Filter：在 DispatcherServlet 之前截断最高频接口，
 * 手动读流 + 校验 + 异步提交即返回，HTTP 线程立即释放（绕过整条 MVC 链路）。
 *
 * <p>由 {@code app.mode} 控制是否启用短路：仅 {@code mode=filter-async} 时对 {@code /api/report} 短路；
 * 其余情况（含 {@code mode=controller-sync} 的基线对照）走 {@code chain.doFilter} 进入正常 MVC。
 * 这样同一份二进制、仅靠属性切换即可做公平对照。
 *
 * <p>注意事项（踩坑）：
 * ① 必须在提交异步任务前用局部变量捕获 clientIp / id，异步内不得再读 request；
 * ② getInputStream() 只能读一次，提前 return 不调 chain 因此无冲突；
 * ③ 异步任务失败客户端已 200，只能兜底日志/计数，无法回写响应。
 */
public class EarlyReportFilter implements Filter {

    private static final String HOT_PATH = "/api/report";
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EarlyReportFilter.class);

    private final ReportService reportService;
    private final Executor reportExecutor;
    private final String mode;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EarlyReportFilter(ReportService reportService, Executor reportExecutor, String mode) {
        this.reportService = reportService;
        this.reportExecutor = reportExecutor;
        this.mode = mode;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        long start = System.currentTimeMillis();
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest req = (HttpServletRequest) request;
            HttpServletResponse resp = (HttpServletResponse) response;

            if ("filter-async".equals(mode) && req.getRequestURI().endsWith(HOT_PATH)) {
                handleHotPath(req, resp);   // 命中热路径：自己处理并 return，不走 chain
                return;
            }

            chain.doFilter(request, response);   // 非热路径 / 基线模式：走正常 MVC 链路
            long cost = System.currentTimeMillis() - start;
            if (cost > 100) {
                log.warn("slow request {} cost {} ms", req.getRequestURI(), cost);
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    private void handleHotPath(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // 1. 手动读流反序列化（只取必要字段，避免 Spring 全量参数绑定）
        StatusReport report = objectMapper.readValue(req.getInputStream(), StatusReport.class);
        if (report == null || !StringUtils.hasText(report.getId())) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);   // 非法请求同步拒绝，不浪费异步资源
            return;
        }
        String clientIp = req.getRemoteAddr();   // 先捕获，避免异步内再读 request

        // 2. 异步提交：交给独立线程池，HTTP 线程立即返回 200
        CompletableFuture
                .runAsync(() -> reportService.submitReport(report, clientIp), reportExecutor)
                .whenComplete((v, ex) -> {
                    if (ex != null) {
                        // 客户端已收到 200，这里只能兜底：日志 + 计数/死信，不能回写响应
                        log.error("submitReport async failed, id={}", report.getId(), ex);
                    }
                });

        // 3. 立即响应
        resp.setStatus(HttpServletResponse.SC_OK);
    }
}
