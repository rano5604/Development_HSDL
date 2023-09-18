package com.mysql.jdbc.exceptions.jdbc4;

import java.sql.SQLDataException;
























public class MySQLDataException
  extends SQLDataException
{
  static final long serialVersionUID = 4317904269797988676L;
  
  public MySQLDataException() {}
  
  public MySQLDataException(String reason, String SQLState, int vendorCode)
  {
    super(reason, SQLState, vendorCode);
  }
  
  public MySQLDataException(String reason, String SQLState) {
    super(reason, SQLState);
  }
  
  public MySQLDataException(String reason) {
    super(reason);
  }
}
