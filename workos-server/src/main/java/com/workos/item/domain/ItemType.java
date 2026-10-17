package com.workos.item.domain;

/** 事项类型：只有两种；Follow-up 是能力（next_follow_up_at 非空），不是类型 */
public enum ItemType {

  /** 我要做的事 */
  TASK,

  /** 只记录，不进入执行流 */
  NOTE
}
