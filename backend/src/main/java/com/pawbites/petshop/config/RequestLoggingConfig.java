package com.pawbites.petshop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

/**
 * 请求日志配置
 * <p>Spring Boot 内嵌 Tomcat 默认不输出 HTTP 访问日志，成功请求走完 Controller/Service
 * 也不会产生任何 INFO 级日志，本地联调时控制台完全静默。注册 Spring 自带的
 * {@link CommonsRequestLoggingFilter}，为每个进入应用的请求记录一行 INFO 日志
 * （方法 + URI + 查询串），便于观察接口流量。</p>
 * <p>默认不记录请求头与请求体（避免泄露 token/密码等敏感信息）；如需更详细的
 * 出参/入参排查，建议为具体接口补充 DEBUG 级业务日志而非在此放开 payload。</p>
 */
@Configuration
public class RequestLoggingConfig {

  /** 请求日志过滤器：以 INFO 级别输出 "Before request [GET /api/products?...]" 形式的访问日志 */
  @Bean
  public CommonsRequestLoggingFilter requestLoggingFilter() {
    CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
    // 记录查询字符串（分页、筛选参数）
    filter.setIncludeQueryString(true);
    // 不记录请求头（含 Authorization，避免 token 泄露到日志）
    filter.setIncludeHeaders(false);
    // 不记录请求体（含密码等敏感字段）
    filter.setIncludePayload(false);
    // 不记录客户端信息
    filter.setIncludeClientInfo(false);
    return filter;
  }
}
