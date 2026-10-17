package com.workos.today.application.query;

import com.workos.today.application.readmodel.TodayResp;

/** Today 工作台查询端口；组合视图不是领域聚合 */
public interface TodayQuery {

  /** 汇总六个互斥分区 */
  TodayResp today();
}
