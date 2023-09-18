package com.mysql.jdbc;

import java.sql.SQLException;
import java.util.Properties;

public abstract interface Extension
{
  public abstract void init(Connection paramConnection, Properties paramProperties)
    throws SQLException;
  
  public abstract void destroy();
}
