package com.workos.desktop;

import com.workos.common.model.BaseResponse;
import com.workos.common.enums.ResponseCode;
import java.sql.SQLException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller @Profile("desktop")
public class DesktopController {
    private final DesktopStorage storage;
    public DesktopController(DesktopStorage storage) { this.storage = storage; }
    @GetMapping({"/today", "/inbox", "/items", "/projects", "/projects/{id}", "/daily", "/desktop-data"})
    public String app() { return "forward:/index.html"; }
    @GetMapping("/api/desktop/storage") @ResponseBody
    public BaseResponse<?> info() throws Exception { return BaseResponse.success(storage.info()); }
    @PostMapping("/api/desktop/backup") @ResponseBody
    public BaseResponse<?> backup() throws Exception { return BaseResponse.success(storage.backup("manual")); }
    @PostMapping("/api/desktop/import") @ResponseBody
    public BaseResponse<?> migrate(@RequestBody ImportRequest request) throws Exception { return BaseResponse.success(storage.importPostgres(request.database())); }
    @PostMapping("/api/desktop/restore") @ResponseBody
    public BaseResponse<?> restore(@RequestBody RestoreRequest request) throws Exception { return BaseResponse.success(storage.restore(request.name())); }
    public record ImportRequest(String database) {}
    public record RestoreRequest(String name) {}

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class}) @ResponseBody
    public BaseResponse<?> invalid(RuntimeException error) {
        return BaseResponse.failure(ResponseCode.PARAM_ERROR.getCode(), error.getMessage());
    }
    @ExceptionHandler(SQLException.class) @ResponseBody
    public BaseResponse<?> databaseError(SQLException error) {
        org.slf4j.LoggerFactory.getLogger(getClass()).warn("桌面数据操作失败", error);
        return BaseResponse.failure(ResponseCode.SYSTEM_ERROR.getCode(), "数据操作未完成，当前数据已保留。导入旧数据时请确认原数据库已启动；其他问题可从菜单查看日志后重试。");
    }
}
