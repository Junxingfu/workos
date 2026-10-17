package com.workos.project.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workos.project.infrastructure.persistence.entity.ProjectContextPO;
import org.apache.ibatis.annotations.Mapper;

/** 项目现状数据访问 */
@Mapper
public interface ProjectContextMapper extends BaseMapper<ProjectContextPO> {}
