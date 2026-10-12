package com.workos.item.data.mapper;

import com.workos.item.model.dto.ItemProjectDTO;
import org.apache.ibatis.annotations.*;
import java.util.Collection;
import java.util.List;

/** 同一事项与多个项目的关联，不复制事项或状态。 */
@Mapper
public interface ItemProjectMapper {
    @Select("""
        <script>
        SELECT l.item_id, l.project_id, p.name AS project_name
        FROM item_project_link l JOIN project p ON p.id = l.project_id
        WHERE l.item_id IN
        <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
        ORDER BY l.project_id
        </script>
        """)
    List<ItemProjectDTO> findByItems(@Param("ids") Collection<Long> ids);

    @Delete("DELETE FROM item_project_link WHERE item_id = #{id}")
    void removeForItem(@Param("id") Long id);

    @Insert("""
        <script>
        INSERT INTO item_project_link(item_id, project_id) VALUES
        <foreach collection="projectIds" item="projectId" separator=",">(#{itemId}, #{projectId})</foreach>
        </script>
        """)
    void addForItem(@Param("itemId") Long itemId, @Param("projectIds") Collection<Long> projectIds);
}
