package com.workos.exchange.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workos.exchange.infrastructure.persistence.entity.SpreadsheetImportPO;
import org.apache.ibatis.annotations.Mapper;

/** 导入幂等账本的 MyBatis 映射；仅存储成功提交的批次结果。 */
@Mapper
public interface SpreadsheetImportMapper extends BaseMapper<SpreadsheetImportPO> {}
