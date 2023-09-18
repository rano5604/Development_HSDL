package com.mysql.jdbc;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;




























public abstract class MultiHostConnectionProxy
  implements InvocationHandler
{
  private static final String METHOD_GET_MULTI_HOST_SAFE_PROXY = "getMultiHostSafeProxy";
  private static final String METHOD_EQUALS = "equals";
  private static final String METHOD_HASH_CODE = "hashCode";
  private static final String METHOD_CLOSE = "close";
  private static final String METHOD_ABORT_INTERNAL = "abortInternal";
  private static final String METHOD_ABORT = "abort";
  private static final String METHOD_IS_CLOSED = "isClosed";
  private static final String METHOD_GET_AUTO_COMMIT = "getAutoCommit";
  private static final String METHOD_GET_CATALOG = "getCatalog";
  private static final String METHOD_GET_TRANSACTION_ISOLATION = "getTransactionIsolation";
  private static final String METHOD_GET_SESSION_MAX_ROWS = "getSessionMaxRows";
  List<String> hostList;
  Properties localProps;
  boolean autoReconnect = false;
  
  MySQLConnection thisAsConnection = null;
  MySQLConnection proxyConnection = null;
  
  MySQLConnection currentConnection = null;
  
  boolean isClosed = false;
  boolean closedExplicitly = false;
  String closedReason = null;
  


  protected Throwable lastExceptionDealtWith = null;
  private static Constructor<?> JDBC_4_MS_CONNECTION_CTOR;
  
  static
  {
    if (Util.isJdbc4()) {
      try {
        JDBC_4_MS_CONNECTION_CTOR = Class.forName("com.mysql.jdbc.JDBC4MultiHostMySQLConnection").getConstructor(new Class[] { MultiHostConnectionProxy.class });
      }
      catch (SecurityException e) {
        throw new RuntimeException(e);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
  }
  

  class JdbcInterfaceProxy
    implements InvocationHandler
  {
    Object invokeOn = null;
    
    JdbcInterfaceProxy(Object toInvokeOn) {
      invokeOn = toInvokeOn;
    }
    
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if ("equals".equals(method.getName()))
      {
        return Boolean.valueOf(args[0].equals(this));
      }
      
      synchronized (MultiHostConnectionProxy.this) {
        Object result = null;
        try
        {
          result = method.invoke(invokeOn, args);
          result = proxyIfReturnTypeIsJdbcInterface(method.getReturnType(), result);
        } catch (InvocationTargetException e) {
          dealWithInvocationException(e);
        }
        
        return result;
      }
    }
  }
  




  MultiHostConnectionProxy()
    throws SQLException
  {
    thisAsConnection = getNewWrapperForThisAsConnection();
  }
  






  MultiHostConnectionProxy(List<String> hosts, Properties props)
    throws SQLException
  {
    this();
    initializeHostsSpecs(hosts, props);
  }
  










  int initializeHostsSpecs(List<String> hosts, Properties props)
  {
    autoReconnect = (("true".equalsIgnoreCase(props.getProperty("autoReconnect"))) || ("true".equalsIgnoreCase(props.getProperty("autoReconnectForPools"))));
    
    hostList = hosts;
    int numHosts = hostList.size();
    
    localProps = ((Properties)props.clone());
    localProps.remove("HOST");
    localProps.remove("PORT");
    
    for (int i = 0; i < numHosts; i++) {
      localProps.remove("HOST." + (i + 1));
      localProps.remove("PORT." + (i + 1));
    }
    
    localProps.remove("NUM_HOSTS");
    
    return numHosts;
  }
  




  MySQLConnection getNewWrapperForThisAsConnection()
    throws SQLException
  {
    if ((Util.isJdbc4()) || (JDBC_4_MS_CONNECTION_CTOR != null)) {
      return (MySQLConnection)Util.handleNewInstance(JDBC_4_MS_CONNECTION_CTOR, new Object[] { this }, null);
    }
    
    return new MultiHostMySQLConnection(this);
  }
  







  protected MySQLConnection getProxy()
  {
    return proxyConnection != null ? proxyConnection : thisAsConnection;
  }
  






  protected final void setProxy(MySQLConnection proxyConn)
  {
    proxyConnection = proxyConn;
    propagateProxyDown(proxyConn);
  }
  






  protected void propagateProxyDown(MySQLConnection proxyConn)
  {
    currentConnection.setProxy(proxyConn);
  }
  









  Object proxyIfReturnTypeIsJdbcInterface(Class<?> returnType, Object toProxy)
  {
    if ((toProxy != null) && 
      (Util.isJdbcInterface(returnType))) {
      Class<?> toProxyClass = toProxy.getClass();
      return Proxy.newProxyInstance(toProxyClass.getClassLoader(), Util.getImplementedInterfaces(toProxyClass), getNewJdbcInterfaceProxy(toProxy));
    }
    
    return toProxy;
  }
  







  InvocationHandler getNewJdbcInterfaceProxy(Object toProxy)
  {
    return new JdbcInterfaceProxy(toProxy);
  }
  




  void dealWithInvocationException(InvocationTargetException e)
    throws SQLException, Throwable, InvocationTargetException
  {
    Throwable t = e.getTargetException();
    
    if (t != null) {
      if ((lastExceptionDealtWith != t) && (shouldExceptionTriggerConnectionSwitch(t))) {
        invalidateCurrentConnection();
        pickNewConnection();
        lastExceptionDealtWith = t;
      }
      throw t;
    }
    throw e;
  }
  














  synchronized void invalidateCurrentConnection()
    throws SQLException
  {
    invalidateConnection(currentConnection);
  }
  



  synchronized void invalidateConnection(MySQLConnection conn)
    throws SQLException
  {
    try
    {
      if ((conn != null) && (!conn.isClosed())) {
        conn.realClose(true, !conn.getAutoCommit(), true, null);
      }
    }
    catch (SQLException e) {}
  }
  












  synchronized ConnectionImpl createConnectionForHost(String hostPortSpec)
    throws SQLException
  {
    Properties connProps = (Properties)localProps.clone();
    
    String[] hostPortPair = NonRegisteringDriver.parseHostPortPair(hostPortSpec);
    String hostName = hostPortPair[0];
    String portNumber = hostPortPair[1];
    String dbName = connProps.getProperty("DBNAME");
    
    if (hostName == null) {
      throw new SQLException("Could not find a hostname to start a connection to");
    }
    if (portNumber == null) {
      portNumber = "3306";
    }
    
    connProps.setProperty("HOST", hostName);
    connProps.setProperty("PORT", portNumber);
    connProps.setProperty("HOST.1", hostName);
    connProps.setProperty("PORT.1", portNumber);
    connProps.setProperty("NUM_HOSTS", "1");
    connProps.setProperty("roundRobinLoadBalance", "false");
    
    ConnectionImpl conn = (ConnectionImpl)ConnectionImpl.getInstance(hostName, Integer.parseInt(portNumber), connProps, dbName, "jdbc:mysql://" + hostName + ":" + portNumber + "/");
    

    conn.setProxy(getProxy());
    
    return conn;
  }
  






  void syncSessionState(Connection source, Connection target)
    throws SQLException
  {
    if ((source == null) || (target == null)) {
      return;
    }
    
    boolean prevUseLocalSessionState = source.getUseLocalSessionState();
    source.setUseLocalSessionState(true);
    boolean readOnly = source.isReadOnly();
    source.setUseLocalSessionState(prevUseLocalSessionState);
    
    syncSessionState(source, target, readOnly);
  }
  








  void syncSessionState(Connection source, Connection target, boolean readOnly)
    throws SQLException
  {
    if (target != null) {
      target.setReadOnly(readOnly);
    }
    
    if ((source == null) || (target == null)) {
      return;
    }
    
    boolean prevUseLocalSessionState = source.getUseLocalSessionState();
    source.setUseLocalSessionState(true);
    
    target.setAutoCommit(source.getAutoCommit());
    target.setCatalog(source.getCatalog());
    target.setTransactionIsolation(source.getTransactionIsolation());
    target.setSessionMaxRows(source.getSessionMaxRows());
    
    source.setUseLocalSessionState(prevUseLocalSessionState);
  }
  


















  public synchronized Object invoke(Object proxy, Method method, Object[] args)
    throws Throwable
  {
    String methodName = method.getName();
    
    if ("getMultiHostSafeProxy".equals(methodName)) {
      return thisAsConnection;
    }
    
    if ("equals".equals(methodName))
    {
      return Boolean.valueOf(args[0].equals(this));
    }
    
    if ("hashCode".equals(methodName)) {
      return Integer.valueOf(hashCode());
    }
    
    if ("close".equals(methodName)) {
      doClose();
      isClosed = true;
      closedReason = "Connection explicitly closed.";
      closedExplicitly = true;
      return null;
    }
    
    if ("abortInternal".equals(methodName)) {
      doAbortInternal();
      currentConnection.abortInternal();
      isClosed = true;
      closedReason = "Connection explicitly closed.";
      return null;
    }
    
    if (("abort".equals(methodName)) && (args.length == 1)) {
      doAbort((Executor)args[0]);
      isClosed = true;
      closedReason = "Connection explicitly closed.";
      return null;
    }
    
    if ("isClosed".equals(methodName)) {
      return Boolean.valueOf(isClosed);
    }
    try
    {
      return invokeMore(proxy, method, args);
    } catch (InvocationTargetException e) {
      throw (e.getCause() != null ? e.getCause() : e);
    }
    catch (Exception e) {
      Class<?>[] declaredException = method.getExceptionTypes();
      for (Class<?> declEx : declaredException) {
        if (declEx.isAssignableFrom(e.getClass())) {
          throw e;
        }
      }
      throw new IllegalStateException(e.getMessage(), e);
    }
  }
  







  protected boolean allowedOnClosedConnection(Method method)
  {
    String methodName = method.getName();
    
    return (methodName.equals("getAutoCommit")) || (methodName.equals("getCatalog")) || (methodName.equals("getTransactionIsolation")) || (methodName.equals("getSessionMaxRows"));
  }
  
  abstract boolean shouldExceptionTriggerConnectionSwitch(Throwable paramThrowable);
  
  abstract boolean isMasterConnection();
  
  abstract void pickNewConnection()
    throws SQLException;
  
  abstract void doClose()
    throws SQLException;
  
  abstract void doAbortInternal()
    throws SQLException;
  
  abstract void doAbort(Executor paramExecutor)
    throws SQLException;
  
  abstract Object invokeMore(Object paramObject, Method paramMethod, Object[] paramArrayOfObject)
    throws Throwable;
}
