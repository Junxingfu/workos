package com.workos;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 启动类；Reminder 由客户端驱动，MVP 不开启服务端调度 */
@SpringBootApplication
@MapperScan("com.workos.**.infrastructure.persistence.mapper")
public class WorkosApplication {

  /** 启动入口 */
  public static void main(String[] args) {
    SpringApplication.run(WorkosApplication.class, args);
  }
}
