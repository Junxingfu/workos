package com.workos.item.domain;

/** 跟进结果：三选一 */
public enum FollowUpOutcome {

  /** 我催了，对方还没回：更新下次跟进时间，状态保持 WAITING */
  STILL_WAITING,

  /** 对方给了：回到执行或结束 */
  RESOLVED,

  /** 暂时不跟：清空 next_follow_up_at，但不改状态 */
  ABANDONED
}
