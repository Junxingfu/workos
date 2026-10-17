package com.workos.item.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** 一段等待的值对象；对象、开始时间与对方预计时间一起表达业务含义。 */
public record WaitingPeriod(String person, Instant since, Instant expectedAt) {
  public WaitingPeriod {
    if (person == null || person.isBlank()) {
      throw new IllegalArgumentException("进入等待时请填写等待对象");
    }
    person = person.trim();
    Objects.requireNonNull(since, "等待开始时间不能为空");
  }

  public long elapsedDays(Instant now) {
    return Math.max(0, Duration.between(since, now).toDays());
  }
}
