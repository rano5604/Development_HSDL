package com.mysql.jdbc;

import java.rmi.server.UID;
import java.sql.SQLException;
import java.sql.Savepoint;






















public class MysqlSavepoint
  implements Savepoint
{
  private String savepointName;
  private ExceptionInterceptor exceptionInterceptor;
  
  private static String getUniqueId()
  {
    String uidStr = new UID().toString();
    
    int uidLength = uidStr.length();
    
    StringBuilder safeString = new StringBuilder(uidLength + 1);
    safeString.append('_');
    
    for (int i = 0; i < uidLength; i++) {
      char c = uidStr.charAt(i);
      
      if ((Character.isLetter(c)) || (Character.isDigit(c))) {
        safeString.append(c);
      } else {
        safeString.append('_');
      }
    }
    
    return safeString.toString();
  }
  










  MysqlSavepoint(ExceptionInterceptor exceptionInterceptor)
    throws SQLException
  {
    this(getUniqueId(), exceptionInterceptor);
  }
  







  MysqlSavepoint(String name, ExceptionInterceptor exceptionInterceptor)
    throws SQLException
  {
    if ((name == null) || (name.length() == 0)) {
      throw SQLError.createSQLException("Savepoint name can not be NULL or empty", "S1009", exceptionInterceptor);
    }
    
    savepointName = name;
    
    this.exceptionInterceptor = exceptionInterceptor;
  }
  

  public int getSavepointId()
    throws SQLException
  {
    throw SQLError.createSQLException("Only named savepoints are supported.", "S1C00", exceptionInterceptor);
  }
  

  public String getSavepointName()
    throws SQLException
  {
    return savepointName;
  }
}
