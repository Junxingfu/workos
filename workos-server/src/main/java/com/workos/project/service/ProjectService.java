package com.workos.project.service;

import com.workos.common.model.BasePageResult;
import com.workos.project.model.request.ProjectContextUpdateReq;
import com.workos.project.model.request.ProjectCreateReq;
import com.workos.project.model.request.ProjectPageReq;
import com.workos.project.model.request.ProjectUpdateReq;
import com.workos.project.model.response.ProjectContextResp;
import com.workos.project.model.response.ProjectResp;

/**
 * @Class ProjectService
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 项目业务接口
 */
public interface ProjectService {

    /**
     * @Method create
     * @Param [req]
     * @Return Long
     * @Desc 创建项目
     */
    Long create(ProjectCreateReq req);

    /**
     * @Method detail
     * @Param [id]
     * @Return ProjectResp
     * @Desc 项目详情（含现状与统计）
     */
    ProjectResp detail(Long id);

    /**
     * @Method page
     * @Param [req]
     * @Return BasePageResult<ProjectResp>
     * @Desc 项目分页
     */
    BasePageResult<ProjectResp> page(ProjectPageReq req);

    /**
     * @Method update
     * @Param [id, req]
     * @Return Boolean
     * @Desc 修改项目
     */
    Boolean update(Long id, ProjectUpdateReq req);

    /**
     * @Method archive
     * @Param [id]
     * @Return Boolean
     * @Desc 归档（业务状态，不做物理删除）
     */
    Boolean archive(Long id);

    /**
     * @Method restore
     * @Param [id]
     * @Return Boolean
     * @Desc 恢复归档项目
     */
    Boolean restore(Long id);

    /**
     * @Method context
     * @Param [id]
     * @Return ProjectContextResp
     * @Desc 获取项目现状
     */
    ProjectContextResp context(Long id);

    /**
     * @Method updateContext
     * @Param [id, req]
     * @Return ProjectContextResp
     * @Desc 更新项目现状
     */
    ProjectContextResp updateContext(Long id, ProjectContextUpdateReq req);
}
