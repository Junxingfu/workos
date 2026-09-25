package com.workos.search.data.mapper;

import com.workos.search.model.dto.SearchHitDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Class SearchMapper
 * @Author WorkOS
 * @Date 2026年09月25日 10:00
 * @Desc 全局搜索（事项 / 项目 / 日报）
 */
@Mapper
public interface SearchMapper {

    /**
     * @Method search
     * @Param [like, item, project, daily, limit, offset]
     * @Return List<SearchHitDTO>
     * @Desc 三种类型合并检索，按更新时间倒序
     */
    List<SearchHitDTO> search(@Param("like") String like,
                              @Param("item") boolean item,
                              @Param("project") boolean project,
                              @Param("daily") boolean daily,
                              @Param("limit") int limit,
                              @Param("offset") int offset);

    /**
     * @Method count
     * @Param [like, item, project, daily]
     * @Return long
     * @Desc 命中总数
     */
    long count(@Param("like") String like,
               @Param("item") boolean item,
               @Param("project") boolean project,
               @Param("daily") boolean daily);
}
