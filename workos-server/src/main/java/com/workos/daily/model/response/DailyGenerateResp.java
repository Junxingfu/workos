package com.workos.daily.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * @Class DailyGenerateResp
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 生成结果（预览，不覆盖 content）
 */
@Data
public class DailyGenerateResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private LocalDate date;

    @Schema(title = "生成内容快照")
    private String generatedContent;

    @Schema(title = "来源记录数")
    private Integer sourceWorkLogCount;

    private Integer version;
}
