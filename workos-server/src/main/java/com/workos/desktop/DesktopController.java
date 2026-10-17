package com.workos.desktop;

import com.workos.common.enums.ResponseCode;
import com.workos.common.model.BaseResponse;
import java.sql.SQLException;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

/** 桌面数据管理 HTTP 入口；只在 desktop profile 启用，委托存储适配器执行备份和恢复。 */
@Controller
@Profile("desktop")
public class DesktopController {
  private final DesktopStorage storage;

  public DesktopController(DesktopStorage storage) {
    this.storage = storage;
  }

  @GetMapping({
    "/today",
    "/inbox",
    "/items",
    "/projects",
    "/projects/{id}",
    "/daily",
    "/desktop-data",
    "/exchange",
    "/notifications",
    "/focus"
  })
  public String app() {
    return "forward:/index.html";
  }

  /** 浏览器访问桌面构建时回退为普通页面，原生应用会先拦截并打开浮窗。 */
  @GetMapping("/desktop/window/focus")
  public String focusFallback() {
    return "redirect:/focus";
  }

  @GetMapping("/desktop/window/main")
  public String mainFallback() {
    return "redirect:/today";
  }

  @GetMapping("/api/desktop/storage")
  @ResponseBody
  public BaseResponse<?> info() throws Exception {
    return BaseResponse.success(storage.info());
  }

  @PostMapping("/api/desktop/backup")
  @ResponseBody
  public BaseResponse<?> backup() throws Exception {
    return BaseResponse.success(storage.backup("manual"));
  }

  @PostMapping("/api/desktop/import")
  @ResponseBody
  public BaseResponse<?> migrate(@RequestBody ImportRequest request) throws Exception {
    return BaseResponse.success(storage.importPostgres(request.database()));
  }

  @PostMapping("/api/desktop/restore")
  @ResponseBody
  public BaseResponse<?> restore(@RequestBody RestoreRequest request) throws Exception {
    return BaseResponse.success(storage.restore(request.name()));
  }

  public record ImportRequest(String database) {}

  public record RestoreRequest(String name) {}

  @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
  @ResponseBody
  public BaseResponse<?> invalid(RuntimeException error) {
    return BaseResponse.failure(ResponseCode.PARAM_ERROR.getCode(), error.getMessage());
  }

  @ExceptionHandler(SQLException.class)
  @ResponseBody
  public BaseResponse<?> databaseError(SQLException error) {
    LoggerFactory.getLogger(getClass()).warn("桌面数据操作失败", error);
    return BaseResponse.failure(
        ResponseCode.SYSTEM_ERROR.getCode(), "数据操作未完成，当前数据已保留。导入旧数据时请确认原数据库已启动；其他问题可从菜单查看日志后重试。");
  }
}
