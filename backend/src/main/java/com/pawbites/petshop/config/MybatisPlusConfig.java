package com.pawbites.petshop.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置：注册框架内置拦截器。
 */
@Configuration
public class MybatisPlusConfig {

  /**
   * 构建拦截器链，加入 MySQL 分页内拦截器，
   * 使 Mapper 方法中以 Page 参数触发的分页查询自动追加 LIMIT 与 COUNT。
   *
   * @return 已装载分页插件的 MybatisPlusInterceptor
   */
  @Bean
  public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
    return interceptor;
  }
}
