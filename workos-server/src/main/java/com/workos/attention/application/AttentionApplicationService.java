package com.workos.attention.application;

import com.workos.attention.domain.AttentionPolicy;
import com.workos.attention.domain.AttentionReceipt;
import com.workos.attention.domain.AttentionRepository;
import com.workos.common.support.TimeZoneHolder;
import com.workos.item.domain.ItemStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 关注理由与回执的应用服务；输入事实通过端口获取，领域负责规则。 */
@Service
@RequiredArgsConstructor
public class AttentionApplicationService {
  private final AttentionRepository repository;
  private final TimeZoneHolder clock;

  public record Notice(
      String key,
      String kind,
      String reason,
      long itemId,
      String title,
      ItemStatus status,
      boolean read,
      Instant snoozedUntil) {}

  /** 读取任务事实和回执，依据领域策略派生关注理由，不改变任务。 */
  public List<Notice> list() {
    Map<String, AttentionReceipt> receipts = repository.receipts();
    LocalDate today = clock.today();
    Instant now = clock.now();
    AttentionPolicy policy = new AttentionPolicy();
    List<Notice> result = new ArrayList<>();
    for (AttentionRepository.TaskFact task : repository.activeTasks()) {
      for (AttentionPolicy.Reason reason :
          policy.reasons(
              task.facts(), today, clock.startOfDay(today), clock.endOfDay(today), now)) {
        AttentionReceipt receipt =
            receipts.getOrDefault(reason.key(), new AttentionReceipt(reason.key(), null, null));
        result.add(
            new Notice(
                reason.key(),
                reason.kind(),
                reason.label(),
                task.facts().id(),
                task.title(),
                task.facts().status(),
                receipt.readAt() != null,
                receipt.snoozedUntil()));
      }
    }
    return result;
  }

  /** 仅接受仍有效的关注键；已读或暂缓只保存回执。 */
  @Transactional
  public void respond(String key, String action) {
    if (key == null || list().stream().noneMatch(n -> n.key().equals(key))) {
      throw new IllegalArgumentException("此提醒已经失效，请刷新列表");
    }
    repository.save(new AttentionReceipt(key, null, null).respond(action, clock.now()));
  }
}
