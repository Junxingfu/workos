package com.workos.common.support;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** "今天"的唯一真源；由 settings 驱动，未初始化时取运行环境时区 */
@Slf4j
@Component
public class TimeZoneHolder {

  /** 当前生效时区 */
  private volatile ZoneId zoneId = ZoneId.systemDefault();

  /** 日界线小时；MVP 固定自然日（0），不开放给界面 */
  private volatile int dayBoundaryHour = 0;

  /** 设置变更后刷新 */
  public void refresh(String zone, Integer boundaryHour) {
    if (zone != null && !zone.isBlank()) {
      try {
        this.zoneId = ZoneId.of(zone);
      } catch (Exception e) {
        log.warn("非法时区配置 zone={}，保持原值 {}", zone, this.zoneId);
      }
    }
    if (boundaryHour != null && boundaryHour >= 0 && boundaryHour <= 23) {
      this.dayBoundaryHour = boundaryHour;
    }
  }

  /** 当前时区 */
  public ZoneId zone() {
    return zoneId;
  }

  /** 今天（业务日） */
  public LocalDate today() {
    return toDate(Instant.now());
  }

  /** 时间点归属的业务日 */
  public LocalDate toDate(Instant instant) {
    if (instant == null) {
      return null;
    }
    return instant.atZone(zoneId).minusHours(dayBoundaryHour).toLocalDate();
  }

  /** 业务日 00:00 对应的绝对时间点 */
  public Instant startOfDay(LocalDate date) {
    return date.atStartOfDay(zoneId).plusHours(dayBoundaryHour).toInstant();
  }

  /** 业务日次日 00:00 对应的绝对时间点 */
  public Instant endOfDay(LocalDate date) {
    return startOfDay(date.plusDays(1));
  }

  /** 当前时间点 */
  public Instant now() {
    return Instant.now();
  }
}
