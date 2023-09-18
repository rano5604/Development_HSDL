package com.mysql.jdbc.exceptions;









public class MySQLInvalidAuthorizationSpecException
  extends MySQLNonTransientException
{
  static final long serialVersionUID = 6878889837492500030L;
  








  public MySQLInvalidAuthorizationSpecException() {}
  







  public MySQLInvalidAuthorizationSpecException(String reason, String SQLState, int vendorCode)
  {
    super(reason, SQLState, vendorCode);
  }
  
  public MySQLInvalidAuthorizationSpecException(String reason, String SQLState) {
    super(reason, SQLState);
  }
  
  public MySQLInvalidAuthorizationSpecException(String reason) {
    super(reason);
  }
}
