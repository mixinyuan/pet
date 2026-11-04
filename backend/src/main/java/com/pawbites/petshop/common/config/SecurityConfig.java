package com.pawbites.petshop.common.config;

import com.pawbites.petshop.common.security.JwtAuthenticationFilter;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtAuthenticationFilter jwtAuthenticationFilter
  ) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> {
        })
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll()
            .requestMatchers(HttpMethod.GET, "/api", "/api/health").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/categories/**", "/api/products/**", "/api/bundles/**", "/api/reviews/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/subscriptions").permitAll()
            .requestMatchers("/api/cart/**", "/api/orders/**").permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(
        properties.allowedOriginPatterns() == null || properties.allowedOriginPatterns().isEmpty()
            ? List.of("*")
            : properties.allowedOriginPatterns()
    );
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setExposedHeaders(List.of("Authorization"));
    configuration.setAllowCredentials(false);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
