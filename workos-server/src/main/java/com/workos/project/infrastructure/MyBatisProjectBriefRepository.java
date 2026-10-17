package com.workos.project.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.workos.project.domain.ProjectBrief;
import com.workos.project.domain.ProjectBriefRepository;
import com.workos.project.infrastructure.persistence.entity.ProjectContextPO;
import com.workos.project.infrastructure.persistence.mapper.ProjectContextMapper;
import com.workos.shared.domain.ConcurrentChange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** ProjectBrief 的 MyBatis-Plus 适配器；显式写 null，防止清空被 ORM 的非空策略忽略。 */
@Repository
@RequiredArgsConstructor
public class MyBatisProjectBriefRepository implements ProjectBriefRepository {
  private final ProjectContextMapper mapper;

  @Override
  public ProjectBrief find(Long key) {
    ProjectContextPO entity =
        mapper.selectOne(
            new LambdaQueryWrapper<ProjectContextPO>().eq(ProjectContextPO::getProjectId, key));
    return entity == null
        ? null
        : new ProjectBrief(
            new ProjectBrief.State(
                entity.getId(),
                entity.getProjectId(),
                entity.getGoal(),
                entity.getKeyPeople(),
                entity.getKeyLinks(),
                entity.getFocus(),
                entity.getBlocker(),
                entity.getNextAction(),
                entity.getProgressNote(),
                entity.getNotesMarkdown(),
                entity.getUpdatedAt(),
                entity.getVersion()));
  }

  @Override
  public void save(ProjectBrief aggregate) {
    ProjectBrief.State state = aggregate.snapshot();
    ProjectContextPO entity = new ProjectContextPO();
    entity.setId(state.id());
    entity.setProjectId(state.projectId());
    entity.setGoal(state.goal());
    entity.setKeyPeople(state.keyPeople());
    entity.setKeyLinks(state.keyLinks());
    entity.setFocus(state.focus());
    entity.setBlocker(state.blocker());
    entity.setNextAction(state.nextAction());
    entity.setProgressNote(state.progressNote());
    entity.setNotesMarkdown(state.notesMarkdown());
    entity.setUpdatedAt(state.updatedAt());
    entity.setVersion(state.version());
    if (state.id() == null) {
      mapper.insert(entity);
      aggregate.persisted(entity.getId(), entity.getVersion());
      return;
    }
    LambdaUpdateWrapper<ProjectContextPO> update =
        new LambdaUpdateWrapper<ProjectContextPO>()
            .eq(ProjectContextPO::getId, state.id())
            .eq(ProjectContextPO::getVersion, state.version())
            .set(ProjectContextPO::getVersion, state.version() + 1)
            .set(ProjectContextPO::getProjectId, state.projectId())
            .set(ProjectContextPO::getGoal, state.goal())
            .set(ProjectContextPO::getKeyPeople, state.keyPeople())
            .set(ProjectContextPO::getKeyLinks, state.keyLinks())
            .set(ProjectContextPO::getFocus, state.focus())
            .set(ProjectContextPO::getBlocker, state.blocker())
            .set(ProjectContextPO::getNextAction, state.nextAction())
            .set(ProjectContextPO::getProgressNote, state.progressNote())
            .set(ProjectContextPO::getNotesMarkdown, state.notesMarkdown())
            .set(ProjectContextPO::getUpdatedAt, state.updatedAt());
    if (mapper.update(null, update) != 1) {
      throw new ConcurrentChange("内容已改变，请重新载入");
    }
    aggregate.persisted(state.id(), state.version() + 1);
  }
}
