package com.workos.daily.interfaces.http;

import com.workos.daily.application.DailyApplicationService;
import com.workos.daily.application.query.DailyQuery;
import com.workos.daily.application.readmodel.DailyGenerateResp;
import com.workos.daily.application.readmodel.DailyResp;
import com.workos.daily.domain.DailyReport;
import com.workos.daily.interfaces.request.DailyUpdateReq;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** HTTP 契约适配：将请求交给日报用例，返回稳定的旧接口格式。 */
@Component
@RequiredArgsConstructor
public class DailyHttpAdapter {
  private final DailyApplicationService commands;
  private final DailyQuery queries;

  /** 获取日报，首次访问自动创建 */
  public DailyResp get(LocalDate date) {
    return response(commands.get(date));
  }

  /** 更新日报内容 */
  public DailyResp update(LocalDate date, DailyUpdateReq req) {
    return response(commands.write(date, req.getContent(), req.getVersion(), false));
  }

  /** 用户确认后覆盖 content（乐观锁） */
  public DailyResp applyGenerated(LocalDate date, DailyUpdateReq req) {
    return response(commands.write(date, req.getContent(), req.getVersion(), true));
  }

  /** 生成快照（只写 generatedContent） */
  public DailyGenerateResp generate(LocalDate date) {
    DailyApplicationService.Generated generated = commands.generate(date);
    DailyGenerateResp result = new DailyGenerateResp();
    result.setDate(date);
    result.setGeneratedContent(generated.report().generatedContent());
    result.setSourceWorkLogCount(generated.sourceCount());
    result.setVersion(generated.report().version());
    return result;
  }

  /** 从指定日期开始的七天只读汇总 */
  public List<DailyResp> week(LocalDate date) {
    return queries.week(date);
  }

  private DailyResp response(DailyReport.State state) {
    DailyResp result = new DailyResp();
    result.setId(state.id());
    result.setDate(state.dailyDate());
    result.setContent(state.content());
    result.setGeneratedContent(state.generatedContent());
    result.setGenerated(state.generatedContent() != null);
    result.setCreatedAt(state.createdAt());
    result.setUpdatedAt(state.updatedAt());
    result.setVersion(state.version());
    return result;
  }
}
