package com.workos.worklog.infrastructure.mapping;

import com.workos.worklog.application.readmodel.WorkLogResp;
import com.workos.worklog.infrastructure.persistence.entity.WorkLogPO;
import org.mapstruct.Mapper;

/** 只读存储行到展示视图的映射；业务写入经过聚合仓储。 */
@Mapper(componentModel = "spring")
public interface WorkLogConverter {
  /** 保留查询字段语义，不在转换时修改业务状态。 */
  WorkLogResp toResp(WorkLogPO po);
}
