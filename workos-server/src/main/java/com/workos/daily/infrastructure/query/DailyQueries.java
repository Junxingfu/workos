package com.workos.daily.infrastructure.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.workos.common.support.TimeZoneHolder;
import com.workos.daily.application.DailySources;
import com.workos.daily.application.query.DailyQuery;
import com.workos.daily.application.readmodel.DailyResp;
import com.workos.daily.domain.DailyComposition;
import com.workos.daily.infrastructure.mapping.DailyConverter;
import com.workos.daily.infrastructure.persistence.entity.DailyPO;
import com.workos.daily.infrastructure.persistence.mapper.DailyMapper;
import com.workos.item.domain.ItemStatus;
import com.workos.item.domain.ItemType;
import com.workos.item.infrastructure.persistence.entity.ItemPO;
import com.workos.item.infrastructure.persistence.mapper.ItemMapper;
import com.workos.project.infrastructure.persistence.mapper.ProjectMapper;
import com.workos.worklog.infrastructure.persistence.entity.WorkLogPO;
import com.workos.worklog.infrastructure.persistence.mapper.WorkLogMapper;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 日报来源查询适配器，业务编排与 Markdown 生成均不依赖存储对象。 */
@Component
@RequiredArgsConstructor
public class DailyQueries implements DailySources, DailyQuery {
  private final DailyMapper reports;
  private final DailyConverter converter;
  private final WorkLogMapper logs;
  private final ItemMapper items;
  private final ProjectMapper projects;
  private final TimeZoneHolder clock;

  public DailyComposition.Source on(LocalDate day) {
    HashMap<Long, String> names = new HashMap<Long, String>();
    projects.selectList(null).forEach(p -> names.put(p.getId(), p.getName()));
    List<DailyComposition.Entry> records =
        logs
            .selectList(
                new LambdaQueryWrapper<WorkLogPO>()
                    .eq(WorkLogPO::getWorkDate, day)
                    .orderByAsc(WorkLogPO::getOccurredAt))
            .stream()
            .map(log -> new DailyComposition.Entry(names.get(log.getProjectId()), log.getContent()))
            .toList();
    List<String> completed =
        items
            .selectList(
                new LambdaQueryWrapper<ItemPO>()
                    .eq(ItemPO::getStatus, ItemStatus.DONE)
                    .eq(ItemPO::getItemType, ItemType.TASK)
                    .ge(ItemPO::getCompletedAt, clock.startOfDay(day))
                    .lt(ItemPO::getCompletedAt, clock.endOfDay(day)))
            .stream()
            .map(ItemPO::getTitle)
            .toList();
    return new DailyComposition.Source(records, completed);
  }

  public List<DailyResp> week(LocalDate date) {
    LocalDate start = date == null ? clock.today() : date;
    return reports
        .selectList(
            new LambdaQueryWrapper<DailyPO>()
                .ge(DailyPO::getDailyDate, start)
                .lt(DailyPO::getDailyDate, start.plusDays(7))
                .orderByAsc(DailyPO::getDailyDate))
        .stream()
        .map(converter::toResp)
        .toList();
  }
}
