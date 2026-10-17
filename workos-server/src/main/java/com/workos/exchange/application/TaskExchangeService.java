package com.workos.exchange.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workos.common.enums.ResponseCode;
import com.workos.common.exception.BizException;
import com.workos.common.support.TimeZoneHolder;
import com.workos.item.application.ItemCommands;
import com.workos.item.application.ItemEditingService;
import com.workos.item.application.ItemLifecycleService;
import com.workos.item.domain.ItemDetails;
import com.workos.item.domain.ItemPriority;
import com.workos.item.domain.ItemRevision;
import com.workos.item.domain.ItemStatus;
import com.workos.item.domain.ItemType;
import com.workos.item.domain.WorkItem;
import com.workos.item.domain.WorkItemRepository;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** 用例：解析→预览→一次性事务提交。Excel 不绕过事项服务和聚合。 */
@Service
@RequiredArgsConstructor
public class TaskExchangeService {
  private final ItemEditingService editing;
  private final ItemLifecycleService lifecycle;
  private final WorkbookPort workbooks;
  private final ImportBackup backup;
  private final ExchangeStore store;
  private final TimeZoneHolder clock;
  private final WorkItemRepository repository;
  private final TransactionTemplate transactions;
  private final ObjectMapper json;

  private static final Map<ItemStatus, String> STATUS =
      Map.of(
          ItemStatus.INBOX,
          "待整理",
          ItemStatus.TODO,
          "待办",
          ItemStatus.DOING,
          "进行中",
          ItemStatus.WAITING,
          "等待中",
          ItemStatus.DONE,
          "已完成",
          ItemStatus.CANCELLED,
          "已取消");

  public record Change(String field, String before, String after) {}

  public record PreviewRow(
      int row, String action, String title, List<Change> changes, String error) {}

  public record Preview(String token, List<PreviewRow> rows, boolean canApply, String message) {}

  public record Result(int created, int updated, int unchanged, List<String> itemIds) {}

  private record Stage(List<WorkbookPort.Row> rows, String fingerprint, Instant expires) {}

  private record Command(
      int row, Long id, Integer version, List<String> values, List<Change> changes) {}

  private final Map<String, Stage> stages = new HashMap<>();

  private static String str(Object value) {
    return value == null ? "" : value.toString();
  }

  private String time(Instant value) {
    return value == null ? "" : value.toString();
  }

  private List<String> values(TaskSnapshot item) {
    String related =
        store.links(List.of(item.getId())).stream()
            .map(link -> link.getProjectId().toString())
            .sorted()
            .collect(Collectors.joining("|"));
    return values(item, related);
  }

  private List<String> values(TaskSnapshot item, String related) {
    return List.of(
        str(item.getId()),
        str(item.getVersion()),
        str(item.getCreatedAt()),
        item.getTitle(),
        STATUS.get(item.getStatus()),
        str(item.getPriority()),
        str(item.getProjectId()),
        related,
        str(item.getPlannedDate()),
        time(item.getDueAt()),
        time(item.getNextFollowUpAt()),
        str(item.getWaitingFor()),
        str(item.getDescription()),
        str(item.getRelatedPerson()));
  }

  public byte[] export(boolean template) throws Exception {
    List<TaskSnapshot> all = store.tasks();
    if (!template && all.size() > WorkbookPort.LIMIT) {
      throw new IllegalArgumentException("任务超过2000条，暂不支持一次导出全部；请先使用数据库备份保存完整数据");
    }
    List<List<String>> refs =
        store.projects().stream()
            .map(p -> List.of(str(p.getId()), p.getName(), p.getStatus()))
            .toList();
    Map<Long, List<String>> relatedByItem = new HashMap<>();
    if (!template && !all.isEmpty()) {
      for (ExchangeStore.Link link : store.links(all.stream().map(TaskSnapshot::getId).toList())) {
        relatedByItem
            .computeIfAbsent(link.getItemId(), key -> new ArrayList<>())
            .add(link.getProjectId().toString());
      }
    }
    List<List<String>> rows =
        template
            ? List.<List<String>>of()
            : all.stream()
                .map(
                    item ->
                        values(
                            item,
                            relatedByItem.getOrDefault(item.getId(), List.of()).stream()
                                .sorted()
                                .collect(Collectors.joining("|"))))
                .toList();
    return workbooks.encode(rows, refs, clock.zone().toString());
  }

  /** 解析并校验所有行，展示差异；有效预览只暂存在内存，十五分钟后失效。 */
  public synchronized Preview preview(byte[] bytes) throws Exception {
    List<WorkbookPort.Row> rows = workbooks.decode(bytes);
    String fingerprint =
        HexFormat.of()
            .formatHex(
                MessageDigest.getInstance("SHA-256")
                    .digest(
                        json.writeValueAsBytes(
                            rows.stream().map(WorkbookPort.Row::cells).toList())));
    if (completed(fingerprint) != null) {
      return new Preview(null, List.of(), false, "这份表格已成功导入，无需重复提交");
    }
    List<PreviewRow> results = new ArrayList<>();
    Set<Long> seen = new HashSet<>();
    boolean valid = true;
    for (WorkbookPort.Row row : rows) {
      try {
        Command command = command(row);
        if (command.id() != null && !seen.add(command.id())) {
          throw new IllegalArgumentException("同一事项ID重复出现");
        }
        results.add(
            new PreviewRow(
                row.number(),
                command.id() == null ? "新增" : command.changes().isEmpty() ? "未变化" : "修改",
                command.values().get(3),
                command.changes(),
                null));
      } catch (RuntimeException e) {
        valid = false;
        results.add(
            new PreviewRow(row.number(), "错误", row.cells().get(3), List.of(), e.getMessage()));
      }
    }
    stages.entrySet().removeIf(e -> e.getValue().expires().isBefore(clock.now()));
    if (stages.size() >= 5) {
      throw new IllegalArgumentException("预览过多，请稍后重试");
    }
    String token = valid ? UUID.randomUUID().toString() : null;
    if (valid) {
      stages.put(token, new Stage(rows, fingerprint, clock.now().plusSeconds(900)));
    }
    return new Preview(
        token,
        List.copyOf(results),
        valid,
        valid ? "请核对变更后提交。预览15分钟内有效。" : "请修正错误行后重新选择文件，当前数据未修改。");
  }

  /** 再次检查版本并备份，以单事务提交整批事项与成功指纹；失败整批回滚。 */
  public synchronized Result apply(String token) throws Exception {
    Stage stage = stages.get(token);
    if (stage == null || stage.expires().isBefore(clock.now())) {
      throw new IllegalArgumentException("预览已过期，请重新选择文件预览");
    }
    Result previous = completed(stage.fingerprint());
    if (previous != null) {
      return previous;
    }
    // 先验证所有行；备份在事务外完成，避免 SQLite 单连接互相等待。
    for (WorkbookPort.Row row : stage.rows()) {
      command(row);
    }
    backup.beforeImport();
    return transactions.execute(
        status -> {
          Result done = completed(stage.fingerprint());
          if (done != null) {
            return done;
          }
          List<Command> commands = stage.rows().stream().map(this::command).toList();
          int created = 0, updated = 0, unchanged = 0;
          List<String> ids = new ArrayList<>();
          for (Command command : commands) {
            if (command.id() != null && command.changes().isEmpty()) {
              unchanged++;
              continue;
            }
            Long id = execute(command);
            ids.add(id.toString());
            if (command.id() == null) {
              created++;
            } else {
              updated++;
            }
          }
          Result result = new Result(created, updated, unchanged, List.copyOf(ids));
          try {
            store.record(stage.fingerprint(), json.writeValueAsString(result), clock.now());
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }
          return result;
        });
  }

  private Result completed(String fingerprint) {
    String record = store.completed(fingerprint);
    if (record == null) {
      return null;
    }
    try {
      return json.readValue(record, Result.class);
    } catch (Exception e) {
      throw new IllegalStateException("导入记录读取失败", e);
    }
  }

  /** 将一行外部表格转成规范化变更，预演领域规则；此阶段不保存业务数据。 */
  private Command command(WorkbookPort.Row row) {
    List<String> raw = row.cells();
    Long id = raw.get(0).isBlank() ? null : positiveId(raw.get(0));
    TaskSnapshot item = id == null ? null : store.task(id);
    if (id != null && (item == null || item.getItemType() != ItemType.TASK)) {
      throw new IllegalArgumentException("事项不存在或不是任务");
    }
    if (id == null && (!raw.get(1).isBlank() || !raw.get(2).isBlank())) {
      throw new IllegalArgumentException("新增行的版本和创建标识须留空");
    }
    if (item != null
        && (!str(item.getVersion()).equals(raw.get(1))
            || !str(item.getCreatedAt()).equals(raw.get(2)))) {
      throw new IllegalArgumentException("版本或创建标识不匹配，请重新导出；不能覆盖其他工作区的任务");
    }
    List<String> old = item == null ? new ArrayList<>(Collections.nCopies(14, "")) : values(item);
    List<String> values = new ArrayList<>(old);
    for (int c = 3; c < 14; c++) {
      if (!raw.get(c).isBlank()) {
        values.set(c, raw.get(c).equals("[清空]") ? "" : raw.get(c));
      }
    }
    if (item == null) {
      if (values.get(4).isEmpty()) {
        values.set(4, "待办");
      }
      if (values.get(5).isEmpty()) {
        values.set(5, "P2");
      }
    }
    if (values.get(3).isBlank() || values.get(3).length() > 500) {
      throw new IllegalArgumentException("标题必填且最多500字");
    }
    if (values.get(12).length() > 20000) {
      throw new IllegalArgumentException("描述最多20000字");
    }
    if (values.get(11).length() > 500 || values.get(13).length() > 500) {
      throw new IllegalArgumentException("等待对象/相关人最多500字");
    }
    ItemStatus target = parseStatus(values.get(4));
    values.set(4, STATUS.get(target));
    try {
      ItemPriority.valueOf(values.get(5));
    } catch (Exception e) {
      throw new IllegalArgumentException("优先级须为 P0/P1/P2/P3");
    }
    values.set(6, resolveProject(values.get(6)));
    Set<String> related = new TreeSet<>();
    if (!values.get(7).isBlank()) {
      for (String p : values.get(7).split("\\|")) {
        related.add(resolveProject(p.trim()));
      }
    }
    related.remove(values.get(6));
    related.remove("");
    if (related.size() > 20 || (!related.isEmpty() && values.get(6).isBlank())) {
      throw new IllegalArgumentException("关联项目最多20个，且必须有主项目");
    }
    values.set(7, String.join("|", related));
    if (!values.get(8).isBlank()) {
      try {
        values.set(8, LocalDate.parse(values.get(8)).toString());
      } catch (Exception e) {
        throw new IllegalArgumentException("计划日期请填 YYYY-MM-DD");
      }
    }
    for (int c = 9; c <= 10; c++) {
      if (!values.get(c).isBlank()) {
        values.set(c, parseTime(values.get(c)).toString());
      }
    }
    if (target == ItemStatus.WAITING && values.get(11).isBlank()) {
      throw new IllegalArgumentException("等待中任务必须填写等待对象");
    }
    WorkItem aggregate =
        item == null
            ? new WorkItem(
                new WorkItem.State(
                    0,
                    0,
                    ItemType.TASK,
                    ItemStatus.TODO,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null))
            : repository.get(id);
    if (item == null && target == ItemStatus.INBOX) {
      aggregate =
          new WorkItem(
              new WorkItem.State(
                  0,
                  0,
                  ItemType.TASK,
                  ItemStatus.INBOX,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  null));
    }
    if (target != aggregate.state().status()) {
      aggregate.transition(
          target, values.get(11), null, instant(values.get(10)), "Excel 批量维护", clock.now());
    }
    if (item != null && item.getStatus() == ItemStatus.WAITING && target != ItemStatus.WAITING) {
      values.set(11, "");
      values.set(10, "");
    }
    if (!values.get(8).isBlank() && !values.get(8).equals(old.get(8))) {
      aggregate.plan(LocalDate.parse(values.get(8)), clock.today(), 1, clock.now());
    }
    List<Change> changes = new ArrayList<>();
    for (int c = 3; c < 14; c++) {
      if (!values.get(c).equals(old.get(c))) {
        changes.add(new Change(WorkbookPort.HEADERS.get(c), old.get(c), values.get(c)));
      }
    }
    return new Command(
        row.number(),
        id,
        item == null ? null : item.getVersion(),
        List.copyOf(values),
        List.copyOf(changes));
  }

  /** 各行复用普通事项命令；外层事务负责整批原子提交。 */
  private Long execute(Command command) {
    List<String> values = command.values();
    ItemStatus target = parseStatus(values.get(4));
    Long id = command.id();
    if (id == null) {
      id =
          editing.create(
              new ItemDetails(values.get(3), null, null, null, null, null, null),
              ItemType.TASK,
              target == ItemStatus.INBOX ? ItemStatus.INBOX : ItemStatus.TODO,
              null,
              null,
              null);
    }
    WorkItem.State current = repository.get(id).state();
    if (command.id() != null && current.version() != command.version()) {
      throw new BizException(ResponseCode.VERSION_CONFLICT);
    }
    // 离开等待先执行流转，再清空等待字段；不能产生“等待中但没有等待对象”的中间聚合。
    if (current.status() == ItemStatus.WAITING && target != ItemStatus.WAITING) {
      lifecycle.transition(
          new ItemCommands.Transition(
              id, current.version(), target, null, null, null, "Excel 批量维护"));
      current = repository.get(id).state();
    }
    editing.revise(
        id,
        current.version(),
        new ItemRevision(
            values.get(3),
            values.get(12),
            values.get(6).isBlank() ? null : Long.valueOf(values.get(6)),
            values.get(7).isBlank()
                ? List.of()
                : Arrays.stream(values.get(7).split("\\|")).map(Long::valueOf).toList(),
            values.get(6).isBlank(),
            null,
            ItemPriority.valueOf(values.get(5)),
            null,
            instant(values.get(9)),
            values.get(9).isBlank(),
            values.get(11),
            instant(values.get(10)),
            values.get(10).isBlank(),
            values.get(13)));
    current = repository.get(id).state();
    if (current.status() != target) {
      lifecycle.transition(
          new ItemCommands.Transition(
              id,
              current.version(),
              target,
              values.get(11),
              null,
              instant(values.get(10)),
              "Excel 批量维护"));
    }
    current = repository.get(id).state();
    if (!str(current.plannedDate()).equals(values.get(8))) {
      if (values.get(8).isBlank()) {
        lifecycle.unplan(id);
      } else {
        lifecycle.plan(
            new ItemCommands.Plan(id, current.version(), LocalDate.parse(values.get(8))));
      }
    }
    return id;
  }

  private String resolveProject(String value) {
    if (value.isBlank()) {
      return "";
    }
    List<ExchangeStore.ProjectRef> all = store.projects();
    Optional<ExchangeStore.ProjectRef> found =
        all.stream().filter(p -> p.getId().toString().equals(value)).findFirst();
    if (found.isPresent()) {
      return value;
    }
    List<ExchangeStore.ProjectRef> matches =
        all.stream().filter(p -> p.getName().equals(value)).toList();
    if (matches.size() != 1) {
      throw new IllegalArgumentException("项目不存在或名称重复：" + value + "；请参考“项目参考”表");
    }
    return matches.get(0).getId().toString();
  }

  private long positiveId(String s) {
    try {
      long id = Long.parseLong(s);
      if (id > 0) {
        return id;
      }
    } catch (Exception ignored) {
    }
    throw new IllegalArgumentException("事项ID必须是完整整数，请保留文本格式");
  }

  private ItemStatus parseStatus(String value) {
    for (Map.Entry<ItemStatus, String> e : STATUS.entrySet()) {
      if (e.getValue().equals(value) || e.getKey().name().equals(value)) {
        return e.getKey();
      }
    }
    throw new IllegalArgumentException("状态无效：" + value);
  }

  private Instant instant(String value) {
    return value.isBlank() ? null : Instant.parse(value);
  }

  private Instant parseTime(String value) {
    try {
      return Instant.parse(value);
    } catch (Exception ignored) {
    }
    try {
      return LocalDateTime.parse(value.replace(' ', 'T')).atZone(clock.zone()).toInstant();
    } catch (Exception ignored) {
    }
    try {
      return LocalDate.parse(value).atStartOfDay(clock.zone()).toInstant();
    } catch (Exception ignored) {
    }
    throw new IllegalArgumentException("时间格式应为 YYYY-MM-DD HH:mm 或 ISO 时间");
  }
}
