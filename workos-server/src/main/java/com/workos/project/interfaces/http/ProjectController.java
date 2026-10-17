package com.workos.project.interfaces.http;

import com.workos.common.model.BasePageResult;
import com.workos.common.model.BaseResponse;
import com.workos.project.application.query.ProjectCriteria;
import com.workos.project.application.readmodel.ProjectContextResp;
import com.workos.project.application.readmodel.ProjectResp;
import com.workos.project.interfaces.request.ProjectContextUpdateReq;
import com.workos.project.interfaces.request.ProjectCreateReq;
import com.workos.project.interfaces.request.ProjectUpdateReq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 项目接口：归档代替删除 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects")
@Tag(name = "项目", description = "项目的创建、查询、归档与现状维护")
public class ProjectController {

  private final ProjectHttpAdapter projectService;

  /** 创建项目 */
  @PostMapping
  @Operation(summary = "创建项目")
  public BaseResponse<Long> create(@Valid @RequestBody ProjectCreateReq req) {
    return BaseResponse.success(projectService.create(req));
  }

  /** 项目分页 */
  @GetMapping
  @Operation(summary = "项目列表")
  public BaseResponse<BasePageResult<ProjectResp>> page(ProjectCriteria req) {
    return BaseResponse.success(projectService.page(req));
  }

  /** 项目详情 */
  @GetMapping("/{id}")
  @Operation(summary = "项目详情")
  public BaseResponse<ProjectResp> detail(@PathVariable("id") Long id) {
    return BaseResponse.success(projectService.detail(id));
  }

  /** 修改项目 */
  @PutMapping("/{id}")
  @Operation(summary = "修改项目")
  public BaseResponse<Boolean> update(
      @PathVariable("id") Long id, @Valid @RequestBody ProjectUpdateReq req) {
    return BaseResponse.success(projectService.update(id, req));
  }

  /** 归档项目 */
  @PostMapping("/{id}/archive")
  @Operation(summary = "归档项目")
  public BaseResponse<Boolean> archive(@PathVariable("id") Long id) {
    return BaseResponse.success(projectService.archive(id));
  }

  /** 恢复项目 */
  @PostMapping("/{id}/restore")
  @Operation(summary = "恢复项目")
  public BaseResponse<Boolean> restore(@PathVariable("id") Long id) {
    return BaseResponse.success(projectService.restore(id));
  }

  /** 获取项目现状 */
  @GetMapping("/{id}/context")
  @Operation(summary = "获取项目现状")
  public BaseResponse<ProjectContextResp> context(@PathVariable("id") Long id) {
    return BaseResponse.success(projectService.context(id));
  }

  /** 更新项目现状 */
  @PutMapping("/{id}/context")
  @Operation(summary = "更新项目现状")
  public BaseResponse<ProjectContextResp> updateContext(
      @PathVariable("id") Long id, @Valid @RequestBody ProjectContextUpdateReq req) {
    return BaseResponse.success(projectService.updateContext(id, req));
  }
}
