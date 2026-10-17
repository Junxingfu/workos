package com.workos.worklog.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workos.worklog.infrastructure.persistence.entity.WorkLogPO;
import org.apache.ibatis.annotations.Mapper;

/** 工作记录数据访问 */
@Mapper
public interface WorkLogMapper extends BaseMapper<WorkLogPO> {}
