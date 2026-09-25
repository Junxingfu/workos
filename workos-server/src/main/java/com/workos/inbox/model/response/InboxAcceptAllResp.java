package com.workos.inbox.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Class InboxAcceptAllResp
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 批量收下结果
 */
@Data
public class InboxAcceptAllResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(title = "已收下数量")
    private Integer accepted;
}
