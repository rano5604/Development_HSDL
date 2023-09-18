package com.mysql.jdbc;

import java.sql.SQLException;

public abstract interface ReplicationConnection
  extends MySQLConnection
{
  public abstract long getConnectionGroupId();
  
  public abstract Connection getCurrentConnection();
  
  public abstract Connection getMasterConnection();
  
  public abstract void promoteSlaveToMaster(String paramString)
    throws SQLException;
  
  public abstract void removeMasterHost(String paramString)
    throws SQLException;
  
  public abstract void removeMasterHost(String paramString, boolean paramBoolean)
    throws SQLException;
  
  public abstract boolean isHostMaster(String paramString);
  
  public abstract Connection getSlavesConnection();
  
  public abstract void addSlaveHost(String paramString)
    throws SQLException;
  
  public abstract void removeSlave(String paramString)
    throws SQLException;
  
  public abstract void removeSlave(String paramString, boolean paramBoolean)
    throws SQLException;
  
  public abstract boolean isHostSlave(String paramString);
}
