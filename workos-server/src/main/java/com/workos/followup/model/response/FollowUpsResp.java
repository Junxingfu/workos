package com.workos.followup.model.response;

import com.workos.item.model.response.ItemResp;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @Class FollowUpsResp
 * @Author WorkOS
 * @Date 2026年09月25日 10:00
 * @Desc 等待与跟进工作台
 */
@Data
public class FollowUpsResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 今天到点
     */
    private List<ItemResp> today;

    /**
     * 已过期且未处理
     */
    private List<ItemResp> overdue;

    /**
     * 未来 7 天内
     */
    private List<ItemResp> thisWeek;

    /**
     * 正在等待且未到跟进时间
     */
    private List<ItemResp> waiting;

    /**
     * 最近完成
     */
    private List<ItemResp> recentlyDone;

    /**
     * 按人聚合
     */
    private List<PersonFollowUpResp> byPerson;
}
