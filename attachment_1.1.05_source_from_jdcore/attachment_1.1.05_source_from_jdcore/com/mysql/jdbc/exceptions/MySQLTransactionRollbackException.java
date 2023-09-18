package com.mysql.jdbc.exceptions;











public class MySQLTransactionRollbackException
  extends MySQLTransientException
  implements DeadlockTimeoutRollbackMarker
{
  static final long serialVersionUID = 6034999468737801730L;
  










  public MySQLTransactionRollbackException(String reason, String SQLState, int vendorCode)
  {
    super(reason, SQLState, vendorCode);
  }
  
  public MySQLTransactionRollbackException(String reason, String SQLState) {
    super(reason, SQLState);
  }
  
  public MySQLTransactionRollbackException(String reason) {
    super(reason);
  }
  
  public MySQLTransactionRollbackException() {}
}
