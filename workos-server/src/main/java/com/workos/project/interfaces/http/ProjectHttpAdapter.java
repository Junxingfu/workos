package com.workos.project.interfaces.http;

import com.workos.common.model.BasePageResult;
import com.workos.project.application.ProjectApplicationService;
import com.workos.project.application.query.ProjectCriteria;
import com.workos.project.application.query.ProjectQuery;
import com.workos.project.application.readmodel.ProjectContextResp;
import com.workos.project.application.readmodel.ProjectResp;
import com.workos.project.domain.Project;
import com.workos.project.domain.ProjectBrief;
import com.workos.project.interfaces.request.ProjectContextUpdateReq;
import com.workos.project.interfaces.request.ProjectCreateReq;
import com.workos.project.interfaces.request.ProjectUpdateReq;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Project 接口适配器：转换输入并调用用例，读取由查询投影负责。 */
@Component
@RequiredArgsConstructor
public class ProjectHttpAdapter {
  private final ProjectQuery queries;
  private final ProjectApplicationService commands;

  /** 创建项目 */
  public Long create(ProjectCreateReq req) {
    return commands.create(
        req.getName(), req.getCode(), req.getDescription(), req.getColor(), req.getSortOrder());
  }

  /** 项目详情（含现状与统计） */
  public ProjectResp detail(Long id) {
    return queries.detail(id);
  }

  /** 项目分页 */
  public BasePageResult<ProjectResp> page(ProjectCriteria req) {
    return queries.page(req);
  }

  /** 修改项目 */
  public Boolean update(Long id, ProjectUpdateReq req) {
    commands.revise(
        id,
        req.getVersion(),
        new Project.Edit(
            req.getName(),
            req.getDescription(),
            req.getStatus(),
            req.getColor(),
            req.getSortOrder()));
    return true;
  }

  /** 归档（业务状态，不做物理删除） */
  public Boolean archive(Long id) {
    commands.archive(id, true);
    return true;
  }

  /** 恢复归档项目 */
  public Boolean restore(Long id) {
    commands.archive(id, false);
    return true;
  }

  /** 获取项目现状 */
  public ProjectContextResp context(Long id) {
    return queries.context(id);
  }

  /** 更新项目现状 */
  public ProjectContextResp updateContext(Long id, ProjectContextUpdateReq req) {
    commands.reviseBrief(
        id,
        req.getVersion(),
        new ProjectBrief.Revision(
            req.getGoal(),
            req.getKeyPeople(),
            req.getKeyLinks(),
            req.getCurrentFocus(),
            req.getBlocker(),
            req.getNextAction(),
            req.getProgressNote(),
            req.getNotesMarkdown()));
    return context(id);
  }
}
