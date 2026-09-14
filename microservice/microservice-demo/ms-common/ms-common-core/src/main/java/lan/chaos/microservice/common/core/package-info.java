/**
 * common-core：跨服务基础契约。
 * 承载统一响应体 {@code R<T>}、业务异常 {@code BizException}、错误码、常量、分页对象与通用工具。
 * 被所有 common 子模块与业务服务依赖，必须保持零业务、零框架强依赖（纯 POJO + 工具）。
 */
package lan.chaos.microservice.common.core;
