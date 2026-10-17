package com.workos.project.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workos.project.infrastructure.persistence.entity.ProjectPO;
import org.apache.ibatis.annotations.Mapper;

/** 项目数据访问 */
@Mapper
public interface ProjectMapper extends BaseMapper<ProjectPO> {}
