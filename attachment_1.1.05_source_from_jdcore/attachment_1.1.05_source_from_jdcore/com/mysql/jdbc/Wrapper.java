package com.mysql.jdbc;

import java.sql.SQLException;

public abstract interface Wrapper
{
  public abstract <T> T unwrap(Class<T> paramClass)
    throws SQLException;
  
  public abstract boolean isWrapperFor(Class<?> paramClass)
    throws SQLException;
}
