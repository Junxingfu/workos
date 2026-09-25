package com.workos.item.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workos.item.data.entity.ItemActivityPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Class ItemActivityMapper
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 系统事件数据访问
 */
@Mapper
public interface ItemActivityMapper extends BaseMapper<ItemActivityPO> {
}
