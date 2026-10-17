package com.workos.exchange.application;

import java.io.IOException;
import java.util.List;

/** 工作簿的发布语言；应用层只认识列和值，不依赖 POI。 */
public interface WorkbookPort {
  int LIMIT = 2000;
  List<String> HEADERS =
      List.of(
          "事项ID", "版本", "创建标识", "标题", "状态", "优先级", "主项目", "关联项目", "计划日期", "截止时间", "下次跟进时间", "等待对象",
          "描述", "相关人");

  record Row(int number, List<String> cells) {
    public Row {
      cells = List.copyOf(cells);
    }
  }

  List<Row> decode(byte[] bytes) throws IOException;

  byte[] encode(List<List<String>> tasks, List<List<String>> projects, String zone)
      throws IOException;
}
