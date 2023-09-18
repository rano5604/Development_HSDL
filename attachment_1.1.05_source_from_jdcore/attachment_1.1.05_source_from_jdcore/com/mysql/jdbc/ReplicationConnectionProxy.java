package com.mysql.jdbc;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;


























public class ReplicationConnectionProxy
  extends MultiHostConnectionProxy
  implements PingTarget
{
  private ReplicationConnection thisAsReplicationConnection;
  private NonRegisteringDriver driver;
  protected boolean enableJMX = false;
  protected boolean allowMasterDownConnections = false;
  protected boolean allowSlaveDownConnections = false;
  protected boolean readFromMasterWhenNoSlaves = false;
  protected boolean readFromMasterWhenNoSlavesOriginal = false;
  protected boolean readOnly = false;
  
  ReplicationConnectionGroup connectionGroup;
  private long connectionGroupID = -1L;
  
  private List<String> masterHosts;
  
  private Properties masterProperties;
  protected LoadBalancedConnection masterConnection;
  private List<String> slaveHosts;
  private Properties slaveProperties;
  protected LoadBalancedConnection slavesConnection;
  private static Constructor<?> JDBC_4_REPL_CONNECTION_CTOR;
  private static Class<?>[] INTERFACES_TO_PROXY;
  
  static
  {
    if (Util.isJdbc4()) {
      try {
        JDBC_4_REPL_CONNECTION_CTOR = Class.forName("com.mysql.jdbc.JDBC4ReplicationMySQLConnection").getConstructor(new Class[] { ReplicationConnectionProxy.class });
        
        INTERFACES_TO_PROXY = new Class[] { ReplicationConnection.class, Class.forName("com.mysql.jdbc.JDBC4MySQLConnection") };
      } catch (SecurityException e) {
        throw new RuntimeException(e);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    } else {
      INTERFACES_TO_PROXY = new Class[] { ReplicationConnection.class };
    }
  }
  
  public static ReplicationConnection createProxyInstance(List<String> masterHostList, Properties masterProperties, List<String> slaveHostList, Properties slaveProperties) throws SQLException
  {
    ReplicationConnectionProxy connProxy = new ReplicationConnectionProxy(masterHostList, masterProperties, slaveHostList, slaveProperties);
    
    return (ReplicationConnection)Proxy.newProxyInstance(ReplicationConnection.class.getClassLoader(), INTERFACES_TO_PROXY, connProxy);
  }
  















  private ReplicationConnectionProxy(List<String> masterHostList, Properties masterProperties, List<String> slaveHostList, Properties slaveProperties)
    throws SQLException
  {
    thisAsReplicationConnection = ((ReplicationConnection)thisAsConnection);
    
    String enableJMXAsString = masterProperties.getProperty("replicationEnableJMX", "false");
    try {
      enableJMX = Boolean.parseBoolean(enableJMXAsString);
    } catch (Exception e) {
      throw SQLError.createSQLException(Messages.getString("ReplicationConnectionProxy.badValueForReplicationEnableJMX", new Object[] { enableJMXAsString }), "S1009", null);
    }
    


    String allowMasterDownConnectionsAsString = masterProperties.getProperty("allowMasterDownConnections", "false");
    try {
      allowMasterDownConnections = Boolean.parseBoolean(allowMasterDownConnectionsAsString);
    } catch (Exception e) {
      throw SQLError.createSQLException(Messages.getString("ReplicationConnectionProxy.badValueForAllowMasterDownConnections", new Object[] { allowMasterDownConnectionsAsString }), "S1009", null);
    }
    


    String allowSlaveDownConnectionsAsString = masterProperties.getProperty("allowSlaveDownConnections", "false");
    try {
      allowSlaveDownConnections = Boolean.parseBoolean(allowSlaveDownConnectionsAsString);
    } catch (Exception e) {
      throw SQLError.createSQLException(Messages.getString("ReplicationConnectionProxy.badValueForAllowSlaveDownConnections", new Object[] { allowSlaveDownConnectionsAsString }), "S1009", null);
    }
    


    String readFromMasterWhenNoSlavesAsString = masterProperties.getProperty("readFromMasterWhenNoSlaves");
    try {
      readFromMasterWhenNoSlavesOriginal = Boolean.parseBoolean(readFromMasterWhenNoSlavesAsString);
    }
    catch (Exception e) {
      throw SQLError.createSQLException(Messages.getString("ReplicationConnectionProxy.badValueForReadFromMasterWhenNoSlaves", new Object[] { readFromMasterWhenNoSlavesAsString }), "S1009", null);
    }
    


    String group = masterProperties.getProperty("replicationConnectionGroup", null);
    if (group != null) {
      connectionGroup = ReplicationConnectionGroupManager.getConnectionGroupInstance(group);
      if (enableJMX) {
        ReplicationConnectionGroupManager.registerJmx();
      }
      connectionGroupID = connectionGroup.registerReplicationConnection(thisAsReplicationConnection, masterHostList, slaveHostList);
      
      slaveHosts = new ArrayList(connectionGroup.getSlaveHosts());
      masterHosts = new ArrayList(connectionGroup.getMasterHosts());
    } else {
      slaveHosts = new ArrayList(slaveHostList);
      masterHosts = new ArrayList(masterHostList);
    }
    
    driver = new NonRegisteringDriver();
    this.slaveProperties = slaveProperties;
    this.masterProperties = masterProperties;
    
    resetReadFromMasterWhenNoSlaves();
    
    try
    {
      initializeSlavesConnection();
    } catch (SQLException e) {
      if (!allowSlaveDownConnections) {
        if (connectionGroup != null) {
          connectionGroup.handleCloseConnection(thisAsReplicationConnection);
        }
        throw e;
      }
    }
    
    SQLException exCaught = null;
    try {
      currentConnection = initializeMasterConnection();
    } catch (SQLException e) {
      exCaught = e;
    }
    
    if (currentConnection == null) {
      if ((allowMasterDownConnections) && (slavesConnection != null))
      {
        readOnly = true;
        currentConnection = slavesConnection;
      } else {
        if (connectionGroup != null) {
          connectionGroup.handleCloseConnection(thisAsReplicationConnection);
        }
        if (exCaught != null) {
          throw exCaught;
        }
        throw SQLError.createSQLException(Messages.getString("ReplicationConnectionProxy.initializationWithEmptyHostsLists"), "S1009", null);
      }
    }
  }
  






  MySQLConnection getNewWrapperForThisAsConnection()
    throws SQLException
  {
    if ((Util.isJdbc4()) || (JDBC_4_REPL_CONNECTION_CTOR != null)) {
      return (MySQLConnection)Util.handleNewInstance(JDBC_4_REPL_CONNECTION_CTOR, new Object[] { this }, null);
    }
    return new ReplicationMySQLConnection(this);
  }
  






  protected void propagateProxyDown(MySQLConnection proxyConn)
  {
    if (masterConnection != null) {
      masterConnection.setProxy(proxyConn);
    }
    if (slavesConnection != null) {
      slavesConnection.setProxy(proxyConn);
    }
  }
  






  boolean shouldExceptionTriggerConnectionSwitch(Throwable t)
  {
    return false;
  }
  



  public boolean isMasterConnection()
  {
    return (currentConnection != null) && (currentConnection == masterConnection);
  }
  


  public boolean isSlavesConnection()
  {
    return (currentConnection != null) && (currentConnection == slavesConnection);
  }
  



  void syncSessionState(Connection source, Connection target, boolean readOnly)
    throws SQLException
  {
    try
    {
      super.syncSessionState(source, target, readOnly);
    }
    catch (SQLException e1) {
      try {
        super.syncSessionState(source, target, readOnly);
      }
      catch (SQLException e2) {}
    }
  }
  
  void doClose()
    throws SQLException
  {
    if (masterConnection != null) {
      masterConnection.close();
    }
    if (slavesConnection != null) {
      slavesConnection.close();
    }
    
    if (connectionGroup != null) {
      connectionGroup.handleCloseConnection(thisAsReplicationConnection);
    }
  }
  
  void doAbortInternal() throws SQLException
  {
    masterConnection.abortInternal();
    slavesConnection.abortInternal();
    if (connectionGroup != null) {
      connectionGroup.handleCloseConnection(thisAsReplicationConnection);
    }
  }
  
  void doAbort(Executor executor) throws SQLException
  {
    masterConnection.abort(executor);
    slavesConnection.abort(executor);
    if (connectionGroup != null) {
      connectionGroup.handleCloseConnection(thisAsReplicationConnection);
    }
  }
  



  Object invokeMore(Object proxy, Method method, Object[] args)
    throws Throwable
  {
    checkConnectionCapabilityForMethod(method);
    
    boolean invokeAgain = false;
    for (;;) {
      try {
        Object result = method.invoke(thisAsConnection, args);
        if ((result != null) && ((result instanceof Statement))) {
          ((Statement)result).setPingTarget(this);
        }
        return result;
      } catch (InvocationTargetException e) {
        if (invokeAgain) {
          invokeAgain = false;
        } else if ((e.getCause() != null) && ((e.getCause() instanceof SQLException)) && (((SQLException)e.getCause()).getSQLState() == "25000") && (((SQLException)e.getCause()).getErrorCode() == 1000001))
        {
          try
          {

            setReadOnly(readOnly);
            invokeAgain = true;
          }
          catch (SQLException sqlEx) {}
        }
        
        if (!invokeAgain) {
          throw e;
        }
      }
    }
  }
  



  private void checkConnectionCapabilityForMethod(Method method)
    throws Throwable
  {
    if ((masterHosts.isEmpty()) && (slaveHosts.isEmpty()) && (!ReplicationConnection.class.isAssignableFrom(method.getDeclaringClass()))) {
      throw SQLError.createSQLException(Messages.getString("ReplicationConnectionProxy.noHostsInconsistentState"), "25000", 1000002, true, null);
    }
  }
  


  public void doPing()
    throws SQLException
  {
    boolean isMasterConn = isMasterConnection();
    
    SQLException mastersPingException = null;
    SQLException slavesPingException = null;
    
    if (masterConnection != null) {
      try {
        masterConnection.ping();
      } catch (SQLException e) {
        mastersPingException = e;
      }
    } else {
      initializeMasterConnection();
    }
    
    if (slavesConnection != null) {
      try {
        slavesConnection.ping();
      } catch (SQLException e) {
        slavesPingException = e;
      }
    } else {
      try {
        initializeSlavesConnection();
        if (switchToSlavesConnectionIfNecessary()) {
          isMasterConn = false;
        }
      } catch (SQLException e) {
        if ((masterConnection == null) || (!readFromMasterWhenNoSlaves)) {
          throw e;
        }
      }
    }
    
    if ((isMasterConn) && (mastersPingException != null))
    {
      if ((slavesConnection != null) && (slavesPingException == null)) {
        masterConnection = null;
        currentConnection = slavesConnection;
        readOnly = true;
      }
      throw mastersPingException;
    }
    if ((!isMasterConn) && ((slavesPingException != null) || (slavesConnection == null)))
    {
      if ((masterConnection != null) && (readFromMasterWhenNoSlaves) && (mastersPingException == null)) {
        slavesConnection = null;
        currentConnection = masterConnection;
        readOnly = true;
        currentConnection.setReadOnly(true);
      }
      if (slavesPingException != null) {
        throw slavesPingException;
      }
    }
  }
  
  private MySQLConnection initializeMasterConnection() throws SQLException {
    masterConnection = null;
    
    if (masterHosts.size() == 0) {
      return null;
    }
    
    LoadBalancedConnection newMasterConn = (LoadBalancedConnection)driver.connect(buildURL(masterHosts, masterProperties), masterProperties);
    
    newMasterConn.setProxy(getProxy());
    
    masterConnection = newMasterConn;
    return masterConnection;
  }
  
  private MySQLConnection initializeSlavesConnection() throws SQLException {
    slavesConnection = null;
    
    if (slaveHosts.size() == 0) {
      return null;
    }
    
    LoadBalancedConnection newSlavesConn = (LoadBalancedConnection)driver.connect(buildURL(slaveHosts, slaveProperties), slaveProperties);
    
    newSlavesConn.setProxy(getProxy());
    newSlavesConn.setReadOnly(true);
    
    slavesConnection = newSlavesConn;
    return slavesConnection;
  }
  
  private String buildURL(List<String> hosts, Properties props) {
    StringBuilder url = new StringBuilder("jdbc:mysql:loadbalance://");
    
    boolean firstHost = true;
    for (String host : hosts) {
      if (!firstHost) {
        url.append(',');
      }
      url.append(host);
      firstHost = false;
    }
    url.append("/");
    String masterDb = props.getProperty("DBNAME");
    if (masterDb != null) {
      url.append(masterDb);
    }
    
    return url.toString();
  }
  
  private synchronized boolean switchToMasterConnection() throws SQLException {
    if ((masterConnection == null) || (masterConnection.isClosed())) {
      try {
        if (initializeMasterConnection() == null) {
          return false;
        }
      } catch (SQLException e) {
        currentConnection = null;
        throw e;
      }
    }
    if ((!isMasterConnection()) && (masterConnection != null)) {
      syncSessionState(currentConnection, masterConnection, false);
      currentConnection = masterConnection;
    }
    return true;
  }
  
  private synchronized boolean switchToSlavesConnection() throws SQLException {
    if ((slavesConnection == null) || (slavesConnection.isClosed())) {
      try {
        if (initializeSlavesConnection() == null) {
          return false;
        }
      } catch (SQLException e) {
        currentConnection = null;
        throw e;
      }
    }
    if ((!isSlavesConnection()) && (slavesConnection != null)) {
      syncSessionState(currentConnection, slavesConnection, true);
      currentConnection = slavesConnection;
    }
    return true;
  }
  



  private boolean switchToSlavesConnectionIfNecessary()
    throws SQLException
  {
    if ((currentConnection == null) || ((isMasterConnection()) && ((readOnly) || ((masterHosts.isEmpty()) && (currentConnection.isClosed())))) || ((!isMasterConnection()) && (currentConnection.isClosed())))
    {
      return switchToSlavesConnection();
    }
    return false;
  }
  
  public synchronized Connection getCurrentConnection() {
    return currentConnection == null ? LoadBalancedConnectionProxy.getNullLoadBalancedConnectionInstance() : currentConnection;
  }
  
  public long getConnectionGroupId() {
    return connectionGroupID;
  }
  
  public synchronized Connection getMasterConnection() {
    return masterConnection;
  }
  
  public synchronized void promoteSlaveToMaster(String hostPortPair) throws SQLException {
    masterHosts.add(hostPortPair);
    removeSlave(hostPortPair);
    if (masterConnection != null) {
      masterConnection.addHost(hostPortPair);
    }
    

    if ((!readOnly) && (!isMasterConnection())) {
      switchToMasterConnection();
    }
  }
  
  public synchronized void removeMasterHost(String hostPortPair) throws SQLException {
    removeMasterHost(hostPortPair, true);
  }
  
  public synchronized void removeMasterHost(String hostPortPair, boolean waitUntilNotInUse) throws SQLException {
    removeMasterHost(hostPortPair, waitUntilNotInUse, false);
  }
  
  public synchronized void removeMasterHost(String hostPortPair, boolean waitUntilNotInUse, boolean isNowSlave) throws SQLException {
    if (isNowSlave) {
      slaveHosts.add(hostPortPair);
      resetReadFromMasterWhenNoSlaves();
    }
    masterHosts.remove(hostPortPair);
    

    if ((masterConnection == null) || (masterConnection.isClosed())) {
      masterConnection = null;
      return;
    }
    
    if (waitUntilNotInUse) {
      masterConnection.removeHostWhenNotInUse(hostPortPair);
    } else {
      masterConnection.removeHost(hostPortPair);
    }
    

    if (masterHosts.isEmpty()) {
      masterConnection.close();
      masterConnection = null;
      

      switchToSlavesConnectionIfNecessary();
    }
  }
  
  public boolean isHostMaster(String hostPortPair) {
    if (hostPortPair == null) {
      return false;
    }
    for (String masterHost : masterHosts) {
      if (masterHost.equalsIgnoreCase(hostPortPair)) {
        return true;
      }
    }
    return false;
  }
  
  public synchronized Connection getSlavesConnection() {
    return slavesConnection;
  }
  
  public synchronized void addSlaveHost(String hostPortPair) throws SQLException {
    if (isHostSlave(hostPortPair)) {
      return;
    }
    slaveHosts.add(hostPortPair);
    resetReadFromMasterWhenNoSlaves();
    if (slavesConnection == null) {
      initializeSlavesConnection();
      switchToSlavesConnectionIfNecessary();
    } else {
      slavesConnection.addHost(hostPortPair);
    }
  }
  
  public synchronized void removeSlave(String hostPortPair) throws SQLException {
    removeSlave(hostPortPair, true);
  }
  
  public synchronized void removeSlave(String hostPortPair, boolean closeGently) throws SQLException {
    slaveHosts.remove(hostPortPair);
    resetReadFromMasterWhenNoSlaves();
    
    if ((slavesConnection == null) || (slavesConnection.isClosed())) {
      slavesConnection = null;
      return;
    }
    
    if (closeGently) {
      slavesConnection.removeHostWhenNotInUse(hostPortPair);
    } else {
      slavesConnection.removeHost(hostPortPair);
    }
    

    if (slaveHosts.isEmpty()) {
      slavesConnection.close();
      slavesConnection = null;
      

      switchToMasterConnection();
      if (isMasterConnection()) {
        currentConnection.setReadOnly(readOnly);
      }
    }
  }
  
  public boolean isHostSlave(String hostPortPair) {
    if (hostPortPair == null) {
      return false;
    }
    for (String test : slaveHosts) {
      if (test.equalsIgnoreCase(hostPortPair)) {
        return true;
      }
    }
    return false;
  }
  
  public synchronized void setReadOnly(boolean readOnly) throws SQLException
  {
    if (readOnly) {
      if ((!isSlavesConnection()) || (currentConnection.isClosed())) {
        boolean switched = true;
        SQLException exceptionCaught = null;
        try {
          switched = switchToSlavesConnection();
        } catch (SQLException e) {
          switched = false;
          exceptionCaught = e;
        }
        if ((!switched) && (readFromMasterWhenNoSlaves) && (switchToMasterConnection())) {
          exceptionCaught = null;
        }
        if (exceptionCaught != null) {
          throw exceptionCaught;
        }
      }
    }
    else if ((!isMasterConnection()) || (currentConnection.isClosed())) {
      boolean switched = true;
      SQLException exceptionCaught = null;
      try {
        switched = switchToMasterConnection();
      } catch (SQLException e) {
        switched = false;
        exceptionCaught = e;
      }
      if ((!switched) && (switchToSlavesConnectionIfNecessary())) {
        exceptionCaught = null;
      }
      if (exceptionCaught != null) {
        throw exceptionCaught;
      }
    }
    
    this.readOnly = readOnly;
    




    if ((readFromMasterWhenNoSlaves) && (isMasterConnection())) {
      currentConnection.setReadOnly(this.readOnly);
    }
  }
  
  public boolean isReadOnly() throws SQLException {
    return (!isMasterConnection()) || (readOnly);
  }
  
  private void resetReadFromMasterWhenNoSlaves() {
    readFromMasterWhenNoSlaves = ((slaveHosts.isEmpty()) || (readFromMasterWhenNoSlavesOriginal));
  }
  
  void pickNewConnection()
    throws SQLException
  {}
}
