package com.mysql.jdbc;

import java.sql.SQLException;

public abstract interface ExceptionInterceptor
  extends Extension
{
  public abstract SQLException interceptException(SQLException paramSQLException, Connection paramConnection);
}
