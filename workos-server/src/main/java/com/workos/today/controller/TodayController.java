package com.workos.today.controller;

import com.workos.common.model.BaseResponse;
import com.workos.item.model.request.ItemPlanReorderReq;
import com.workos.item.service.ItemService;
import com.workos.today.model.response.TodayResp;
import com.workos.today.service.TodayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Class TodayController
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc Today 聚合接口
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/today")
@Tag(name = "Today", description = "今天做什么 / 今天别忘了 / 我在等谁")
public class TodayController {

    private final TodayService todayService;

    private final ItemService itemService;

    /**
     * @Method today
     * @Return BaseResponse<TodayResp>
     * @Desc Today 聚合
     */
    @GetMapping
    @Operation(summary = "Today 聚合")
    public BaseResponse<TodayResp> today() {
        return BaseResponse.success(todayService.today());
    }

    /**
     * @Method reorderPlan
     * @Param [req]
     * @Return BaseResponse<Boolean>
     * @Desc 调整今日计划顺序
     */
    @PostMapping("/plan/reorder")
    @Operation(summary = "调整今日计划顺序")
    public BaseResponse<Boolean> reorderPlan(@Valid @RequestBody ItemPlanReorderReq req) {
        return BaseResponse.success(itemService.reorderPlan(req));
    }
}
