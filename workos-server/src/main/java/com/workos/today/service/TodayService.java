package com.workos.today.service;

import com.workos.today.model.response.TodayResp;

/**
 * @Class TodayService
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc Today 聚合业务接口
 */
public interface TodayService {

    /**
     * @Method today
     * @Return TodayResp
     * @Desc 聚合六个互斥分区
     */
    TodayResp today();
}
