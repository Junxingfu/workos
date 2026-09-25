package com.workos.item.controller;

import com.workos.common.model.BasePageResult;
import com.workos.common.model.BaseResponse;
import com.workos.item.model.request.ItemCreateReq;
import com.workos.item.model.request.ItemFollowUpActionReq;
import com.workos.item.model.request.ItemPageReq;
import com.workos.item.model.request.ItemPlanReorderReq;
import com.workos.item.model.request.ItemPlanReq;
import com.workos.item.model.request.ItemQuickCaptureReq;
import com.workos.item.model.request.ItemTransitionReq;
import com.workos.item.model.request.ItemUpdateReq;
import com.workos.item.model.response.ItemActivityResp;
import com.workos.item.model.response.ItemResp;
import com.workos.item.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

import java.util.List;

/**
 * @Class ItemController
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 事项接口：状态变更唯一走 transitions
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/items")
@Tag(name = "事项", description = "事项的创建、查询、修改与业务动作")
public class ItemController {

    private final ItemService itemService;

    /**
     * @Method create
     * @Param [req]
     * @Return BaseResponse<Long>
     * @Desc 创建事项
     */
    @PostMapping
    @Operation(summary = "创建事项")
    public BaseResponse<Long> create(@Valid @RequestBody ItemCreateReq req) {
        return BaseResponse.success(itemService.create(req));
    }

    /**
     * @Method quickCapture
     * @Param [req]
     * @Return BaseResponse<Long>
     * @Desc 快速记录：一句话进 Inbox
     */
    @PostMapping("/quick-capture")
    @Operation(summary = "快速记录")
    public BaseResponse<Long> quickCapture(@Valid @RequestBody ItemQuickCaptureReq req) {
        return BaseResponse.success(itemService.quickCapture(req));
    }

    /**
     * @Method page
     * @Param [req]
     * @Return BaseResponse<BasePageResult<ItemResp>>
     * @Desc 全部事项分页
     */
    @GetMapping
    @Operation(summary = "全部事项分页")
    public BaseResponse<BasePageResult<ItemResp>> page(ItemPageReq req) {
        return BaseResponse.success(itemService.page(req));
    }

    /**
     * @Method detail
     * @Param [id]
     * @Return BaseResponse<ItemResp>
     * @Desc 事项详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "事项详情")
    public BaseResponse<ItemResp> detail(@PathVariable("id") Long id) {
        return BaseResponse.success(itemService.detail(id));
    }

    /**
     * @Method update
     * @Param [id, req]
     * @Return BaseResponse<Boolean>
     * @Desc 属性修改（不允许改状态）
     */
    @PutMapping("/{id}")
    @Operation(summary = "修改事项属性")
    public BaseResponse<Boolean> update(@PathVariable("id") Long id, @Valid @RequestBody ItemUpdateReq req) {
        return BaseResponse.success(itemService.update(id, req));
    }

    /**
     * @Method delete
     * @Param [id]
     * @Return BaseResponse<Boolean>
     * @Desc 删除未整理事项
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除未整理事项")
    public BaseResponse<Boolean> delete(@PathVariable("id") Long id) {
        return BaseResponse.success(itemService.delete(id));
    }

    /**
     * @Method transition
     * @Param [id, req]
     * @Return BaseResponse<ItemResp>
     * @Desc 状态转换
     */
    @PostMapping("/{id}/transitions")
    @Operation(summary = "状态转换（唯一状态变更入口）")
    public BaseResponse<ItemResp> transition(@PathVariable("id") Long id,
                                             @Valid @RequestBody ItemTransitionReq req) {
        return BaseResponse.success(itemService.transition(id, req));
    }

    /**
     * @Method followUpAction
     * @Param [id, req]
     * @Return BaseResponse<ItemResp>
     * @Desc 跟进动作三选一
     */
    @PostMapping("/{id}/follow-up-actions")
    @Operation(summary = "跟进动作（还没回 / 对方给了 / 暂时不跟）")
    public BaseResponse<ItemResp> followUpAction(@PathVariable("id") Long id,
                                                 @Valid @RequestBody ItemFollowUpActionReq req) {
        return BaseResponse.success(itemService.followUpAction(id, req));
    }

    /**
     * @Method plan
     * @Param [id, req]
     * @Return BaseResponse<ItemResp>
     * @Desc 加入今日计划 / 排到某天
     */
    @PostMapping("/{id}/plan")
    @Operation(summary = "加入计划")
    public BaseResponse<ItemResp> plan(@PathVariable("id") Long id, @Valid @RequestBody ItemPlanReq req) {
        return BaseResponse.success(itemService.plan(id, req));
    }

    /**
     * @Method unplan
     * @Param [id]
     * @Return BaseResponse<Boolean>
     * @Desc 移出计划
     */
    @DeleteMapping("/{id}/plan")
    @Operation(summary = "移出计划")
    public BaseResponse<Boolean> unplan(@PathVariable("id") Long id) {
        return BaseResponse.success(itemService.unplan(id));
    }

    /**
     * @Method reorderPlan
     * @Param [req]
     * @Return BaseResponse<Boolean>
     * @Desc 调整计划顺序
     */
    @PostMapping("/plan/reorder")
    @Operation(summary = "调整计划顺序")
    public BaseResponse<Boolean> reorderPlan(@Valid @RequestBody ItemPlanReorderReq req) {
        return BaseResponse.success(itemService.reorderPlan(req));
    }

    /**
     * @Method activities
     * @Param [id]
     * @Return BaseResponse<List<ItemActivityResp>>
     * @Desc 系统事件
     */
    @GetMapping("/{id}/activities")
    @Operation(summary = "系统事件")
    public BaseResponse<List<ItemActivityResp>> activities(@PathVariable("id") Long id) {
        return BaseResponse.success(itemService.activities(id));
    }
}
