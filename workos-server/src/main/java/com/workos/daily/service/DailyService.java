package com.workos.daily.service;

import com.workos.daily.model.request.DailyUpdateReq;
import com.workos.daily.model.response.DailyGenerateResp;
import com.workos.daily.model.response.DailyResp;

import java.time.LocalDate;
import java.util.List;

/**
 * @Class DailyService
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 日报业务接口
 */
public interface DailyService {

    /**
     * @Method get
     * @Param [date]
     * @Return DailyResp
     * @Desc 获取日报，首次访问自动创建
     */
    DailyResp get(LocalDate date);

    /**
     * @Method update
     * @Param [date, req]
     * @Return DailyResp
     * @Desc 更新日报内容
     */
    DailyResp update(LocalDate date, DailyUpdateReq req);

    /**
     * @Method generate
     * @Param [date]
     * @Return DailyGenerateResp
     * @Desc 生成快照（只写 generatedContent）
     */
    DailyGenerateResp generate(LocalDate date);

    /**
     * @Method applyGenerated
     * @Param [date, req]
     * @Return DailyResp
     * @Desc 用户确认后覆盖 content（乐观锁）
     */
    DailyResp applyGenerated(LocalDate date, DailyUpdateReq req);

    /**
     * @Method week
     * @Param [weekStart]
     * @Return List<DailyResp>
     * @Desc 本周只读聚合
     */
    List<DailyResp> week(LocalDate weekStart);
}
