package com.mysql.jdbc.jdbc2.optional;

import java.sql.SQLException;
import javax.sql.XAConnection;
import javax.sql.XADataSource;





























public class MysqlXADataSource
  extends MysqlDataSource
  implements XADataSource
{
  static final long serialVersionUID = 7911390333152247455L;
  
  public MysqlXADataSource() {}
  
  public XAConnection getXAConnection()
    throws SQLException
  {
    java.sql.Connection conn = getConnection();
    
    return wrapConnection(conn);
  }
  


  public XAConnection getXAConnection(String u, String p)
    throws SQLException
  {
    java.sql.Connection conn = getConnection(u, p);
    
    return wrapConnection(conn);
  }
  


  private XAConnection wrapConnection(java.sql.Connection conn)
    throws SQLException
  {
    if ((getPinGlobalTxToPhysicalConnection()) || (((com.mysql.jdbc.Connection)conn).getPinGlobalTxToPhysicalConnection())) {
      return SuspendableXAConnection.getInstance((com.mysql.jdbc.Connection)conn);
    }
    
    return MysqlXAConnection.getInstance((com.mysql.jdbc.Connection)conn, getLogXaCommands());
  }
}
