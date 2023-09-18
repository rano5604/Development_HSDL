package com.mysql.fabric.jdbc;

import com.mysql.fabric.FabricCommunicationException;
import com.mysql.fabric.FabricConnection;
import com.mysql.fabric.Server;
import com.mysql.fabric.ServerGroup;
import com.mysql.fabric.ShardMapping;
import com.mysql.fabric.proto.xmlrpc.XmlRpcClient;
import com.mysql.jdbc.Buffer;
import com.mysql.jdbc.CachedResultSetMetaData;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.ConnectionProperties;
import com.mysql.jdbc.ConnectionPropertiesImpl;
import com.mysql.jdbc.ExceptionInterceptor;
import com.mysql.jdbc.Extension;
import com.mysql.jdbc.Field;
import com.mysql.jdbc.MySQLConnection;
import com.mysql.jdbc.MysqlIO;
import com.mysql.jdbc.ReplicationConnection;
import com.mysql.jdbc.ReplicationConnectionGroup;
import com.mysql.jdbc.ReplicationConnectionGroupManager;
import com.mysql.jdbc.ReplicationConnectionProxy;
import com.mysql.jdbc.ResultSetInternalMethods;
import com.mysql.jdbc.SQLError;
import com.mysql.jdbc.ServerPreparedStatement;
import com.mysql.jdbc.SingleByteCharsetConverter;
import com.mysql.jdbc.StatementImpl;
import com.mysql.jdbc.StatementInterceptorV2;
import com.mysql.jdbc.Util;
import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;
import com.mysql.jdbc.log.Log;
import com.mysql.jdbc.log.LogFactory;
import com.mysql.jdbc.profiler.ProfilerEventHandler;
import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.Timer;
import java.util.concurrent.Executor;


































public class FabricMySQLConnectionProxy
  extends ConnectionPropertiesImpl
  implements FabricMySQLConnection, FabricMySQLConnectionProperties
{
  private static final long serialVersionUID = 5845485979107347258L;
  private Log log;
  protected FabricConnection fabricConnection;
  protected boolean closed = false;
  
  protected boolean transactionInProgress = false;
  

  protected Map<ServerGroup, ReplicationConnection> serverConnections = new HashMap();
  

  protected ReplicationConnection currentConnection;
  

  protected String shardKey;
  
  protected String shardTable;
  
  protected String serverGroupName;
  
  protected Set<String> queryTables = new HashSet();
  
  protected ServerGroup serverGroup;
  
  protected String host;
  
  protected String port;
  
  protected String username;
  protected String password;
  protected String database;
  protected ShardMapping shardMapping;
  protected boolean readOnly = false;
  protected boolean autoCommit = true;
  protected int transactionIsolation = 4;
  
  private String fabricShardKey;
  private String fabricShardTable;
  private String fabricServerGroup;
  private String fabricProtocol;
  private String fabricUsername;
  private String fabricPassword;
  private boolean reportErrors = false;
  


  private static final Set<String> replConnGroupLocks = Collections.synchronizedSet(new HashSet());
  private static final Class<?> JDBC4_NON_TRANSIENT_CONN_EXCEPTION;
  
  static
  {
    Class<?> clazz = null;
    try {
      if (Util.isJdbc4()) {
        clazz = Class.forName("com.mysql.jdbc.exceptions.jdbc4.MySQLNonTransientConnectionException");
      }
    }
    catch (ClassNotFoundException e) {}
    
    JDBC4_NON_TRANSIENT_CONN_EXCEPTION = clazz;
  }
  
  public FabricMySQLConnectionProxy(Properties props) throws SQLException
  {
    fabricShardKey = props.getProperty("fabricShardKey");
    fabricShardTable = props.getProperty("fabricShardTable");
    fabricServerGroup = props.getProperty("fabricServerGroup");
    fabricProtocol = props.getProperty("fabricProtocol");
    fabricUsername = props.getProperty("fabricUsername");
    fabricPassword = props.getProperty("fabricPassword");
    reportErrors = Boolean.valueOf(props.getProperty("fabricReportErrors")).booleanValue();
    props.remove("fabricShardKey");
    props.remove("fabricShardTable");
    props.remove("fabricServerGroup");
    props.remove("fabricProtocol");
    props.remove("fabricUsername");
    props.remove("fabricPassword");
    props.remove("fabricReportErrors");
    
    host = props.getProperty("HOST");
    port = props.getProperty("PORT");
    username = props.getProperty("user");
    password = props.getProperty("password");
    database = props.getProperty("DBNAME");
    if (username == null) {
      username = "";
    }
    if (password == null) {
      password = "";
    }
    

    String exceptionInterceptors = props.getProperty("exceptionInterceptors");
    if ((exceptionInterceptors == null) || ("null".equals("exceptionInterceptors"))) {
      exceptionInterceptors = "";
    } else {
      exceptionInterceptors = exceptionInterceptors + ",";
    }
    exceptionInterceptors = exceptionInterceptors + "com.mysql.fabric.jdbc.ErrorReportingExceptionInterceptor";
    props.setProperty("exceptionInterceptors", exceptionInterceptors);
    
    initializeProperties(props);
    

    if ((fabricServerGroup != null) && (fabricShardTable != null)) {
      throw SQLError.createSQLException("Server group and shard table are mutually exclusive. Only one may be provided.", "08004", null, getExceptionInterceptor(), this);
    }
    
    try
    {
      String url = fabricProtocol + "://" + host + ":" + port;
      fabricConnection = new FabricConnection(url, fabricUsername, fabricPassword);
    } catch (FabricCommunicationException ex) {
      throw SQLError.createSQLException("Unable to establish connection to the Fabric server", "08004", ex, getExceptionInterceptor(), this);
    }
    


    log = LogFactory.getLogger(getLogger(), "FabricMySQLConnectionProxy", null);
    
    setShardTable(fabricShardTable);
    setShardKey(fabricShardKey);
    
    setServerGroupName(fabricServerGroup);
  }
  











  synchronized SQLException interceptException(SQLException sqlEx, Connection conn, String groupName, String hostname, String portNumber)
    throws FabricCommunicationException
  {
    if (((sqlEx.getSQLState() != null) && (sqlEx.getSQLState().startsWith("08"))) || (((!MySQLNonTransientConnectionException.class.isAssignableFrom(sqlEx.getClass())) && ((JDBC4_NON_TRANSIENT_CONN_EXCEPTION == null) || (!JDBC4_NON_TRANSIENT_CONN_EXCEPTION.isAssignableFrom(sqlEx.getClass())))) || ((sqlEx.getCause() != null) && (FabricCommunicationException.class.isAssignableFrom(sqlEx.getCause().getClass())))))
    {


      return null;
    }
    

    Server currentServer = serverGroup.getServer(hostname + ":" + portNumber);
    

    if (currentServer == null) {
      return null;
    }
    

    if (reportErrors) {
      fabricConnection.getClient().reportServerError(currentServer, sqlEx.toString(), true);
    }
    

    if (replConnGroupLocks.add(serverGroup.getName())) {
      try {
        SQLException localSQLException1;
        try {
          fabricConnection.refreshStatePassive();
          setCurrentServerGroup(serverGroup.getName());
        } catch (SQLException ex) {
          return SQLError.createSQLException("Unable to refresh Fabric state. Failover impossible", "08006", ex, null);
        }
        
        try
        {
          syncGroupServersToReplicationConnectionGroup(ReplicationConnectionGroupManager.getConnectionGroup(groupName));
        } catch (SQLException ex) {
          return ex;
        }
      } finally {
        replConnGroupLocks.remove(serverGroup.getName());
      }
    } else {
      return SQLError.createSQLException("Fabric state syncing already in progress in another thread.", "08006", sqlEx, null);
    }
    
    return null;
  }
  

  private void refreshStateIfNecessary()
    throws SQLException
  {
    if (fabricConnection.isStateExpired()) {
      fabricConnection.refreshStatePassive();
      if (serverGroup != null) {
        setCurrentServerGroup(serverGroup.getName());
      }
    }
  }
  

  public void setShardKey(String shardKey)
    throws SQLException
  {
    ensureNoTransactionInProgress();
    
    currentConnection = null;
    
    if (shardKey != null) {
      if (serverGroupName != null) {
        throw SQLError.createSQLException("Shard key cannot be provided when server group is chosen directly.", "S1009", null, getExceptionInterceptor(), this);
      }
      if (shardTable == null) {
        throw SQLError.createSQLException("Shard key cannot be provided without a shard table.", "S1009", null, getExceptionInterceptor(), this);
      }
      


      setCurrentServerGroup(shardMapping.getGroupNameForKey(shardKey));
    } else if (shardTable != null) {
      setCurrentServerGroup(shardMapping.getGlobalGroupName());
    }
    this.shardKey = shardKey;
  }
  
  public String getShardKey() {
    return shardKey;
  }
  
  public void setShardTable(String shardTable) throws SQLException {
    ensureNoTransactionInProgress();
    
    currentConnection = null;
    
    if (serverGroupName != null) {
      throw SQLError.createSQLException("Server group and shard table are mutually exclusive. Only one may be provided.", "S1009", null, getExceptionInterceptor(), this);
    }
    

    shardKey = null;
    serverGroup = null;
    this.shardTable = shardTable;
    if (shardTable == null) {
      shardMapping = null;
    }
    else {
      String table = shardTable;
      String db = database;
      if (shardTable.contains(".")) {
        String[] pair = shardTable.split("\\.");
        db = pair[0];
        table = pair[1];
      }
      shardMapping = fabricConnection.getShardMapping(db, table);
      if (shardMapping == null) {
        throw SQLError.createSQLException("Shard mapping not found for table `" + shardTable + "'", "S1009", null, getExceptionInterceptor(), this);
      }
      

      setCurrentServerGroup(shardMapping.getGlobalGroupName());
    }
  }
  
  public String getShardTable() {
    return shardTable;
  }
  
  public void setServerGroupName(String serverGroupName) throws SQLException {
    ensureNoTransactionInProgress();
    
    currentConnection = null;
    

    if (serverGroupName != null) {
      setCurrentServerGroup(serverGroupName);
    }
    
    this.serverGroupName = serverGroupName;
  }
  
  public String getServerGroupName() {
    return serverGroupName;
  }
  
  public void clearServerSelectionCriteria() throws SQLException {
    ensureNoTransactionInProgress();
    shardTable = null;
    shardKey = null;
    serverGroupName = null;
    serverGroup = null;
    queryTables.clear();
    currentConnection = null;
  }
  
  public ServerGroup getCurrentServerGroup() {
    return serverGroup;
  }
  
  public void clearQueryTables() throws SQLException {
    ensureNoTransactionInProgress();
    
    currentConnection = null;
    
    queryTables.clear();
    setShardTable(null);
  }
  






  public void addQueryTable(String tableName)
    throws SQLException
  {
    ensureNoTransactionInProgress();
    
    currentConnection = null;
    

    if (shardMapping == null) {
      if (fabricConnection.getShardMapping(database, tableName) != null) {
        setShardTable(tableName);
      }
    } else {
      ShardMapping mappingForTableName = fabricConnection.getShardMapping(database, tableName);
      if ((mappingForTableName != null) && (!mappingForTableName.equals(shardMapping))) {
        throw SQLError.createSQLException("Cross-shard query not allowed", "S1009", null, getExceptionInterceptor(), this);
      }
    }
    queryTables.add(tableName);
  }
  


  public Set<String> getQueryTables()
  {
    return queryTables;
  }
  

  protected void setCurrentServerGroup(String serverGroupName)
    throws SQLException
  {
    serverGroup = fabricConnection.getServerGroup(serverGroupName);
    
    if (serverGroup == null) {
      throw SQLError.createSQLException("Cannot find server group: `" + serverGroupName + "'", "S1009", null, getExceptionInterceptor(), this);
    }
    


    ReplicationConnectionGroup replConnGroup = ReplicationConnectionGroupManager.getConnectionGroup(serverGroupName);
    if ((replConnGroup != null) && 
      (replConnGroupLocks.add(serverGroup.getName()))) {
      try {
        syncGroupServersToReplicationConnectionGroup(replConnGroup);
      } finally {
        replConnGroupLocks.remove(serverGroup.getName());
      }
    }
  }
  












  protected MySQLConnection getActiveMySQLConnectionChecked()
    throws SQLException
  {
    ReplicationConnection c = (ReplicationConnection)getActiveConnection();
    MySQLConnection mc = (MySQLConnection)c.getCurrentConnection();
    return mc;
  }
  
  public MySQLConnection getActiveMySQLConnection() {
    try {
      return getActiveMySQLConnectionChecked();
    } catch (SQLException ex) {
      throw new IllegalStateException("Unable to determine active connection", ex);
    }
  }
  
  protected Connection getActiveConnectionPassive() {
    try {
      return getActiveConnection();
    } catch (SQLException ex) {
      throw new IllegalStateException("Unable to determine active connection", ex);
    }
  }
  





  private void syncGroupServersToReplicationConnectionGroup(ReplicationConnectionGroup replConnGroup)
    throws SQLException
  {
    String currentMasterString = null;
    if (replConnGroup.getMasterHosts().size() == 1) {
      currentMasterString = (String)replConnGroup.getMasterHosts().iterator().next();
    }
    
    if ((currentMasterString != null) && ((serverGroup.getMaster() == null) || (!currentMasterString.equals(serverGroup.getMaster().getHostPortString()))))
    {
      try
      {
        replConnGroup.removeMasterHost(currentMasterString, false);
      }
      catch (SQLException ex) {
        getLog().logWarn("Unable to remove master: " + currentMasterString, ex);
      }
    }
    

    Server newMaster = serverGroup.getMaster();
    if ((newMaster != null) && (replConnGroup.getMasterHosts().size() == 0)) {
      getLog().logInfo("Changing master for group '" + replConnGroup.getGroupName() + "' to: " + newMaster);
      try {
        if (!replConnGroup.getSlaveHosts().contains(newMaster.getHostPortString())) {
          replConnGroup.addSlaveHost(newMaster.getHostPortString());
        }
        replConnGroup.promoteSlaveToMaster(newMaster.getHostPortString());
      } catch (SQLException ex) {
        throw SQLError.createSQLException("Unable to promote new master '" + newMaster.toString() + "'", ex.getSQLState(), ex, null);
      }
    }
    


    for (Server s : serverGroup.getServers()) {
      if (s.isSlave()) {
        try
        {
          replConnGroup.addSlaveHost(s.getHostPortString());
        }
        catch (SQLException ex) {
          getLog().logWarn("Unable to add slave: " + s.toString(), ex);
        }
      }
    }
    
    for (String hostPortString : replConnGroup.getSlaveHosts()) {
      Server fabServer = serverGroup.getServer(hostPortString);
      if ((fabServer == null) || (!fabServer.isSlave())) {
        try {
          replConnGroup.removeSlaveHost(hostPortString, true);
        }
        catch (SQLException ex) {
          getLog().logWarn("Unable to remove slave: " + hostPortString, ex);
        }
      }
    }
  }
  
  protected Connection getActiveConnection() throws SQLException {
    if (!transactionInProgress) {
      refreshStateIfNecessary();
    }
    
    if (currentConnection != null) {
      return currentConnection;
    }
    
    if (getCurrentServerGroup() == null) {
      throw SQLError.createSQLException("No server group selected.", "08004", null, getExceptionInterceptor(), this);
    }
    

    currentConnection = ((ReplicationConnection)serverConnections.get(serverGroup));
    if (currentConnection != null) {
      return currentConnection;
    }
    

    List<String> masterHost = new ArrayList();
    List<String> slaveHosts = new ArrayList();
    for (Server s : serverGroup.getServers()) {
      if (s.isMaster()) {
        masterHost.add(s.getHostPortString());
      } else if (s.isSlave()) {
        slaveHosts.add(s.getHostPortString());
      }
    }
    Properties info = exposeAsProperties(null);
    ReplicationConnectionGroup replConnGroup = ReplicationConnectionGroupManager.getConnectionGroup(serverGroup.getName());
    if ((replConnGroup != null) && 
      (replConnGroupLocks.add(serverGroup.getName()))) {
      try {
        syncGroupServersToReplicationConnectionGroup(replConnGroup);
      } finally {
        replConnGroupLocks.remove(serverGroup.getName());
      }
    }
    
    info.put("replicationConnectionGroup", serverGroup.getName());
    info.setProperty("user", username);
    info.setProperty("password", password);
    info.setProperty("DBNAME", getCatalog());
    info.setProperty("connectionAttributes", "fabricHaGroup:" + serverGroup.getName());
    info.setProperty("retriesAllDown", "1");
    info.setProperty("allowMasterDownConnections", "true");
    info.setProperty("allowSlaveDownConnections", "true");
    info.setProperty("readFromMasterWhenNoSlaves", "true");
    currentConnection = ReplicationConnectionProxy.createProxyInstance(masterHost, info, slaveHosts, info);
    serverConnections.put(serverGroup, currentConnection);
    
    currentConnection.setProxy(this);
    currentConnection.setAutoCommit(autoCommit);
    currentConnection.setReadOnly(readOnly);
    currentConnection.setTransactionIsolation(transactionIsolation);
    return currentConnection;
  }
  
  private void ensureOpen() throws SQLException {
    if (closed) {
      throw SQLError.createSQLException("No operations allowed after connection closed.", "08003", getExceptionInterceptor());
    }
  }
  
  private void ensureNoTransactionInProgress() throws SQLException
  {
    ensureOpen();
    if ((transactionInProgress) && (!autoCommit)) {
      throw SQLError.createSQLException("Not allow while a transaction is active.", "25000", getExceptionInterceptor());
    }
  }
  


  public void close()
    throws SQLException
  {
    closed = true;
    for (Connection c : serverConnections.values()) {
      try {
        c.close();
      }
      catch (SQLException ex) {}
    }
  }
  
  public boolean isClosed() {
    return closed;
  }
  


  public boolean isValid(int timeout)
    throws SQLException
  {
    return !closed;
  }
  
  public void setReadOnly(boolean readOnly) throws SQLException {
    this.readOnly = readOnly;
    for (ReplicationConnection conn : serverConnections.values()) {
      conn.setReadOnly(readOnly);
    }
  }
  
  public boolean isReadOnly() throws SQLException {
    return readOnly;
  }
  
  public boolean isReadOnly(boolean useSessionStatus) throws SQLException {
    return readOnly;
  }
  
  public void setCatalog(String catalog) throws SQLException {
    database = catalog;
    for (Connection c : serverConnections.values()) {
      c.setCatalog(catalog);
    }
  }
  
  public String getCatalog() {
    return database;
  }
  
  public void rollback() throws SQLException {
    getActiveConnection().rollback();
    transactionCompleted();
  }
  
  public void rollback(Savepoint savepoint) throws SQLException {
    getActiveConnection().rollback();
    transactionCompleted();
  }
  
  public void commit() throws SQLException {
    getActiveConnection().commit();
    transactionCompleted();
  }
  
  public void setAutoCommit(boolean autoCommit) throws SQLException {
    this.autoCommit = autoCommit;
    for (Connection c : serverConnections.values()) {
      c.setAutoCommit(this.autoCommit);
    }
  }
  
  public void transactionBegun() throws SQLException {
    if (!autoCommit) {
      transactionInProgress = true;
    }
  }
  
  public void transactionCompleted() throws SQLException {
    transactionInProgress = false;
    refreshStateIfNecessary();
  }
  
  public boolean getAutoCommit() {
    return autoCommit;
  }
  


  @Deprecated
  public MySQLConnection getLoadBalanceSafeProxy()
  {
    return getMultiHostSafeProxy();
  }
  
  public MySQLConnection getMultiHostSafeProxy() {
    return getActiveMySQLConnection();
  }
  

  public void setTransactionIsolation(int level)
    throws SQLException
  {
    transactionIsolation = level;
    for (Connection c : serverConnections.values()) {
      c.setTransactionIsolation(level);
    }
  }
  
  public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
    for (Connection c : serverConnections.values()) {
      c.setTypeMap(map);
    }
  }
  
  public void setHoldability(int holdability) throws SQLException {
    for (Connection c : serverConnections.values()) {
      c.setHoldability(holdability);
    }
  }
  




  public Savepoint setSavepoint()
    throws SQLException
  {
    return getActiveConnection().setSavepoint();
  }
  
  public Savepoint setSavepoint(String name) throws SQLException {
    transactionInProgress = true;
    return getActiveConnection().setSavepoint(name);
  }
  

  public CallableStatement prepareCall(String sql)
    throws SQLException
  {
    transactionBegun();
    return getActiveConnection().prepareCall(sql);
  }
  
  public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
    transactionBegun();
    return getActiveConnection().prepareCall(sql, resultSetType, resultSetConcurrency);
  }
  
  public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
    transactionBegun();
    return getActiveConnection().prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
  }
  
  public PreparedStatement prepareStatement(String sql) throws SQLException {
    transactionBegun();
    return getActiveConnection().prepareStatement(sql);
  }
  
  public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
    transactionBegun();
    return getActiveConnection().prepareStatement(sql, autoGeneratedKeys);
  }
  
  public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
    transactionBegun();
    return getActiveConnection().prepareStatement(sql, columnIndexes);
  }
  
  public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
    transactionBegun();
    return getActiveConnection().prepareStatement(sql, resultSetType, resultSetConcurrency);
  }
  
  public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
    transactionBegun();
    return getActiveConnection().prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
  }
  
  public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
    transactionBegun();
    return getActiveConnection().prepareStatement(sql, columnNames);
  }
  
  public PreparedStatement clientPrepareStatement(String sql) throws SQLException {
    transactionBegun();
    return getActiveConnection().clientPrepareStatement(sql);
  }
  
  public PreparedStatement clientPrepareStatement(String sql, int autoGenKeyIndex) throws SQLException {
    transactionBegun();
    return getActiveConnection().clientPrepareStatement(sql, autoGenKeyIndex);
  }
  
  public PreparedStatement clientPrepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
    transactionBegun();
    return getActiveConnection().clientPrepareStatement(sql, resultSetType, resultSetConcurrency);
  }
  
  public PreparedStatement clientPrepareStatement(String sql, int[] autoGenKeyIndexes) throws SQLException {
    transactionBegun();
    return getActiveConnection().clientPrepareStatement(sql, autoGenKeyIndexes);
  }
  
  public PreparedStatement clientPrepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
  {
    transactionBegun();
    return getActiveConnection().clientPrepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
  }
  
  public PreparedStatement clientPrepareStatement(String sql, String[] autoGenKeyColNames) throws SQLException {
    transactionBegun();
    return getActiveConnection().clientPrepareStatement(sql, autoGenKeyColNames);
  }
  
  public PreparedStatement serverPrepareStatement(String sql) throws SQLException {
    transactionBegun();
    return getActiveConnection().serverPrepareStatement(sql);
  }
  
  public PreparedStatement serverPrepareStatement(String sql, int autoGenKeyIndex) throws SQLException {
    transactionBegun();
    return getActiveConnection().serverPrepareStatement(sql, autoGenKeyIndex);
  }
  
  public PreparedStatement serverPrepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
    transactionBegun();
    return getActiveConnection().serverPrepareStatement(sql, resultSetType, resultSetConcurrency);
  }
  
  public PreparedStatement serverPrepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
  {
    transactionBegun();
    return getActiveConnection().serverPrepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
  }
  
  public PreparedStatement serverPrepareStatement(String sql, int[] autoGenKeyIndexes) throws SQLException {
    transactionBegun();
    return getActiveConnection().serverPrepareStatement(sql, autoGenKeyIndexes);
  }
  
  public PreparedStatement serverPrepareStatement(String sql, String[] autoGenKeyColNames) throws SQLException {
    transactionBegun();
    return getActiveConnection().serverPrepareStatement(sql, autoGenKeyColNames);
  }
  
  public java.sql.Statement createStatement() throws SQLException {
    transactionBegun();
    return getActiveConnection().createStatement();
  }
  
  public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
    transactionBegun();
    return getActiveConnection().createStatement(resultSetType, resultSetConcurrency);
  }
  
  public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
    transactionBegun();
    return getActiveConnection().createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
  }
  
  public ResultSetInternalMethods execSQL(StatementImpl callingStatement, String sql, int maxRows, Buffer packet, int resultSetType, int resultSetConcurrency, boolean streamResults, String catalog, Field[] cachedMetadata) throws SQLException
  {
    return getActiveMySQLConnectionChecked().execSQL(callingStatement, sql, maxRows, packet, resultSetType, resultSetConcurrency, streamResults, catalog, cachedMetadata);
  }
  
  public ResultSetInternalMethods execSQL(StatementImpl callingStatement, String sql, int maxRows, Buffer packet, int resultSetType, int resultSetConcurrency, boolean streamResults, String catalog, Field[] cachedMetadata, boolean isBatch)
    throws SQLException
  {
    return getActiveMySQLConnectionChecked().execSQL(callingStatement, sql, maxRows, packet, resultSetType, resultSetConcurrency, streamResults, catalog, cachedMetadata, isBatch);
  }
  
  public String extractSqlFromPacket(String possibleSqlQuery, Buffer queryPacket, int endOfQueryPacketPosition) throws SQLException
  {
    return getActiveMySQLConnectionChecked().extractSqlFromPacket(possibleSqlQuery, queryPacket, endOfQueryPacketPosition);
  }
  
  public StringBuilder generateConnectionCommentBlock(StringBuilder buf) {
    return getActiveMySQLConnection().generateConnectionCommentBlock(buf);
  }
  
  public MysqlIO getIO() throws SQLException {
    return getActiveMySQLConnectionChecked().getIO();
  }
  
  public Calendar getCalendarInstanceForSessionOrNew() {
    return getActiveMySQLConnection().getCalendarInstanceForSessionOrNew();
  }
  


  @Deprecated
  public String getServerCharacterEncoding()
  {
    return getServerCharset();
  }
  
  public String getServerCharset() {
    return getActiveMySQLConnection().getServerCharset();
  }
  
  public TimeZone getServerTimezoneTZ() {
    return getActiveMySQLConnection().getServerTimezoneTZ();
  }
  


  public boolean versionMeetsMinimum(int major, int minor, int subminor)
    throws SQLException
  {
    return getActiveConnection().versionMeetsMinimum(major, minor, subminor);
  }
  


  public boolean supportsIsolationLevel()
  {
    return getActiveConnectionPassive().supportsIsolationLevel();
  }
  


  public boolean supportsQuotedIdentifiers()
  {
    return getActiveConnectionPassive().supportsQuotedIdentifiers();
  }
  
  public DatabaseMetaData getMetaData() throws SQLException {
    return getActiveConnection().getMetaData();
  }
  
  public String getCharacterSetMetadata() {
    return getActiveMySQLConnection().getCharacterSetMetadata();
  }
  
  public java.sql.Statement getMetadataSafeStatement() throws SQLException {
    return getActiveMySQLConnectionChecked().getMetadataSafeStatement();
  }
  




  public boolean isWrapperFor(Class<?> iface)
  {
    return false;
  }
  


  public <T> T unwrap(Class<T> iface)
  {
    return null;
  }
  



  public boolean supportsTransactions()
  {
    return true;
  }
  
  public boolean isRunningOnJDK13() {
    return false;
  }
  
  public void createNewIO(boolean isForReconnect) throws SQLException {
    throw SQLError.createSQLFeatureNotSupportedException();
  }
  







  public boolean isServerLocal()
    throws SQLException
  {
    return false;
  }
  
  public void shutdownServer() throws SQLException {
    throw SQLError.createSQLFeatureNotSupportedException();
  }
  




  @Deprecated
  public boolean hasTriedMaster()
  {
    return false;
  }
  
  public boolean isInGlobalTx()
  {
    return false;
  }
  
  public void setInGlobalTx(boolean flag)
  {
    throw new RuntimeException("Global transactions not supported.");
  }
  
  public void changeUser(String userName, String newPassword) throws SQLException {
    throw SQLError.createSQLException("User change not allowed.", getExceptionInterceptor());
  }
  


  public void setFabricShardKey(String value)
  {
    fabricShardKey = value;
  }
  
  public String getFabricShardKey() {
    return fabricShardKey;
  }
  
  public void setFabricShardTable(String value) {
    fabricShardTable = value;
  }
  
  public String getFabricShardTable() {
    return fabricShardTable;
  }
  
  public void setFabricServerGroup(String value) {
    fabricServerGroup = value;
  }
  
  public String getFabricServerGroup() {
    return fabricServerGroup;
  }
  
  public void setFabricProtocol(String value) {
    fabricProtocol = value;
  }
  
  public String getFabricProtocol() {
    return fabricProtocol;
  }
  
  public void setFabricUsername(String value) {
    fabricUsername = value;
  }
  
  public String getFabricUsername() {
    return fabricUsername;
  }
  
  public void setFabricPassword(String value) {
    fabricPassword = value;
  }
  
  public String getFabricPassword() {
    return fabricPassword;
  }
  
  public void setFabricReportErrors(boolean value) {
    reportErrors = value;
  }
  
  public boolean getFabricReportErrors() {
    return reportErrors;
  }
  



  public void setAllowLoadLocalInfile(boolean property)
  {
    super.setAllowLoadLocalInfile(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setAllowLoadLocalInfile(property);
    }
  }
  
  public void setAllowMultiQueries(boolean property)
  {
    super.setAllowMultiQueries(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setAllowMultiQueries(property);
    }
  }
  
  public void setAllowNanAndInf(boolean flag)
  {
    super.setAllowNanAndInf(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setAllowNanAndInf(flag);
    }
  }
  
  public void setAllowUrlInLocalInfile(boolean flag)
  {
    super.setAllowUrlInLocalInfile(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setAllowUrlInLocalInfile(flag);
    }
  }
  
  public void setAlwaysSendSetIsolation(boolean flag)
  {
    super.setAlwaysSendSetIsolation(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setAlwaysSendSetIsolation(flag);
    }
  }
  
  public void setAutoDeserialize(boolean flag)
  {
    super.setAutoDeserialize(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setAutoDeserialize(flag);
    }
  }
  
  public void setAutoGenerateTestcaseScript(boolean flag)
  {
    super.setAutoGenerateTestcaseScript(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setAutoGenerateTestcaseScript(flag);
    }
  }
  
  public void setAutoReconnect(boolean flag)
  {
    super.setAutoReconnect(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setAutoReconnect(flag);
    }
  }
  
  public void setAutoReconnectForConnectionPools(boolean property)
  {
    super.setAutoReconnectForConnectionPools(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setAutoReconnectForConnectionPools(property);
    }
  }
  
  public void setAutoReconnectForPools(boolean flag)
  {
    super.setAutoReconnectForPools(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setAutoReconnectForPools(flag);
    }
  }
  
  public void setBlobSendChunkSize(String value) throws SQLException
  {
    super.setBlobSendChunkSize(value);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setBlobSendChunkSize(value);
    }
  }
  
  public void setCacheCallableStatements(boolean flag)
  {
    super.setCacheCallableStatements(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setCacheCallableStatements(flag);
    }
  }
  
  public void setCachePreparedStatements(boolean flag)
  {
    super.setCachePreparedStatements(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setCachePreparedStatements(flag);
    }
  }
  
  public void setCacheResultSetMetadata(boolean property)
  {
    super.setCacheResultSetMetadata(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setCacheResultSetMetadata(property);
    }
  }
  
  public void setCacheServerConfiguration(boolean flag)
  {
    super.setCacheServerConfiguration(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setCacheServerConfiguration(flag);
    }
  }
  
  public void setCallableStatementCacheSize(int size) throws SQLException
  {
    super.setCallableStatementCacheSize(size);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setCallableStatementCacheSize(size);
    }
  }
  
  public void setCapitalizeDBMDTypes(boolean property)
  {
    super.setCapitalizeDBMDTypes(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setCapitalizeDBMDTypes(property);
    }
  }
  
  public void setCapitalizeTypeNames(boolean flag)
  {
    super.setCapitalizeTypeNames(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setCapitalizeTypeNames(flag);
    }
  }
  
  public void setCharacterEncoding(String encoding)
  {
    super.setCharacterEncoding(encoding);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setCharacterEncoding(encoding);
    }
  }
  
  public void setCharacterSetResults(String characterSet)
  {
    super.setCharacterSetResults(characterSet);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setCharacterSetResults(characterSet);
    }
  }
  
  public void setClobberStreamingResults(boolean flag)
  {
    super.setClobberStreamingResults(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setClobberStreamingResults(flag);
    }
  }
  
  public void setClobCharacterEncoding(String encoding)
  {
    super.setClobCharacterEncoding(encoding);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setClobCharacterEncoding(encoding);
    }
  }
  
  public void setConnectionCollation(String collation)
  {
    super.setConnectionCollation(collation);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setConnectionCollation(collation);
    }
  }
  
  public void setConnectTimeout(int timeoutMs) throws SQLException
  {
    super.setConnectTimeout(timeoutMs);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setConnectTimeout(timeoutMs);
    }
  }
  
  public void setContinueBatchOnError(boolean property)
  {
    super.setContinueBatchOnError(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setContinueBatchOnError(property);
    }
  }
  
  public void setCreateDatabaseIfNotExist(boolean flag)
  {
    super.setCreateDatabaseIfNotExist(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setCreateDatabaseIfNotExist(flag);
    }
  }
  
  public void setDefaultFetchSize(int n) throws SQLException
  {
    super.setDefaultFetchSize(n);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setDefaultFetchSize(n);
    }
  }
  
  public void setDetectServerPreparedStmts(boolean property)
  {
    super.setDetectServerPreparedStmts(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setDetectServerPreparedStmts(property);
    }
  }
  
  public void setDontTrackOpenResources(boolean flag)
  {
    super.setDontTrackOpenResources(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setDontTrackOpenResources(flag);
    }
  }
  
  public void setDumpQueriesOnException(boolean flag)
  {
    super.setDumpQueriesOnException(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setDumpQueriesOnException(flag);
    }
  }
  
  public void setDynamicCalendars(boolean flag)
  {
    super.setDynamicCalendars(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setDynamicCalendars(flag);
    }
  }
  
  public void setElideSetAutoCommits(boolean flag)
  {
    super.setElideSetAutoCommits(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setElideSetAutoCommits(flag);
    }
  }
  
  public void setEmptyStringsConvertToZero(boolean flag)
  {
    super.setEmptyStringsConvertToZero(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setEmptyStringsConvertToZero(flag);
    }
  }
  
  public void setEmulateLocators(boolean property)
  {
    super.setEmulateLocators(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setEmulateLocators(property);
    }
  }
  
  public void setEmulateUnsupportedPstmts(boolean flag)
  {
    super.setEmulateUnsupportedPstmts(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setEmulateUnsupportedPstmts(flag);
    }
  }
  
  public void setEnablePacketDebug(boolean flag)
  {
    super.setEnablePacketDebug(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setEnablePacketDebug(flag);
    }
  }
  
  public void setEncoding(String property)
  {
    super.setEncoding(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setEncoding(property);
    }
  }
  
  public void setExplainSlowQueries(boolean flag)
  {
    super.setExplainSlowQueries(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setExplainSlowQueries(flag);
    }
  }
  
  public void setFailOverReadOnly(boolean flag)
  {
    super.setFailOverReadOnly(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setFailOverReadOnly(flag);
    }
  }
  
  public void setGatherPerformanceMetrics(boolean flag)
  {
    super.setGatherPerformanceMetrics(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setGatherPerformanceMetrics(flag);
    }
  }
  
  public void setHoldResultsOpenOverStatementClose(boolean flag)
  {
    super.setHoldResultsOpenOverStatementClose(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setHoldResultsOpenOverStatementClose(flag);
    }
  }
  
  public void setIgnoreNonTxTables(boolean property)
  {
    super.setIgnoreNonTxTables(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setIgnoreNonTxTables(property);
    }
  }
  
  public void setInitialTimeout(int property) throws SQLException
  {
    super.setInitialTimeout(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setInitialTimeout(property);
    }
  }
  
  public void setIsInteractiveClient(boolean property)
  {
    super.setIsInteractiveClient(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setIsInteractiveClient(property);
    }
  }
  
  public void setJdbcCompliantTruncation(boolean flag)
  {
    super.setJdbcCompliantTruncation(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setJdbcCompliantTruncation(flag);
    }
  }
  
  public void setLocatorFetchBufferSize(String value) throws SQLException
  {
    super.setLocatorFetchBufferSize(value);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setLocatorFetchBufferSize(value);
    }
  }
  
  public void setLogger(String property)
  {
    super.setLogger(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setLogger(property);
    }
  }
  
  public void setLoggerClassName(String className)
  {
    super.setLoggerClassName(className);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setLoggerClassName(className);
    }
  }
  
  public void setLogSlowQueries(boolean flag)
  {
    super.setLogSlowQueries(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setLogSlowQueries(flag);
    }
  }
  
  public void setMaintainTimeStats(boolean flag)
  {
    super.setMaintainTimeStats(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setMaintainTimeStats(flag);
    }
  }
  
  public void setMaxQuerySizeToLog(int sizeInBytes) throws SQLException
  {
    super.setMaxQuerySizeToLog(sizeInBytes);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setMaxQuerySizeToLog(sizeInBytes);
    }
  }
  
  public void setMaxReconnects(int property) throws SQLException
  {
    super.setMaxReconnects(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setMaxReconnects(property);
    }
  }
  
  public void setMaxRows(int property) throws SQLException
  {
    super.setMaxRows(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setMaxRows(property);
    }
  }
  
  public void setMetadataCacheSize(int value) throws SQLException
  {
    super.setMetadataCacheSize(value);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setMetadataCacheSize(value);
    }
  }
  
  public void setNoDatetimeStringSync(boolean flag)
  {
    super.setNoDatetimeStringSync(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setNoDatetimeStringSync(flag);
    }
  }
  
  public void setNullCatalogMeansCurrent(boolean value)
  {
    super.setNullCatalogMeansCurrent(value);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setNullCatalogMeansCurrent(value);
    }
  }
  
  public void setNullNamePatternMatchesAll(boolean value)
  {
    super.setNullNamePatternMatchesAll(value);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setNullNamePatternMatchesAll(value);
    }
  }
  
  public void setPacketDebugBufferSize(int size) throws SQLException
  {
    super.setPacketDebugBufferSize(size);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setPacketDebugBufferSize(size);
    }
  }
  
  public void setParanoid(boolean property)
  {
    super.setParanoid(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setParanoid(property);
    }
  }
  
  public void setPedantic(boolean property)
  {
    super.setPedantic(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setPedantic(property);
    }
  }
  
  public void setPreparedStatementCacheSize(int cacheSize) throws SQLException
  {
    super.setPreparedStatementCacheSize(cacheSize);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setPreparedStatementCacheSize(cacheSize);
    }
  }
  
  public void setPreparedStatementCacheSqlLimit(int cacheSqlLimit) throws SQLException
  {
    super.setPreparedStatementCacheSqlLimit(cacheSqlLimit);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setPreparedStatementCacheSqlLimit(cacheSqlLimit);
    }
  }
  
  public void setProfileSql(boolean property)
  {
    super.setProfileSql(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setProfileSql(property);
    }
  }
  
  public void setProfileSQL(boolean flag)
  {
    super.setProfileSQL(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setProfileSQL(flag);
    }
  }
  
  public void setPropertiesTransform(String value)
  {
    super.setPropertiesTransform(value);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setPropertiesTransform(value);
    }
  }
  
  public void setQueriesBeforeRetryMaster(int property) throws SQLException
  {
    super.setQueriesBeforeRetryMaster(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setQueriesBeforeRetryMaster(property);
    }
  }
  
  public void setReconnectAtTxEnd(boolean property)
  {
    super.setReconnectAtTxEnd(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setReconnectAtTxEnd(property);
    }
  }
  
  public void setRelaxAutoCommit(boolean property)
  {
    super.setRelaxAutoCommit(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setRelaxAutoCommit(property);
    }
  }
  
  public void setReportMetricsIntervalMillis(int millis) throws SQLException
  {
    super.setReportMetricsIntervalMillis(millis);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setReportMetricsIntervalMillis(millis);
    }
  }
  
  public void setRequireSSL(boolean property)
  {
    super.setRequireSSL(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setRequireSSL(property);
    }
  }
  
  public void setRetainStatementAfterResultSetClose(boolean flag)
  {
    super.setRetainStatementAfterResultSetClose(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setRetainStatementAfterResultSetClose(flag);
    }
  }
  
  public void setRollbackOnPooledClose(boolean flag)
  {
    super.setRollbackOnPooledClose(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setRollbackOnPooledClose(flag);
    }
  }
  
  public void setRoundRobinLoadBalance(boolean flag)
  {
    super.setRoundRobinLoadBalance(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setRoundRobinLoadBalance(flag);
    }
  }
  
  public void setRunningCTS13(boolean flag)
  {
    super.setRunningCTS13(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setRunningCTS13(flag);
    }
  }
  
  public void setSecondsBeforeRetryMaster(int property) throws SQLException
  {
    super.setSecondsBeforeRetryMaster(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setSecondsBeforeRetryMaster(property);
    }
  }
  
  public void setServerTimezone(String property)
  {
    super.setServerTimezone(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setServerTimezone(property);
    }
  }
  
  public void setSessionVariables(String variables)
  {
    super.setSessionVariables(variables);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setSessionVariables(variables);
    }
  }
  
  public void setSlowQueryThresholdMillis(int millis) throws SQLException
  {
    super.setSlowQueryThresholdMillis(millis);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setSlowQueryThresholdMillis(millis);
    }
  }
  
  public void setSocketFactoryClassName(String property)
  {
    super.setSocketFactoryClassName(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setSocketFactoryClassName(property);
    }
  }
  
  public void setSocketTimeout(int property) throws SQLException
  {
    super.setSocketTimeout(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setSocketTimeout(property);
    }
  }
  
  public void setStrictFloatingPoint(boolean property)
  {
    super.setStrictFloatingPoint(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setStrictFloatingPoint(property);
    }
  }
  
  public void setStrictUpdates(boolean property)
  {
    super.setStrictUpdates(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setStrictUpdates(property);
    }
  }
  
  public void setTinyInt1isBit(boolean flag)
  {
    super.setTinyInt1isBit(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setTinyInt1isBit(flag);
    }
  }
  
  public void setTraceProtocol(boolean flag)
  {
    super.setTraceProtocol(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setTraceProtocol(flag);
    }
  }
  
  public void setTransformedBitIsBoolean(boolean flag)
  {
    super.setTransformedBitIsBoolean(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setTransformedBitIsBoolean(flag);
    }
  }
  
  public void setUseCompression(boolean property)
  {
    super.setUseCompression(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseCompression(property);
    }
  }
  
  public void setUseFastIntParsing(boolean flag)
  {
    super.setUseFastIntParsing(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseFastIntParsing(flag);
    }
  }
  
  public void setUseHostsInPrivileges(boolean property)
  {
    super.setUseHostsInPrivileges(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseHostsInPrivileges(property);
    }
  }
  
  public void setUseInformationSchema(boolean flag)
  {
    super.setUseInformationSchema(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseInformationSchema(flag);
    }
  }
  
  public void setUseLocalSessionState(boolean flag)
  {
    super.setUseLocalSessionState(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseLocalSessionState(flag);
    }
  }
  
  public void setUseOldUTF8Behavior(boolean flag)
  {
    super.setUseOldUTF8Behavior(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseOldUTF8Behavior(flag);
    }
  }
  
  public void setUseOnlyServerErrorMessages(boolean flag)
  {
    super.setUseOnlyServerErrorMessages(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseOnlyServerErrorMessages(flag);
    }
  }
  
  public void setUseReadAheadInput(boolean flag)
  {
    super.setUseReadAheadInput(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseReadAheadInput(flag);
    }
  }
  
  public void setUseServerPreparedStmts(boolean flag)
  {
    super.setUseServerPreparedStmts(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseServerPreparedStmts(flag);
    }
  }
  
  public void setUseSqlStateCodes(boolean flag)
  {
    super.setUseSqlStateCodes(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseSqlStateCodes(flag);
    }
  }
  
  public void setUseSSL(boolean property)
  {
    super.setUseSSL(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseSSL(property);
    }
  }
  
  public void setUseStreamLengthsInPrepStmts(boolean property)
  {
    super.setUseStreamLengthsInPrepStmts(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseStreamLengthsInPrepStmts(property);
    }
  }
  
  public void setUseTimezone(boolean property)
  {
    super.setUseTimezone(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseTimezone(property);
    }
  }
  
  public void setUseUltraDevWorkAround(boolean property)
  {
    super.setUseUltraDevWorkAround(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseUltraDevWorkAround(property);
    }
  }
  
  public void setUseUnbufferedInput(boolean flag)
  {
    super.setUseUnbufferedInput(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseUnbufferedInput(flag);
    }
  }
  
  public void setUseUnicode(boolean flag)
  {
    super.setUseUnicode(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseUnicode(flag);
    }
  }
  
  public void setUseUsageAdvisor(boolean useUsageAdvisorFlag)
  {
    super.setUseUsageAdvisor(useUsageAdvisorFlag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseUsageAdvisor(useUsageAdvisorFlag);
    }
  }
  
  public void setYearIsDateType(boolean flag)
  {
    super.setYearIsDateType(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setYearIsDateType(flag);
    }
  }
  
  public void setZeroDateTimeBehavior(String behavior)
  {
    super.setZeroDateTimeBehavior(behavior);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setZeroDateTimeBehavior(behavior);
    }
  }
  
  public void setUseCursorFetch(boolean flag)
  {
    super.setUseCursorFetch(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseCursorFetch(flag);
    }
  }
  
  public void setOverrideSupportsIntegrityEnhancementFacility(boolean flag)
  {
    super.setOverrideSupportsIntegrityEnhancementFacility(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setOverrideSupportsIntegrityEnhancementFacility(flag);
    }
  }
  
  public void setNoTimezoneConversionForTimeType(boolean flag)
  {
    super.setNoTimezoneConversionForTimeType(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setNoTimezoneConversionForTimeType(flag);
    }
  }
  
  public void setUseJDBCCompliantTimezoneShift(boolean flag)
  {
    super.setUseJDBCCompliantTimezoneShift(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseJDBCCompliantTimezoneShift(flag);
    }
  }
  
  public void setAutoClosePStmtStreams(boolean flag)
  {
    super.setAutoClosePStmtStreams(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setAutoClosePStmtStreams(flag);
    }
  }
  
  public void setProcessEscapeCodesForPrepStmts(boolean flag)
  {
    super.setProcessEscapeCodesForPrepStmts(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setProcessEscapeCodesForPrepStmts(flag);
    }
  }
  
  public void setUseGmtMillisForDatetimes(boolean flag)
  {
    super.setUseGmtMillisForDatetimes(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseGmtMillisForDatetimes(flag);
    }
  }
  
  public void setDumpMetadataOnColumnNotFound(boolean flag)
  {
    super.setDumpMetadataOnColumnNotFound(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setDumpMetadataOnColumnNotFound(flag);
    }
  }
  
  public void setResourceId(String resourceId)
  {
    super.setResourceId(resourceId);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setResourceId(resourceId);
    }
  }
  
  public void setRewriteBatchedStatements(boolean flag)
  {
    super.setRewriteBatchedStatements(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setRewriteBatchedStatements(flag);
    }
  }
  
  public void setJdbcCompliantTruncationForReads(boolean jdbcCompliantTruncationForReads)
  {
    super.setJdbcCompliantTruncationForReads(jdbcCompliantTruncationForReads);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setJdbcCompliantTruncationForReads(jdbcCompliantTruncationForReads);
    }
  }
  
  public void setUseJvmCharsetConverters(boolean flag)
  {
    super.setUseJvmCharsetConverters(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseJvmCharsetConverters(flag);
    }
  }
  
  public void setPinGlobalTxToPhysicalConnection(boolean flag)
  {
    super.setPinGlobalTxToPhysicalConnection(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setPinGlobalTxToPhysicalConnection(flag);
    }
  }
  
  public void setGatherPerfMetrics(boolean flag)
  {
    super.setGatherPerfMetrics(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setGatherPerfMetrics(flag);
    }
  }
  
  public void setUltraDevHack(boolean flag)
  {
    super.setUltraDevHack(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUltraDevHack(flag);
    }
  }
  
  public void setInteractiveClient(boolean property)
  {
    super.setInteractiveClient(property);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setInteractiveClient(property);
    }
  }
  
  public void setSocketFactory(String name)
  {
    super.setSocketFactory(name);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setSocketFactory(name);
    }
  }
  
  public void setUseServerPrepStmts(boolean flag)
  {
    super.setUseServerPrepStmts(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseServerPrepStmts(flag);
    }
  }
  
  public void setCacheCallableStmts(boolean flag)
  {
    super.setCacheCallableStmts(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setCacheCallableStmts(flag);
    }
  }
  
  public void setCachePrepStmts(boolean flag)
  {
    super.setCachePrepStmts(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setCachePrepStmts(flag);
    }
  }
  
  public void setCallableStmtCacheSize(int cacheSize) throws SQLException
  {
    super.setCallableStmtCacheSize(cacheSize);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setCallableStmtCacheSize(cacheSize);
    }
  }
  
  public void setPrepStmtCacheSize(int cacheSize) throws SQLException
  {
    super.setPrepStmtCacheSize(cacheSize);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setPrepStmtCacheSize(cacheSize);
    }
  }
  
  public void setPrepStmtCacheSqlLimit(int sqlLimit) throws SQLException
  {
    super.setPrepStmtCacheSqlLimit(sqlLimit);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setPrepStmtCacheSqlLimit(sqlLimit);
    }
  }
  
  public void setNoAccessToProcedureBodies(boolean flag)
  {
    super.setNoAccessToProcedureBodies(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setNoAccessToProcedureBodies(flag);
    }
  }
  
  public void setUseOldAliasMetadataBehavior(boolean flag)
  {
    super.setUseOldAliasMetadataBehavior(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseOldAliasMetadataBehavior(flag);
    }
  }
  
  public void setClientCertificateKeyStorePassword(String value)
  {
    super.setClientCertificateKeyStorePassword(value);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setClientCertificateKeyStorePassword(value);
    }
  }
  
  public void setClientCertificateKeyStoreType(String value)
  {
    super.setClientCertificateKeyStoreType(value);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setClientCertificateKeyStoreType(value);
    }
  }
  
  public void setClientCertificateKeyStoreUrl(String value)
  {
    super.setClientCertificateKeyStoreUrl(value);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setClientCertificateKeyStoreUrl(value);
    }
  }
  
  public void setTrustCertificateKeyStorePassword(String value)
  {
    super.setTrustCertificateKeyStorePassword(value);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setTrustCertificateKeyStorePassword(value);
    }
  }
  
  public void setTrustCertificateKeyStoreType(String value)
  {
    super.setTrustCertificateKeyStoreType(value);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setTrustCertificateKeyStoreType(value);
    }
  }
  
  public void setTrustCertificateKeyStoreUrl(String value)
  {
    super.setTrustCertificateKeyStoreUrl(value);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setTrustCertificateKeyStoreUrl(value);
    }
  }
  
  public void setUseSSPSCompatibleTimezoneShift(boolean flag)
  {
    super.setUseSSPSCompatibleTimezoneShift(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseSSPSCompatibleTimezoneShift(flag);
    }
  }
  
  public void setTreatUtilDateAsTimestamp(boolean flag)
  {
    super.setTreatUtilDateAsTimestamp(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setTreatUtilDateAsTimestamp(flag);
    }
  }
  
  public void setUseFastDateParsing(boolean flag)
  {
    super.setUseFastDateParsing(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseFastDateParsing(flag);
    }
  }
  
  public void setLocalSocketAddress(String address)
  {
    super.setLocalSocketAddress(address);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setLocalSocketAddress(address);
    }
  }
  
  public void setUseConfigs(String configs)
  {
    super.setUseConfigs(configs);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseConfigs(configs);
    }
  }
  
  public void setGenerateSimpleParameterMetadata(boolean flag)
  {
    super.setGenerateSimpleParameterMetadata(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setGenerateSimpleParameterMetadata(flag);
    }
  }
  
  public void setLogXaCommands(boolean flag)
  {
    super.setLogXaCommands(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setLogXaCommands(flag);
    }
  }
  
  public void setResultSetSizeThreshold(int threshold) throws SQLException
  {
    super.setResultSetSizeThreshold(threshold);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setResultSetSizeThreshold(threshold);
    }
  }
  
  public void setNetTimeoutForStreamingResults(int value) throws SQLException
  {
    super.setNetTimeoutForStreamingResults(value);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setNetTimeoutForStreamingResults(value);
    }
  }
  
  public void setEnableQueryTimeouts(boolean flag)
  {
    super.setEnableQueryTimeouts(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setEnableQueryTimeouts(flag);
    }
  }
  
  public void setPadCharsWithSpace(boolean flag)
  {
    super.setPadCharsWithSpace(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setPadCharsWithSpace(flag);
    }
  }
  
  public void setUseDynamicCharsetInfo(boolean flag)
  {
    super.setUseDynamicCharsetInfo(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseDynamicCharsetInfo(flag);
    }
  }
  
  public void setClientInfoProvider(String classname)
  {
    super.setClientInfoProvider(classname);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setClientInfoProvider(classname);
    }
  }
  
  public void setPopulateInsertRowWithDefaultValues(boolean flag)
  {
    super.setPopulateInsertRowWithDefaultValues(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setPopulateInsertRowWithDefaultValues(flag);
    }
  }
  
  public void setLoadBalanceStrategy(String strategy)
  {
    super.setLoadBalanceStrategy(strategy);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setLoadBalanceStrategy(strategy);
    }
  }
  
  public void setTcpNoDelay(boolean flag)
  {
    super.setTcpNoDelay(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setTcpNoDelay(flag);
    }
  }
  
  public void setTcpKeepAlive(boolean flag)
  {
    super.setTcpKeepAlive(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setTcpKeepAlive(flag);
    }
  }
  
  public void setTcpRcvBuf(int bufSize) throws SQLException
  {
    super.setTcpRcvBuf(bufSize);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setTcpRcvBuf(bufSize);
    }
  }
  
  public void setTcpSndBuf(int bufSize) throws SQLException
  {
    super.setTcpSndBuf(bufSize);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setTcpSndBuf(bufSize);
    }
  }
  
  public void setTcpTrafficClass(int classFlags) throws SQLException
  {
    super.setTcpTrafficClass(classFlags);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setTcpTrafficClass(classFlags);
    }
  }
  
  public void setUseNanosForElapsedTime(boolean flag)
  {
    super.setUseNanosForElapsedTime(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseNanosForElapsedTime(flag);
    }
  }
  
  public void setSlowQueryThresholdNanos(long nanos) throws SQLException
  {
    super.setSlowQueryThresholdNanos(nanos);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setSlowQueryThresholdNanos(nanos);
    }
  }
  
  public void setStatementInterceptors(String value)
  {
    super.setStatementInterceptors(value);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setStatementInterceptors(value);
    }
  }
  
  public void setUseDirectRowUnpack(boolean flag)
  {
    super.setUseDirectRowUnpack(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseDirectRowUnpack(flag);
    }
  }
  
  public void setLargeRowSizeThreshold(String value) throws SQLException
  {
    super.setLargeRowSizeThreshold(value);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setLargeRowSizeThreshold(value);
    }
  }
  
  public void setUseBlobToStoreUTF8OutsideBMP(boolean flag)
  {
    super.setUseBlobToStoreUTF8OutsideBMP(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseBlobToStoreUTF8OutsideBMP(flag);
    }
  }
  
  public void setUtf8OutsideBmpExcludedColumnNamePattern(String regexPattern)
  {
    super.setUtf8OutsideBmpExcludedColumnNamePattern(regexPattern);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUtf8OutsideBmpExcludedColumnNamePattern(regexPattern);
    }
  }
  
  public void setUtf8OutsideBmpIncludedColumnNamePattern(String regexPattern)
  {
    super.setUtf8OutsideBmpIncludedColumnNamePattern(regexPattern);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUtf8OutsideBmpIncludedColumnNamePattern(regexPattern);
    }
  }
  
  public void setIncludeInnodbStatusInDeadlockExceptions(boolean flag)
  {
    super.setIncludeInnodbStatusInDeadlockExceptions(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setIncludeInnodbStatusInDeadlockExceptions(flag);
    }
  }
  
  public void setIncludeThreadDumpInDeadlockExceptions(boolean flag)
  {
    super.setIncludeThreadDumpInDeadlockExceptions(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setIncludeThreadDumpInDeadlockExceptions(flag);
    }
  }
  
  public void setIncludeThreadNamesAsStatementComment(boolean flag)
  {
    super.setIncludeThreadNamesAsStatementComment(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setIncludeThreadNamesAsStatementComment(flag);
    }
  }
  
  public void setBlobsAreStrings(boolean flag)
  {
    super.setBlobsAreStrings(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setBlobsAreStrings(flag);
    }
  }
  
  public void setFunctionsNeverReturnBlobs(boolean flag)
  {
    super.setFunctionsNeverReturnBlobs(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setFunctionsNeverReturnBlobs(flag);
    }
  }
  
  public void setAutoSlowLog(boolean flag)
  {
    super.setAutoSlowLog(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setAutoSlowLog(flag);
    }
  }
  
  public void setConnectionLifecycleInterceptors(String interceptors)
  {
    super.setConnectionLifecycleInterceptors(interceptors);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setConnectionLifecycleInterceptors(interceptors);
    }
  }
  
  public void setProfilerEventHandler(String handler)
  {
    super.setProfilerEventHandler(handler);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setProfilerEventHandler(handler);
    }
  }
  
  public void setVerifyServerCertificate(boolean flag)
  {
    super.setVerifyServerCertificate(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setVerifyServerCertificate(flag);
    }
  }
  
  public void setUseLegacyDatetimeCode(boolean flag)
  {
    super.setUseLegacyDatetimeCode(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseLegacyDatetimeCode(flag);
    }
  }
  
  public void setSelfDestructOnPingSecondsLifetime(int seconds) throws SQLException
  {
    super.setSelfDestructOnPingSecondsLifetime(seconds);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setSelfDestructOnPingSecondsLifetime(seconds);
    }
  }
  
  public void setSelfDestructOnPingMaxOperations(int maxOperations) throws SQLException
  {
    super.setSelfDestructOnPingMaxOperations(maxOperations);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setSelfDestructOnPingMaxOperations(maxOperations);
    }
  }
  
  public void setUseColumnNamesInFindColumn(boolean flag)
  {
    super.setUseColumnNamesInFindColumn(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseColumnNamesInFindColumn(flag);
    }
  }
  
  public void setUseLocalTransactionState(boolean flag)
  {
    super.setUseLocalTransactionState(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseLocalTransactionState(flag);
    }
  }
  
  public void setCompensateOnDuplicateKeyUpdateCounts(boolean flag)
  {
    super.setCompensateOnDuplicateKeyUpdateCounts(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setCompensateOnDuplicateKeyUpdateCounts(flag);
    }
  }
  
  public void setUseAffectedRows(boolean flag)
  {
    super.setUseAffectedRows(flag);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setUseAffectedRows(flag);
    }
  }
  
  public void setPasswordCharacterEncoding(String characterSet)
  {
    super.setPasswordCharacterEncoding(characterSet);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setPasswordCharacterEncoding(characterSet);
    }
  }
  
  public void setLoadBalanceBlacklistTimeout(int loadBalanceBlacklistTimeout) throws SQLException
  {
    super.setLoadBalanceBlacklistTimeout(loadBalanceBlacklistTimeout);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setLoadBalanceBlacklistTimeout(loadBalanceBlacklistTimeout);
    }
  }
  
  public void setRetriesAllDown(int retriesAllDown) throws SQLException
  {
    super.setRetriesAllDown(retriesAllDown);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setRetriesAllDown(retriesAllDown);
    }
  }
  
  public void setExceptionInterceptors(String exceptionInterceptors)
  {
    super.setExceptionInterceptors(exceptionInterceptors);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setExceptionInterceptors(exceptionInterceptors);
    }
  }
  
  public void setQueryTimeoutKillsConnection(boolean queryTimeoutKillsConnection)
  {
    super.setQueryTimeoutKillsConnection(queryTimeoutKillsConnection);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setQueryTimeoutKillsConnection(queryTimeoutKillsConnection);
    }
  }
  
  public void setLoadBalancePingTimeout(int loadBalancePingTimeout) throws SQLException
  {
    super.setLoadBalancePingTimeout(loadBalancePingTimeout);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setLoadBalancePingTimeout(loadBalancePingTimeout);
    }
  }
  
  public void setLoadBalanceValidateConnectionOnSwapServer(boolean loadBalanceValidateConnectionOnSwapServer)
  {
    super.setLoadBalanceValidateConnectionOnSwapServer(loadBalanceValidateConnectionOnSwapServer);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setLoadBalanceValidateConnectionOnSwapServer(loadBalanceValidateConnectionOnSwapServer);
    }
  }
  
  public void setLoadBalanceConnectionGroup(String loadBalanceConnectionGroup)
  {
    super.setLoadBalanceConnectionGroup(loadBalanceConnectionGroup);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setLoadBalanceConnectionGroup(loadBalanceConnectionGroup);
    }
  }
  
  public void setLoadBalanceExceptionChecker(String loadBalanceExceptionChecker)
  {
    super.setLoadBalanceExceptionChecker(loadBalanceExceptionChecker);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setLoadBalanceExceptionChecker(loadBalanceExceptionChecker);
    }
  }
  
  public void setLoadBalanceSQLStateFailover(String loadBalanceSQLStateFailover)
  {
    super.setLoadBalanceSQLStateFailover(loadBalanceSQLStateFailover);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setLoadBalanceSQLStateFailover(loadBalanceSQLStateFailover);
    }
  }
  
  public void setLoadBalanceSQLExceptionSubclassFailover(String loadBalanceSQLExceptionSubclassFailover)
  {
    super.setLoadBalanceSQLExceptionSubclassFailover(loadBalanceSQLExceptionSubclassFailover);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setLoadBalanceSQLExceptionSubclassFailover(loadBalanceSQLExceptionSubclassFailover);
    }
  }
  
  public void setLoadBalanceEnableJMX(boolean loadBalanceEnableJMX)
  {
    super.setLoadBalanceEnableJMX(loadBalanceEnableJMX);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setLoadBalanceEnableJMX(loadBalanceEnableJMX);
    }
  }
  
  public void setLoadBalanceAutoCommitStatementThreshold(int loadBalanceAutoCommitStatementThreshold) throws SQLException
  {
    super.setLoadBalanceAutoCommitStatementThreshold(loadBalanceAutoCommitStatementThreshold);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setLoadBalanceAutoCommitStatementThreshold(loadBalanceAutoCommitStatementThreshold);
    }
  }
  
  public void setLoadBalanceAutoCommitStatementRegex(String loadBalanceAutoCommitStatementRegex)
  {
    super.setLoadBalanceAutoCommitStatementRegex(loadBalanceAutoCommitStatementRegex);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setLoadBalanceAutoCommitStatementRegex(loadBalanceAutoCommitStatementRegex);
    }
  }
  
  public void setAuthenticationPlugins(String authenticationPlugins)
  {
    super.setAuthenticationPlugins(authenticationPlugins);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setAuthenticationPlugins(authenticationPlugins);
    }
  }
  
  public void setDisabledAuthenticationPlugins(String disabledAuthenticationPlugins)
  {
    super.setDisabledAuthenticationPlugins(disabledAuthenticationPlugins);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setDisabledAuthenticationPlugins(disabledAuthenticationPlugins);
    }
  }
  
  public void setDefaultAuthenticationPlugin(String defaultAuthenticationPlugin)
  {
    super.setDefaultAuthenticationPlugin(defaultAuthenticationPlugin);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setDefaultAuthenticationPlugin(defaultAuthenticationPlugin);
    }
  }
  
  public void setParseInfoCacheFactory(String factoryClassname)
  {
    super.setParseInfoCacheFactory(factoryClassname);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setParseInfoCacheFactory(factoryClassname);
    }
  }
  
  public void setServerConfigCacheFactory(String factoryClassname)
  {
    super.setServerConfigCacheFactory(factoryClassname);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setServerConfigCacheFactory(factoryClassname);
    }
  }
  
  public void setDisconnectOnExpiredPasswords(boolean disconnectOnExpiredPasswords)
  {
    super.setDisconnectOnExpiredPasswords(disconnectOnExpiredPasswords);
    for (ConnectionProperties cp : serverConnections.values()) {
      cp.setDisconnectOnExpiredPasswords(disconnectOnExpiredPasswords);
    }
  }
  
  public void setGetProceduresReturnsFunctions(boolean getProcedureReturnsFunctions)
  {
    super.setGetProceduresReturnsFunctions(getProcedureReturnsFunctions);
  }
  

  public int getActiveStatementCount()
  {
    return -1;
  }
  
  public long getIdleFor() {
    return -1L;
  }
  
  public Log getLog() {
    return log;
  }
  
  public boolean isMasterConnection() {
    return false;
  }
  
  public boolean isNoBackslashEscapesSet() {
    return false;
  }
  
  public boolean isSameResource(Connection c) {
    return false;
  }
  
  public boolean parserKnowsUnicode() {
    return false;
  }
  


















  public boolean isAbonormallyLongQuery(long millisOrNanos)
  {
    return false;
  }
  


  public int getAutoIncrementIncrement()
  {
    return -1;
  }
  
  public boolean hasSameProperties(Connection c) {
    return false;
  }
  
  public Properties getProperties() {
    return null;
  }
  

  public String getSchema()
    throws SQLException
  {
    return null;
  }
  




  public int getNetworkTimeout()
    throws SQLException
  {
    return -1;
  }
  


  public Object getConnectionMutex()
  {
    return this;
  }
  
  public void setSessionMaxRows(int max) throws SQLException {
    for (Connection c : serverConnections.values()) {
      c.setSessionMaxRows(max);
    }
  }
  
  public int getSessionMaxRows() {
    return getActiveConnectionPassive().getSessionMaxRows();
  }
  
  public boolean isProxySet()
  {
    return false;
  }
  
  public Connection duplicate() throws SQLException {
    return null;
  }
  
  public CachedResultSetMetaData getCachedMetaData(String sql) {
    return null;
  }
  
  public Timer getCancelTimer() {
    return null;
  }
  
  public SingleByteCharsetConverter getCharsetConverter(String javaEncodingName) throws SQLException {
    return null;
  }
  

  @Deprecated
  public String getCharsetNameForIndex(int charsetIndex)
    throws SQLException
  {
    return getEncodingForIndex(charsetIndex);
  }
  
  public String getEncodingForIndex(int charsetIndex) throws SQLException {
    return null;
  }
  
  public TimeZone getDefaultTimeZone() {
    return null;
  }
  
  public String getErrorMessageEncoding() {
    return null;
  }
  
  public ExceptionInterceptor getExceptionInterceptor()
  {
    if (currentConnection == null) {
      return null;
    }
    
    return currentConnection.getExceptionInterceptor();
  }
  
  public String getHost() {
    return null;
  }
  
  public String getHostPortPair() {
    return getActiveMySQLConnection().getHostPortPair();
  }
  
  public long getId() {
    return -1L;
  }
  
  public int getMaxBytesPerChar(String javaCharsetName) throws SQLException {
    return -1;
  }
  
  public int getMaxBytesPerChar(Integer charsetIndex, String javaCharsetName) throws SQLException {
    return -1;
  }
  
  public int getNetBufferLength() {
    return -1;
  }
  
  public boolean getRequiresEscapingEncoder() {
    return false;
  }
  
  public int getServerMajorVersion() {
    return -1;
  }
  
  public int getServerMinorVersion() {
    return -1;
  }
  
  public int getServerSubMinorVersion() {
    return -1;
  }
  
  public String getServerVariable(String variableName) {
    return null;
  }
  
  public String getServerVersion() {
    return null;
  }
  
  public Calendar getSessionLockedCalendar() {
    return null;
  }
  
  public String getStatementComment() {
    return null;
  }
  
  public List<StatementInterceptorV2> getStatementInterceptorsInstances() {
    return null;
  }
  
  public String getURL() {
    return null;
  }
  
  public String getUser() {
    return null;
  }
  
  public Calendar getUtcCalendar() {
    return null;
  }
  














  public boolean isClientTzUTC()
  {
    return false;
  }
  
  public boolean isCursorFetchEnabled() throws SQLException {
    return false;
  }
  
  public boolean isReadInfoMsgEnabled() {
    return false;
  }
  
  public boolean isServerTzUTC() {
    return false;
  }
  
  public boolean lowerCaseTableNames() {
    return getActiveMySQLConnection().lowerCaseTableNames();
  }
  























  public boolean serverSupportsConvertFn()
    throws SQLException
  {
    return getActiveMySQLConnectionChecked().serverSupportsConvertFn();
  }
  





  public boolean storesLowerCaseTableName()
  {
    return getActiveMySQLConnection().storesLowerCaseTableName();
  }
  













  public boolean useAnsiQuotedIdentifiers()
  {
    return false;
  }
  
  public boolean useMaxRows() {
    return false;
  }
  



  public Properties getClientInfo()
  {
    return null;
  }
  




  public String getClientInfo(String name)
  {
    return null;
  }
  
  public int getHoldability() {
    return -1;
  }
  
  public int getTransactionIsolation() {
    return -1;
  }
  
  public Map<String, Class<?>> getTypeMap() {
    return null;
  }
  
  public SQLWarning getWarnings() throws SQLException {
    return getActiveMySQLConnectionChecked().getWarnings();
  }
  
  public String nativeSQL(String sql) throws SQLException {
    return getActiveMySQLConnectionChecked().nativeSQL(sql);
  }
  
  public ProfilerEventHandler getProfilerEventHandlerInstance() {
    return null;
  }
  
  public void setProxy(MySQLConnection proxy) {}
  
  public void releaseSavepoint(Savepoint savepoint) {}
  
  public void unSafeStatementInterceptors()
    throws SQLException
  {}
  
  public void dumpTestcaseQuery(String query) {}
  
  public void abortInternal()
    throws SQLException
  {}
  
  @Deprecated
  public void clearHasTriedMaster() {}
  
  public void ping()
    throws SQLException
  {}
  
  public void resetServerState()
    throws SQLException
  {}
  
  public void setFailedOver(boolean flag) {}
  
  @Deprecated
  public void setPreferSlaveDuringFailover(boolean flag) {}
  
  public void setStatementComment(String comment) {}
  
  public void reportQueryTime(long millisOrNanos) {}
  
  public void initializeExtension(Extension ex)
    throws SQLException
  {}
  
  public void setSchema(String schema)
    throws SQLException
  {}
  
  public void abort(Executor executor)
    throws SQLException
  {}
  
  public void setNetworkTimeout(Executor executor, int milliseconds)
    throws SQLException
  {}
  
  public void checkClosed()
    throws SQLException
  {}
  
  public void incrementNumberOfPreparedExecutes() {}
  
  public void incrementNumberOfPrepares() {}
  
  public void incrementNumberOfResultSetsCreated() {}
  
  public void initializeResultsMetadataFromCache(String sql, CachedResultSetMetaData cachedMetaData, ResultSetInternalMethods resultSet)
    throws SQLException
  {}
  
  public void initializeSafeStatementInterceptors()
    throws SQLException
  {}
  
  public void maxRowsChanged(com.mysql.jdbc.Statement stmt) {}
  
  public void pingInternal(boolean checkForClosedConnection, int timeoutMillis)
    throws SQLException
  {}
  
  public void realClose(boolean calledExplicitly, boolean issueRollback, boolean skipLocalTeardown, Throwable reason)
    throws SQLException
  {}
  
  public void recachePreparedStatement(ServerPreparedStatement pstmt)
    throws SQLException
  {}
  
  public void registerQueryExecutionTime(long queryTimeMs) {}
  
  public void registerStatement(com.mysql.jdbc.Statement stmt) {}
  
  public void reportNumberOfTablesAccessed(int numTablesAccessed) {}
  
  public void setReadInfoMsgEnabled(boolean flag) {}
  
  public void setReadOnlyInternal(boolean readOnlyFlag)
    throws SQLException
  {}
  
  public void throwConnectionClosedException()
    throws SQLException
  {}
  
  public void unregisterStatement(com.mysql.jdbc.Statement stmt) {}
  
  public void unsetMaxRows(com.mysql.jdbc.Statement stmt)
    throws SQLException
  {}
  
  public void clearWarnings() {}
  
  public void setProfilerEventHandlerInstance(ProfilerEventHandler h) {}
  
  public void decachePreparedStatement(ServerPreparedStatement pstmt)
    throws SQLException
  {}
}
