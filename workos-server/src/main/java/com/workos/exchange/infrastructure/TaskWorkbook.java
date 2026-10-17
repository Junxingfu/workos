package com.workos.exchange.infrastructure;

import com.workos.exchange.application.WorkbookPort;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipInputStream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** Excel 是输入适配器：公式不执行，ID 用文本，业务规则仍由应用和领域负责。 */
public final class TaskWorkbook {
  public static final List<String> HEADERS = WorkbookPort.HEADERS;
  public static final int LIMIT = WorkbookPort.LIMIT;

  public record Row(int number, List<String> cells) {}

  public static List<Row> read(byte[] bytes) throws IOException {
    if (bytes.length > 5 * 1024 * 1024) {
      throw new IllegalArgumentException("文件不能超过 5 MB");
    }
    // 压缩文件大小不能代表展开后的内存占用，读取工作簿前先做有界扫描。
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
      byte[] buffer = new byte[8192];
      long expanded = 0;
      while (zip.getNextEntry() != null) {
        int count;
        while ((count = zip.read(buffer)) != -1) {
          expanded += count;
          if (expanded > 20L * 1024 * 1024) {
            throw new IllegalArgumentException("表格展开内容超过20MB，请减少行数或移除无关工作表");
          }
        }
      }
    }
    try (XSSFWorkbook book = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
      XSSFSheet sheet = book.getSheet("任务");
      if (sheet == null || sheet.getRow(0) == null) {
        throw new IllegalArgumentException("找不到“任务”工作表，请使用导出表或模板");
      }
      for (int i = 0; i < HEADERS.size(); i++) {
        if (!HEADERS.get(i).equals(text(sheet.getRow(0).getCell(i)))) {
          throw new IllegalArgumentException("表头不匹配：第 " + (i + 1) + " 列应为“" + HEADERS.get(i) + "”");
        }
      }
      if (sheet.getLastRowNum() > LIMIT) {
        throw new IllegalArgumentException("单次最多 2000 行，请分批导入");
      }
      List<Row> result = new ArrayList<>();
      for (int r = 1; r <= sheet.getLastRowNum(); r++) {
        XSSFRow row = sheet.getRow(r);
        if (row == null) {
          continue;
        }
        List<String> values = new ArrayList<>();
        for (int c = 0; c < HEADERS.size(); c++) {
          values.add(text(row.getCell(c)));
        }
        if (values.stream().allMatch(String::isBlank)) {
          continue;
        }
        result.add(new Row(r + 1, List.copyOf(values)));
      }
      if (result.isEmpty()) {
        throw new IllegalArgumentException("表格没有任务数据");
      }
      return List.copyOf(result);
    }
  }

  private static String text(Cell cell) {
    if (cell == null) {
      return "";
    }
    if (cell.getCellType() == CellType.FORMULA) {
      throw new IllegalArgumentException("第 " + (cell.getRowIndex() + 1) + " 行含公式，请粘贴为值后再导入");
    }
    if (cell.getCellType() == CellType.ERROR) {
      throw new IllegalArgumentException("表格含错误单元格");
    }
    if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
      LocalDateTime value = cell.getLocalDateTimeCellValue();
      return value.toLocalTime().equals(LocalTime.MIDNIGHT)
          ? value.toLocalDate().toString()
          : value.toString();
    }
    return new DataFormatter(Locale.ROOT).formatCellValue(cell).trim();
  }

  public static byte[] write(List<List<String>> rows, List<List<String>> projects, String timezone)
      throws IOException {
    try (XSSFWorkbook book = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      XSSFSheet sheet = book.createSheet("任务");
      XSSFCellStyle textStyle = book.createCellStyle();
      textStyle.setDataFormat(book.createDataFormat().getFormat("@"));
      XSSFCellStyle header = book.createCellStyle();
      XSSFFont font = book.createFont();
      font.setBold(true);
      header.setFont(font);
      header.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
      header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      for (int c = 0; c < HEADERS.size(); c++) {
        sheet.setDefaultColumnStyle(c, textStyle);
        sheet.setColumnWidth(c, (c == 3 || c == 12 ? 48 : c == 2 ? 31 : 22) * 256);
      }
      List<List<String>> all = new ArrayList<>();
      all.add(HEADERS);
      all.addAll(rows);
      for (int r = 0; r < all.size(); r++) {
        XSSFRow row = sheet.createRow(r);
        for (int c = 0; c < HEADERS.size(); c++) {
          XSSFCell cell = row.createCell(c, CellType.STRING);
          cell.setCellValue(all.get(r).get(c));
          cell.setCellStyle(r == 0 ? header : textStyle);
        }
      }
      sheet.createFreezePane(4, 1);
      sheet.setAutoFilter(new CellRangeAddress(0, Math.max(1, rows.size()), 0, HEADERS.size() - 1));
      XSSFSheet guide = book.createSheet("填写说明");
      String[] lines = {
        "新增：事项ID、版本、创建标识留空。标题必填，状态默认待办。",
        "修改：保留导出的前三列；空白表示不修改。[清空] 用于清空可选字段。",
        "删除一行不会删除任务。导入前先预览；任意错误或版本冲突会阻止整批提交。",
        "状态：待整理、待办、进行中、等待中、已完成、已取消；修改遵循界面相同的状态流转。",
        "优先级：P0、P1、P2、P3。主项目填项目名称或ID；关联项目用 | 分隔，必须有主项目。",
        "计划日期 YYYY-MM-DD；时间 YYYY-MM-DD HH:mm 或带时区的 ISO 时间。工作区时区：" + timezone,
        "进入等待需填写等待对象；结束等待清空本段等待和跟进时间。",
        "只支持 .xlsx，最多2000行/5MB；公式需粘贴为值。已提交的相同表格不会重复导入。",
        "该表用于任务批量维护，工作记录、日报、项目现状请用数据备份保护。"
      };
      for (int i = 0; i < lines.length; i++) {
        guide.createRow(i).createCell(0).setCellValue(lines[i]);
      }
      guide.setColumnWidth(0, 120 * 256);
      XSSFSheet reference = book.createSheet("项目参考");
      XSSFRow h = reference.createRow(0);
      h.createCell(0).setCellValue("项目ID");
      h.createCell(1).setCellValue("项目名称");
      h.createCell(2).setCellValue("状态");
      for (int i = 0; i < projects.size(); i++) {
        XSSFRow row = reference.createRow(i + 1);
        for (int c = 0; c < 3; c++) {
          row.createCell(c, CellType.STRING).setCellValue(projects.get(i).get(c));
        }
      }
      reference.setColumnWidth(0, 20 * 256);
      reference.setColumnWidth(1, 40 * 256);
      reference.setColumnWidth(2, 20 * 256);
      book.write(out);
      return out.toByteArray();
    }
  }
}
