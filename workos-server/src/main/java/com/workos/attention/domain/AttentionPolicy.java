package com.workos.attention.domain;

import com.workos.item.domain.ItemStatus;
import com.workos.item.domain.ItemType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** 根据业务事实计算应关注的原因；已读、稍后提醒不改变事实。 */
public final class AttentionPolicy {
  public record Facts(
      long id, ItemType type, ItemStatus status, LocalDate plan, Instant due, Instant followUp) {}

  public record Reason(String key, String kind, String label) {}

  public List<Reason> reasons(
      Facts f, LocalDate today, Instant dayStart, Instant dayEnd, Instant now) {
    if (f.type() != ItemType.TASK
        || !Set.of(ItemStatus.TODO, ItemStatus.DOING, ItemStatus.WAITING).contains(f.status())) {
      return List.of();
    }
    ArrayList<AttentionPolicy.Reason> reasons = new ArrayList<Reason>();
    if (f.due() != null && f.due().isBefore(dayEnd)) {
      reasons.add(
          new Reason(
              "DUE:" + f.id() + ":" + f.due(), "DUE", f.due().isBefore(dayStart) ? "已逾期" : "今天到期"));
    }
    if (f.followUp() != null && !f.followUp().isAfter(now)) {
      reasons.add(new Reason("FOLLOW_UP:" + f.id() + ":" + f.followUp(), "FOLLOW_UP", "该跟进了"));
    }
    if (today.equals(f.plan())) {
      reasons.add(new Reason("PLAN:" + f.id() + ":" + f.plan(), "PLAN", "今日计划"));
    }
    return List.copyOf(reasons);
  }
}
