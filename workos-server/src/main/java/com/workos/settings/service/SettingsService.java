package com.workos.settings.service;

import com.workos.settings.model.request.SettingsUpdateReq;
import com.workos.settings.model.response.SettingsResp;

/**
 * @Class SettingsService
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 设置业务接口
 */
public interface SettingsService {

    /**
     * @Method get
     * @Return SettingsResp
     * @Desc 获取设置
     */
    SettingsResp get();

    /**
     * @Method update
     * @Param [req]
     * @Return SettingsResp
     * @Desc 更新设置并刷新时区真源
     */
    SettingsResp update(SettingsUpdateReq req);
}
