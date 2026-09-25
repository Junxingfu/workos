package com.workos.item.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.workos.item.enums.ActivityType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * @Class ItemActivityPO
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 系统事件（用于历史与审计，不作为日报的"工作内容"）
 */
@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@TableName("item_activity")
public class ItemActivityPO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 事项主键
     */
    private Long itemId;

    /**
     * 事件类型
     */
    private ActivityType activityType;

    /**
     * 变更前状态
     */
    private String fromStatus;

    /**
     * 变更后状态
     */
    private String toStatus;

    /**
     * 变更字段
     */
    private String fieldName;

    /**
     * 旧值
     */
    private String oldValue;

    /**
     * 新值
     */
    private String newValue;

    /**
     * 备注
     */
    private String remark;

    /**
     * 操作者：USER / SYSTEM
     */
    private String actor;

    /**
     * 发生时间
     */
    private Instant occurredAt;
}
