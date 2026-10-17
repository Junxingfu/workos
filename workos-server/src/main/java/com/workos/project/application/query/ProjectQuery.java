package com.workos.project.application.query;

import com.workos.common.model.BasePageResult;
import com.workos.project.application.readmodel.ProjectContextResp;
import com.workos.project.application.readmodel.ProjectResp;

/** Project 的只读查询端口；只返回展示数据，不暴露 PO，也不修改聚合。 */
public interface ProjectQuery {
  /** 读取项目基本信息、统计与现状。 */
  ProjectResp detail(Long id);

  /** 分页展示项目，不加载任务聚合。 */
  BasePageResult<ProjectResp> page(ProjectCriteria criteria);

  /** 读取项目现状与新鲜度。 */
  ProjectContextResp context(Long id);
}
