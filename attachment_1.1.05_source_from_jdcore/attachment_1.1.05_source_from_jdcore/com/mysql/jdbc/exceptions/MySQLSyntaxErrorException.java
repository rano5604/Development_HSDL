package com.mysql.jdbc.exceptions;









public class MySQLSyntaxErrorException
  extends MySQLNonTransientException
{
  static final long serialVersionUID = 6919059513432113764L;
  








  public MySQLSyntaxErrorException() {}
  







  public MySQLSyntaxErrorException(String reason, String SQLState, int vendorCode)
  {
    super(reason, SQLState, vendorCode);
  }
  
  public MySQLSyntaxErrorException(String reason, String SQLState) {
    super(reason, SQLState);
  }
  
  public MySQLSyntaxErrorException(String reason) {
    super(reason);
  }
}
