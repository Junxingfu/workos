package com.workos.daily.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.workos.daily.domain.DailyReport;
import com.workos.daily.domain.DailyReportRepository;
import com.workos.daily.infrastructure.persistence.entity.DailyPO;
import com.workos.daily.infrastructure.persistence.mapper.DailyMapper;
import com.workos.shared.domain.ConcurrentChange;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** DailyReport 的 MyBatis-Plus 适配器；显式写 null，防止清空被 ORM 的非空策略忽略。 */
@Repository
@RequiredArgsConstructor
public class MyBatisDailyReportRepository implements DailyReportRepository {
  private final DailyMapper mapper;

  @Override
  public DailyReport find(LocalDate key) {
    DailyPO entity =
        mapper.selectOne(new LambdaQueryWrapper<DailyPO>().eq(DailyPO::getDailyDate, key));
    return entity == null
        ? null
        : new DailyReport(
            new DailyReport.State(
                entity.getId(),
                entity.getDailyDate(),
                entity.getContent(),
                entity.getGeneratedContent(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()));
  }

  @Override
  public void save(DailyReport aggregate) {
    DailyReport.State state = aggregate.snapshot();
    DailyPO entity = new DailyPO();
    entity.setId(state.id());
    entity.setDailyDate(state.dailyDate());
    entity.setContent(state.content());
    entity.setGeneratedContent(state.generatedContent());
    entity.setStatus(state.status());
    entity.setCreatedAt(state.createdAt());
    entity.setUpdatedAt(state.updatedAt());
    entity.setVersion(state.version());
    if (state.id() == null) {
      mapper.insert(entity);
      aggregate.persisted(entity.getId(), entity.getVersion());
      return;
    }
    LambdaUpdateWrapper<DailyPO> update =
        new LambdaUpdateWrapper<DailyPO>()
            .eq(DailyPO::getId, state.id())
            .eq(DailyPO::getVersion, state.version())
            .set(DailyPO::getVersion, state.version() + 1)
            .set(DailyPO::getDailyDate, state.dailyDate())
            .set(DailyPO::getContent, state.content())
            .set(DailyPO::getGeneratedContent, state.generatedContent())
            .set(DailyPO::getStatus, state.status())
            .set(DailyPO::getCreatedAt, state.createdAt())
            .set(DailyPO::getUpdatedAt, state.updatedAt());
    if (mapper.update(null, update) != 1) {
      throw new ConcurrentChange("内容已改变，请重新载入");
    }
    aggregate.persisted(state.id(), state.version() + 1);
  }
}
