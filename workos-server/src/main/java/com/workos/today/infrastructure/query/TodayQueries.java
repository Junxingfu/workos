package com.workos.today.infrastructure.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.workos.common.support.TimeZoneHolder;
import com.workos.item.domain.ItemStatus;
import com.workos.item.infrastructure.persistence.entity.ItemPO;
import com.workos.item.infrastructure.persistence.mapper.ItemMapper;
import com.workos.item.infrastructure.query.ItemQueries;
import com.workos.today.application.query.TodayQuery;
import com.workos.today.application.readmodel.TodayResp;
import com.workos.today.application.readmodel.TodaySectionsResp;
import com.workos.today.application.readmodel.TodaySummaryResp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Today 聚合实现：①→⑥ 顺序判断，命中即停止，保证分区互斥 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TodayQueries implements TodayQuery {

  private final ItemMapper itemMapper;

  private final ItemQueries itemService;

  private final TimeZoneHolder timeZoneHolder;

  /** 聚合六个互斥分区 */
  @Override
  public TodayResp today() {
    LocalDate today = timeZoneHolder.today();
    Instant todayStart = timeZoneHolder.startOfDay(today);
    Instant tomorrowStart = timeZoneHolder.endOfDay(today);
    Instant now = timeZoneHolder.now();

    List<ItemPO> plan = itemMapper.selectPlan(today);
    Set<Long> used = idsOf(plan);
    List<ItemPO> overdue = distinct(itemMapper.selectOverdue(todayStart), used);
    List<ItemPO> dueToday = distinct(itemMapper.selectDueToday(todayStart, tomorrowStart), used);
    List<ItemPO> followUp = distinct(itemMapper.selectFollowUpDue(now), used);
    List<ItemPO> waiting = distinct(itemMapper.selectWaiting(now), used);
    List<ItemPO> carriedOver = distinct(itemMapper.selectCarriedOverPlan(today), used);
    List<ItemPO> suggested =
        distinct(itemMapper.selectPendingDecision(new ArrayList<>(used)), used);

    TodaySectionsResp sections = new TodaySectionsResp();
    sections.setPlan(itemService.assembles(plan));
    sections.setOverdue(itemService.assembles(overdue));
    sections.setToday(itemService.assembles(dueToday));
    sections.setFollowUp(itemService.assembles(followUp));
    sections.setWaiting(itemService.assembles(waiting));
    sections.setSuggested(itemService.assembles(suggested));
    sections.setCarriedOverPlan(itemService.assembles(carriedOver));

    TodaySummaryResp summary = new TodaySummaryResp();
    summary.setPlan(sections.getPlan().size());
    summary.setOverdue(sections.getOverdue().size());
    summary.setToday(sections.getToday().size());
    summary.setFollowUp(sections.getFollowUp().size());
    summary.setWaiting(sections.getWaiting().size());
    summary.setSuggested(sections.getSuggested().size());
    summary.setInbox((int) itemMapper.countInbox());
    summary.setUnclassified((int) itemMapper.countUnclassified());
    summary.setCarriedOverPlan(carriedOverPlan(today));

    TodayResp resp = new TodayResp();
    resp.setDate(today);
    resp.setSummary(summary);
    resp.setSections(sections);
    return resp;
  }

  private Set<Long> idsOf(List<ItemPO> poList) {
    Set<Long> ids = new LinkedHashSet<>();
    poList.forEach(po -> ids.add(po.getId()));
    return ids;
  }

  private List<ItemPO> distinct(List<ItemPO> poList, Set<Long> used) {
    List<ItemPO> result = new ArrayList<>();
    poList.forEach(
        po -> {
          if (used.add(po.getId())) {
            result.add(po);
          }
        });
    return result;
  }

  private int carriedOverPlan(LocalDate today) {
    Long count =
        itemMapper.selectCount(
            new LambdaQueryWrapper<ItemPO>()
                .lt(ItemPO::getPlannedDate, today)
                .notIn(ItemPO::getStatus, ItemStatus.DONE, ItemStatus.CANCELLED));
    return count.intValue();
  }
}
