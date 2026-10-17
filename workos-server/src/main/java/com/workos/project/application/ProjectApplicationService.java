package com.workos.project.application;

import com.workos.common.enums.ResponseCode;
import com.workos.common.exception.BizException;
import com.workos.common.support.TimeZoneHolder;
import com.workos.project.domain.Project;
import com.workos.project.domain.ProjectBrief;
import com.workos.project.domain.ProjectBriefRepository;
import com.workos.project.domain.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 项目用例：引用检查和事务编排，项目及现状分别保护自己的版本。 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProjectApplicationService {
  private final ProjectRepository projects;
  private final ProjectBriefRepository briefs;
  private final TimeZoneHolder clock;

  /** 创建项目并验证名称唯一性，保存后返回稳定身份。 */
  public long create(
      String name, String code, String description, String color, Integer sortOrder) {
    Project project = Project.create(name, code, description, color, sortOrder, clock.now());
    requireUnique(project.snapshot().name(), null);
    projects.save(project);
    return project.snapshot().id();
  }

  /** 检查编辑版本与名称唯一性，再保存项目的业务修改。 */
  public void revise(long id, Integer version, Project.Edit edit) {
    Project project = require(id);
    expect(project.snapshot().version(), version);
    project.revise(edit, clock.now());
    requireUnique(project.snapshot().name(), id);
    projects.save(project);
  }

  /** 协调归档或恢复用例；具体状态变化仍通过 Project 的行为完成。 */
  public void archive(long id, boolean archive) {
    Project project = require(id);
    if (archive) {
      project.archive(clock.now());
    } else {
      project.restore(clock.now());
    }
    projects.save(project);
  }

  /** 检查项目存在，按现状自己的版本修订；没有现状时创建空白版本。 */
  public void reviseBrief(long id, Integer version, ProjectBrief.Revision change) {
    require(id);
    ProjectBrief brief = briefs.find(id);
    if (brief == null) {
      brief = ProjectBrief.empty(id, clock.now());
    }
    expect(brief.snapshot().version(), version);
    brief.revise(change, clock.now());
    briefs.save(brief);
  }

  private Project require(long id) {
    Project project = projects.find(id);
    if (project == null) {
      throw new BizException(ResponseCode.PROJECT_NOT_FOUND);
    }
    return project;
  }

  private void requireUnique(String name, Long except) {
    if (projects.nameUsed(name, except)) {
      throw new BizException(ResponseCode.PROJECT_NAME_DUPLICATE);
    }
  }

  private void expect(int current, Integer expected) {
    if (expected != null && current != expected) {
      throw new BizException(ResponseCode.VERSION_CONFLICT);
    }
  }
}
