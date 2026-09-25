package com.workos.common.model;

import com.workos.common.enums.ResponseCode;
import com.workos.common.exception.BizException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.slf4j.MDC;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * @Class BaseResponse
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 通用接口响应对象
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
public class BaseResponse<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务编码：0 表示成功
     */
    private Integer code = ResponseCode.SUCCESS.getCode();

    /**
     * 响应消息
     */
    private String message = ResponseCode.SUCCESS.getDescription();

    /**
     * 响应数据
     */
    private T data;

    /**
     * 参数校验明细
     */
    private List<String> messageArray;

    /**
     * 链路标识
     */
    private String traceId;

    public BaseResponse() {
        this.traceId = MDC.get("traceId");
    }

    /**
     * 成功响应
     *
     * @return 成功响应
     */
    public static <T> BaseResponse<T> success() {
        return new BaseResponse<>();
    }

    /**
     * 成功响应
     *
     * @param data 响应数据
     * @return 成功响应
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<T>().setData(data);
    }

    /**
     * 失败响应
     *
     * @param responseCode 响应编码
     * @return 失败响应
     */
    public static <T> BaseResponse<T> failure(ResponseCode responseCode) {
        return new BaseResponse<T>()
                .setCode(responseCode.getCode())
                .setMessage(responseCode.getDescription());
    }

    /**
     * 失败响应
     *
     * @param responseCode 响应编码
     * @param message 响应消息
     * @return 失败响应
     */
    public static <T> BaseResponse<T> failure(ResponseCode responseCode, String message) {
        return new BaseResponse<T>()
                .setCode(responseCode.getCode())
                .setMessage(message);
    }

    /**
     * 失败响应
     *
     * @param code 业务编码
     * @param message 响应消息
     * @return 失败响应
     */
    public static <T> BaseResponse<T> failure(Integer code, String message) {
        return new BaseResponse<T>()
                .setCode(code)
                .setMessage(message);
    }

    /**
     * 校验
     *
     * @return 是否成功
     */
    @JsonIgnore
    public boolean isSuccess() {
        return Objects.equals(this.code, ResponseCode.SUCCESS.getCode());
    }

    /**
     * 校验
     *
     * @param response 响应
     */
    public static void verify(BaseResponse<?> response) {
        if (Objects.nonNull(response) && !response.isSuccess()) {
            throw new BizException(response.getCode(), response.getMessage());
        }
    }
}
