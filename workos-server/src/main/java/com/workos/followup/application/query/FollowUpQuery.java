package com.workos.followup.application.query;

import com.workos.followup.application.readmodel.FollowUpsResp;

/** 等待与跟进工作台业务接口 */
public interface FollowUpQuery {

  /** 聚合等待与跟进 */
  FollowUpsResp followUps();
}
