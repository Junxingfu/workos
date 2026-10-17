package com.workos.item.interfaces.http;

import com.workos.common.model.BasePageResult;
import com.workos.common.model.BaseResponse;
import com.workos.item.application.query.ItemCriteria;
import com.workos.item.application.readmodel.ItemActivityResp;
import com.workos.item.application.readmodel.ItemResp;
import com.workos.item.interfaces.request.ItemCreateReq;
import com.workos.item.interfaces.request.ItemFollowUpActionReq;
import com.workos.item.interfaces.request.ItemPlanReorderReq;
import com.workos.item.interfaces.request.ItemPlanReq;
import com.workos.item.interfaces.request.ItemQuickCaptureReq;
import com.workos.item.interfaces.request.ItemTransitionReq;
import com.workos.item.interfaces.request.ItemUpdateReq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 事项接口：状态变更唯一走 transitions */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/items")
@Tag(name = "事项", description = "事项的创建、查询、修改与业务动作")
public class ItemController {

  private final ItemHttpAdapter itemService;

  /** 创建事项 */
  @PostMapping
  @Operation(summary = "创建事项")
  public BaseResponse<Long> create(@Valid @RequestBody ItemCreateReq req) {
    return BaseResponse.success(itemService.create(req));
  }

  /** 快速记录：一句话进 Inbox */
  @PostMapping("/quick-capture")
  @Operation(summary = "快速记录")
  public BaseResponse<Long> quickCapture(@Valid @RequestBody ItemQuickCaptureReq req) {
    return BaseResponse.success(itemService.quickCapture(req));
  }

  /** 全部事项分页 */
  @GetMapping
  @Operation(summary = "全部事项分页")
  public BaseResponse<BasePageResult<ItemResp>> page(ItemCriteria req) {
    return BaseResponse.success(itemService.page(req));
  }

  /** 事项详情 */
  @GetMapping("/{id}")
  @Operation(summary = "事项详情")
  public BaseResponse<ItemResp> detail(@PathVariable("id") Long id) {
    return BaseResponse.success(itemService.detail(id));
  }

  /** 属性修改（不允许改状态） */
  @PutMapping("/{id}")
  @Operation(summary = "修改事项属性")
  public BaseResponse<Boolean> update(
      @PathVariable("id") Long id, @Valid @RequestBody ItemUpdateReq req) {
    return BaseResponse.success(itemService.update(id, req));
  }

  /** 删除未整理事项 */
  @DeleteMapping("/{id}")
  @Operation(summary = "删除未整理事项")
  public BaseResponse<Boolean> delete(@PathVariable("id") Long id) {
    return BaseResponse.success(itemService.delete(id));
  }

  /** 状态转换 */
  @PostMapping("/{id}/transitions")
  @Operation(summary = "状态转换（唯一状态变更入口）")
  public BaseResponse<ItemResp> transition(
      @PathVariable("id") Long id, @Valid @RequestBody ItemTransitionReq req) {
    return BaseResponse.success(itemService.transition(id, req));
  }

  /** 跟进动作三选一 */
  @PostMapping("/{id}/follow-up-actions")
  @Operation(summary = "跟进动作（还没回 / 对方给了 / 暂时不跟）")
  public BaseResponse<ItemResp> followUpAction(
      @PathVariable("id") Long id, @Valid @RequestBody ItemFollowUpActionReq req) {
    return BaseResponse.success(itemService.followUpAction(id, req));
  }

  /** 加入今日计划 / 排到某天 */
  @PostMapping("/{id}/plan")
  @Operation(summary = "加入计划")
  public BaseResponse<ItemResp> plan(
      @PathVariable("id") Long id, @Valid @RequestBody ItemPlanReq req) {
    return BaseResponse.success(itemService.plan(id, req));
  }

  /** 移出计划 */
  @DeleteMapping("/{id}/plan")
  @Operation(summary = "移出计划")
  public BaseResponse<Boolean> unplan(@PathVariable("id") Long id) {
    return BaseResponse.success(itemService.unplan(id));
  }

  /** 调整计划顺序 */
  @PostMapping("/plan/reorder")
  @Operation(summary = "调整计划顺序")
  public BaseResponse<Boolean> reorderPlan(@Valid @RequestBody ItemPlanReorderReq req) {
    return BaseResponse.success(itemService.reorderPlan(req));
  }

  /** 系统事件 */
  @GetMapping("/{id}/activities")
  @Operation(summary = "系统事件")
  public BaseResponse<List<ItemActivityResp>> activities(@PathVariable("id") Long id) {
    return BaseResponse.success(itemService.activities(id));
  }
}
