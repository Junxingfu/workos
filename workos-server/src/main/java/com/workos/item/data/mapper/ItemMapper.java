package com.workos.item.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workos.item.data.entity.ItemPO;
import com.workos.item.model.dto.LastLogDTO;
import com.workos.item.model.dto.LastActivityDTO;
import com.workos.project.model.dto.ProjectStatRowDTO;
import com.workos.project.model.dto.ProjectCountRowDTO;
import com.workos.followup.model.dto.PersonCountRowDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * @Class ItemMapper
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 事项数据访问
 */
@Mapper
public interface ItemMapper extends BaseMapper<ItemPO> {

    /**
     * @Method selectPlan
     * @Param [date]
     * @Return List<ItemPO>
     * @Desc ① 今日计划
     */
    List<ItemPO> selectPlan(@Param("date") LocalDate date);

    List<ItemPO> selectCarriedOverPlan(@Param("date") LocalDate date);

    /**
     * @Method selectOverdue
     * @Param [todayStart]
     * @Return List<ItemPO>
     * @Desc ② 逾期（只统计 TODO / DOING）
     */
    List<ItemPO> selectOverdue(@Param("todayStart") Instant todayStart);

    /**
     * @Method selectDueToday
     * @Param [todayStart, tomorrowStart]
     * @Return List<ItemPO>
     * @Desc ③ 今日到期
     */
    List<ItemPO> selectDueToday(@Param("todayStart") Instant todayStart,
                                @Param("tomorrowStart") Instant tomorrowStart);

    /**
     * @Method selectFollowUpDue
     * @Param [now]
     * @Return List<ItemPO>
     * @Desc ④ 待跟进
     */
    List<ItemPO> selectFollowUpDue(@Param("now") Instant now);

    /**
     * @Method selectWaiting
     * @Param [now]
     * @Return List<ItemPO>
     * @Desc ⑤ 等待中
     */
    List<ItemPO> selectWaiting(@Param("now") Instant now);

    /**
     * @Method selectPendingDecision
     * @Param [excludedIds]
     * @Return List<ItemPO>
     * @Desc ⑥ 待安排（决策队列，上限 5 条）
     */
    List<ItemPO> selectPendingDecision(@Param("excludedIds") Collection<Long> excludedIds);

    /**
     * @Method selectLastLogs
     * @Param [itemIds]
     * @Return List<LastLogDTO>
     * @Desc 卡片"最近一条记录"：优先工作记录
     */
    List<LastLogDTO> selectLastLogs(@Param("itemIds") Collection<Long> itemIds);

    /**
     * @Method selectLastActivities
     * @Param [itemIds]
     * @Return List<Map<String, Object>>
     * @Desc 没有工作记录时，回退到最近一条系统活动
     */
    List<LastActivityDTO> selectLastActivities(@Param("itemIds") Collection<Long> itemIds);

    /**
     * @Method countInbox
     * @Return long
     * @Desc 待整理数量
     */
    long countInbox();

    /**
     * @Method countUnclassified
     * @Return long
     * @Desc 未分类：project 为空且未完成
     */
    long countUnclassified();

    /**
     * @Method countOldestInboxDays
     * @Return long
     * @Desc Inbox 里最老一条已经压了多少天
     */
    long countOldestInboxDays();

    /**
     * @Method selectProjectStatRows
     * @Param [projectIds]
     * @Return List<ProjectStatRowDTO>
     * @Desc 项目维度统计：状态计数 + 最近活动时间
     */
    List<ProjectStatRowDTO> selectProjectStatRows(@Param("projectIds") Collection<Long> projectIds);

    /**
     * @Method selectProjectTodayCount
     * @Param [projectIds, today]
     * @Return List<ProjectCountRowDTO>
     * @Desc 项目维度：今天计划了做多少条
     */
    List<ProjectCountRowDTO> selectProjectTodayCount(@Param("projectIds") Collection<Long> projectIds,
                                                     @Param("today") LocalDate today);

    /**
     * @Method selectFollowUpOverdue
     * @Param [todayStart]
     * @Return List<ItemPO>
     * @Desc 待跟进：已过期且未处理
     */
    List<ItemPO> selectFollowUpOverdue(@Param("todayStart") Instant todayStart);

    /**
     * @Method selectFollowUpToday
     * @Param [todayStart, todayEnd]
     * @Return List<ItemPO>
     * @Desc 待跟进：今天到点
     */
    List<ItemPO> selectFollowUpToday(@Param("todayStart") Instant todayStart,
                                     @Param("todayEnd") Instant todayEnd);

    /**
     * @Method selectFollowUpThisWeek
     * @Param [from, to]
     * @Return List<ItemPO>
     * @Desc 待跟进：未来 7 天内
     */
    List<ItemPO> selectFollowUpThisWeek(@Param("from") Instant from, @Param("to") Instant to);

    /**
     * @Method selectWaitingAll
     * @Param [now]
     * @Return List<ItemPO>
     * @Desc 等待中（不限定是否有计划日期，供跟进工作台使用）
     */
    List<ItemPO> selectWaitingAll(@Param("now") Instant now);

    /**
     * @Method selectRecentlyDone
     * @Param [since]
     * @Return List<ItemPO>
     * @Desc 最近完成
     */
    List<ItemPO> selectRecentlyDone(@Param("since") Instant since);

    /**
     * @Method selectWaitingByPerson
     * @Return List<PersonCountRowDTO>
     * @Desc 按等待对象聚合
     */
    List<PersonCountRowDTO> selectWaitingByPerson();
}
