package com.workos.project.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workos.project.data.entity.ProjectPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Class ProjectMapper
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 项目数据访问
 */
@Mapper
public interface ProjectMapper extends BaseMapper<ProjectPO> {
}
