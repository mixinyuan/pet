package com.pawbites.petshop.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CORS 配置属性（配置前缀 app.cors），由 SecurityConfig 启用。
 *
 * @param allowedOriginPatterns 允许的来源模式列表（支持 * 通配）；未配置时回退为放行所有来源
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOriginPatterns) {
}
