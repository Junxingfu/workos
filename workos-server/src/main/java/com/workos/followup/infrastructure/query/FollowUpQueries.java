package com.workos.followup.infrastructure.query;

import com.workos.common.support.TimeZoneHolder;
import com.workos.followup.application.query.FollowUpQuery;
import com.workos.followup.application.readmodel.FollowUpsResp;
import com.workos.followup.application.readmodel.PersonFollowUpResp;
import com.workos.item.infrastructure.persistence.mapper.ItemMapper;
import com.workos.item.infrastructure.query.ItemQueries;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 跟进工作台实现：判定口径与 Today 保持一致 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FollowUpQueries implements FollowUpQuery {

  /** 最近完成回看窗口 */
  private static final int RECENT_DONE_DAYS = 7;

  private final ItemMapper itemMapper;

  private final ItemQueries itemService;

  private final TimeZoneHolder timeZoneHolder;

  /** 聚合等待与跟进 */
  @Override
  public FollowUpsResp followUps() {
    LocalDate today = timeZoneHolder.today();
    Instant todayStart = timeZoneHolder.startOfDay(today);
    Instant todayEnd = timeZoneHolder.endOfDay(today);
    Instant now = timeZoneHolder.now();

    FollowUpsResp resp = new FollowUpsResp();
    resp.setOverdue(itemService.assembles(itemMapper.selectFollowUpOverdue(todayStart)));
    resp.setToday(itemService.assembles(itemMapper.selectFollowUpToday(todayStart, todayEnd)));
    resp.setThisWeek(
        itemService.assembles(
            itemMapper.selectFollowUpThisWeek(
                todayEnd, timeZoneHolder.startOfDay(today.plusDays(7)))));
    resp.setWaiting(itemService.assembles(itemMapper.selectWaitingAll(now)));
    resp.setRecentlyDone(
        itemService.assembles(
            itemMapper.selectRecentlyDone(now.minus(Duration.ofDays(RECENT_DONE_DAYS)))));
    resp.setByPerson(
        itemMapper.selectWaitingByPerson().stream()
            .map(
                row -> {
                  PersonFollowUpResp person = new PersonFollowUpResp();
                  person.setPerson(row.getPerson());
                  person.setWaitingOn(row.getCnt() == null ? 0 : row.getCnt().intValue());
                  // MVP 没有"别人在等我"的数据模型，如实返回 0，不做推测
                  person.setWaitingMe(0);
                  return person;
                })
            .toList());
    return resp;
  }
}
