package com.workos.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Class WebMvcConfig
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 前后端分开部署时的 CORS 开关（正式环境同域反代可以不开）
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 开发期必须按"任意本地端口"放行：
     * 5173 被占用时 Vite 会自动顺延到 5174/5175，而代理转发会原样带上 Origin，
     * 写死端口会让 POST/PUT/DELETE 全部 403（GET 不带 Origin，所以看着像是"只有某个接口坏了"）
     */
    @Value("${workos.cors.allowed-origin-patterns:http://localhost:[*],http://127.0.0.1:[*]}")
    private String allowedOriginPatterns;

    /**
     * @Method addCorsMappings
     * @Param [registry]
     * @Desc 放行本地 Vite 开发端口
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOriginPatterns.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
