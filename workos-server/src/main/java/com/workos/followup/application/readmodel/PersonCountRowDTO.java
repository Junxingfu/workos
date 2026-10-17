package com.workos.followup.application.readmodel;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/** 按等待对象聚合的计数行 */
@Data
public class PersonCountRowDTO implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  /** 等待对象（waiting_for） */
  private String person;

  private Long cnt;
}
