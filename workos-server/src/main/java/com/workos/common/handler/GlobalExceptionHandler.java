package com.workos.common.handler;

import com.workos.common.enums.ResponseCode;
import com.workos.common.exception.BaseException;
import com.workos.common.model.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Objects;

/**
 * @Class GlobalExceptionHandler
 * @Author WorkOS
 * @Date 2026年09月24日 10:00
 * @Desc 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * @Method handleBaseException
     * @Param [e]
     * @Return BaseResponse<Void>
     * @Desc 业务异常
     */
    @ExceptionHandler(BaseException.class)
    public BaseResponse<Void> handleBaseException(BaseException e) {
        log.warn("业务异常 code={} message={}", e.getCode(), e.getMessage());
        return BaseResponse.failure(e.getCode(), e.getMessage());
    }

    /**
     * @Method handleValidation
     * @Param [e]
     * @Return BaseResponse<Void>
     * @Desc 参数校验异常
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public BaseResponse<Void> handleValidation(Exception e) {
        List<String> details = e instanceof MethodArgumentNotValidException manv
                ? manv.getBindingResult().getFieldErrors().stream().map(this::format).toList()
                : ((BindException) e).getBindingResult().getFieldErrors().stream().map(this::format).toList();
        BaseResponse<Void> response = BaseResponse.failure(ResponseCode.PARAM_ERROR);
        response.setMessageArray(details);
        if (!details.isEmpty()) {
            response.setMessage(details.get(0));
        }
        return response;
    }

    /**
     * @Method handleOptimisticLock
     * @Param [e]
     * @Return BaseResponse<Void>
     * @Desc 乐观锁冲突
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public BaseResponse<Void> handleOptimisticLock(OptimisticLockingFailureException e) {
        log.warn("乐观锁冲突: {}", e.getMessage());
        return BaseResponse.failure(ResponseCode.VERSION_CONFLICT);
    }

    /**
     * @Method handleException
     * @Param [e]
     * @Return BaseResponse<Void>
     * @Desc 兜底异常
     */
    @ExceptionHandler(Exception.class)
    public BaseResponse<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return BaseResponse.failure(ResponseCode.SYSTEM_ERROR);
    }

    private String format(FieldError fieldError) {
        String message = Objects.requireNonNullElse(fieldError.getDefaultMessage(), "参数错误");
        return fieldError.getField() + ": " + message;
    }
}
