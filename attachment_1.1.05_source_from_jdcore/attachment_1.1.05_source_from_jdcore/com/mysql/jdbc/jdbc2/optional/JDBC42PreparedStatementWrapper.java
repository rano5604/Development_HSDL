package com.mysql.jdbc.jdbc2.optional;

import com.mysql.jdbc.SQLError;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLType;
























public class JDBC42PreparedStatementWrapper
  extends JDBC4PreparedStatementWrapper
{
  public JDBC42PreparedStatementWrapper(ConnectionWrapper c, MysqlPooledConnection conn, PreparedStatement toWrap)
  {
    super(c, conn, toWrap);
  }
  





  public void setObject(int parameterIndex, Object x, SQLType targetSqlType)
    throws SQLException
  {
    try
    {
      if (wrappedStmt != null) {
        ((PreparedStatement)wrappedStmt).setObject(parameterIndex, x, targetSqlType);
      } else {
        throw SQLError.createSQLException("No operations allowed after statement closed", "S1000", exceptionInterceptor);
      }
    } catch (SQLException sqlEx) {
      checkAndFireConnectionError(sqlEx);
    }
  }
  






  public void setObject(int parameterIndex, Object x, SQLType targetSqlType, int scaleOrLength)
    throws SQLException
  {
    try
    {
      if (wrappedStmt != null) {
        ((PreparedStatement)wrappedStmt).setObject(parameterIndex, x, targetSqlType, scaleOrLength);
      } else {
        throw SQLError.createSQLException("No operations allowed after statement closed", "S1000", exceptionInterceptor);
      }
    } catch (SQLException sqlEx) {
      checkAndFireConnectionError(sqlEx);
    }
  }
}
