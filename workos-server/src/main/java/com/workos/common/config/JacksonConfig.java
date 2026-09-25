package com.workos.common.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Class JacksonConfig
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc JSON 序列化配置
 */
@Configuration
public class JacksonConfig {

    /**
     * @Method jacksonCustomizer
     * @Return Jackson2ObjectMapperBuilderCustomizer
     * @Desc Long 统一序列化为字符串，避免前端精度丢失；时间点输出 ISO-8601
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);

            // Instant 输出带 Z 的 ISO-8601，例如 2026-09-24T02:30:00Z
            module.addSerializer(java.time.Instant.class, InstantSerializer.INSTANCE);

            // 必须用 modulesToInstall 追加，不能用 modules 覆盖，
            // 否则会丢掉 JavaTimeModule，LocalDate / LocalDateTime 直接序列化失败
            builder.modulesToInstall(module);
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        };
    }
}
