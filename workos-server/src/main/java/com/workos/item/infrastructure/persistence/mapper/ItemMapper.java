package com.workos.item.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workos.followup.application.readmodel.PersonCountRowDTO;
import com.workos.item.application.readmodel.LastActivityDTO;
import com.workos.item.application.readmodel.LastLogDTO;
import com.workos.item.infrastructure.persistence.entity.ItemPO;
import com.workos.project.application.readmodel.ProjectCountRowDTO;
import com.workos.project.application.readmodel.ProjectStatRowDTO;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 事项数据访问 */
@Mapper
public interface ItemMapper extends BaseMapper<ItemPO> {

  /** ① 今日计划 */
  List<ItemPO> selectPlan(@Param("date") LocalDate date);

  List<ItemPO> selectCarriedOverPlan(@Param("date") LocalDate date);

  /** ② 逾期（只统计 TODO / DOING） */
  List<ItemPO> selectOverdue(@Param("todayStart") Instant todayStart);

  /** ③ 今日到期 */
  List<ItemPO> selectDueToday(
      @Param("todayStart") Instant todayStart, @Param("tomorrowStart") Instant tomorrowStart);

  /** ④ 待跟进 */
  List<ItemPO> selectFollowUpDue(@Param("now") Instant now);

  /** ⑤ 等待中 */
  List<ItemPO> selectWaiting(@Param("now") Instant now);

  /** ⑥ 待安排（决策队列，上限 5 条） */
  List<ItemPO> selectPendingDecision(@Param("excludedIds") Collection<Long> excludedIds);

  /** 卡片"最近一条记录"：优先工作记录 */
  List<LastLogDTO> selectLastLogs(@Param("itemIds") Collection<Long> itemIds);

  /** 没有工作记录时，回退到最近一条系统活动 */
  List<LastActivityDTO> selectLastActivities(@Param("itemIds") Collection<Long> itemIds);

  /** 待整理数量 */
  long countInbox();

  /** 未分类：project 为空且未完成 */
  long countUnclassified();

  /** Inbox 里最老一条已经压了多少天 */
  long countOldestInboxDays();

  /** 项目维度统计：状态计数 + 最近活动时间 */
  List<ProjectStatRowDTO> selectProjectStatRows(@Param("projectIds") Collection<Long> projectIds);

  /** 项目维度：今天计划了做多少条 */
  List<ProjectCountRowDTO> selectProjectTodayCount(
      @Param("projectIds") Collection<Long> projectIds, @Param("today") LocalDate today);

  /** 待跟进：已过期且未处理 */
  List<ItemPO> selectFollowUpOverdue(@Param("todayStart") Instant todayStart);

  /** 待跟进：今天到点 */
  List<ItemPO> selectFollowUpToday(
      @Param("todayStart") Instant todayStart, @Param("todayEnd") Instant todayEnd);

  /** 待跟进：未来 7 天内 */
  List<ItemPO> selectFollowUpThisWeek(@Param("from") Instant from, @Param("to") Instant to);

  /** 等待中（不限定是否有计划日期，供跟进工作台使用） */
  List<ItemPO> selectWaitingAll(@Param("now") Instant now);

  /** 最近完成 */
  List<ItemPO> selectRecentlyDone(@Param("since") Instant since);

  /** 按等待对象聚合 */
  List<PersonCountRowDTO> selectWaitingByPerson();
}
