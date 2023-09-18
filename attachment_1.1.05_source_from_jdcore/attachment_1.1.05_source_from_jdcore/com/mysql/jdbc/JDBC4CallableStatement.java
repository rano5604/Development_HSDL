package com.mysql.jdbc;

import java.io.Reader;
import java.sql.NClob;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
























public class JDBC4CallableStatement
  extends CallableStatement
{
  public JDBC4CallableStatement(MySQLConnection conn, CallableStatement.CallableStatementParamInfo paramInfo)
    throws SQLException
  {
    super(conn, paramInfo);
  }
  
  public JDBC4CallableStatement(MySQLConnection conn, String sql, String catalog, boolean isFunctionCall) throws SQLException {
    super(conn, sql, catalog, isFunctionCall);
  }
  
  public void setRowId(int parameterIndex, RowId x) throws SQLException {
    JDBC4PreparedStatementHelper.setRowId(this, parameterIndex, x);
  }
  
  public void setRowId(String parameterName, RowId x) throws SQLException {
    JDBC4PreparedStatementHelper.setRowId(this, getNamedParamIndex(parameterName, false), x);
  }
  
  public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
    JDBC4PreparedStatementHelper.setSQLXML(this, parameterIndex, xmlObject);
  }
  
  public void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException {
    JDBC4PreparedStatementHelper.setSQLXML(this, getNamedParamIndex(parameterName, false), xmlObject);
  }
  
  public SQLXML getSQLXML(int parameterIndex) throws SQLException
  {
    ResultSetInternalMethods rs = getOutputParameters(parameterIndex);
    
    SQLXML retValue = ((JDBC4ResultSet)rs).getSQLXML(mapOutputParameterIndexToRsIndex(parameterIndex));
    
    outputParamWasNull = rs.wasNull();
    
    return retValue;
  }
  
  public SQLXML getSQLXML(String parameterName) throws SQLException
  {
    ResultSetInternalMethods rs = getOutputParameters(0);
    
    SQLXML retValue = ((JDBC4ResultSet)rs).getSQLXML(fixParameterName(parameterName));
    
    outputParamWasNull = rs.wasNull();
    
    return retValue;
  }
  
  public RowId getRowId(int parameterIndex) throws SQLException {
    ResultSetInternalMethods rs = getOutputParameters(parameterIndex);
    
    RowId retValue = ((JDBC4ResultSet)rs).getRowId(mapOutputParameterIndexToRsIndex(parameterIndex));
    
    outputParamWasNull = rs.wasNull();
    
    return retValue;
  }
  
  public RowId getRowId(String parameterName) throws SQLException {
    ResultSetInternalMethods rs = getOutputParameters(0);
    
    RowId retValue = ((JDBC4ResultSet)rs).getRowId(fixParameterName(parameterName));
    
    outputParamWasNull = rs.wasNull();
    
    return retValue;
  }
  









  public void setNClob(int parameterIndex, NClob value)
    throws SQLException
  {
    JDBC4PreparedStatementHelper.setNClob(this, parameterIndex, value);
  }
  
  public void setNClob(String parameterName, NClob value) throws SQLException {
    JDBC4PreparedStatementHelper.setNClob(this, getNamedParamIndex(parameterName, false), value);
  }
  
  public void setNClob(String parameterName, Reader reader) throws SQLException
  {
    setNClob(getNamedParamIndex(parameterName, false), reader);
  }
  
  public void setNClob(String parameterName, Reader reader, long length) throws SQLException
  {
    setNClob(getNamedParamIndex(parameterName, false), reader, length);
  }
  
  public void setNString(String parameterName, String value) throws SQLException
  {
    setNString(getNamedParamIndex(parameterName, false), value);
  }
  

  public Reader getCharacterStream(int parameterIndex)
    throws SQLException
  {
    ResultSetInternalMethods rs = getOutputParameters(parameterIndex);
    
    Reader retValue = rs.getCharacterStream(mapOutputParameterIndexToRsIndex(parameterIndex));
    
    outputParamWasNull = rs.wasNull();
    
    return retValue;
  }
  

  public Reader getCharacterStream(String parameterName)
    throws SQLException
  {
    ResultSetInternalMethods rs = getOutputParameters(0);
    
    Reader retValue = rs.getCharacterStream(fixParameterName(parameterName));
    
    outputParamWasNull = rs.wasNull();
    
    return retValue;
  }
  

  public Reader getNCharacterStream(int parameterIndex)
    throws SQLException
  {
    ResultSetInternalMethods rs = getOutputParameters(parameterIndex);
    
    Reader retValue = ((JDBC4ResultSet)rs).getNCharacterStream(mapOutputParameterIndexToRsIndex(parameterIndex));
    
    outputParamWasNull = rs.wasNull();
    
    return retValue;
  }
  

  public Reader getNCharacterStream(String parameterName)
    throws SQLException
  {
    ResultSetInternalMethods rs = getOutputParameters(0);
    
    Reader retValue = ((JDBC4ResultSet)rs).getNCharacterStream(fixParameterName(parameterName));
    
    outputParamWasNull = rs.wasNull();
    
    return retValue;
  }
  

  public NClob getNClob(int parameterIndex)
    throws SQLException
  {
    ResultSetInternalMethods rs = getOutputParameters(parameterIndex);
    
    NClob retValue = ((JDBC4ResultSet)rs).getNClob(mapOutputParameterIndexToRsIndex(parameterIndex));
    
    outputParamWasNull = rs.wasNull();
    
    return retValue;
  }
  

  public NClob getNClob(String parameterName)
    throws SQLException
  {
    ResultSetInternalMethods rs = getOutputParameters(0);
    
    NClob retValue = ((JDBC4ResultSet)rs).getNClob(fixParameterName(parameterName));
    
    outputParamWasNull = rs.wasNull();
    
    return retValue;
  }
  

  public String getNString(int parameterIndex)
    throws SQLException
  {
    ResultSetInternalMethods rs = getOutputParameters(parameterIndex);
    
    String retValue = ((JDBC4ResultSet)rs).getNString(mapOutputParameterIndexToRsIndex(parameterIndex));
    
    outputParamWasNull = rs.wasNull();
    
    return retValue;
  }
  

  public String getNString(String parameterName)
    throws SQLException
  {
    ResultSetInternalMethods rs = getOutputParameters(0);
    
    String retValue = ((JDBC4ResultSet)rs).getNString(fixParameterName(parameterName));
    
    outputParamWasNull = rs.wasNull();
    
    return retValue;
  }
}
