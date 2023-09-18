package com.mysql.jdbc;

import java.sql.SQLException;

public abstract interface LoadBalanceExceptionChecker
  extends Extension
{
  public abstract boolean shouldExceptionTriggerFailover(SQLException paramSQLException);
}
