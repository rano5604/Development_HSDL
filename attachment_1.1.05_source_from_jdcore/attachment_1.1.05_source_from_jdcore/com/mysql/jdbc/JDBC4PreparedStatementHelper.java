package com.mysql.jdbc;

import java.io.Reader;
import java.sql.NClob;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;


























public class JDBC4PreparedStatementHelper
{
  private JDBC4PreparedStatementHelper() {}
  
  static void setRowId(PreparedStatement pstmt, int parameterIndex, RowId x)
    throws SQLException
  {
    throw SQLError.createSQLFeatureNotSupportedException();
  }
  









  static void setNClob(PreparedStatement pstmt, int parameterIndex, NClob value)
    throws SQLException
  {
    if (value == null) {
      pstmt.setNull(parameterIndex, 2011);
    } else {
      pstmt.setNCharacterStream(parameterIndex, value.getCharacterStream(), value.length());
    }
  }
  
  static void setNClob(PreparedStatement pstmt, int parameterIndex, Reader reader) throws SQLException {
    pstmt.setNCharacterStream(parameterIndex, reader);
  }
  











  static void setNClob(PreparedStatement pstmt, int parameterIndex, Reader reader, long length)
    throws SQLException
  {
    if (reader == null) {
      pstmt.setNull(parameterIndex, 2011);
    } else {
      pstmt.setNCharacterStream(parameterIndex, reader, length);
    }
  }
  
  static void setSQLXML(PreparedStatement pstmt, int parameterIndex, SQLXML xmlObject) throws SQLException {
    if (xmlObject == null) {
      pstmt.setNull(parameterIndex, 2009);
    }
    else {
      pstmt.setCharacterStream(parameterIndex, ((JDBC4MysqlSQLXML)xmlObject).serializeAsCharacterStream());
    }
  }
}
