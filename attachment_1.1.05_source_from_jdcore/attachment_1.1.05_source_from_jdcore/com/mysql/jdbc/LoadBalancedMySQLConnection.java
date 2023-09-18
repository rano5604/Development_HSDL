package com.mysql.jdbc;

import java.sql.SQLException;





















public class LoadBalancedMySQLConnection
  extends MultiHostMySQLConnection
  implements LoadBalancedConnection
{
  public LoadBalancedMySQLConnection(LoadBalancedConnectionProxy proxy)
  {
    super(proxy);
  }
  
  protected LoadBalancedConnectionProxy getThisAsProxy()
  {
    return (LoadBalancedConnectionProxy)super.getThisAsProxy();
  }
  
  public void close() throws SQLException
  {
    getThisAsProxy().doClose();
  }
  
  public void ping() throws SQLException
  {
    ping(true);
  }
  
  public void ping(boolean allConnections) throws SQLException {
    if (allConnections) {
      getThisAsProxy().doPing();
    } else {
      getActiveMySQLConnection().ping();
    }
  }
  
  public boolean addHost(String host) throws SQLException {
    return getThisAsProxy().addHost(host);
  }
  
  public void removeHost(String host) throws SQLException {
    getThisAsProxy().removeHost(host);
  }
  
  public void removeHostWhenNotInUse(String host) throws SQLException {
    getThisAsProxy().removeHostWhenNotInUse(host);
  }
}
