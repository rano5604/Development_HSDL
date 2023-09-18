package com.mysql.fabric.jdbc;

import com.mysql.jdbc.ConnectionProperties;

public abstract interface FabricMySQLConnectionProperties
  extends ConnectionProperties
{
  public abstract void setFabricShardKey(String paramString);
  
  public abstract String getFabricShardKey();
  
  public abstract void setFabricShardTable(String paramString);
  
  public abstract String getFabricShardTable();
  
  public abstract void setFabricServerGroup(String paramString);
  
  public abstract String getFabricServerGroup();
  
  public abstract void setFabricProtocol(String paramString);
  
  public abstract String getFabricProtocol();
  
  public abstract void setFabricUsername(String paramString);
  
  public abstract String getFabricUsername();
  
  public abstract void setFabricPassword(String paramString);
  
  public abstract String getFabricPassword();
  
  public abstract void setFabricReportErrors(boolean paramBoolean);
  
  public abstract boolean getFabricReportErrors();
}
