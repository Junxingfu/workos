package com.workos.exchange.infrastructure;

import com.workos.exchange.application.WorkbookPort;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Component;

/** POI 文件格式适配器，业务用例可替换此实现而不改变聚合。 */
@Component
public class ExcelWorkbookAdapter implements WorkbookPort {
  public List<WorkbookPort.Row> decode(byte[] bytes) throws IOException {
    return TaskWorkbook.read(bytes).stream()
        .map(r -> new WorkbookPort.Row(r.number(), r.cells()))
        .toList();
  }

  public byte[] encode(List<List<String>> tasks, List<List<String>> projects, String zone)
      throws IOException {
    return TaskWorkbook.write(tasks, projects, zone);
  }
}
