package com.mysql.jdbc.jmx;

import java.sql.SQLException;

public abstract interface LoadBalanceConnectionGroupManagerMBean
{
  public abstract int getActiveHostCount(String paramString);
  
  public abstract int getTotalHostCount(String paramString);
  
  public abstract long getTotalLogicalConnectionCount(String paramString);
  
  public abstract long getActiveLogicalConnectionCount(String paramString);
  
  public abstract long getActivePhysicalConnectionCount(String paramString);
  
  public abstract long getTotalPhysicalConnectionCount(String paramString);
  
  public abstract long getTotalTransactionCount(String paramString);
  
  public abstract void removeHost(String paramString1, String paramString2)
    throws SQLException;
  
  public abstract void stopNewConnectionsToHost(String paramString1, String paramString2)
    throws SQLException;
  
  public abstract void addHost(String paramString1, String paramString2, boolean paramBoolean);
  
  public abstract String getActiveHostsList(String paramString);
  
  public abstract String getRegisteredConnectionGroups();
}
