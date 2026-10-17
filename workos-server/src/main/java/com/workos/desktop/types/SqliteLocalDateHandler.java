package com.workos.desktop.types;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

/** SQLite DATE 类型适配；数据库使用 ISO 日期文本，Java 使用 LocalDate。 */
@MappedTypes(LocalDate.class)
public class SqliteLocalDateHandler extends BaseTypeHandler<LocalDate> {
  private LocalDate parse(String v) {
    return v == null ? null : LocalDate.parse(v);
  }

  @Override
  public void setNonNullParameter(PreparedStatement s, int i, LocalDate value, JdbcType type)
      throws SQLException {
    s.setString(i, value.toString());
  }

  @Override
  public LocalDate getNullableResult(ResultSet r, String c) throws SQLException {
    return parse(r.getString(c));
  }

  @Override
  public LocalDate getNullableResult(ResultSet r, int c) throws SQLException {
    return parse(r.getString(c));
  }

  @Override
  public LocalDate getNullableResult(CallableStatement r, int c) throws SQLException {
    return parse(r.getString(c));
  }
}
