package com.workos.shared.domain;

/** 聚合版本冲突；接口层决定如何呈现，领域不依赖 HTTP 响应码。 */
public final class ConcurrentChange extends RuntimeException {
  public ConcurrentChange(String message) {
    super(message);
  }
}
