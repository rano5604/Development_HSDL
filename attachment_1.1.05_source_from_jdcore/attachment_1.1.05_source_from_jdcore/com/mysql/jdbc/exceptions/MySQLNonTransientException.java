package com.mysql.jdbc.exceptions;

import java.sql.SQLException;
























public class MySQLNonTransientException
  extends SQLException
{
  static final long serialVersionUID = -8714521137552613517L;
  
  public MySQLNonTransientException() {}
  
  public MySQLNonTransientException(String reason, String SQLState, int vendorCode)
  {
    super(reason, SQLState, vendorCode);
  }
  
  public MySQLNonTransientException(String reason, String SQLState) {
    super(reason, SQLState);
  }
  
  public MySQLNonTransientException(String reason) {
    super(reason);
  }
}
