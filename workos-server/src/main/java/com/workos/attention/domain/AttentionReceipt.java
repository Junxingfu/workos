package com.workos.attention.domain;

import java.time.Instant;

/** 一个关注理由的回执聚合；读过、稍后、恢复未读均不修改事项。 */
public record AttentionReceipt(String key, Instant readAt, Instant snoozedUntil) {
  public AttentionReceipt {
    if (key == null || key.isBlank()) {
      throw new IllegalArgumentException("提醒标识不能为空");
    }
  }

  public AttentionReceipt respond(String action, Instant now) {
    if (action == null) {
      throw new IllegalArgumentException("提醒操作不能为空");
    }
    return switch (action) {
      case "READ" -> new AttentionReceipt(key, now, null);
      case "UNREAD" -> new AttentionReceipt(key, null, null);
      case "SNOOZE" -> new AttentionReceipt(key, null, now.plusSeconds(3600));
      default -> throw new IllegalArgumentException("未知提醒操作");
    };
  }
}
