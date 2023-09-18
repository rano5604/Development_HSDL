package com.mysql.jdbc;

import java.sql.SQLException;
import java.util.List;

public abstract interface AuthenticationPlugin
  extends Extension
{
  public abstract String getProtocolPluginName();
  
  public abstract boolean requiresConfidentiality();
  
  public abstract boolean isReusable();
  
  public abstract void setAuthenticationParameters(String paramString1, String paramString2);
  
  public abstract boolean nextAuthenticationStep(Buffer paramBuffer, List<Buffer> paramList)
    throws SQLException;
  
  public abstract void reset();
}
