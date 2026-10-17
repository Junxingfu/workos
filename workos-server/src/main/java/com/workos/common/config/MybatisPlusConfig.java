package com.workos.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import java.time.Instant;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** MyBatis-Plus 配置（乐观锁 + 分页 + 公共字段填充） */
@Configuration
public class MybatisPlusConfig {

  /** 乐观锁与分页插件 */
  @Bean
  public MybatisPlusInterceptor mybatisPlusInterceptor(
      @Value("${workos.database:postgres}") String database) {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
    interceptor.addInnerInterceptor(
        new PaginationInnerInterceptor(
            "sqlite".equals(database) ? DbType.SQLITE : DbType.POSTGRE_SQL));
    return interceptor;
  }

  /** 公共字段自动填充 */
  @Bean
  public MetaObjectHandler metaObjectHandler() {
    return new MetaObjectHandler() {

      @Override
      public void insertFill(MetaObject metaObject) {
        Instant now = Instant.now();
        strictInsertFill(metaObject, "createdAt", Instant.class, now);
        strictInsertFill(metaObject, "updatedAt", Instant.class, now);
      }

      @Override
      public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updatedAt", Instant.class, Instant.now());
      }
    };
  }
}
