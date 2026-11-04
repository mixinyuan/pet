package com.pawbites.petshop.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置：注册框架内置拦截器链（分页 + 防全表更新删除）。
 */
@Configuration
public class MybatisPlusConfig {

  /**
   * 构建拦截器链：
   * <ul>
   *   <li>分页内拦截器（MySQL 方言）：使 Mapper 方法中以 Page 参数触发的分页查询自动追加
   *       LIMIT 与 COUNT，并设置单页上限 100 条（setMaxLimit），防止超大分页拖垮数据库；</li>
   *   <li>防攻击内拦截器（BlockAttackInnerInterceptor）：拦截无 WHERE 条件的整表
   *       UPDATE/DELETE 语句并抛错，防止误操作清空全表数据。</li>
   * </ul>
   *
   * @return 已装载分页与防全表更新删除插件的 MybatisPlusInterceptor
   */
  @Bean
  public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
    paginationInnerInterceptor.setMaxLimit(100L);
    interceptor.addInnerInterceptor(paginationInnerInterceptor);
    interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
    return interceptor;
  }
}
