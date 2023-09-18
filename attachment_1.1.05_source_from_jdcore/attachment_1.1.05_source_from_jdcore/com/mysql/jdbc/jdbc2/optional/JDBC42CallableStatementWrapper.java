package com.mysql.jdbc.jdbc2.optional;

import com.mysql.jdbc.SQLError;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.SQLType;
























public class JDBC42CallableStatementWrapper
  extends JDBC4CallableStatementWrapper
{
  public JDBC42CallableStatementWrapper(ConnectionWrapper c, MysqlPooledConnection conn, CallableStatement toWrap)
  {
    super(c, conn, toWrap);
  }
  




  public void registerOutParameter(int parameterIndex, SQLType sqlType)
    throws SQLException
  {
    try
    {
      if (wrappedStmt != null) {
        ((CallableStatement)wrappedStmt).registerOutParameter(parameterIndex, sqlType);
      } else {
        throw SQLError.createSQLException("No operations allowed after statement closed", "S1000", exceptionInterceptor);
      }
    } catch (SQLException sqlEx) {
      checkAndFireConnectionError(sqlEx);
    }
  }
  





  public void registerOutParameter(int parameterIndex, SQLType sqlType, int scale)
    throws SQLException
  {
    try
    {
      if (wrappedStmt != null) {
        ((CallableStatement)wrappedStmt).registerOutParameter(parameterIndex, sqlType, scale);
      } else {
        throw SQLError.createSQLException("No operations allowed after statement closed", "S1000", exceptionInterceptor);
      }
    } catch (SQLException sqlEx) {
      checkAndFireConnectionError(sqlEx);
    }
  }
  





  public void registerOutParameter(int parameterIndex, SQLType sqlType, String typeName)
    throws SQLException
  {
    try
    {
      if (wrappedStmt != null) {
        ((CallableStatement)wrappedStmt).registerOutParameter(parameterIndex, sqlType, typeName);
      } else {
        throw SQLError.createSQLException("No operations allowed after statement closed", "S1000", exceptionInterceptor);
      }
    } catch (SQLException sqlEx) {
      checkAndFireConnectionError(sqlEx);
    }
  }
  




  public void registerOutParameter(String parameterName, SQLType sqlType)
    throws SQLException
  {
    try
    {
      if (wrappedStmt != null) {
        ((CallableStatement)wrappedStmt).registerOutParameter(parameterName, sqlType);
      } else {
        throw SQLError.createSQLException("No operations allowed after statement closed", "S1000", exceptionInterceptor);
      }
    } catch (SQLException sqlEx) {
      checkAndFireConnectionError(sqlEx);
    }
  }
  





  public void registerOutParameter(String parameterName, SQLType sqlType, int scale)
    throws SQLException
  {
    try
    {
      if (wrappedStmt != null) {
        ((CallableStatement)wrappedStmt).registerOutParameter(parameterName, sqlType, scale);
      } else {
        throw SQLError.createSQLException("No operations allowed after statement closed", "S1000", exceptionInterceptor);
      }
    } catch (SQLException sqlEx) {
      checkAndFireConnectionError(sqlEx);
    }
  }
  





  public void registerOutParameter(String parameterName, SQLType sqlType, String typeName)
    throws SQLException
  {
    try
    {
      if (wrappedStmt != null) {
        ((CallableStatement)wrappedStmt).registerOutParameter(parameterName, sqlType, typeName);
      } else {
        throw SQLError.createSQLException("No operations allowed after statement closed", "S1000", exceptionInterceptor);
      }
    } catch (SQLException sqlEx) {
      checkAndFireConnectionError(sqlEx);
    }
  }
  





  public void setObject(int parameterIndex, Object x, SQLType targetSqlType)
    throws SQLException
  {
    try
    {
      if (wrappedStmt != null) {
        ((CallableStatement)wrappedStmt).setObject(parameterIndex, x, targetSqlType);
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
        ((CallableStatement)wrappedStmt).setObject(parameterIndex, x, targetSqlType, scaleOrLength);
      } else {
        throw SQLError.createSQLException("No operations allowed after statement closed", "S1000", exceptionInterceptor);
      }
    } catch (SQLException sqlEx) {
      checkAndFireConnectionError(sqlEx);
    }
  }
  





  public void setObject(String parameterName, Object x, SQLType targetSqlType)
    throws SQLException
  {
    try
    {
      if (wrappedStmt != null) {
        ((CallableStatement)wrappedStmt).setObject(parameterName, x, targetSqlType);
      } else {
        throw SQLError.createSQLException("No operations allowed after statement closed", "S1000", exceptionInterceptor);
      }
    } catch (SQLException sqlEx) {
      checkAndFireConnectionError(sqlEx);
    }
  }
  






  public void setObject(String parameterName, Object x, SQLType targetSqlType, int scaleOrLength)
    throws SQLException
  {
    try
    {
      if (wrappedStmt != null) {
        ((CallableStatement)wrappedStmt).setObject(parameterName, x, targetSqlType, scaleOrLength);
      } else {
        throw SQLError.createSQLException("No operations allowed after statement closed", "S1000", exceptionInterceptor);
      }
    } catch (SQLException sqlEx) {
      checkAndFireConnectionError(sqlEx);
    }
  }
}
