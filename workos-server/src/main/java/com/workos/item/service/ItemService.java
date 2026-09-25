package com.workos.item.service;

import com.workos.common.model.BasePageResult;
import com.workos.item.data.entity.ItemPO;
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

import java.util.List;

/**
 * @Class ItemService
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 事项业务接口
 */
public interface ItemService {

    /**
     * @Method create
     * @Param [req]
     * @Return Long
     * @Desc 创建事项
     */
    Long create(ItemCreateReq req);

    /**
     * @Method quickCapture
     * @Param [req]
     * @Return Long
     * @Desc 快速记录：一句话进 Inbox
     */
    Long quickCapture(ItemQuickCaptureReq req);

    /**
     * @Method detail
     * @Param [id]
     * @Return ItemResp
     * @Desc 事项详情
     */
    ItemResp detail(Long id);

    /**
     * @Method page
     * @Param [req]
     * @Return BasePageResult<ItemResp>
     * @Desc 全部事项分页
     */
    BasePageResult<ItemResp> page(ItemPageReq req);

    /**
     * @Method update
     * @Param [id, req]
     * @Return Boolean
     * @Desc 属性修改（不允许改状态）
     */
    Boolean update(Long id, ItemUpdateReq req);

    /**
     * @Method transition
     * @Param [id, req]
     * @Return ItemResp
     * @Desc 状态转换（唯一入口）
     */
    ItemResp transition(Long id, ItemTransitionReq req);

    /**
     * @Method followUpAction
     * @Param [id, req]
     * @Return ItemResp
     * @Desc 跟进动作三选一
     */
    ItemResp followUpAction(Long id, ItemFollowUpActionReq req);

    /**
     * @Method plan
     * @Param [id, req]
     * @Return ItemResp
     * @Desc 加入今日计划 / 排到某天
     */
    ItemResp plan(Long id, ItemPlanReq req);

    /**
     * @Method unplan
     * @Param [id]
     * @Return Boolean
     * @Desc 移出今日计划
     */
    Boolean unplan(Long id);

    /**
     * @Method reorderPlan
     * @Param [req]
     * @Return Boolean
     * @Desc 调整今日计划顺序
     */
    Boolean reorderPlan(ItemPlanReorderReq req);

    /**
     * @Method delete
     * @Param [id]
     * @Return Boolean
     * @Desc 删除（仅限未整理）
     */
    Boolean delete(Long id);

    /**
     * @Method activities
     * @Param [id]
     * @Return List<ItemActivityResp>
     * @Desc 查询系统事件
     */
    List<ItemActivityResp> activities(Long id);

    /**
     * @Method assembles
     * @Param [poList]
     * @Return List<ItemResp>
     * @Desc 批量装配展示字段（供 Today / Inbox 复用）
     */
    List<ItemResp> assembles(List<ItemPO> poList);
}
