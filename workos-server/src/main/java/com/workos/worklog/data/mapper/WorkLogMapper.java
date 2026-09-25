package com.workos.worklog.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workos.worklog.data.entity.WorkLogPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Class WorkLogMapper
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 工作记录数据访问
 */
@Mapper
public interface WorkLogMapper extends BaseMapper<WorkLogPO> {
}
