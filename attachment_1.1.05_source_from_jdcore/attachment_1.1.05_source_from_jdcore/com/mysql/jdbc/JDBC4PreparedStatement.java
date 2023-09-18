package com.mysql.jdbc;

import java.sql.NClob;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;




























public class JDBC4PreparedStatement
  extends PreparedStatement
{
  public JDBC4PreparedStatement(MySQLConnection conn, String catalog)
    throws SQLException
  {
    super(conn, catalog);
  }
  
  public JDBC4PreparedStatement(MySQLConnection conn, String sql, String catalog) throws SQLException {
    super(conn, sql, catalog);
  }
  
  public JDBC4PreparedStatement(MySQLConnection conn, String sql, String catalog, PreparedStatement.ParseInfo cachedParseInfo) throws SQLException {
    super(conn, sql, catalog, cachedParseInfo);
  }
  
  public void setRowId(int parameterIndex, RowId x) throws SQLException {
    JDBC4PreparedStatementHelper.setRowId(this, parameterIndex, x);
  }
  









  public void setNClob(int parameterIndex, NClob value)
    throws SQLException
  {
    JDBC4PreparedStatementHelper.setNClob(this, parameterIndex, value);
  }
  
  public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
    JDBC4PreparedStatementHelper.setSQLXML(this, parameterIndex, xmlObject);
  }
}
