package com.mysql.jdbc;

import java.sql.ParameterMetaData;
import java.sql.SQLException;























public class MysqlParameterMetadata
  implements ParameterMetaData
{
  boolean returnSimpleMetadata = false;
  
  ResultSetMetaData metadata = null;
  
  int parameterCount = 0;
  private ExceptionInterceptor exceptionInterceptor;
  
  MysqlParameterMetadata(Field[] fieldInfo, int parameterCount, ExceptionInterceptor exceptionInterceptor)
  {
    metadata = new ResultSetMetaData(fieldInfo, false, true, exceptionInterceptor);
    
    this.parameterCount = parameterCount;
    this.exceptionInterceptor = exceptionInterceptor;
  }
  





  MysqlParameterMetadata(int count)
  {
    parameterCount = count;
    returnSimpleMetadata = true;
  }
  
  public int getParameterCount() throws SQLException {
    return parameterCount;
  }
  
  public int isNullable(int arg0) throws SQLException {
    checkAvailable();
    
    return metadata.isNullable(arg0);
  }
  
  private void checkAvailable() throws SQLException {
    if ((metadata == null) || (metadata.fields == null)) {
      throw SQLError.createSQLException("Parameter metadata not available for the given statement", "S1C00", exceptionInterceptor);
    }
  }
  
  public boolean isSigned(int arg0) throws SQLException
  {
    if (returnSimpleMetadata) {
      checkBounds(arg0);
      
      return false;
    }
    
    checkAvailable();
    
    return metadata.isSigned(arg0);
  }
  
  public int getPrecision(int arg0) throws SQLException {
    if (returnSimpleMetadata) {
      checkBounds(arg0);
      
      return 0;
    }
    
    checkAvailable();
    
    return metadata.getPrecision(arg0);
  }
  
  public int getScale(int arg0) throws SQLException {
    if (returnSimpleMetadata) {
      checkBounds(arg0);
      
      return 0;
    }
    
    checkAvailable();
    
    return metadata.getScale(arg0);
  }
  
  public int getParameterType(int arg0) throws SQLException {
    if (returnSimpleMetadata) {
      checkBounds(arg0);
      
      return 12;
    }
    
    checkAvailable();
    
    return metadata.getColumnType(arg0);
  }
  
  public String getParameterTypeName(int arg0) throws SQLException {
    if (returnSimpleMetadata) {
      checkBounds(arg0);
      
      return "VARCHAR";
    }
    
    checkAvailable();
    
    return metadata.getColumnTypeName(arg0);
  }
  
  public String getParameterClassName(int arg0) throws SQLException {
    if (returnSimpleMetadata) {
      checkBounds(arg0);
      
      return "java.lang.String";
    }
    
    checkAvailable();
    
    return metadata.getColumnClassName(arg0);
  }
  
  public int getParameterMode(int arg0) throws SQLException {
    return 1;
  }
  
  private void checkBounds(int paramNumber) throws SQLException {
    if (paramNumber < 1) {
      throw SQLError.createSQLException("Parameter index of '" + paramNumber + "' is invalid.", "S1009", exceptionInterceptor);
    }
    

    if (paramNumber > parameterCount) {
      throw SQLError.createSQLException("Parameter index of '" + paramNumber + "' is greater than number of parameters, which is '" + parameterCount + "'.", "S1009", exceptionInterceptor);
    }
  }
  





  public boolean isWrapperFor(Class<?> iface)
    throws SQLException
  {
    return iface.isInstance(this);
  }
  

  public <T> T unwrap(Class<T> iface)
    throws SQLException
  {
    try
    {
      return iface.cast(this);
    } catch (ClassCastException cce) {
      throw SQLError.createSQLException("Unable to unwrap to " + iface.toString(), "S1009", exceptionInterceptor);
    }
  }
}
