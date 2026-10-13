package com.workos.desktop.types;

import org.apache.ibatis.type.*;
import java.sql.*;
import java.time.LocalDate;

@MappedTypes(LocalDate.class)
public class SqliteLocalDateHandler extends BaseTypeHandler<LocalDate> {
    private LocalDate parse(String v) { return v == null ? null : LocalDate.parse(v); }
    @Override public void setNonNullParameter(PreparedStatement s, int i, LocalDate value, JdbcType type) throws SQLException { s.setString(i, value.toString()); }
    @Override public LocalDate getNullableResult(ResultSet r, String c) throws SQLException { return parse(r.getString(c)); }
    @Override public LocalDate getNullableResult(ResultSet r, int c) throws SQLException { return parse(r.getString(c)); }
    @Override public LocalDate getNullableResult(CallableStatement r, int c) throws SQLException { return parse(r.getString(c)); }
}
