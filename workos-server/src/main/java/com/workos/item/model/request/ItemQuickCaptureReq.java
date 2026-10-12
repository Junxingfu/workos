package com.workos.item.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Class ItemQuickCaptureReq
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 快速记录：只要求一句话，默认进 Inbox
 */
@Data
public class ItemQuickCaptureReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 记录内容
     */
    @NotBlank(message = "记录内容不能为空")
    @Size(max = 500, message = "内容长度不能超过500")
    @Schema(title = "记录内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    /** 记录去向；今天的业务日期由后端计算。 */
    private Destination destination = Destination.INBOX;

    /** 从项目中记录时继承主项目；全局记录可留空。 */
    private Long projectId;

    public enum Destination {
        INBOX, TODO, TODAY
    }
}
