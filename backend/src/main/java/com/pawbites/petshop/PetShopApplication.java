package com.pawbites.petshop;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 宠物商城后端启动类。
 *
 * <p>通过 {@code @SpringBootApplication} 开启组件扫描与自动装配；
 * 通过 {@code @MapperScan} 扫描 com.pawbites.petshop.mapper 包，
 * 将其中的 MyBatis 映射接口注册为 Bean。
 */
@MapperScan("com.pawbites.petshop.mapper")
@SpringBootApplication
public class PetShopApplication {

  /**
   * 应用入口：启动 Spring Boot 内嵌容器。
   *
   * @param args 命令行启动参数
   */
  public static void main(String[] args) {
    SpringApplication.run(PetShopApplication.class, args);
  }
}
