package com.workos.common.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Class BasePageReq
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 分页查询请求基类
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
public abstract class BasePageReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 页码
     */
    @NotNull(message = "页码不能为空", groups = ValidateGroup.Query.class)
    @Min(value = 1, message = "页码必须≥1", groups = ValidateGroup.Query.class)
    @Schema(title = "页码")
    private Integer pageNo;

    /**
     * 每页条数
     */
    @NotNull(message = "每页条数不能为空", groups = ValidateGroup.Query.class)
    @Min(value = 1, message = "每页条数≥1", groups = ValidateGroup.Query.class)
    @Max(value = 100, message = "每页条数最多100条", groups = ValidateGroup.Query.class)
    @Schema(title = "每页条数")
    private Integer pageSize;

    /**
     * @Method fillPageDefault
     * @Desc 分页参数默认值填充
     */
    public void fillPageDefault() {
        if (this.pageNo == null || this.pageNo < 1) {
            this.pageNo = 1;
        }
        if (this.pageSize == null || this.pageSize < 1) {
            this.pageSize = 20;
        }
    }

    /**
     * @Method getStartRow
     * @Return Integer
     * @Desc 分页起始条数
     */
    public Integer getStartRow() {
        fillPageDefault();
        return (this.pageNo - 1) * this.pageSize;
    }
}
