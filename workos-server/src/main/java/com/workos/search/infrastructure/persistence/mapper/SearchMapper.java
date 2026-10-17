package com.workos.search.infrastructure.persistence.mapper;

import com.workos.search.application.readmodel.SearchHitDTO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 全局搜索（事项 / 项目 / 日报） */
@Mapper
public interface SearchMapper {

  /** 三种类型合并检索，按更新时间倒序 */
  List<SearchHitDTO> search(
      @Param("like") String like,
      @Param("item") boolean item,
      @Param("project") boolean project,
      @Param("daily") boolean daily,
      @Param("limit") int limit,
      @Param("offset") int offset);

  /** 命中总数 */
  long count(
      @Param("like") String like,
      @Param("item") boolean item,
      @Param("project") boolean project,
      @Param("daily") boolean daily);
}
