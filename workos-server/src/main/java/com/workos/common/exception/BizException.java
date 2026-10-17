package com.workos.common.exception;

import com.workos.common.enums.ResponseCode;
import java.io.Serial;

/** 业务异常 */
public class BizException extends BaseException {

  @Serial private static final long serialVersionUID = 1L;

  public BizException(ResponseCode responseCode) {
    super(responseCode);
  }

  public BizException(ResponseCode responseCode, String message) {
    super(responseCode, message);
  }

  public BizException(Integer code, String message) {
    super(code, message);
  }
}
