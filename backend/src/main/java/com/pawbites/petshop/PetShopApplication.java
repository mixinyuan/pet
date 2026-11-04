package com.pawbites.petshop;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.pawbites.petshop.mapper")
@SpringBootApplication
public class PetShopApplication {

  public static void main(String[] args) {
    SpringApplication.run(PetShopApplication.class, args);
  }
}
