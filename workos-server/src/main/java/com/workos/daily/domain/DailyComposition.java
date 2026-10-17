package com.workos.daily.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/** 日报生成策略：只归集记录当时的主项目，关联项目不会复制工作成果。 */
public final class DailyComposition {
  private DailyComposition() {}

  public record Entry(String projectName, String content) {}

  public record Source(List<Entry> records, List<String> completedTitles) {
    public Source {
      records = List.copyOf(records);
      completedTitles = List.copyOf(completedTitles);
    }
  }

  public static String compose(LocalDate day, Source source) {
    StringBuilder text = new StringBuilder("# " + day + " 工作记录\n");
    TreeMap<String, List<String>> groups =
        new TreeMap<String, List<String>>(
            (a, b) -> {
              if (a.equals(b)) {
                return 0;
              }
              if (a.equals("未归属")) {
                return 1;
              }
              if (b.equals("未归属")) {
                return -1;
              }
              return a.compareTo(b);
            });
    for (DailyComposition.Entry entry : source.records()) {
      groups
          .computeIfAbsent(
              entry.projectName() == null ? "未归属" : entry.projectName(), key -> new ArrayList<>())
          .add(entry.content());
    }
    groups.forEach(
        (project, entries) -> {
          text.append("\n## ").append(project).append("\n\n");
          entries.forEach(entry -> text.append("- ").append(entry).append("\n"));
        });
    if (!source.completedTitles().isEmpty()) {
      text.append("\n## 完成事项\n\n");
      source.completedTitles().forEach(title -> text.append("- ").append(title).append("\n"));
    }
    return text.toString();
  }
}
