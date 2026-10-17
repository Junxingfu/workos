package com.workos.common.handler;

import com.workos.common.enums.ResponseCode;
import com.workos.common.exception.BaseException;
import com.workos.common.model.BaseResponse;
import com.workos.shared.domain.ConcurrentChange;
import java.time.DateTimeException;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 全局异常处理 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /** 业务异常 */
  @ExceptionHandler(BaseException.class)
  public BaseResponse<Void> handleBaseException(BaseException e) {
    log.warn("业务异常 code={} message={}", e.getCode(), e.getMessage());
    return BaseResponse.failure(e.getCode(), e.getMessage());
  }

  /** 参数校验异常 */
  @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
  public BaseResponse<Void> handleValidation(Exception e) {
    List<String> details =
        e instanceof MethodArgumentNotValidException manv
            ? manv.getBindingResult().getFieldErrors().stream().map(this::format).toList()
            : ((BindException) e)
                .getBindingResult().getFieldErrors().stream().map(this::format).toList();
    BaseResponse<Void> response = BaseResponse.failure(ResponseCode.PARAM_ERROR);
    response.setMessageArray(details);
    if (!details.isEmpty()) {
      response.setMessage(details.get(0));
    }
    return response;
  }

  /** 乐观锁冲突 */
  @ExceptionHandler(OptimisticLockingFailureException.class)
  public BaseResponse<Void> handleOptimisticLock(OptimisticLockingFailureException e) {
    log.warn("乐观锁冲突: {}", e.getMessage());
    return BaseResponse.failure(ResponseCode.VERSION_CONFLICT);
  }

  /** 兜底异常 */
  @ExceptionHandler(Exception.class)
  public BaseResponse<Void> handleException(Exception e) {
    log.error("系统异常", e);
    return BaseResponse.failure(ResponseCode.SYSTEM_ERROR);
  }

  /** 领域约束以可理解的参数错误返回，不泄漏为服务器异常。 */
  @ExceptionHandler({IllegalArgumentException.class, DateTimeException.class})
  public BaseResponse<Void> domainRule(RuntimeException error) {
    return BaseResponse.failure(ResponseCode.PARAM_ERROR.getCode(), error.getMessage());
  }

  @ExceptionHandler(ConcurrentChange.class)
  public BaseResponse<Void> concurrentChange(ConcurrentChange error) {
    return BaseResponse.failure(ResponseCode.VERSION_CONFLICT);
  }

  private String format(FieldError fieldError) {
    String message = Objects.requireNonNullElse(fieldError.getDefaultMessage(), "参数错误");
    return fieldError.getField() + ": " + message;
  }
}
