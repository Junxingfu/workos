package com.workos.followup.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Class PersonCountRowDTO
 * @Author WorkOS
 * @Date 2026年09月25日 10:00
 * @Desc 按等待对象聚合的计数行
 */
@Data
public class PersonCountRowDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 等待对象（waiting_for）
     */
    private String person;

    private Long cnt;
}
