package com.workos.project.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workos.project.data.entity.ProjectContextPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Class ProjectContextMapper
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 项目现状数据访问
 */
@Mapper
public interface ProjectContextMapper extends BaseMapper<ProjectContextPO> {
}
