package com.mysql.jdbc.exceptions;









public class MySQLNonTransientConnectionException
  extends MySQLNonTransientException
{
  static final long serialVersionUID = -3050543822763367670L;
  








  public MySQLNonTransientConnectionException() {}
  







  public MySQLNonTransientConnectionException(String reason, String SQLState, int vendorCode)
  {
    super(reason, SQLState, vendorCode);
  }
  
  public MySQLNonTransientConnectionException(String reason, String SQLState) {
    super(reason, SQLState);
  }
  
  public MySQLNonTransientConnectionException(String reason) {
    super(reason);
  }
}
