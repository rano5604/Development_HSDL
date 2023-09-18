package com.mysql.jdbc.exceptions;

import java.sql.SQLException;






















public class MySQLTransientException
  extends SQLException
{
  static final long serialVersionUID = -1885878228558607563L;
  
  public MySQLTransientException(String reason, String SQLState, int vendorCode)
  {
    super(reason, SQLState, vendorCode);
  }
  
  public MySQLTransientException(String reason, String SQLState) {
    super(reason, SQLState);
  }
  
  public MySQLTransientException(String reason) {
    super(reason);
  }
  
  public MySQLTransientException() {}
}
