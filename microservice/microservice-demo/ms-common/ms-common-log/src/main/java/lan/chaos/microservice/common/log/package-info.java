/**
 * common-log：可观测性基础。
 * 承载 MDC traceId 工具、访问日志约定与 logback 配置约定。
 * 注意：本模块不依赖 Servlet API，访问日志切面（基于 HttpServletRequest）放在 common-web，
 * 以避免与网关（WebFlux）冲突。
 */
package lan.chaos.microservice.common.log;
