package com.workos.attention.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** attention_receipt 表的回执映射；记录对关注理由的响应，不保存事项状态。 */
@Data
@TableName("attention_receipt")
public class AttentionReceiptPO {
  /** 关注理由的稳定键：理由类型、事项 ID 与事实日期/时间组合；作为主键。 */
  @TableId(type = IdType.INPUT)
  private String notificationKey;

  /** 已读时间的 ISO Instant 文本；为空表示未读。 */
  private String readAt;

  /** 暂缓至何时的 ISO Instant 文本；为空表示未暂缓，不修改事项计划。 */
  private String snoozedUntil;
}
