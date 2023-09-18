package com.mysql.jdbc;

import java.io.InputStream;
import java.sql.SQLException;

public abstract interface Statement
  extends java.sql.Statement, Wrapper
{
  public abstract void enableStreamingResults()
    throws SQLException;
  
  public abstract void disableStreamingResults()
    throws SQLException;
  
  public abstract void setLocalInfileInputStream(InputStream paramInputStream);
  
  public abstract InputStream getLocalInfileInputStream();
  
  public abstract void setPingTarget(PingTarget paramPingTarget);
  
  public abstract ExceptionInterceptor getExceptionInterceptor();
  
  public abstract void removeOpenResultSet(ResultSetInternalMethods paramResultSetInternalMethods);
  
  public abstract int getOpenResultSetCount();
  
  public abstract void setHoldResultsOpenOverClose(boolean paramBoolean);
}
