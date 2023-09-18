package com.mysql.jdbc;

import java.sql.SQLException;





















public class NdbLoadBalanceExceptionChecker
  extends StandardLoadBalanceExceptionChecker
{
  public NdbLoadBalanceExceptionChecker() {}
  
  public boolean shouldExceptionTriggerFailover(SQLException ex)
  {
    return (super.shouldExceptionTriggerFailover(ex)) || (checkNdbException(ex));
  }
  
  private boolean checkNdbException(SQLException ex)
  {
    return (ex.getMessage().startsWith("Lock wait timeout exceeded")) || ((ex.getMessage().startsWith("Got temporary error")) && (ex.getMessage().endsWith("from NDB")));
  }
}
