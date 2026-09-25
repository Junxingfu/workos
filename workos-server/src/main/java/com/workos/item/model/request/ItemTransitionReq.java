package com.workos.item.model.request;

import com.workos.item.enums.ItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * @Class ItemTransitionReq
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 状态转换请求（唯一的状态变更入口，可携带业务载荷）
 */
@Data
public class ItemTransitionReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 乐观锁版本
     */
    @Schema(title = "版本号")
    private Integer version;

    /**
     * 目标状态
     */
    @NotNull(message = "目标状态不能为空")
    @Schema(title = "目标状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private ItemStatus toStatus;

    /**
     * 等待对象（进入 WAITING 时必填）
     */
    @Schema(title = "等待对象")
    private String waitingFor;

    /**
     * 对方预计什么时候给
     */
    @Schema(title = "对方预计时间")
    private Instant waitingExpectedAt;

    /**
     * 下次跟进时间
     */
    @Schema(title = "下次跟进时间")
    private Instant nextFollowUpAt;

    /**
     * 备注
     */
    @Schema(title = "备注")
    private String remark;
}
