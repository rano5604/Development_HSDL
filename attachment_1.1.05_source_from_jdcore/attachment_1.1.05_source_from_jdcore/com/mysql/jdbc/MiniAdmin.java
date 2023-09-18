package com.mysql.jdbc;

import java.sql.SQLException;
import java.util.Properties;

































public class MiniAdmin
{
  private Connection conn;
  
  public MiniAdmin(java.sql.Connection conn)
    throws SQLException
  {
    if (conn == null) {
      throw SQLError.createSQLException(Messages.getString("MiniAdmin.0"), "S1000", null);
    }
    
    if (!(conn instanceof Connection)) {
      throw SQLError.createSQLException(Messages.getString("MiniAdmin.1"), "S1000", ((ConnectionImpl)conn).getExceptionInterceptor());
    }
    

    this.conn = ((Connection)conn);
  }
  







  public MiniAdmin(String jdbcUrl)
    throws SQLException
  {
    this(jdbcUrl, new Properties());
  }
  










  public MiniAdmin(String jdbcUrl, Properties props)
    throws SQLException
  {
    conn = ((Connection)new Driver().connect(jdbcUrl, props));
  }
  





  public void shutdown()
    throws SQLException
  {
    conn.shutdownServer();
  }
}
