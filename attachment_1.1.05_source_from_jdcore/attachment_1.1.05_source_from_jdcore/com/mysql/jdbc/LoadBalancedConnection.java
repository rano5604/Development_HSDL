package com.mysql.jdbc;

import java.sql.SQLException;

public abstract interface LoadBalancedConnection
  extends MySQLConnection
{
  public abstract boolean addHost(String paramString)
    throws SQLException;
  
  public abstract void removeHost(String paramString)
    throws SQLException;
  
  public abstract void removeHostWhenNotInUse(String paramString)
    throws SQLException;
  
  public abstract void ping(boolean paramBoolean)
    throws SQLException;
}
