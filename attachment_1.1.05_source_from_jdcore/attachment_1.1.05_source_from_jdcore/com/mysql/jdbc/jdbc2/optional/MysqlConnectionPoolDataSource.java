package com.mysql.jdbc.jdbc2.optional;

import java.sql.SQLException;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;































public class MysqlConnectionPoolDataSource
  extends MysqlDataSource
  implements ConnectionPoolDataSource
{
  static final long serialVersionUID = -7767325445592304961L;
  
  public MysqlConnectionPoolDataSource() {}
  
  public synchronized PooledConnection getPooledConnection()
    throws SQLException
  {
    java.sql.Connection connection = getConnection();
    MysqlPooledConnection mysqlPooledConnection = MysqlPooledConnection.getInstance((com.mysql.jdbc.Connection)connection);
    
    return mysqlPooledConnection;
  }
  










  public synchronized PooledConnection getPooledConnection(String s, String s1)
    throws SQLException
  {
    java.sql.Connection connection = getConnection(s, s1);
    MysqlPooledConnection mysqlPooledConnection = MysqlPooledConnection.getInstance((com.mysql.jdbc.Connection)connection);
    
    return mysqlPooledConnection;
  }
}
