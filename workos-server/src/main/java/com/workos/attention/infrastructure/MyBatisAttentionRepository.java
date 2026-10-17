package com.workos.attention.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.workos.attention.domain.AttentionPolicy;
import com.workos.attention.domain.AttentionReceipt;
import com.workos.attention.domain.AttentionRepository;
import com.workos.attention.infrastructure.persistence.entity.AttentionReceiptPO;
import com.workos.attention.infrastructure.persistence.mapper.AttentionReceiptMapper;
import com.workos.item.domain.ItemStatus;
import com.workos.item.domain.ItemType;
import com.workos.item.infrastructure.persistence.entity.ItemPO;
import com.workos.item.infrastructure.persistence.mapper.ItemMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** 将执行模型转换为关注事实；回执采用单条原子 upsert。 */
@Repository
@RequiredArgsConstructor
public class MyBatisAttentionRepository implements AttentionRepository {
  private final ItemMapper items;
  private final AttentionReceiptMapper receipts;

  public List<TaskFact> activeTasks() {
    return items
        .selectList(
            new LambdaQueryWrapper<ItemPO>()
                .eq(ItemPO::getItemType, ItemType.TASK)
                .in(ItemPO::getStatus, ItemStatus.TODO, ItemStatus.DOING, ItemStatus.WAITING)
                .orderByAsc(ItemPO::getPriority)
                .orderByAsc(ItemPO::getId))
        .stream()
        .map(
            p ->
                new TaskFact(
                    new AttentionPolicy.Facts(
                        p.getId(),
                        p.getItemType(),
                        p.getStatus(),
                        p.getPlannedDate(),
                        p.getDueAt(),
                        p.getNextFollowUpAt()),
                    p.getTitle()))
        .toList();
  }

  public Map<String, AttentionReceipt> receipts() {
    Map<String, AttentionReceipt> result = new HashMap<>();
    for (AttentionReceiptPO po : receipts.selectList(null)) {
      result.put(
          po.getNotificationKey(),
          new AttentionReceipt(
              po.getNotificationKey(),
              po.getReadAt() == null ? null : Instant.parse(po.getReadAt()),
              po.getSnoozedUntil() == null ? null : Instant.parse(po.getSnoozedUntil())));
    }
    return result;
  }

  public void save(AttentionReceipt receipt) {
    AttentionReceiptPO po = new AttentionReceiptPO();
    po.setNotificationKey(receipt.key());
    po.setReadAt(receipt.readAt() == null ? null : receipt.readAt().toString());
    po.setSnoozedUntil(receipt.snoozedUntil() == null ? null : receipt.snoozedUntil().toString());
    receipts.saveReceipt(po);
  }
}
