package com.workos.item.domain;

/** 事项状态 */
public enum ItemStatus {

  /** 未整理 */
  INBOX,

  /** 待办 */
  TODO,

  /** 进行中 */
  DOING,

  /** 等待别人 */
  WAITING,

  /** 已完成 */
  DONE,

  /** 已取消 */
  CANCELLED
}
