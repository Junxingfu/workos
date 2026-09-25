package com.workos;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @Class WorkosApplication
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 启动类；Reminder 由客户端驱动，MVP 不开启服务端调度
 */
@SpringBootApplication
@MapperScan("com.workos.**.data.mapper")
public class WorkosApplication {

    /**
     * @Method main
     * @Param [args]
     * @Desc 启动入口
     */
    public static void main(String[] args) {
        SpringApplication.run(WorkosApplication.class, args);
    }
}
