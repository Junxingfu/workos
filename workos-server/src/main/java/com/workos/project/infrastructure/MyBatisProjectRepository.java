package com.workos.project.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.workos.project.domain.Project;
import com.workos.project.domain.ProjectRepository;
import com.workos.project.infrastructure.persistence.entity.ProjectPO;
import com.workos.project.infrastructure.persistence.mapper.ProjectMapper;
import com.workos.shared.domain.ConcurrentChange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Project 的 MyBatis-Plus 适配器；显式写 null，防止清空被 ORM 的非空策略忽略。 */
@Repository
@RequiredArgsConstructor
public class MyBatisProjectRepository implements ProjectRepository {
  private final ProjectMapper mapper;

  @Override
  public Project find(Long key) {
    ProjectPO entity = mapper.selectById(key);
    return entity == null
        ? null
        : new Project(
            new Project.State(
                entity.getId(),
                entity.getName(),
                entity.getCode(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getColor(),
                entity.getSortOrder(),
                entity.getArchivedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()));
  }

  @Override
  public void save(Project aggregate) {
    Project.State state = aggregate.snapshot();
    ProjectPO entity = new ProjectPO();
    entity.setId(state.id());
    entity.setName(state.name());
    entity.setCode(state.code());
    entity.setDescription(state.description());
    entity.setStatus(state.status());
    entity.setColor(state.color());
    entity.setSortOrder(state.sortOrder());
    entity.setArchivedAt(state.archivedAt());
    entity.setCreatedAt(state.createdAt());
    entity.setUpdatedAt(state.updatedAt());
    entity.setVersion(state.version());
    if (state.id() == null) {
      mapper.insert(entity);
      aggregate.persisted(entity.getId(), entity.getVersion());
      return;
    }
    LambdaUpdateWrapper<ProjectPO> update =
        new LambdaUpdateWrapper<ProjectPO>()
            .eq(ProjectPO::getId, state.id())
            .eq(ProjectPO::getVersion, state.version())
            .set(ProjectPO::getVersion, state.version() + 1)
            .set(ProjectPO::getName, state.name())
            .set(ProjectPO::getCode, state.code())
            .set(ProjectPO::getDescription, state.description())
            .set(ProjectPO::getStatus, state.status())
            .set(ProjectPO::getColor, state.color())
            .set(ProjectPO::getSortOrder, state.sortOrder())
            .set(ProjectPO::getArchivedAt, state.archivedAt())
            .set(ProjectPO::getCreatedAt, state.createdAt())
            .set(ProjectPO::getUpdatedAt, state.updatedAt());
    if (mapper.update(null, update) != 1) {
      throw new ConcurrentChange("内容已改变，请重新载入");
    }
    aggregate.persisted(state.id(), state.version() + 1);
  }

  @Override
  public boolean nameUsed(String name, Long exceptId) {
    return mapper.selectCount(
            new LambdaQueryWrapper<ProjectPO>()
                .eq(ProjectPO::getName, name)
                .ne(exceptId != null, ProjectPO::getId, exceptId))
        > 0;
  }
}
