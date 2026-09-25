package com.workos.project.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Class ProjectContextUpdateReq
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 更新项目现状请求
 */
@Data
public class ProjectContextUpdateReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(title = "目标")
    private String goal;

    @Schema(title = "当前焦点")
    private String currentFocus;

    @Schema(title = "进展备注")
    private String progressNote;

    @Schema(title = "当前阻塞")
    private String blocker;

    @Schema(title = "下一步")
    private String nextAction;

    @Schema(title = "关键人")
    private String keyPeople;

    @Schema(title = "关键链接")
    private String keyLinks;

    @Schema(title = "补充说明")
    private String notesMarkdown;
}
