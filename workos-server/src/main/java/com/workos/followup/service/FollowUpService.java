package com.workos.followup.service;

import com.workos.followup.model.response.FollowUpsResp;

/**
 * @Class FollowUpService
 * @Author WorkOS
 * @Date 2026年09月25日 10:00
 * @Desc 等待与跟进工作台业务接口
 */
public interface FollowUpService {

    /**
     * @Method followUps
     * @Return FollowUpsResp
     * @Desc 聚合等待与跟进
     */
    FollowUpsResp followUps();
}
