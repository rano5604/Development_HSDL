package com.mysql.fabric.jdbc;

import com.mysql.fabric.ServerGroup;
import com.mysql.jdbc.JDBC4MySQLConnection;
import java.sql.SQLException;
import java.util.Set;

public abstract interface JDBC4FabricMySQLConnection
  extends JDBC4MySQLConnection
{
  public abstract void clearServerSelectionCriteria()
    throws SQLException;
  
  public abstract void setShardKey(String paramString)
    throws SQLException;
  
  public abstract String getShardKey();
  
  public abstract void setShardTable(String paramString)
    throws SQLException;
  
  public abstract String getShardTable();
  
  public abstract void setServerGroupName(String paramString)
    throws SQLException;
  
  public abstract String getServerGroupName();
  
  public abstract ServerGroup getCurrentServerGroup();
  
  public abstract void clearQueryTables()
    throws SQLException;
  
  public abstract void addQueryTable(String paramString)
    throws SQLException;
  
  public abstract Set<String> getQueryTables();
}
