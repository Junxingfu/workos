package com.workos.project.infrastructure.mapping;

import com.workos.project.application.readmodel.ProjectContextResp;
import com.workos.project.application.readmodel.ProjectResp;
import com.workos.project.infrastructure.persistence.entity.ProjectContextPO;
import com.workos.project.infrastructure.persistence.entity.ProjectPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** 查询行到展示视图的机械映射；统计和现状新鲜度由查询实现补齐。 */
@Mapper(componentModel = "spring")
public interface ProjectConverter {
  /** 项目基本信息；统计采用批量查询，避免逐项目加载任务聚合。 */
  @Mapping(target = "doingCount", ignore = true)
  @Mapping(target = "waitingCount", ignore = true)
  @Mapping(target = "openCount", ignore = true)
  @Mapping(target = "todayCount", ignore = true)
  @Mapping(target = "currentFocus", ignore = true)
  @Mapping(target = "lastActivityAt", ignore = true)
  @Mapping(target = "statistics", ignore = true)
  @Mapping(target = "context", ignore = true)
  ProjectResp toResp(ProjectPO po);

  /** 数据库 focus 列对应界面的当前重点，不承载现状修订规则。 */
  @Mapping(target = "currentFocus", source = "focus")
  @Mapping(target = "staleDays", ignore = true)
  ProjectContextResp toResp(ProjectContextPO po);
}
