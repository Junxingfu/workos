package com.workos.exchange.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.workos.exchange.application.ExchangeStore;
import com.workos.exchange.application.TaskSnapshot;
import com.workos.exchange.infrastructure.persistence.entity.SpreadsheetImportPO;
import com.workos.exchange.infrastructure.persistence.mapper.SpreadsheetImportMapper;
import com.workos.item.domain.ItemType;
import com.workos.item.infrastructure.persistence.entity.ItemPO;
import com.workos.item.infrastructure.persistence.mapper.ItemMapper;
import com.workos.item.infrastructure.persistence.mapper.ItemProjectMapper;
import com.workos.project.infrastructure.persistence.mapper.ProjectMapper;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** MyBatis 读投影和导入成功账本；与任务命令参加同一个事务。 */
@Repository
@RequiredArgsConstructor
public class MyBatisExchangeStore implements ExchangeStore {
  private final ItemMapper items;
  private final ProjectMapper projects;
  private final ItemProjectMapper links;
  private final SpreadsheetImportMapper ledger;

  public List<TaskSnapshot> tasks() {
    return items
        .selectList(
            new LambdaQueryWrapper<ItemPO>()
                .eq(ItemPO::getItemType, ItemType.TASK)
                .orderByAsc(ItemPO::getId)
                .last("LIMIT 2001"))
        .stream()
        .map(this::snapshot)
        .toList();
  }

  public TaskSnapshot task(long id) {
    ItemPO po = items.selectById(id);
    return po == null ? null : snapshot(po);
  }

  public List<ProjectRef> projects() {
    return projects.selectList(null).stream()
        .map(p -> new ProjectRef(p.getId(), p.getName(), p.getStatus()))
        .toList();
  }

  public List<Link> links(List<Long> ids) {
    return links.findByItems(ids).stream()
        .map(l -> new Link(l.getItemId(), l.getProjectId()))
        .toList();
  }

  public String completed(String fingerprint) {
    SpreadsheetImportPO row = ledger.selectById(fingerprint);
    return row == null ? null : row.getResultJson();
  }

  public void record(String fingerprint, String result, Instant now) {
    SpreadsheetImportPO row = new SpreadsheetImportPO();
    row.setFingerprint(fingerprint);
    row.setResultJson(result);
    row.setCompletedAt(now.toString());
    ledger.insert(row);
  }

  private TaskSnapshot snapshot(ItemPO p) {
    return new TaskSnapshot(
        p.getId(),
        p.getProjectId(),
        p.getTitle(),
        p.getDescription(),
        p.getItemType(),
        p.getStatus(),
        p.getPriority(),
        p.getPlannedDate(),
        p.getPlanRank(),
        p.getDueAt(),
        p.getNextFollowUpAt(),
        p.getWaitingFor(),
        p.getWaitingSince(),
        p.getWaitingExpectedAt(),
        p.getRelatedPerson(),
        p.getCompletedAt(),
        p.getCancelledAt(),
        p.getSource(),
        p.getCreatedAt(),
        p.getUpdatedAt(),
        p.getVersion());
  }
}
