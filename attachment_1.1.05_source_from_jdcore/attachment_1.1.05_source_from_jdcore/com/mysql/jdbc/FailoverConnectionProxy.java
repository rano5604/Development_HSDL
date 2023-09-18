package com.mysql.jdbc;

import com.mysql.jdbc.log.Log;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;




























public class FailoverConnectionProxy
  extends MultiHostConnectionProxy
{
  private static final String METHOD_SET_READ_ONLY = "setReadOnly";
  private static final String METHOD_SET_AUTO_COMMIT = "setAutoCommit";
  private static final String METHOD_COMMIT = "commit";
  private static final String METHOD_ROLLBACK = "rollback";
  private static final int NO_CONNECTION_INDEX = -1;
  private static final int DEFAULT_PRIMARY_HOST_INDEX = 0;
  private int secondsBeforeRetryPrimaryHost;
  private long queriesBeforeRetryPrimaryHost;
  private boolean failoverReadOnly;
  private int retriesAllDown;
  private int currentHostIndex = -1;
  private int primaryHostIndex = 0;
  private Boolean explicitlyReadOnly = null;
  private boolean explicitlyAutoCommit = true;
  
  private boolean enableFallBackToPrimaryHost = true;
  private long primaryHostFailTimeMillis = 0L;
  private long queriesIssuedSinceFailover = 0L;
  private static Class<?>[] INTERFACES_TO_PROXY;
  
  static
  {
    if (Util.isJdbc4()) {
      try {
        INTERFACES_TO_PROXY = new Class[] { Class.forName("com.mysql.jdbc.JDBC4MySQLConnection") };
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    } else {
      INTERFACES_TO_PROXY = new Class[] { MySQLConnection.class };
    }
  }
  

  class FailoverJdbcInterfaceProxy
    extends MultiHostConnectionProxy.JdbcInterfaceProxy
  {
    FailoverJdbcInterfaceProxy(Object toInvokeOn)
    {
      super(toInvokeOn);
    }
    
    public Object invoke(Object proxy, Method method, Object[] args)
      throws Throwable
    {
      String methodName = method.getName();
      
      boolean isExecute = methodName.startsWith("execute");
      
      if ((connectedToSecondaryHost()) && (isExecute)) {
        incrementQueriesIssuedSinceFailover();
      }
      
      Object result = super.invoke(proxy, method, args);
      
      if ((explicitlyAutoCommit) && (isExecute) && (readyToFallBackToPrimaryHost()))
      {
        fallBackToPrimaryIfAvailable();
      }
      
      return result;
    }
  }
  
  public static Connection createProxyInstance(List<String> hosts, Properties props) throws SQLException {
    FailoverConnectionProxy connProxy = new FailoverConnectionProxy(hosts, props);
    
    return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(), INTERFACES_TO_PROXY, connProxy);
  }
  






  private FailoverConnectionProxy(List<String> hosts, Properties props)
    throws SQLException
  {
    super(hosts, props);
    
    ConnectionPropertiesImpl connProps = new ConnectionPropertiesImpl();
    connProps.initializeProperties(props);
    
    secondsBeforeRetryPrimaryHost = connProps.getSecondsBeforeRetryMaster();
    queriesBeforeRetryPrimaryHost = connProps.getQueriesBeforeRetryMaster();
    failoverReadOnly = connProps.getFailOverReadOnly();
    retriesAllDown = connProps.getRetriesAllDown();
    
    enableFallBackToPrimaryHost = ((secondsBeforeRetryPrimaryHost > 0) || (queriesBeforeRetryPrimaryHost > 0L));
    
    pickNewConnection();
    
    explicitlyAutoCommit = currentConnection.getAutoCommit();
  }
  





  MultiHostConnectionProxy.JdbcInterfaceProxy getNewJdbcInterfaceProxy(Object toProxy)
  {
    return new FailoverJdbcInterfaceProxy(toProxy);
  }
  





  boolean shouldExceptionTriggerConnectionSwitch(Throwable t)
  {
    if (!(t instanceof SQLException)) {
      return false;
    }
    
    String sqlState = ((SQLException)t).getSQLState();
    if ((sqlState != null) && 
      (sqlState.startsWith("08")))
    {
      return true;
    }
    


    if ((t instanceof CommunicationsException)) {
      return true;
    }
    
    return false;
  }
  



  boolean isMasterConnection()
  {
    return connectedToPrimaryHost();
  }
  




  synchronized void pickNewConnection()
    throws SQLException
  {
    if ((isClosed) && (closedExplicitly)) {
      return;
    }
    
    if ((!isConnected()) || (readyToFallBackToPrimaryHost())) {
      try {
        connectTo(primaryHostIndex);
      } catch (SQLException e) {
        resetAutoFallBackCounters();
        failOver(primaryHostIndex);
      }
    } else {
      failOver();
    }
  }
  






  synchronized ConnectionImpl createConnectionForHostIndex(int hostIndex)
    throws SQLException
  {
    return createConnectionForHost((String)hostList.get(hostIndex));
  }
  



  private synchronized void connectTo(int hostIndex)
    throws SQLException
  {
    try
    {
      switchCurrentConnectionTo(hostIndex, createConnectionForHostIndex(hostIndex));
    } catch (SQLException e) {
      if (currentConnection != null) {
        StringBuilder msg = new StringBuilder("Connection to ").append(isPrimaryHostIndex(hostIndex) ? "primary" : "secondary").append(" host '").append((String)hostList.get(hostIndex)).append("' failed");
        
        currentConnection.getLog().logWarn(msg.toString(), e);
      }
      throw e;
    }
  }
  






  private synchronized void switchCurrentConnectionTo(int hostIndex, MySQLConnection connection)
    throws SQLException
  {
    invalidateCurrentConnection();
    boolean readOnly;
    boolean readOnly;
    if (isPrimaryHostIndex(hostIndex)) {
      readOnly = explicitlyReadOnly == null ? false : explicitlyReadOnly.booleanValue(); } else { boolean readOnly;
      if (failoverReadOnly) {
        readOnly = true; } else { boolean readOnly;
        if (explicitlyReadOnly != null) {
          readOnly = explicitlyReadOnly.booleanValue(); } else { boolean readOnly;
          if (currentConnection != null) {
            readOnly = currentConnection.isReadOnly();
          } else
            readOnly = false;
        } } }
    syncSessionState(currentConnection, connection, readOnly);
    currentConnection = connection;
    currentHostIndex = hostIndex;
  }
  

  private synchronized void failOver()
    throws SQLException
  {
    failOver(currentHostIndex);
  }
  





  private synchronized void failOver(int failedHostIdx)
    throws SQLException
  {
    int prevHostIndex = currentHostIndex;
    int nextHostIndex = nextHost(failedHostIdx, false);
    int firstHostIndexTried = nextHostIndex;
    
    SQLException lastExceptionCaught = null;
    int attempts = 0;
    boolean gotConnection = false;
    boolean firstConnOrPassedByPrimaryHost = (prevHostIndex == -1) || (isPrimaryHostIndex(prevHostIndex));
    do {
      try {
        firstConnOrPassedByPrimaryHost = (firstConnOrPassedByPrimaryHost) || (isPrimaryHostIndex(nextHostIndex));
        
        connectTo(nextHostIndex);
        
        if ((firstConnOrPassedByPrimaryHost) && (connectedToSecondaryHost())) {
          resetAutoFallBackCounters();
        }
        gotConnection = true;
      }
      catch (SQLException e) {
        lastExceptionCaught = e;
        
        if (shouldExceptionTriggerConnectionSwitch(e)) {
          int newNextHostIndex = nextHost(nextHostIndex, attempts > 0);
          
          if ((newNextHostIndex == firstHostIndexTried) && (newNextHostIndex == (newNextHostIndex = nextHost(nextHostIndex, true)))) {
            attempts++;
            try
            {
              Thread.sleep(250L);
            }
            catch (InterruptedException ie) {}
          }
          
          nextHostIndex = newNextHostIndex;
        }
        else {
          throw e;
        }
      }
    } while ((attempts < retriesAllDown) && (!gotConnection));
    
    if (!gotConnection) {
      throw lastExceptionCaught;
    }
  }
  


  synchronized void fallBackToPrimaryIfAvailable()
  {
    MySQLConnection connection = null;
    try {
      connection = createConnectionForHostIndex(primaryHostIndex);
      switchCurrentConnectionTo(primaryHostIndex, connection);
    } catch (SQLException e1) {
      if (connection != null) {
        try {
          connection.close();
        }
        catch (SQLException e2) {}
      }
      
      resetAutoFallBackCounters();
    }
  }
  











  private int nextHost(int currHostIdx, boolean vouchForPrimaryHost)
  {
    int nextHostIdx = (currHostIdx + 1) % hostList.size();
    if ((isPrimaryHostIndex(nextHostIdx)) && (isConnected()) && (!vouchForPrimaryHost) && (enableFallBackToPrimaryHost) && (!readyToFallBackToPrimaryHost()))
    {
      nextHostIdx = nextHost(nextHostIdx, vouchForPrimaryHost);
    }
    return nextHostIdx;
  }
  


  synchronized void incrementQueriesIssuedSinceFailover()
  {
    queriesIssuedSinceFailover += 1L;
  }
  



  synchronized boolean readyToFallBackToPrimaryHost()
  {
    return (enableFallBackToPrimaryHost) && (connectedToSecondaryHost()) && ((secondsBeforeRetryPrimaryHostIsMet()) || (queriesBeforeRetryPrimaryHostIsMet()));
  }
  


  synchronized boolean isConnected()
  {
    return currentHostIndex != -1;
  }
  





  synchronized boolean isPrimaryHostIndex(int hostIndex)
  {
    return hostIndex == primaryHostIndex;
  }
  


  synchronized boolean connectedToPrimaryHost()
  {
    return isPrimaryHostIndex(currentHostIndex);
  }
  


  synchronized boolean connectedToSecondaryHost()
  {
    return (currentHostIndex >= 0) && (!isPrimaryHostIndex(currentHostIndex));
  }
  


  private synchronized boolean secondsBeforeRetryPrimaryHostIsMet()
  {
    return (secondsBeforeRetryPrimaryHost > 0) && (Util.secondsSinceMillis(primaryHostFailTimeMillis) >= secondsBeforeRetryPrimaryHost);
  }
  


  private synchronized boolean queriesBeforeRetryPrimaryHostIsMet()
  {
    return (queriesBeforeRetryPrimaryHost > 0L) && (queriesIssuedSinceFailover >= queriesBeforeRetryPrimaryHost);
  }
  


  private synchronized void resetAutoFallBackCounters()
  {
    primaryHostFailTimeMillis = System.currentTimeMillis();
    queriesIssuedSinceFailover = 0L;
  }
  


  synchronized void doClose()
    throws SQLException
  {
    currentConnection.close();
  }
  


  synchronized void doAbortInternal()
    throws SQLException
  {
    currentConnection.abortInternal();
  }
  


  synchronized void doAbort(Executor executor)
    throws SQLException
  {
    currentConnection.abort(executor);
  }
  



  public synchronized Object invokeMore(Object proxy, Method method, Object[] args)
    throws Throwable
  {
    String methodName = method.getName();
    
    if ("setReadOnly".equals(methodName)) {
      explicitlyReadOnly = ((Boolean)args[0]);
      if ((failoverReadOnly) && (connectedToSecondaryHost())) {
        return null;
      }
    }
    
    if ((isClosed) && (!allowedOnClosedConnection(method))) {
      if ((autoReconnect) && (!closedExplicitly)) {
        currentHostIndex = -1;
        pickNewConnection();
        isClosed = false;
        closedReason = null;
      } else {
        String reason = "No operations allowed after connection closed.";
        if (closedReason != null) {
          reason = reason + "  " + closedReason;
        }
        throw SQLError.createSQLException(reason, "08003", null);
      }
    }
    
    Object result = null;
    try
    {
      result = method.invoke(thisAsConnection, args);
      result = proxyIfReturnTypeIsJdbcInterface(method.getReturnType(), result);
    } catch (InvocationTargetException e) {
      dealWithInvocationException(e);
    }
    
    if ("setAutoCommit".equals(methodName)) {
      explicitlyAutoCommit = ((Boolean)args[0]).booleanValue();
    }
    
    if (((explicitlyAutoCommit) || ("commit".equals(methodName)) || ("rollback".equals(methodName))) && (readyToFallBackToPrimaryHost()))
    {
      fallBackToPrimaryIfAvailable();
    }
    
    return result;
  }
}
