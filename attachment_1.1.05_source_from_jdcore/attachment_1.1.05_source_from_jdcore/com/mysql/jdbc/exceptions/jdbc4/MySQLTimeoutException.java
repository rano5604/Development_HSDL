package com.mysql.jdbc.exceptions.jdbc4;

import java.sql.SQLTimeoutException;






















public class MySQLTimeoutException
  extends SQLTimeoutException
{
  static final long serialVersionUID = -789621240523230339L;
  
  public MySQLTimeoutException(String reason, String SQLState, int vendorCode)
  {
    super(reason, SQLState, vendorCode);
  }
  
  public MySQLTimeoutException(String reason, String SQLState) {
    super(reason, SQLState);
  }
  
  public MySQLTimeoutException(String reason) {
    super(reason);
  }
  
  public MySQLTimeoutException() {
    super("Statement cancelled due to timeout or client request");
  }
  
  public int getErrorCode() {
    return super.getErrorCode();
  }
}
