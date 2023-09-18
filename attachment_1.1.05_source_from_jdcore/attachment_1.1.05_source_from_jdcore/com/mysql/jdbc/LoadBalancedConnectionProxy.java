package com.mysql.jdbc;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;
































public class LoadBalancedConnectionProxy
  extends MultiHostConnectionProxy
  implements PingTarget
{
  private ConnectionGroup connectionGroup = null;
  private long connectionGroupProxyID = 0L;
  
  protected Map<String, ConnectionImpl> liveConnections;
  private Map<String, Integer> hostsToListIndexMap;
  private Map<ConnectionImpl, String> connectionsToHostsMap;
  private long totalPhysicalConnections = 0L;
  
  private long[] responseTimes;
  private int retriesAllDown;
  private BalanceStrategy balancer;
  private int autoCommitSwapThreshold = 0;
  
  public static final String BLACKLIST_TIMEOUT_PROPERTY_KEY = "loadBalanceBlacklistTimeout";
  private int globalBlacklistTimeout = 0;
  private static Map<String, Long> globalBlacklist = new HashMap();
  public static final String HOST_REMOVAL_GRACE_PERIOD_PROPERTY_KEY = "loadBalanceHostRemovalGracePeriod";
  private int hostRemovalGracePeriod = 0;
  
  private Set<String> hostsToRemove = new HashSet();
  
  private boolean inTransaction = false;
  private long transactionStartTime = 0L;
  private long transactionCount = 0L;
  
  private LoadBalanceExceptionChecker exceptionChecker;
  private static Constructor<?> JDBC_4_LB_CONNECTION_CTOR;
  private static Class<?>[] INTERFACES_TO_PROXY;
  
  static
  {
    if (Util.isJdbc4()) {
      try {
        JDBC_4_LB_CONNECTION_CTOR = Class.forName("com.mysql.jdbc.JDBC4LoadBalancedMySQLConnection").getConstructor(new Class[] { LoadBalancedConnectionProxy.class });
        
        INTERFACES_TO_PROXY = new Class[] { LoadBalancedConnection.class, Class.forName("com.mysql.jdbc.JDBC4MySQLConnection") };
      } catch (SecurityException e) {
        throw new RuntimeException(e);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    } else {
      INTERFACES_TO_PROXY = new Class[] { LoadBalancedConnection.class };
    }
  }
  
  public static LoadBalancedConnection createProxyInstance(List<String> hosts, Properties props) throws SQLException {
    LoadBalancedConnectionProxy connProxy = new LoadBalancedConnectionProxy(hosts, props);
    
    return (LoadBalancedConnection)Proxy.newProxyInstance(LoadBalancedConnection.class.getClassLoader(), INTERFACES_TO_PROXY, connProxy);
  }
  









  private LoadBalancedConnectionProxy(List<String> hosts, Properties props)
    throws SQLException
  {
    String group = props.getProperty("loadBalanceConnectionGroup", null);
    boolean enableJMX = false;
    String enableJMXAsString = props.getProperty("loadBalanceEnableJMX", "false");
    try {
      enableJMX = Boolean.parseBoolean(enableJMXAsString);
    } catch (Exception e) {
      throw SQLError.createSQLException(Messages.getString("LoadBalancedConnectionProxy.badValueForLoadBalanceEnableJMX", new Object[] { enableJMXAsString }), "S1009", null);
    }
    


    if (group != null) {
      connectionGroup = ConnectionGroupManager.getConnectionGroupInstance(group);
      if (enableJMX) {
        ConnectionGroupManager.registerJmx();
      }
      connectionGroupProxyID = connectionGroup.registerConnectionProxy(this, hosts);
      hosts = new ArrayList(connectionGroup.getInitialHosts());
    }
    

    int numHosts = initializeHostsSpecs(hosts, props);
    
    liveConnections = new HashMap(numHosts);
    hostsToListIndexMap = new HashMap(numHosts);
    for (int i = 0; i < numHosts; i++) {
      hostsToListIndexMap.put(hostList.get(i), Integer.valueOf(i));
    }
    connectionsToHostsMap = new HashMap(numHosts);
    responseTimes = new long[numHosts];
    
    String retriesAllDownAsString = localProps.getProperty("retriesAllDown", "120");
    try {
      retriesAllDown = Integer.parseInt(retriesAllDownAsString);
    } catch (NumberFormatException nfe) {
      throw SQLError.createSQLException(Messages.getString("LoadBalancedConnectionProxy.badValueForRetriesAllDown", new Object[] { retriesAllDownAsString }), "S1009", null);
    }
    


    String blacklistTimeoutAsString = localProps.getProperty("loadBalanceBlacklistTimeout", "0");
    try {
      globalBlacklistTimeout = Integer.parseInt(blacklistTimeoutAsString);
    } catch (NumberFormatException nfe) {
      throw SQLError.createSQLException(Messages.getString("LoadBalancedConnectionProxy.badValueForLoadBalanceBlacklistTimeout", new Object[] { blacklistTimeoutAsString }), "S1009", null);
    }
    


    String hostRemovalGracePeriodAsString = localProps.getProperty("loadBalanceHostRemovalGracePeriod", "15000");
    try {
      hostRemovalGracePeriod = Integer.parseInt(hostRemovalGracePeriodAsString);
    } catch (NumberFormatException nfe) {
      throw SQLError.createSQLException(Messages.getString("LoadBalancedConnectionProxy.badValueForLoadBalanceHostRemovalGracePeriod", new Object[] { hostRemovalGracePeriodAsString }), "S1009", null);
    }
    

    String strategy = localProps.getProperty("loadBalanceStrategy", "random");
    if ("random".equals(strategy)) {
      balancer = ((BalanceStrategy)Util.loadExtensions(null, props, RandomBalanceStrategy.class.getName(), "InvalidLoadBalanceStrategy", null).get(0));
    }
    else if ("bestResponseTime".equals(strategy)) {
      balancer = ((BalanceStrategy)Util.loadExtensions(null, props, BestResponseTimeBalanceStrategy.class.getName(), "InvalidLoadBalanceStrategy", null).get(0));
    }
    else if ("serverAffinity".equals(strategy)) {
      balancer = ((BalanceStrategy)Util.loadExtensions(null, props, ServerAffinityStrategy.class.getName(), "InvalidLoadBalanceStrategy", null).get(0));
    }
    else {
      balancer = ((BalanceStrategy)Util.loadExtensions(null, props, strategy, "InvalidLoadBalanceStrategy", null).get(0));
    }
    
    String autoCommitSwapThresholdAsString = props.getProperty("loadBalanceAutoCommitStatementThreshold", "0");
    try {
      autoCommitSwapThreshold = Integer.parseInt(autoCommitSwapThresholdAsString);
    } catch (NumberFormatException nfe) {
      throw SQLError.createSQLException(Messages.getString("LoadBalancedConnectionProxy.badValueForLoadBalanceAutoCommitStatementThreshold", new Object[] { autoCommitSwapThresholdAsString }), "S1009", null);
    }
    

    String autoCommitSwapRegex = props.getProperty("loadBalanceAutoCommitStatementRegex", "");
    if (!"".equals(autoCommitSwapRegex)) {
      try {
        "".matches(autoCommitSwapRegex);
      } catch (Exception e) {
        throw SQLError.createSQLException(Messages.getString("LoadBalancedConnectionProxy.badValueForLoadBalanceAutoCommitStatementRegex", new Object[] { autoCommitSwapRegex }), "S1009", null);
      }
    }
    


    if (autoCommitSwapThreshold > 0) {
      String statementInterceptors = localProps.getProperty("statementInterceptors");
      if (statementInterceptors == null) {
        localProps.setProperty("statementInterceptors", "com.mysql.jdbc.LoadBalancedAutoCommitInterceptor");
      } else if (statementInterceptors.length() > 0) {
        localProps.setProperty("statementInterceptors", statementInterceptors + ",com.mysql.jdbc.LoadBalancedAutoCommitInterceptor");
      }
      props.setProperty("statementInterceptors", localProps.getProperty("statementInterceptors"));
    }
    

    balancer.init(null, props);
    
    String lbExceptionChecker = localProps.getProperty("loadBalanceExceptionChecker", "com.mysql.jdbc.StandardLoadBalanceExceptionChecker");
    exceptionChecker = ((LoadBalanceExceptionChecker)Util.loadExtensions(null, props, lbExceptionChecker, "InvalidLoadBalanceExceptionChecker", null).get(0));
    

    pickNewConnection();
  }
  





  MySQLConnection getNewWrapperForThisAsConnection()
    throws SQLException
  {
    if ((Util.isJdbc4()) || (JDBC_4_LB_CONNECTION_CTOR != null)) {
      return (MySQLConnection)Util.handleNewInstance(JDBC_4_LB_CONNECTION_CTOR, new Object[] { this }, null);
    }
    return new LoadBalancedMySQLConnection(this);
  }
  






  protected void propagateProxyDown(MySQLConnection proxyConn)
  {
    for (MySQLConnection c : liveConnections.values()) {
      c.setProxy(proxyConn);
    }
  }
  






  boolean shouldExceptionTriggerConnectionSwitch(Throwable t)
  {
    return ((t instanceof SQLException)) && (exceptionChecker.shouldExceptionTriggerFailover((SQLException)t));
  }
  



  boolean isMasterConnection()
  {
    return true;
  }
  





  synchronized void invalidateConnection(MySQLConnection conn)
    throws SQLException
  {
    super.invalidateConnection(conn);
    

    if (isGlobalBlacklistEnabled()) {
      addToGlobalBlacklist((String)connectionsToHostsMap.get(conn));
    }
    

    liveConnections.remove(connectionsToHostsMap.get(conn));
    Object mappedHost = connectionsToHostsMap.remove(conn);
    if ((mappedHost != null) && (hostsToListIndexMap.containsKey(mappedHost))) {
      int hostIndex = ((Integer)hostsToListIndexMap.get(mappedHost)).intValue();
      
      synchronized (responseTimes) {
        responseTimes[hostIndex] = 0L;
      }
    }
  }
  




  synchronized void pickNewConnection()
    throws SQLException
  {
    if ((isClosed) && (closedExplicitly)) {
      return;
    }
    
    if (currentConnection == null) {
      currentConnection = balancer.pickConnection(this, Collections.unmodifiableList(hostList), Collections.unmodifiableMap(liveConnections), (long[])responseTimes.clone(), retriesAllDown);
      
      return;
    }
    
    if (currentConnection.isClosed()) {
      invalidateCurrentConnection();
    }
    
    int pingTimeout = currentConnection.getLoadBalancePingTimeout();
    boolean pingBeforeReturn = currentConnection.getLoadBalanceValidateConnectionOnSwapServer();
    
    int hostsTried = 0; for (int hostsToTry = hostList.size(); hostsTried < hostsToTry; hostsTried++) {
      ConnectionImpl newConn = null;
      try {
        newConn = balancer.pickConnection(this, Collections.unmodifiableList(hostList), Collections.unmodifiableMap(liveConnections), (long[])responseTimes.clone(), retriesAllDown);
        

        if (currentConnection != null) {
          if (pingBeforeReturn) {
            if (pingTimeout == 0) {
              newConn.ping();
            } else {
              newConn.pingInternal(true, pingTimeout);
            }
          }
          
          syncSessionState(currentConnection, newConn);
        }
        
        currentConnection = newConn;
        return;
      }
      catch (SQLException e) {
        if ((shouldExceptionTriggerConnectionSwitch(e)) && (newConn != null))
        {
          invalidateConnection(newConn);
        }
      }
    }
    

    isClosed = true;
    closedReason = "Connection closed after inability to pick valid new connection during load-balance.";
  }
  






  public synchronized ConnectionImpl createConnectionForHost(String hostPortSpec)
    throws SQLException
  {
    ConnectionImpl conn = super.createConnectionForHost(hostPortSpec);
    
    liveConnections.put(hostPortSpec, conn);
    connectionsToHostsMap.put(conn, hostPortSpec);
    
    totalPhysicalConnections += 1L;
    
    for (StatementInterceptorV2 stmtInterceptor : conn.getStatementInterceptorsInstances()) {
      if ((stmtInterceptor instanceof LoadBalancedAutoCommitInterceptor)) {
        ((LoadBalancedAutoCommitInterceptor)stmtInterceptor).resumeCounters();
        break;
      }
    }
    
    return conn;
  }
  
  void syncSessionState(Connection source, Connection target, boolean readOnly) throws SQLException
  {
    LoadBalancedAutoCommitInterceptor lbAutoCommitStmtInterceptor = null;
    for (StatementInterceptorV2 stmtInterceptor : ((MySQLConnection)target).getStatementInterceptorsInstances()) {
      if ((stmtInterceptor instanceof LoadBalancedAutoCommitInterceptor)) {
        lbAutoCommitStmtInterceptor = (LoadBalancedAutoCommitInterceptor)stmtInterceptor;
        lbAutoCommitStmtInterceptor.pauseCounters();
        break;
      }
    }
    super.syncSessionState(source, target, readOnly);
    if (lbAutoCommitStmtInterceptor != null) {
      lbAutoCommitStmtInterceptor.resumeCounters();
    }
  }
  



  private synchronized void closeAllConnections()
  {
    for (MySQLConnection c : liveConnections.values()) {
      try {
        c.close();
      }
      catch (SQLException e) {}
    }
    
    if (!isClosed) {
      balancer.destroy();
      if (connectionGroup != null) {
        connectionGroup.closeConnectionProxy(this);
      }
    }
    
    liveConnections.clear();
    connectionsToHostsMap.clear();
  }
  



  synchronized void doClose()
  {
    closeAllConnections();
  }
  




  synchronized void doAbortInternal()
  {
    for (MySQLConnection c : liveConnections.values()) {
      try {
        c.abortInternal();
      }
      catch (SQLException e) {}
    }
    
    if (!isClosed) {
      balancer.destroy();
      if (connectionGroup != null) {
        connectionGroup.closeConnectionProxy(this);
      }
    }
    
    liveConnections.clear();
    connectionsToHostsMap.clear();
  }
  




  synchronized void doAbort(Executor executor)
  {
    for (MySQLConnection c : liveConnections.values()) {
      try {
        c.abort(executor);
      }
      catch (SQLException e) {}
    }
    
    if (!isClosed) {
      balancer.destroy();
      if (connectionGroup != null) {
        connectionGroup.closeConnectionProxy(this);
      }
    }
    
    liveConnections.clear();
    connectionsToHostsMap.clear();
  }
  




  public synchronized Object invokeMore(Object proxy, Method method, Object[] args)
    throws Throwable
  {
    String methodName = method.getName();
    
    if ((isClosed) && (!allowedOnClosedConnection(method)) && (method.getExceptionTypes().length > 0)) {
      if ((autoReconnect) && (!closedExplicitly))
      {
        currentConnection = null;
        pickNewConnection();
        isClosed = false;
        closedReason = null;
      } else {
        String reason = "No operations allowed after connection closed.";
        if (closedReason != null) {
          reason = reason + " " + closedReason;
        }
        throw SQLError.createSQLException(reason, "08003", null);
      }
    }
    
    if (!inTransaction) {
      inTransaction = true;
      transactionStartTime = System.nanoTime();
      transactionCount += 1L;
    }
    
    Object result = null;
    try
    {
      result = method.invoke(thisAsConnection, args);
      
      if (result != null) {
        if ((result instanceof Statement)) {
          ((Statement)result).setPingTarget(this);
        }
        result = proxyIfReturnTypeIsJdbcInterface(method.getReturnType(), result);
      }
    }
    catch (InvocationTargetException e) {
      dealWithInvocationException(e);
    }
    finally {
      if (("commit".equals(methodName)) || ("rollback".equals(methodName))) {
        inTransaction = false;
        

        String host = (String)connectionsToHostsMap.get(currentConnection);
        
        if (host != null) {
          synchronized (responseTimes) {
            Integer hostIndex = (Integer)hostsToListIndexMap.get(host);
            
            if ((hostIndex != null) && (hostIndex.intValue() < responseTimes.length)) {
              responseTimes[hostIndex.intValue()] = (System.nanoTime() - transactionStartTime);
            }
          }
        }
        pickNewConnection();
      }
    }
    
    return result;
  }
  

  public synchronized void doPing()
    throws SQLException
  {
    SQLException se = null;
    boolean foundHost = false;
    int pingTimeout = currentConnection.getLoadBalancePingTimeout();
    
    for (Iterator<String> i = hostList.iterator(); i.hasNext();) {
      String host = (String)i.next();
      ConnectionImpl conn = (ConnectionImpl)liveConnections.get(host);
      if (conn != null)
      {
        try
        {
          if (pingTimeout == 0) {
            conn.ping();
          } else {
            conn.pingInternal(true, pingTimeout);
          }
          foundHost = true;
        }
        catch (SQLException e) {
          if (host.equals(connectionsToHostsMap.get(currentConnection)))
          {
            closeAllConnections();
            isClosed = true;
            closedReason = "Connection closed because ping of current connection failed.";
            throw e;
          }
          

          if (e.getMessage().equals(Messages.getString("Connection.exceededConnectionLifetime")))
          {
            if (se == null) {
              se = e;
            }
          }
          else {
            se = e;
            if (isGlobalBlacklistEnabled()) {
              addToGlobalBlacklist(host);
            }
          }
          
          liveConnections.remove(connectionsToHostsMap.get(conn));
        }
      }
    }
    
    if (!foundHost) {
      closeAllConnections();
      isClosed = true;
      closedReason = "Connection closed due to inability to ping any active connections.";
      
      if (se != null) {
        throw se;
      }
      
      ((ConnectionImpl)currentConnection).throwConnectionClosedException();
    }
  }
  







  public void addToGlobalBlacklist(String host, long timeout)
  {
    if (isGlobalBlacklistEnabled()) {
      synchronized (globalBlacklist) {
        globalBlacklist.put(host, Long.valueOf(timeout));
      }
    }
  }
  





  public void addToGlobalBlacklist(String host)
  {
    addToGlobalBlacklist(host, System.currentTimeMillis() + globalBlacklistTimeout);
  }
  


  public boolean isGlobalBlacklistEnabled()
  {
    return globalBlacklistTimeout > 0;
  }
  





  public synchronized Map<String, Long> getGlobalBlacklist()
  {
    if (!isGlobalBlacklistEnabled()) {
      if (hostsToRemove.isEmpty()) {
        return new HashMap(1);
      }
      HashMap<String, Long> fakedBlacklist = new HashMap();
      for (String h : hostsToRemove) {
        fakedBlacklist.put(h, Long.valueOf(System.currentTimeMillis() + 5000L));
      }
      return fakedBlacklist;
    }
    

    Map<String, Long> blacklistClone = new HashMap(globalBlacklist.size());
    
    synchronized (globalBlacklist) {
      blacklistClone.putAll(globalBlacklist);
    }
    Set<String> keys = blacklistClone.keySet();
    

    keys.retainAll(hostList);
    

    for (Iterator<String> i = keys.iterator(); i.hasNext();) {
      String host = (String)i.next();
      
      Long timeout = (Long)globalBlacklist.get(host);
      if ((timeout != null) && (timeout.longValue() < System.currentTimeMillis()))
      {
        synchronized (globalBlacklist) {
          globalBlacklist.remove(host);
        }
        i.remove();
      }
    }
    
    if (keys.size() == hostList.size())
    {

      return new HashMap(1);
    }
    
    return blacklistClone;
  }
  





  public void removeHostWhenNotInUse(String hostPortPair)
    throws SQLException
  {
    if (hostRemovalGracePeriod <= 0) {
      removeHost(hostPortPair);
      return;
    }
    
    int timeBetweenChecks = hostRemovalGracePeriod > 1000 ? 1000 : hostRemovalGracePeriod;
    
    synchronized (this) {
      addToGlobalBlacklist(hostPortPair, System.currentTimeMillis() + hostRemovalGracePeriod + timeBetweenChecks);
      
      long cur = System.currentTimeMillis();
      
      while (System.currentTimeMillis() < cur + hostRemovalGracePeriod) {
        hostsToRemove.add(hostPortPair);
        
        if (!hostPortPair.equals(currentConnection.getHostPortPair())) {
          removeHost(hostPortPair);
          return;
        }
        try
        {
          Thread.sleep(timeBetweenChecks);
        }
        catch (InterruptedException e) {}
      }
    }
    

    removeHost(hostPortPair);
  }
  





  public synchronized void removeHost(String hostPortPair)
    throws SQLException
  {
    if ((connectionGroup != null) && 
      (connectionGroup.getInitialHosts().size() == 1) && (connectionGroup.getInitialHosts().contains(hostPortPair))) {
      throw SQLError.createSQLException("Cannot remove only configured host.", null);
    }
    

    hostsToRemove.add(hostPortPair);
    
    connectionsToHostsMap.remove(liveConnections.remove(hostPortPair));
    if (hostsToListIndexMap.remove(hostPortPair) != null) {
      long[] newResponseTimes = new long[responseTimes.length - 1];
      int newIdx = 0;
      for (String h : hostList) {
        if (!hostsToRemove.contains(h)) {
          Integer idx = (Integer)hostsToListIndexMap.get(h);
          if ((idx != null) && (idx.intValue() < responseTimes.length)) {
            newResponseTimes[newIdx] = responseTimes[idx.intValue()];
          }
          hostsToListIndexMap.put(h, Integer.valueOf(newIdx++));
        }
      }
      responseTimes = newResponseTimes;
    }
    
    if (hostPortPair.equals(currentConnection.getHostPortPair())) {
      invalidateConnection(currentConnection);
      pickNewConnection();
    }
  }
  





  public synchronized boolean addHost(String hostPortPair)
  {
    if (hostsToListIndexMap.containsKey(hostPortPair)) {
      return false;
    }
    
    long[] newResponseTimes = new long[responseTimes.length + 1];
    System.arraycopy(responseTimes, 0, newResponseTimes, 0, responseTimes.length);
    
    responseTimes = newResponseTimes;
    if (!hostList.contains(hostPortPair)) {
      hostList.add(hostPortPair);
    }
    hostsToListIndexMap.put(hostPortPair, Integer.valueOf(responseTimes.length - 1));
    hostsToRemove.remove(hostPortPair);
    
    return true;
  }
  
  public synchronized boolean inTransaction() {
    return inTransaction;
  }
  
  public synchronized long getTransactionCount() {
    return transactionCount;
  }
  
  public synchronized long getActivePhysicalConnectionCount() {
    return liveConnections.size();
  }
  
  public synchronized long getTotalPhysicalConnectionCount() {
    return totalPhysicalConnections;
  }
  
  public synchronized long getConnectionGroupProxyID() {
    return connectionGroupProxyID;
  }
  
  public synchronized String getCurrentActiveHost() {
    MySQLConnection c = currentConnection;
    if (c != null) {
      Object o = connectionsToHostsMap.get(c);
      if (o != null) {
        return o.toString();
      }
    }
    return null;
  }
  
  public synchronized long getCurrentTransactionDuration() {
    if ((inTransaction) && (transactionStartTime > 0L)) {
      return System.nanoTime() - transactionStartTime;
    }
    return 0L;
  }
  

  private static class NullLoadBalancedConnectionProxy
    implements InvocationHandler
  {
    public NullLoadBalancedConnectionProxy() {}
    
    public Object invoke(Object proxy, Method method, Object[] args)
      throws Throwable
    {
      SQLException exceptionToThrow = SQLError.createSQLException(Messages.getString("LoadBalancedConnectionProxy.unusableConnection"), "25000", 1000001, true, null);
      
      Class<?>[] declaredException = method.getExceptionTypes();
      for (Class<?> declEx : declaredException) {
        if (declEx.isAssignableFrom(exceptionToThrow.getClass())) {
          throw exceptionToThrow;
        }
      }
      throw new IllegalStateException(exceptionToThrow.getMessage(), exceptionToThrow);
    }
  }
  
  private static LoadBalancedConnection nullLBConnectionInstance = null;
  
  static synchronized LoadBalancedConnection getNullLoadBalancedConnectionInstance() {
    if (nullLBConnectionInstance == null) {
      nullLBConnectionInstance = (LoadBalancedConnection)Proxy.newProxyInstance(LoadBalancedConnection.class.getClassLoader(), INTERFACES_TO_PROXY, new NullLoadBalancedConnectionProxy());
    }
    
    return nullLBConnectionInstance;
  }
}
