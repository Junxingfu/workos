package com.workos.project.domain;

/** 项目状态：归档是业务状态，不是物理删除 */
public enum ProjectStatus {

  /** 进行中 */
  ACTIVE,

  /** 已归档 */
  ARCHIVED
}
