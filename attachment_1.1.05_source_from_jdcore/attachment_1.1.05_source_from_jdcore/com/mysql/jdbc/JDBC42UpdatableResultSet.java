package com.mysql.jdbc;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.SQLType;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.format.DateTimeParseException;
























public class JDBC42UpdatableResultSet
  extends JDBC4UpdatableResultSet
{
  public JDBC42UpdatableResultSet(String catalog, Field[] fields, RowData tuples, MySQLConnection conn, StatementImpl creatorStmt)
    throws SQLException
  {
    super(catalog, fields, tuples, conn, creatorStmt);
  }
  


  private int translateAndCheckSqlType(SQLType sqlType)
    throws SQLException
  {
    return JDBC42Helper.translateAndCheckSqlType(sqlType, getExceptionInterceptor());
  }
  






  public <T> T getObject(int columnIndex, Class<T> type)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (type == null) {
        throw SQLError.createSQLException("Type parameter can not be null", "S1009", getExceptionInterceptor());
      }
      
      if (type.equals(LocalDate.class))
        return type.cast(getDate(columnIndex).toLocalDate());
      if (type.equals(LocalDateTime.class))
        return type.cast(getTimestamp(columnIndex).toLocalDateTime());
      if (type.equals(LocalTime.class))
        return type.cast(getTime(columnIndex).toLocalTime());
      if (type.equals(OffsetDateTime.class)) {
        try {
          return type.cast(OffsetDateTime.parse(getString(columnIndex)));

        }
        catch (DateTimeParseException localDateTimeParseException) {}
      } else if (type.equals(OffsetTime.class)) {
        try {
          return type.cast(OffsetTime.parse(getString(columnIndex)));
        }
        catch (DateTimeParseException localDateTimeParseException1) {}
      }
      

      return super.getObject(columnIndex, type);
    }
  }
  






  public void updateObject(int columnIndex, Object x)
    throws SQLException
  {
    super.updateObject(columnIndex, JDBC42Helper.convertJavaTimeToJavaSql(x));
  }
  







  public void updateObject(int columnIndex, Object x, int scaleOrLength)
    throws SQLException
  {
    super.updateObject(columnIndex, JDBC42Helper.convertJavaTimeToJavaSql(x), scaleOrLength);
  }
  






  public void updateObject(String columnLabel, Object x)
    throws SQLException
  {
    super.updateObject(columnLabel, JDBC42Helper.convertJavaTimeToJavaSql(x));
  }
  







  public void updateObject(String columnLabel, Object x, int scaleOrLength)
    throws SQLException
  {
    super.updateObject(columnLabel, JDBC42Helper.convertJavaTimeToJavaSql(x), scaleOrLength);
  }
  







  public void updateObject(int columnIndex, Object x, SQLType targetSqlType)
    throws SQLException
  {
    super.updateObjectInternal(columnIndex, JDBC42Helper.convertJavaTimeToJavaSql(x), Integer.valueOf(translateAndCheckSqlType(targetSqlType)), 0);
  }
  








  public void updateObject(int columnIndex, Object x, SQLType targetSqlType, int scaleOrLength)
    throws SQLException
  {
    super.updateObjectInternal(columnIndex, JDBC42Helper.convertJavaTimeToJavaSql(x), Integer.valueOf(translateAndCheckSqlType(targetSqlType)), scaleOrLength);
  }
  







  public void updateObject(String columnLabel, Object x, SQLType targetSqlType)
    throws SQLException
  {
    super.updateObjectInternal(findColumn(columnLabel), JDBC42Helper.convertJavaTimeToJavaSql(x), Integer.valueOf(translateAndCheckSqlType(targetSqlType)), 0);
  }
  








  public void updateObject(String columnLabel, Object x, SQLType targetSqlType, int scaleOrLength)
    throws SQLException
  {
    super.updateObjectInternal(findColumn(columnLabel), JDBC42Helper.convertJavaTimeToJavaSql(x), Integer.valueOf(translateAndCheckSqlType(targetSqlType)), scaleOrLength);
  }
}
