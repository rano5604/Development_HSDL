package com.mysql.jdbc;

import com.mysql.jdbc.log.Log;
import com.mysql.jdbc.profiler.ProfilerEventHandler;
import java.sql.SQLException;

























public class ProfilerEventHandlerFactory
{
  private Connection ownerConnection = null;
  
  protected Log log = null;
  






  public static synchronized ProfilerEventHandler getInstance(MySQLConnection conn)
    throws SQLException
  {
    ProfilerEventHandler handler = conn.getProfilerEventHandlerInstance();
    
    if (handler == null) {
      handler = (ProfilerEventHandler)Util.getInstance(conn.getProfilerEventHandler(), new Class[0], new Object[0], conn.getExceptionInterceptor());
      

      conn.initializeExtension(handler);
      conn.setProfilerEventHandlerInstance(handler);
    }
    
    return handler;
  }
  
  public static synchronized void removeInstance(MySQLConnection conn) {
    ProfilerEventHandler handler = conn.getProfilerEventHandlerInstance();
    
    if (handler != null) {
      handler.destroy();
    }
  }
  
  private ProfilerEventHandlerFactory(Connection conn) {
    ownerConnection = conn;
    try
    {
      log = ownerConnection.getLog();
    } catch (SQLException sqlEx) {
      throw new RuntimeException("Unable to get logger from connection");
    }
  }
}
