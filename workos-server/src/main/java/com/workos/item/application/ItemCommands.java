package com.workos.item.application;

import com.workos.item.domain.FollowUpOutcome;
import com.workos.item.domain.ItemStatus;
import java.time.Instant;
import java.time.LocalDate;

/** 用例输入；独立于 HTTP DTO，桌面、Excel 和其他适配器均可调用。 */
public final class ItemCommands {
  private ItemCommands() {}

  public record Transition(
      long itemId,
      Integer expectedVersion,
      ItemStatus target,
      String waitingFor,
      Instant expectedAt,
      Instant followUpAt,
      String remark) {}

  public record Plan(long itemId, Integer expectedVersion, LocalDate date) {}

  public record FollowUp(
      long itemId,
      Integer expectedVersion,
      FollowUpOutcome outcome,
      ItemStatus target,
      Instant nextAt,
      String note) {}

  public record ReviseTracking(
      long itemId, Integer expectedVersion, String waitingFor, Instant nextAt, boolean clearNext) {}
}
