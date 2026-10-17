package com.workos.daily.application;

import com.workos.common.enums.ResponseCode;
import com.workos.common.exception.BizException;
import com.workos.common.support.TimeZoneHolder;
import com.workos.daily.domain.DailyComposition;
import com.workos.daily.domain.DailyReport;
import com.workos.daily.domain.DailyReportRepository;
import java.time.LocalDate;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 日报用例：生成快照与确认正文是不同命令。 */
@Service
@RequiredArgsConstructor
@Transactional
public class DailyApplicationService {
  private final DailyReportRepository repository;
  private final TimeZoneHolder clock;
  private final DailySources sources;

  public record Generated(DailyReport.State report, int sourceCount) {}

  /** 读取当日事实，由领域策略组合正文，再保存为待审阅快照。 */
  public Generated generate(LocalDate date) {
    DailyComposition.Source source = sources.on(date);
    return new Generated(
        generate(date, DailyComposition.compose(date, source)), source.records().size());
  }

  /** 获取当天日报；没有记录时创建草稿，因此这是用例而不是纯查询。 */
  public DailyReport.State get(LocalDate date) {
    return load(date).snapshot();
  }

  /** 核对期望版本，区分普通编辑和确认应用，不自动覆盖生成快照。 */
  public DailyReport.State write(LocalDate date, String text, Integer expected, boolean confirmed) {
    DailyReport report = load(date);
    if (expected != null && !Objects.equals(expected, report.snapshot().version())) {
      throw new BizException(ResponseCode.DAILY_VERSION_CONFLICT);
    }
    if (confirmed) {
      report.apply(text, clock.now());
    } else {
      report.write(text, clock.now());
    }
    repository.save(report);
    return report.snapshot();
  }

  /** 将已组合的内容存入生成快照；不覆盖用户正文。 */
  public DailyReport.State generate(LocalDate date, String content) {
    DailyReport report = load(date);
    report.generate(content, clock.now());
    repository.save(report);
    return report.snapshot();
  }

  /** 按日期获取聚合；缺失时创建并保存草稿。 */
  private DailyReport load(LocalDate day) {
    DailyReport report = repository.find(day);
    if (report == null) {
      report = DailyReport.draft(day, clock.now());
      repository.save(report);
    }
    return report;
  }
}
