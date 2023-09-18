package com.mysql.jdbc;

import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.Executor;




















public class ReplicationMySQLConnection
  extends MultiHostMySQLConnection
  implements ReplicationConnection
{
  public ReplicationMySQLConnection(MultiHostConnectionProxy proxy)
  {
    super(proxy);
  }
  
  protected ReplicationConnectionProxy getThisAsProxy()
  {
    return (ReplicationConnectionProxy)super.getThisAsProxy();
  }
  
  public MySQLConnection getActiveMySQLConnection()
  {
    return (MySQLConnection)getCurrentConnection();
  }
  
  public synchronized Connection getCurrentConnection() {
    return getThisAsProxy().getCurrentConnection();
  }
  
  public long getConnectionGroupId() {
    return getThisAsProxy().getConnectionGroupId();
  }
  
  public synchronized Connection getMasterConnection() {
    return getThisAsProxy().getMasterConnection();
  }
  
  private Connection getValidatedMasterConnection() {
    Connection conn = getThisAsProxymasterConnection;
    try {
      return (conn == null) || (conn.isClosed()) ? null : conn;
    } catch (SQLException e) {}
    return null;
  }
  
  public void promoteSlaveToMaster(String host) throws SQLException
  {
    getThisAsProxy().promoteSlaveToMaster(host);
  }
  
  public void removeMasterHost(String host) throws SQLException {
    getThisAsProxy().removeMasterHost(host);
  }
  
  public void removeMasterHost(String host, boolean waitUntilNotInUse) throws SQLException {
    getThisAsProxy().removeMasterHost(host, waitUntilNotInUse);
  }
  
  public boolean isHostMaster(String host) {
    return getThisAsProxy().isHostMaster(host);
  }
  
  public synchronized Connection getSlavesConnection() {
    return getThisAsProxy().getSlavesConnection();
  }
  
  private Connection getValidatedSlavesConnection() {
    Connection conn = getThisAsProxyslavesConnection;
    try {
      return (conn == null) || (conn.isClosed()) ? null : conn;
    } catch (SQLException e) {}
    return null;
  }
  
  public void addSlaveHost(String host) throws SQLException
  {
    getThisAsProxy().addSlaveHost(host);
  }
  
  public void removeSlave(String host) throws SQLException {
    getThisAsProxy().removeSlave(host);
  }
  
  public void removeSlave(String host, boolean closeGently) throws SQLException {
    getThisAsProxy().removeSlave(host, closeGently);
  }
  
  public boolean isHostSlave(String host) {
    return getThisAsProxy().isHostSlave(host);
  }
  
  public void setReadOnly(boolean readOnlyFlag) throws SQLException
  {
    getThisAsProxy().setReadOnly(readOnlyFlag);
  }
  
  public boolean isReadOnly() throws SQLException
  {
    return getThisAsProxy().isReadOnly();
  }
  
  public synchronized void ping() throws SQLException
  {
    Connection conn;
    try {
      if ((conn = getValidatedMasterConnection()) != null) {
        conn.ping();
      }
    } catch (SQLException e) {
      if (isMasterConnection()) {
        throw e;
      }
    }
    try {
      if ((conn = getValidatedSlavesConnection()) != null) {
        conn.ping();
      }
    } catch (SQLException e) {
      if (!isMasterConnection()) {
        throw e;
      }
    }
  }
  
  public synchronized void changeUser(String userName, String newPassword) throws SQLException
  {
    Connection conn;
    if ((conn = getValidatedMasterConnection()) != null) {
      conn.changeUser(userName, newPassword);
    }
    if ((conn = getValidatedSlavesConnection()) != null) {
      conn.changeUser(userName, newPassword);
    }
  }
  
  public synchronized void setStatementComment(String comment)
  {
    Connection conn;
    if ((conn = getValidatedMasterConnection()) != null) {
      conn.setStatementComment(comment);
    }
    if ((conn = getValidatedSlavesConnection()) != null) {
      conn.setStatementComment(comment);
    }
  }
  
  public boolean hasSameProperties(Connection c)
  {
    Connection connM = getValidatedMasterConnection();
    Connection connS = getValidatedSlavesConnection();
    if ((connM == null) && (connS == null)) {
      return false;
    }
    return ((connM == null) || (connM.hasSameProperties(c))) && ((connS == null) || (connS.hasSameProperties(c)));
  }
  
  public Properties getProperties()
  {
    Properties props = new Properties();
    Connection conn;
    if ((conn = getValidatedMasterConnection()) != null) {
      props.putAll(conn.getProperties());
    }
    if ((conn = getValidatedSlavesConnection()) != null) {
      props.putAll(conn.getProperties());
    }
    
    return props;
  }
  
  public void abort(Executor executor) throws SQLException
  {
    getThisAsProxy().doAbort(executor);
  }
  
  public void abortInternal() throws SQLException
  {
    getThisAsProxy().doAbortInternal();
  }
  
  public boolean getAllowMasterDownConnections()
  {
    return getThisAsProxyallowMasterDownConnections;
  }
  
  public void setAllowMasterDownConnections(boolean connectIfMasterDown)
  {
    getThisAsProxyallowMasterDownConnections = connectIfMasterDown;
  }
  
  public boolean getReplicationEnableJMX()
  {
    return getThisAsProxyenableJMX;
  }
  
  public void setReplicationEnableJMX(boolean replicationEnableJMX)
  {
    getThisAsProxyenableJMX = replicationEnableJMX;
  }
  
  public void setProxy(MySQLConnection proxy)
  {
    getThisAsProxy().setProxy(proxy);
  }
}
