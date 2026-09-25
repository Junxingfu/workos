package com.workos.worklog.service;

import com.workos.worklog.model.request.WorkLogCreateReq;
import com.workos.worklog.model.response.WorkLogResp;

import java.time.LocalDate;
import java.util.List;

/**
 * @Class WorkLogService
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 工作记录业务接口
 */
public interface WorkLogService {

    /**
     * @Method create
     * @Param [req]
     * @Return Long
     * @Desc 创建工作记录
     */
    Long create(WorkLogCreateReq req);

    /**
     * @Method list
     * @Param [itemId, projectId, workDate]
     * @Return List<WorkLogResp>
     * @Desc 查询工作记录
     */
    List<WorkLogResp> list(Long itemId, Long projectId, LocalDate workDate);
}
