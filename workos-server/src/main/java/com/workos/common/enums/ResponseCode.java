package com.workos.common.enums;

import lombok.Getter;

/** 业务响应码 */
@Getter
public enum ResponseCode {
  SUCCESS(0, "success"),

  PARAM_ERROR(1001, "参数错误"),
  DATA_NOT_FOUND(1002, "数据不存在"),
  VERSION_CONFLICT(1004, "数据已被其他操作修改，请刷新后重试"),
  COMMON_ERROR(1000, "操作失败"),

  PROJECT_NOT_FOUND(2001, "项目不存在"),
  PROJECT_ARCHIVED(2002, "项目已归档"),
  PROJECT_CODE_DUPLICATE(2003, "项目编码重复"),
  PROJECT_NAME_DUPLICATE(2004, "项目名称重复"),

  ITEM_NOT_FOUND(3001, "事项不存在"),
  ITEM_INVALID_TRANSITION(3002, "当前状态不允许执行该操作"),
  ITEM_TYPE_CAPABILITY_DENIED(3003, "当前类型不支持该状态"),
  WAITING_FOR_REQUIRED(3004, "进入等待状态必须填写等待对象"),
  ITEM_DELETE_NOT_ALLOWED(3005, "已整理过的事项不能删除，请使用取消"),

  DAILY_NOT_FOUND(4001, "日报不存在"),
  DAILY_GENERATE_FAILED(4002, "日报生成失败"),
  DAILY_VERSION_CONFLICT(4003, "日报已被修改，请刷新后重试"),

  REMINDER_NOT_FOUND(5001, "提醒不存在"),

  UNAUTHORIZED(6001, "未登录或登录已失效"),
  SYSTEM_ERROR(9999, "系统异常");

  private final Integer code;

  private final String description;

  ResponseCode(Integer code, String description) {
    this.code = code;
    this.description = description;
  }
}
