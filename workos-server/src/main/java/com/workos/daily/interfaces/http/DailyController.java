package com.workos.daily.interfaces.http;

import com.workos.common.model.BaseResponse;
import com.workos.daily.application.readmodel.DailyGenerateResp;
import com.workos.daily.application.readmodel.DailyResp;
import com.workos.daily.interfaces.request.DailyUpdateReq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 日报接口 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dailies")
@Tag(name = "日报", description = "某一天的工作记录与复盘")
public class DailyController {

  private final DailyHttpAdapter dailyService;

  /** 本周只读聚合 */
  @GetMapping
  @Operation(summary = "本周日报聚合")
  public BaseResponse<List<DailyResp>> week(
      @RequestParam(value = "weekStart", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate weekStart) {
    return BaseResponse.success(dailyService.week(weekStart));
  }

  /** 获取日报（首次访问自动创建） */
  @GetMapping("/{date}")
  @Operation(summary = "获取日报")
  public BaseResponse<DailyResp> get(
      @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return BaseResponse.success(dailyService.get(date));
  }

  /** 更新日报 */
  @PutMapping("/{date}")
  @Operation(summary = "更新日报")
  public BaseResponse<DailyResp> update(
      @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @Valid @RequestBody DailyUpdateReq req) {
    return BaseResponse.success(dailyService.update(date, req));
  }

  /** 生成日报快照 */
  @PostMapping("/{date}/generate")
  @Operation(summary = "生成日报快照")
  public BaseResponse<DailyGenerateResp> generate(
      @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return BaseResponse.success(dailyService.generate(date));
  }

  /** 用户确认后应用生成内容 */
  @PostMapping("/{date}/apply-generated")
  @Operation(summary = "应用生成内容")
  public BaseResponse<DailyResp> applyGenerated(
      @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @Valid @RequestBody DailyUpdateReq req) {
    return BaseResponse.success(dailyService.applyGenerated(date, req));
  }
}
