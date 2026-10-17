package com.workos.attention.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workos.attention.infrastructure.persistence.entity.AttentionReceiptPO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

/** 关注回执的 MyBatis 映射；按稳定理由键原子 upsert，适配 PostgreSQL 与 SQLite。 */
@Mapper
public interface AttentionReceiptMapper extends BaseMapper<AttentionReceiptPO> {
  /** PostgreSQL 与 SQLite 共同支持的原子 upsert，避免双窗口首次确认的竞争。 */
  @Insert(
      """
      INSERT INTO attention_receipt(notification_key, read_at, snoozed_until)
      VALUES (#{notificationKey}, #{readAt}, #{snoozedUntil})
      ON CONFLICT(notification_key) DO UPDATE
      SET read_at = excluded.read_at, snoozed_until = excluded.snoozed_until
      """)
  int saveReceipt(AttentionReceiptPO receipt);
}
