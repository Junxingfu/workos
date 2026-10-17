package com.workos.item.domain;

import java.util.Map;
import java.util.Set;

/** 任务与笔记的合法流转策略，由聚合行为和查询投影共同使用。 */
public class TransitionPolicy {

  /**
   * 合法转换表： INBOX -> TODO / CANCELLED TODO / DOING / WAITING -> 见下 DONE / CANCELLED -> TODO（撤销与恢复）
   */
  private static final Map<ItemStatus, Set<ItemStatus>> TRANSITIONS =
      Map.of(
          ItemStatus.INBOX, Set.of(ItemStatus.TODO, ItemStatus.CANCELLED),
          ItemStatus.TODO,
              Set.of(ItemStatus.DOING, ItemStatus.WAITING, ItemStatus.DONE, ItemStatus.CANCELLED),
          ItemStatus.DOING,
              Set.of(ItemStatus.TODO, ItemStatus.WAITING, ItemStatus.DONE, ItemStatus.CANCELLED),
          ItemStatus.WAITING,
              Set.of(ItemStatus.TODO, ItemStatus.DOING, ItemStatus.DONE, ItemStatus.CANCELLED),
          ItemStatus.DONE, Set.of(ItemStatus.TODO),
          ItemStatus.CANCELLED, Set.of(ItemStatus.TODO));

  public boolean canTransition(ItemStatus from, ItemStatus to) {
    if (from == null || to == null) {
      return false;
    }
    return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
  }

  public Set<ItemStatus> allowedTargets(ItemStatus from) {
    return TRANSITIONS.getOrDefault(from, Set.of());
  }

  /** 笔记的收下表示保存，恢复后重新进入收件箱。 */
  public Set<ItemStatus> allowedTargets(ItemStatus from, ItemType type) {
    if (type != ItemType.NOTE) {
      return allowedTargets(from);
    }
    if (from == ItemStatus.INBOX) {
      return Set.of(ItemStatus.DONE, ItemStatus.CANCELLED);
    }
    if (from == ItemStatus.DONE || from == ItemStatus.CANCELLED) {
      return Set.of(ItemStatus.INBOX);
    }
    return Set.of();
  }
}
