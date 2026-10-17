package com.workos.item.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workos.item.infrastructure.persistence.entity.ItemActivityPO;
import org.apache.ibatis.annotations.Mapper;

/** 系统事件数据访问 */
@Mapper
public interface ItemActivityMapper extends BaseMapper<ItemActivityPO> {}
