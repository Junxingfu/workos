package com.workos.exchange.interfaces.http;

import com.workos.common.enums.ResponseCode;
import com.workos.common.model.BaseResponse;
import com.workos.exchange.application.TaskExchangeService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 表格导出、上传预览和确认导入的 HTTP 入口；业务写入由批量用例统一编排。 */
@RestController
@RequestMapping("/api/exchange/tasks")
@RequiredArgsConstructor
public class TaskExchangeController {
  private final TaskExchangeService service;

  @GetMapping
  public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "false") boolean template)
      throws Exception {
    return ResponseEntity.ok()
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .header("Content-Disposition", "attachment; filename=workos-tasks.xlsx")
        .body(service.export(template));
  }

  @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public BaseResponse<?> preview(@RequestParam("file") MultipartFile file) throws Exception {
    if (file.getOriginalFilename() == null
        || !file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
      throw new IllegalArgumentException("请选择 .xlsx 文件");
    }
    if (file.getSize() > 5 * 1024 * 1024) {
      throw new IllegalArgumentException("文件不能超过5MB");
    }
    try {
      return BaseResponse.success(service.preview(file.getBytes()));
    } catch (NotOfficeXmlFileException | IOException e) {
      throw new IllegalArgumentException("无法读取该文件，请使用有效的 .xlsx 工作簿");
    }
  }

  public record Apply(String token) {}

  @PostMapping("/apply")
  public BaseResponse<?> apply(@RequestBody Apply req) throws Exception {
    return BaseResponse.success(service.apply(req.token()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public BaseResponse<?> invalid(IllegalArgumentException e) {
    return BaseResponse.failure(ResponseCode.PARAM_ERROR, e.getMessage());
  }
}
