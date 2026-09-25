package com.workos.common.exception;

import com.workos.common.enums.ResponseCode;
import lombok.Getter;

import java.io.Serial;

/**
 * @Class BaseException
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 基础异常
 */
@Getter
public class BaseException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务编码
     */
    private final Integer code;

    public BaseException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BaseException(ResponseCode responseCode) {
        super(responseCode.getDescription());
        this.code = responseCode.getCode();
    }

    public BaseException(ResponseCode responseCode, String message) {
        super(message);
        this.code = responseCode.getCode();
    }
}
