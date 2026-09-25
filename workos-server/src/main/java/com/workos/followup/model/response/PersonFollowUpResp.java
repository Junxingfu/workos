package com.workos.followup.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Class PersonFollowUpResp
 * @Author WorkOS
 * @Date 2026年09月25日 10:00
 * @Desc 按人聚合的等待与跟进
 */
@Data
public class PersonFollowUpResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(title = "对方")
    private String person;

    @Schema(title = "我在等他的数量")
    private Integer waitingOn;

    @Schema(title = "他在等我的数量；MVP 无此数据模型，恒为 0")
    private Integer waitingMe;
}
