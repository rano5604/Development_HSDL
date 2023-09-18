package com.mysql.jdbc;

import java.sql.SQLException;
import java.sql.SQLType;




















public class JDBC42CallableStatement
  extends JDBC4CallableStatement
{
  public JDBC42CallableStatement(MySQLConnection conn, CallableStatement.CallableStatementParamInfo paramInfo)
    throws SQLException
  {
    super(conn, paramInfo);
  }
  
  public JDBC42CallableStatement(MySQLConnection conn, String sql, String catalog, boolean isFunctionCall) throws SQLException {
    super(conn, sql, catalog, isFunctionCall);
  }
  


  private int checkSqlType(int sqlType)
    throws SQLException
  {
    return JDBC42Helper.checkSqlType(sqlType, getExceptionInterceptor());
  }
  
  private int translateAndCheckSqlType(SQLType sqlType) throws SQLException {
    return JDBC42Helper.translateAndCheckSqlType(sqlType, getExceptionInterceptor());
  }
  





  public void registerOutParameter(int parameterIndex, SQLType sqlType)
    throws SQLException
  {
    super.registerOutParameter(parameterIndex, translateAndCheckSqlType(sqlType));
  }
  






  public void registerOutParameter(int parameterIndex, SQLType sqlType, int scale)
    throws SQLException
  {
    super.registerOutParameter(parameterIndex, translateAndCheckSqlType(sqlType), scale);
  }
  






  public void registerOutParameter(int parameterIndex, SQLType sqlType, String typeName)
    throws SQLException
  {
    super.registerOutParameter(parameterIndex, translateAndCheckSqlType(sqlType), typeName);
  }
  





  public void registerOutParameter(String parameterName, SQLType sqlType)
    throws SQLException
  {
    super.registerOutParameter(parameterName, translateAndCheckSqlType(sqlType));
  }
  






  public void registerOutParameter(String parameterName, SQLType sqlType, int scale)
    throws SQLException
  {
    super.registerOutParameter(parameterName, translateAndCheckSqlType(sqlType), scale);
  }
  






  public void registerOutParameter(String parameterName, SQLType sqlType, String typeName)
    throws SQLException
  {
    super.registerOutParameter(parameterName, translateAndCheckSqlType(sqlType), typeName);
  }
  






  public void setObject(int parameterIndex, Object x)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      super.setObject(parameterIndex, JDBC42Helper.convertJavaTimeToJavaSql(x));
    }
  }
  







  public void setObject(int parameterIndex, Object x, int targetSqlType)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      super.setObject(parameterIndex, JDBC42Helper.convertJavaTimeToJavaSql(x), checkSqlType(targetSqlType));
    }
  }
  








  public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      super.setObject(parameterIndex, JDBC42Helper.convertJavaTimeToJavaSql(x), checkSqlType(targetSqlType), scaleOrLength);
    }
  }
  







  public void setObject(int parameterIndex, Object x, SQLType targetSqlType)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      super.setObject(parameterIndex, JDBC42Helper.convertJavaTimeToJavaSql(x), translateAndCheckSqlType(targetSqlType));
    }
  }
  








  public void setObject(int parameterIndex, Object x, SQLType targetSqlType, int scaleOrLength)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      super.setObject(parameterIndex, JDBC42Helper.convertJavaTimeToJavaSql(x), translateAndCheckSqlType(targetSqlType), scaleOrLength);
    }
  }
  







  public void setObject(String parameterName, Object x, SQLType targetSqlType)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      super.setObject(parameterName, JDBC42Helper.convertJavaTimeToJavaSql(x), translateAndCheckSqlType(targetSqlType));
    }
  }
  








  public void setObject(String parameterName, Object x, SQLType targetSqlType, int scaleOrLength)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      super.setObject(parameterName, JDBC42Helper.convertJavaTimeToJavaSql(x), translateAndCheckSqlType(targetSqlType), scaleOrLength);
    }
  }
}
