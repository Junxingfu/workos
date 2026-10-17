package com.workos.daily.application;

import com.workos.daily.domain.DailyComposition;
import java.time.LocalDate;

/** 日报所需的事实投影；读取不加载其他聚合，也不修改原始事实。 */
public interface DailySources {
  /** 按业务日读取人工记录与完成事项，返回组合日报所需的事实。 */
  DailyComposition.Source on(LocalDate day);
}
