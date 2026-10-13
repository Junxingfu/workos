package com.workos.desktop.types;

import org.apache.ibatis.type.*;
import java.sql.*;
import java.time.Instant;

/** SQLite 时间点统一存 epoch 毫秒，避免字符串精度/时区导致比较错误。 */
@MappedTypes(Instant.class)
public class SqliteInstantHandler extends BaseTypeHandler<Instant> {
    @Override public void setNonNullParameter(PreparedStatement s, int i, Instant value, JdbcType type) throws SQLException { s.setLong(i, value.toEpochMilli()); }
    @Override public Instant getNullableResult(ResultSet r, String c) throws SQLException { long v = r.getLong(c); return r.wasNull() ? null : Instant.ofEpochMilli(v); }
    @Override public Instant getNullableResult(ResultSet r, int c) throws SQLException { long v = r.getLong(c); return r.wasNull() ? null : Instant.ofEpochMilli(v); }
    @Override public Instant getNullableResult(CallableStatement r, int c) throws SQLException { long v = r.getLong(c); return r.wasNull() ? null : Instant.ofEpochMilli(v); }
}
