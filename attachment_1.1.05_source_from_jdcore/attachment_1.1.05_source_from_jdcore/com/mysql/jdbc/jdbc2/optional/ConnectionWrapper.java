package com.mysql.jdbc.jdbc2.optional;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.ExceptionInterceptor;
import com.mysql.jdbc.Extension;
import com.mysql.jdbc.MySQLConnection;
import com.mysql.jdbc.SQLError;
import com.mysql.jdbc.Util;
import com.mysql.jdbc.log.Log;
import java.lang.reflect.Constructor;
import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.Executor;




























public class ConnectionWrapper
  extends WrapperBase
  implements Connection
{
  protected Connection mc = null;
  
  private String invalidHandleStr = "Logical handle no longer valid";
  
  private boolean closed;
  
  private boolean isForXa;
  private static final Constructor<?> JDBC_4_CONNECTION_WRAPPER_CTOR;
  
  static
  {
    if (Util.isJdbc4()) {
      try {
        JDBC_4_CONNECTION_WRAPPER_CTOR = Class.forName("com.mysql.jdbc.jdbc2.optional.JDBC4ConnectionWrapper").getConstructor(new Class[] { MysqlPooledConnection.class, Connection.class, Boolean.TYPE });
      }
      catch (SecurityException e) {
        throw new RuntimeException(e);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    } else {
      JDBC_4_CONNECTION_WRAPPER_CTOR = null;
    }
  }
  
  protected static ConnectionWrapper getInstance(MysqlPooledConnection mysqlPooledConnection, Connection mysqlConnection, boolean forXa) throws SQLException {
    if (!Util.isJdbc4()) {
      return new ConnectionWrapper(mysqlPooledConnection, mysqlConnection, forXa);
    }
    
    return (ConnectionWrapper)Util.handleNewInstance(JDBC_4_CONNECTION_WRAPPER_CTOR, new Object[] { mysqlPooledConnection, mysqlConnection, Boolean.valueOf(forXa) }, mysqlPooledConnection.getExceptionInterceptor());
  }
  










  public ConnectionWrapper(MysqlPooledConnection mysqlPooledConnection, Connection mysqlConnection, boolean forXa)
    throws SQLException
  {
    super(mysqlPooledConnection);
    
    mc = mysqlConnection;
    closed = false;
    isForXa = forXa;
    
    if (isForXa) {
      setInGlobalTx(false);
    }
  }
  




  public void setAutoCommit(boolean autoCommit)
    throws SQLException
  {
    checkClosed();
    
    if ((autoCommit) && (isInGlobalTx())) {
      throw SQLError.createSQLException("Can't set autocommit to 'true' on an XAConnection", "2D000", 1401, exceptionInterceptor);
    }
    
    try
    {
      mc.setAutoCommit(autoCommit);
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
  }
  




  public boolean getAutoCommit()
    throws SQLException
  {
    checkClosed();
    try
    {
      return mc.getAutoCommit();
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return false;
  }
  




  public void setCatalog(String catalog)
    throws SQLException
  {
    checkClosed();
    try
    {
      mc.setCatalog(catalog);
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
  }
  







  public String getCatalog()
    throws SQLException
  {
    checkClosed();
    try
    {
      return mc.getCatalog();
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  




  public boolean isClosed()
    throws SQLException
  {
    return (closed) || (mc.isClosed());
  }
  
  public boolean isMasterConnection() {
    return mc.isMasterConnection();
  }
  

  public void setHoldability(int arg0)
    throws SQLException
  {
    checkClosed();
    try
    {
      mc.setHoldability(arg0);
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
  }
  

  public int getHoldability()
    throws SQLException
  {
    checkClosed();
    try
    {
      return mc.getHoldability();
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return 1;
  }
  





  public long getIdleFor()
  {
    return mc.getIdleFor();
  }
  







  public DatabaseMetaData getMetaData()
    throws SQLException
  {
    checkClosed();
    try
    {
      return mc.getMetaData();
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  




  public void setReadOnly(boolean readOnly)
    throws SQLException
  {
    checkClosed();
    try
    {
      mc.setReadOnly(readOnly);
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
  }
  




  public boolean isReadOnly()
    throws SQLException
  {
    checkClosed();
    try
    {
      return mc.isReadOnly();
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return false;
  }
  

  public Savepoint setSavepoint()
    throws SQLException
  {
    checkClosed();
    
    if (isInGlobalTx()) {
      throw SQLError.createSQLException("Can't set autocommit to 'true' on an XAConnection", "2D000", 1401, exceptionInterceptor);
    }
    
    try
    {
      return mc.setSavepoint();
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  

  public Savepoint setSavepoint(String arg0)
    throws SQLException
  {
    checkClosed();
    
    if (isInGlobalTx()) {
      throw SQLError.createSQLException("Can't set autocommit to 'true' on an XAConnection", "2D000", 1401, exceptionInterceptor);
    }
    
    try
    {
      return mc.setSavepoint(arg0);
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  




  public void setTransactionIsolation(int level)
    throws SQLException
  {
    checkClosed();
    try
    {
      mc.setTransactionIsolation(level);
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
  }
  




  public int getTransactionIsolation()
    throws SQLException
  {
    checkClosed();
    try
    {
      return mc.getTransactionIsolation();
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return 4;
  }
  





  public Map<String, Class<?>> getTypeMap()
    throws SQLException
  {
    checkClosed();
    try
    {
      return mc.getTypeMap();
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  




  public SQLWarning getWarnings()
    throws SQLException
  {
    checkClosed();
    try
    {
      return mc.getWarnings();
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  





  public void clearWarnings()
    throws SQLException
  {
    checkClosed();
    try
    {
      mc.clearWarnings();
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
  }
  








  public void close()
    throws SQLException
  {
    close(true);
  }
  





  public void commit()
    throws SQLException
  {
    checkClosed();
    
    if (isInGlobalTx()) {
      throw SQLError.createSQLException("Can't call commit() on an XAConnection associated with a global transaction", "2D000", 1401, exceptionInterceptor);
    }
    
    try
    {
      mc.commit();
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
  }
  




  public Statement createStatement()
    throws SQLException
  {
    checkClosed();
    try
    {
      return StatementWrapper.getInstance(this, pooledConnection, mc.createStatement());
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  




  public Statement createStatement(int resultSetType, int resultSetConcurrency)
    throws SQLException
  {
    checkClosed();
    try
    {
      return StatementWrapper.getInstance(this, pooledConnection, mc.createStatement(resultSetType, resultSetConcurrency));
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  

  public Statement createStatement(int arg0, int arg1, int arg2)
    throws SQLException
  {
    checkClosed();
    try
    {
      return StatementWrapper.getInstance(this, pooledConnection, mc.createStatement(arg0, arg1, arg2));
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  




  public String nativeSQL(String sql)
    throws SQLException
  {
    checkClosed();
    try
    {
      return mc.nativeSQL(sql);
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  




  public CallableStatement prepareCall(String sql)
    throws SQLException
  {
    checkClosed();
    try
    {
      return CallableStatementWrapper.getInstance(this, pooledConnection, mc.prepareCall(sql));
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  




  public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
    throws SQLException
  {
    checkClosed();
    try
    {
      return CallableStatementWrapper.getInstance(this, pooledConnection, mc.prepareCall(sql, resultSetType, resultSetConcurrency));
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  

  public CallableStatement prepareCall(String arg0, int arg1, int arg2, int arg3)
    throws SQLException
  {
    checkClosed();
    try
    {
      return CallableStatementWrapper.getInstance(this, pooledConnection, mc.prepareCall(arg0, arg1, arg2, arg3));
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  
  public PreparedStatement clientPrepare(String sql) throws SQLException {
    checkClosed();
    try
    {
      return new PreparedStatementWrapper(this, pooledConnection, mc.clientPrepareStatement(sql));
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  
  public PreparedStatement clientPrepare(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
    checkClosed();
    try
    {
      return new PreparedStatementWrapper(this, pooledConnection, mc.clientPrepareStatement(sql, resultSetType, resultSetConcurrency));
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  




  public PreparedStatement prepareStatement(String sql)
    throws SQLException
  {
    checkClosed();
    
    PreparedStatement res = null;
    try
    {
      res = PreparedStatementWrapper.getInstance(this, pooledConnection, mc.prepareStatement(sql));
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return res;
  }
  




  public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
    throws SQLException
  {
    checkClosed();
    try
    {
      return PreparedStatementWrapper.getInstance(this, pooledConnection, mc.prepareStatement(sql, resultSetType, resultSetConcurrency));
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  

  public PreparedStatement prepareStatement(String arg0, int arg1, int arg2, int arg3)
    throws SQLException
  {
    checkClosed();
    try
    {
      return PreparedStatementWrapper.getInstance(this, pooledConnection, mc.prepareStatement(arg0, arg1, arg2, arg3));
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  

  public PreparedStatement prepareStatement(String arg0, int arg1)
    throws SQLException
  {
    checkClosed();
    try
    {
      return PreparedStatementWrapper.getInstance(this, pooledConnection, mc.prepareStatement(arg0, arg1));
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  

  public PreparedStatement prepareStatement(String arg0, int[] arg1)
    throws SQLException
  {
    checkClosed();
    try
    {
      return PreparedStatementWrapper.getInstance(this, pooledConnection, mc.prepareStatement(arg0, arg1));
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  

  public PreparedStatement prepareStatement(String arg0, String[] arg1)
    throws SQLException
  {
    checkClosed();
    try
    {
      return PreparedStatementWrapper.getInstance(this, pooledConnection, mc.prepareStatement(arg0, arg1));
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  

  public void releaseSavepoint(Savepoint arg0)
    throws SQLException
  {
    checkClosed();
    try
    {
      mc.releaseSavepoint(arg0);
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
  }
  




  public void rollback()
    throws SQLException
  {
    checkClosed();
    
    if (isInGlobalTx()) {
      throw SQLError.createSQLException("Can't call rollback() on an XAConnection associated with a global transaction", "2D000", 1401, exceptionInterceptor);
    }
    
    try
    {
      mc.rollback();
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
  }
  

  public void rollback(Savepoint arg0)
    throws SQLException
  {
    checkClosed();
    
    if (isInGlobalTx()) {
      throw SQLError.createSQLException("Can't call rollback() on an XAConnection associated with a global transaction", "2D000", 1401, exceptionInterceptor);
    }
    
    try
    {
      mc.rollback(arg0);
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
  }
  
  public boolean isSameResource(Connection c) {
    if ((c instanceof ConnectionWrapper)) {
      return mc.isSameResource(mc);
    }
    return mc.isSameResource(c);
  }
  
  protected void close(boolean fireClosedEvent) throws SQLException {
    synchronized (pooledConnection) {
      if (closed) {
        return;
      }
      
      if ((!isInGlobalTx()) && (mc.getRollbackOnPooledClose()) && (!getAutoCommit())) {
        rollback();
      }
      
      if (fireClosedEvent) {
        pooledConnection.callConnectionEventListeners(2, null);
      }
      


      closed = true;
    }
  }
  
  public void checkClosed() throws SQLException {
    if (closed) {
      throw SQLError.createSQLException(invalidHandleStr, exceptionInterceptor);
    }
  }
  
  public boolean isInGlobalTx() {
    return mc.isInGlobalTx();
  }
  
  public void setInGlobalTx(boolean flag) {
    mc.setInGlobalTx(flag);
  }
  
  public void ping() throws SQLException {
    if (mc != null) {
      mc.ping();
    }
  }
  
  public void changeUser(String userName, String newPassword) throws SQLException {
    checkClosed();
    try
    {
      mc.changeUser(userName, newPassword);
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
  }
  
  @Deprecated
  public void clearHasTriedMaster() {
    mc.clearHasTriedMaster();
  }
  
  public PreparedStatement clientPrepareStatement(String sql) throws SQLException {
    checkClosed();
    try
    {
      return PreparedStatementWrapper.getInstance(this, pooledConnection, mc.clientPrepareStatement(sql));
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  
  public PreparedStatement clientPrepareStatement(String sql, int autoGenKeyIndex) throws SQLException {
    try {
      return PreparedStatementWrapper.getInstance(this, pooledConnection, mc.clientPrepareStatement(sql, autoGenKeyIndex));
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  
  public PreparedStatement clientPrepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
    try {
      return PreparedStatementWrapper.getInstance(this, pooledConnection, mc.clientPrepareStatement(sql, resultSetType, resultSetConcurrency));
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  
  public PreparedStatement clientPrepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
  {
    try {
      return PreparedStatementWrapper.getInstance(this, pooledConnection, mc.clientPrepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability));
    }
    catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  
  public PreparedStatement clientPrepareStatement(String sql, int[] autoGenKeyIndexes) throws SQLException {
    try {
      return PreparedStatementWrapper.getInstance(this, pooledConnection, mc.clientPrepareStatement(sql, autoGenKeyIndexes));
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  
  public PreparedStatement clientPrepareStatement(String sql, String[] autoGenKeyColNames) throws SQLException {
    try {
      return PreparedStatementWrapper.getInstance(this, pooledConnection, mc.clientPrepareStatement(sql, autoGenKeyColNames));
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  
  public int getActiveStatementCount() {
    return mc.getActiveStatementCount();
  }
  
  public Log getLog() throws SQLException {
    return mc.getLog();
  }
  


  @Deprecated
  public String getServerCharacterEncoding()
  {
    return getServerCharset();
  }
  
  public String getServerCharset() {
    return mc.getServerCharset();
  }
  
  public TimeZone getServerTimezoneTZ() {
    return mc.getServerTimezoneTZ();
  }
  
  public String getStatementComment() {
    return mc.getStatementComment();
  }
  
  @Deprecated
  public boolean hasTriedMaster() {
    return mc.hasTriedMaster();
  }
  
  public boolean isAbonormallyLongQuery(long millisOrNanos) {
    return mc.isAbonormallyLongQuery(millisOrNanos);
  }
  
  public boolean isNoBackslashEscapesSet() {
    return mc.isNoBackslashEscapesSet();
  }
  
  public boolean lowerCaseTableNames() {
    return mc.lowerCaseTableNames();
  }
  
  public boolean parserKnowsUnicode() {
    return mc.parserKnowsUnicode();
  }
  
  public void reportQueryTime(long millisOrNanos) {
    mc.reportQueryTime(millisOrNanos);
  }
  
  public void resetServerState() throws SQLException {
    checkClosed();
    try
    {
      mc.resetServerState();
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
  }
  
  public PreparedStatement serverPrepareStatement(String sql) throws SQLException {
    checkClosed();
    try
    {
      return PreparedStatementWrapper.getInstance(this, pooledConnection, mc.serverPrepareStatement(sql));
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  
  public PreparedStatement serverPrepareStatement(String sql, int autoGenKeyIndex) throws SQLException {
    try {
      return PreparedStatementWrapper.getInstance(this, pooledConnection, mc.serverPrepareStatement(sql, autoGenKeyIndex));
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  
  public PreparedStatement serverPrepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
    try {
      return PreparedStatementWrapper.getInstance(this, pooledConnection, mc.serverPrepareStatement(sql, resultSetType, resultSetConcurrency));
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  
  public PreparedStatement serverPrepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException
  {
    try {
      return PreparedStatementWrapper.getInstance(this, pooledConnection, mc.serverPrepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability));
    }
    catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  
  public PreparedStatement serverPrepareStatement(String sql, int[] autoGenKeyIndexes) throws SQLException {
    try {
      return PreparedStatementWrapper.getInstance(this, pooledConnection, mc.serverPrepareStatement(sql, autoGenKeyIndexes));
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  
  public PreparedStatement serverPrepareStatement(String sql, String[] autoGenKeyColNames) throws SQLException {
    try {
      return PreparedStatementWrapper.getInstance(this, pooledConnection, mc.serverPrepareStatement(sql, autoGenKeyColNames));
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  
  public void setFailedOver(boolean flag) {
    mc.setFailedOver(flag);
  }
  
  @Deprecated
  public void setPreferSlaveDuringFailover(boolean flag)
  {
    mc.setPreferSlaveDuringFailover(flag);
  }
  
  public void setStatementComment(String comment) {
    mc.setStatementComment(comment);
  }
  
  public void shutdownServer() throws SQLException
  {
    checkClosed();
    try
    {
      mc.shutdownServer();
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
  }
  
  public boolean supportsIsolationLevel()
  {
    return mc.supportsIsolationLevel();
  }
  
  public boolean supportsQuotedIdentifiers() {
    return mc.supportsQuotedIdentifiers();
  }
  
  public boolean supportsTransactions() {
    return mc.supportsTransactions();
  }
  
  public boolean versionMeetsMinimum(int major, int minor, int subminor) throws SQLException {
    checkClosed();
    try
    {
      return mc.versionMeetsMinimum(major, minor, subminor);
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return false;
  }
  
  public String exposeAsXml() throws SQLException {
    checkClosed();
    try
    {
      return mc.exposeAsXml();
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  
  public boolean getAllowLoadLocalInfile() {
    return mc.getAllowLoadLocalInfile();
  }
  
  public boolean getAllowMultiQueries() {
    return mc.getAllowMultiQueries();
  }
  
  public boolean getAllowNanAndInf() {
    return mc.getAllowNanAndInf();
  }
  
  public boolean getAllowUrlInLocalInfile() {
    return mc.getAllowUrlInLocalInfile();
  }
  
  public boolean getAlwaysSendSetIsolation() {
    return mc.getAlwaysSendSetIsolation();
  }
  
  public boolean getAutoClosePStmtStreams() {
    return mc.getAutoClosePStmtStreams();
  }
  
  public boolean getAutoDeserialize() {
    return mc.getAutoDeserialize();
  }
  
  public boolean getAutoGenerateTestcaseScript() {
    return mc.getAutoGenerateTestcaseScript();
  }
  
  public boolean getAutoReconnectForPools() {
    return mc.getAutoReconnectForPools();
  }
  
  public boolean getAutoSlowLog() {
    return mc.getAutoSlowLog();
  }
  
  public int getBlobSendChunkSize() {
    return mc.getBlobSendChunkSize();
  }
  
  public boolean getBlobsAreStrings() {
    return mc.getBlobsAreStrings();
  }
  
  public boolean getCacheCallableStatements() {
    return mc.getCacheCallableStatements();
  }
  
  public boolean getCacheCallableStmts() {
    return mc.getCacheCallableStmts();
  }
  
  public boolean getCachePrepStmts() {
    return mc.getCachePrepStmts();
  }
  
  public boolean getCachePreparedStatements() {
    return mc.getCachePreparedStatements();
  }
  
  public boolean getCacheResultSetMetadata() {
    return mc.getCacheResultSetMetadata();
  }
  
  public boolean getCacheServerConfiguration() {
    return mc.getCacheServerConfiguration();
  }
  
  public int getCallableStatementCacheSize() {
    return mc.getCallableStatementCacheSize();
  }
  
  public int getCallableStmtCacheSize() {
    return mc.getCallableStmtCacheSize();
  }
  
  public boolean getCapitalizeTypeNames() {
    return mc.getCapitalizeTypeNames();
  }
  
  public String getCharacterSetResults() {
    return mc.getCharacterSetResults();
  }
  
  public String getClientCertificateKeyStorePassword() {
    return mc.getClientCertificateKeyStorePassword();
  }
  
  public String getClientCertificateKeyStoreType() {
    return mc.getClientCertificateKeyStoreType();
  }
  
  public String getClientCertificateKeyStoreUrl() {
    return mc.getClientCertificateKeyStoreUrl();
  }
  
  public String getClientInfoProvider() {
    return mc.getClientInfoProvider();
  }
  
  public String getClobCharacterEncoding() {
    return mc.getClobCharacterEncoding();
  }
  
  public boolean getClobberStreamingResults() {
    return mc.getClobberStreamingResults();
  }
  
  public int getConnectTimeout() {
    return mc.getConnectTimeout();
  }
  
  public String getConnectionCollation() {
    return mc.getConnectionCollation();
  }
  
  public String getConnectionLifecycleInterceptors() {
    return mc.getConnectionLifecycleInterceptors();
  }
  
  public boolean getContinueBatchOnError() {
    return mc.getContinueBatchOnError();
  }
  
  public boolean getCreateDatabaseIfNotExist() {
    return mc.getCreateDatabaseIfNotExist();
  }
  
  public int getDefaultFetchSize() {
    return mc.getDefaultFetchSize();
  }
  
  public boolean getDontTrackOpenResources() {
    return mc.getDontTrackOpenResources();
  }
  
  public boolean getDumpMetadataOnColumnNotFound() {
    return mc.getDumpMetadataOnColumnNotFound();
  }
  
  public boolean getDumpQueriesOnException() {
    return mc.getDumpQueriesOnException();
  }
  
  public boolean getDynamicCalendars() {
    return mc.getDynamicCalendars();
  }
  
  public boolean getElideSetAutoCommits() {
    return mc.getElideSetAutoCommits();
  }
  
  public boolean getEmptyStringsConvertToZero() {
    return mc.getEmptyStringsConvertToZero();
  }
  
  public boolean getEmulateLocators() {
    return mc.getEmulateLocators();
  }
  
  public boolean getEmulateUnsupportedPstmts() {
    return mc.getEmulateUnsupportedPstmts();
  }
  
  public boolean getEnablePacketDebug() {
    return mc.getEnablePacketDebug();
  }
  
  public boolean getEnableQueryTimeouts() {
    return mc.getEnableQueryTimeouts();
  }
  
  public String getEncoding() {
    return mc.getEncoding();
  }
  
  public boolean getExplainSlowQueries() {
    return mc.getExplainSlowQueries();
  }
  
  public boolean getFailOverReadOnly() {
    return mc.getFailOverReadOnly();
  }
  
  public boolean getFunctionsNeverReturnBlobs() {
    return mc.getFunctionsNeverReturnBlobs();
  }
  
  public boolean getGatherPerfMetrics() {
    return mc.getGatherPerfMetrics();
  }
  
  public boolean getGatherPerformanceMetrics() {
    return mc.getGatherPerformanceMetrics();
  }
  
  public boolean getGenerateSimpleParameterMetadata() {
    return mc.getGenerateSimpleParameterMetadata();
  }
  
  public boolean getHoldResultsOpenOverStatementClose() {
    return mc.getHoldResultsOpenOverStatementClose();
  }
  
  public boolean getIgnoreNonTxTables() {
    return mc.getIgnoreNonTxTables();
  }
  
  public boolean getIncludeInnodbStatusInDeadlockExceptions() {
    return mc.getIncludeInnodbStatusInDeadlockExceptions();
  }
  
  public int getInitialTimeout() {
    return mc.getInitialTimeout();
  }
  
  public boolean getInteractiveClient() {
    return mc.getInteractiveClient();
  }
  
  public boolean getIsInteractiveClient() {
    return mc.getIsInteractiveClient();
  }
  
  public boolean getJdbcCompliantTruncation() {
    return mc.getJdbcCompliantTruncation();
  }
  
  public boolean getJdbcCompliantTruncationForReads() {
    return mc.getJdbcCompliantTruncationForReads();
  }
  
  public String getLargeRowSizeThreshold() {
    return mc.getLargeRowSizeThreshold();
  }
  
  public String getLoadBalanceStrategy() {
    return mc.getLoadBalanceStrategy();
  }
  
  public String getServerAffinityOrder() {
    return mc.getServerAffinityOrder();
  }
  
  public String getLocalSocketAddress() {
    return mc.getLocalSocketAddress();
  }
  
  public int getLocatorFetchBufferSize() {
    return mc.getLocatorFetchBufferSize();
  }
  
  public boolean getLogSlowQueries() {
    return mc.getLogSlowQueries();
  }
  
  public boolean getLogXaCommands() {
    return mc.getLogXaCommands();
  }
  
  public String getLogger() {
    return mc.getLogger();
  }
  
  public String getLoggerClassName() {
    return mc.getLoggerClassName();
  }
  
  public boolean getMaintainTimeStats() {
    return mc.getMaintainTimeStats();
  }
  
  public int getMaxQuerySizeToLog() {
    return mc.getMaxQuerySizeToLog();
  }
  
  public int getMaxReconnects() {
    return mc.getMaxReconnects();
  }
  
  public int getMaxRows() {
    return mc.getMaxRows();
  }
  
  public int getMetadataCacheSize() {
    return mc.getMetadataCacheSize();
  }
  
  public int getNetTimeoutForStreamingResults() {
    return mc.getNetTimeoutForStreamingResults();
  }
  
  public boolean getNoAccessToProcedureBodies() {
    return mc.getNoAccessToProcedureBodies();
  }
  
  public boolean getNoDatetimeStringSync() {
    return mc.getNoDatetimeStringSync();
  }
  
  public boolean getNoTimezoneConversionForTimeType() {
    return mc.getNoTimezoneConversionForTimeType();
  }
  
  public boolean getNoTimezoneConversionForDateType() {
    return mc.getNoTimezoneConversionForDateType();
  }
  
  public boolean getCacheDefaultTimezone() {
    return mc.getCacheDefaultTimezone();
  }
  
  public boolean getNullCatalogMeansCurrent() {
    return mc.getNullCatalogMeansCurrent();
  }
  
  public boolean getNullNamePatternMatchesAll() {
    return mc.getNullNamePatternMatchesAll();
  }
  
  public boolean getOverrideSupportsIntegrityEnhancementFacility() {
    return mc.getOverrideSupportsIntegrityEnhancementFacility();
  }
  
  public int getPacketDebugBufferSize() {
    return mc.getPacketDebugBufferSize();
  }
  
  public boolean getPadCharsWithSpace() {
    return mc.getPadCharsWithSpace();
  }
  
  public boolean getParanoid() {
    return mc.getParanoid();
  }
  
  public boolean getPedantic() {
    return mc.getPedantic();
  }
  
  public boolean getPinGlobalTxToPhysicalConnection() {
    return mc.getPinGlobalTxToPhysicalConnection();
  }
  
  public boolean getPopulateInsertRowWithDefaultValues() {
    return mc.getPopulateInsertRowWithDefaultValues();
  }
  
  public int getPrepStmtCacheSize() {
    return mc.getPrepStmtCacheSize();
  }
  
  public int getPrepStmtCacheSqlLimit() {
    return mc.getPrepStmtCacheSqlLimit();
  }
  
  public int getPreparedStatementCacheSize() {
    return mc.getPreparedStatementCacheSize();
  }
  
  public int getPreparedStatementCacheSqlLimit() {
    return mc.getPreparedStatementCacheSqlLimit();
  }
  
  public boolean getProcessEscapeCodesForPrepStmts() {
    return mc.getProcessEscapeCodesForPrepStmts();
  }
  
  public boolean getProfileSQL() {
    return mc.getProfileSQL();
  }
  
  public boolean getProfileSql() {
    return mc.getProfileSql();
  }
  
  public String getPropertiesTransform() {
    return mc.getPropertiesTransform();
  }
  
  public int getQueriesBeforeRetryMaster() {
    return mc.getQueriesBeforeRetryMaster();
  }
  
  public boolean getReconnectAtTxEnd() {
    return mc.getReconnectAtTxEnd();
  }
  
  public boolean getRelaxAutoCommit() {
    return mc.getRelaxAutoCommit();
  }
  
  public int getReportMetricsIntervalMillis() {
    return mc.getReportMetricsIntervalMillis();
  }
  
  public boolean getRequireSSL() {
    return mc.getRequireSSL();
  }
  
  public String getResourceId() {
    return mc.getResourceId();
  }
  
  public int getResultSetSizeThreshold() {
    return mc.getResultSetSizeThreshold();
  }
  
  public boolean getRewriteBatchedStatements() {
    return mc.getRewriteBatchedStatements();
  }
  
  public boolean getRollbackOnPooledClose() {
    return mc.getRollbackOnPooledClose();
  }
  
  public boolean getRoundRobinLoadBalance() {
    return mc.getRoundRobinLoadBalance();
  }
  
  public boolean getRunningCTS13() {
    return mc.getRunningCTS13();
  }
  
  public int getSecondsBeforeRetryMaster() {
    return mc.getSecondsBeforeRetryMaster();
  }
  
  public String getServerTimezone() {
    return mc.getServerTimezone();
  }
  
  public String getSessionVariables() {
    return mc.getSessionVariables();
  }
  
  public int getSlowQueryThresholdMillis() {
    return mc.getSlowQueryThresholdMillis();
  }
  
  public long getSlowQueryThresholdNanos() {
    return mc.getSlowQueryThresholdNanos();
  }
  
  public String getSocketFactory() {
    return mc.getSocketFactory();
  }
  
  public String getSocketFactoryClassName() {
    return mc.getSocketFactoryClassName();
  }
  
  public int getSocketTimeout() {
    return mc.getSocketTimeout();
  }
  
  public String getStatementInterceptors() {
    return mc.getStatementInterceptors();
  }
  
  public boolean getStrictFloatingPoint() {
    return mc.getStrictFloatingPoint();
  }
  
  public boolean getStrictUpdates() {
    return mc.getStrictUpdates();
  }
  
  public boolean getTcpKeepAlive() {
    return mc.getTcpKeepAlive();
  }
  
  public boolean getTcpNoDelay() {
    return mc.getTcpNoDelay();
  }
  
  public int getTcpRcvBuf() {
    return mc.getTcpRcvBuf();
  }
  
  public int getTcpSndBuf() {
    return mc.getTcpSndBuf();
  }
  
  public int getTcpTrafficClass() {
    return mc.getTcpTrafficClass();
  }
  
  public boolean getTinyInt1isBit() {
    return mc.getTinyInt1isBit();
  }
  
  public boolean getTraceProtocol() {
    return mc.getTraceProtocol();
  }
  
  public boolean getTransformedBitIsBoolean() {
    return mc.getTransformedBitIsBoolean();
  }
  
  public boolean getTreatUtilDateAsTimestamp() {
    return mc.getTreatUtilDateAsTimestamp();
  }
  
  public String getTrustCertificateKeyStorePassword() {
    return mc.getTrustCertificateKeyStorePassword();
  }
  
  public String getTrustCertificateKeyStoreType() {
    return mc.getTrustCertificateKeyStoreType();
  }
  
  public String getTrustCertificateKeyStoreUrl() {
    return mc.getTrustCertificateKeyStoreUrl();
  }
  
  public boolean getUltraDevHack() {
    return mc.getUltraDevHack();
  }
  
  public boolean getUseBlobToStoreUTF8OutsideBMP() {
    return mc.getUseBlobToStoreUTF8OutsideBMP();
  }
  
  public boolean getUseCompression() {
    return mc.getUseCompression();
  }
  
  public String getUseConfigs() {
    return mc.getUseConfigs();
  }
  
  public boolean getUseCursorFetch() {
    return mc.getUseCursorFetch();
  }
  
  public boolean getUseDirectRowUnpack() {
    return mc.getUseDirectRowUnpack();
  }
  
  public boolean getUseDynamicCharsetInfo() {
    return mc.getUseDynamicCharsetInfo();
  }
  
  public boolean getUseFastDateParsing() {
    return mc.getUseFastDateParsing();
  }
  
  public boolean getUseFastIntParsing() {
    return mc.getUseFastIntParsing();
  }
  
  public boolean getUseGmtMillisForDatetimes() {
    return mc.getUseGmtMillisForDatetimes();
  }
  
  public boolean getUseHostsInPrivileges() {
    return mc.getUseHostsInPrivileges();
  }
  
  public boolean getUseInformationSchema() {
    return mc.getUseInformationSchema();
  }
  
  public boolean getUseJDBCCompliantTimezoneShift() {
    return mc.getUseJDBCCompliantTimezoneShift();
  }
  
  public boolean getUseJvmCharsetConverters() {
    return mc.getUseJvmCharsetConverters();
  }
  
  public boolean getUseLocalSessionState() {
    return mc.getUseLocalSessionState();
  }
  
  public boolean getUseNanosForElapsedTime() {
    return mc.getUseNanosForElapsedTime();
  }
  
  public boolean getUseOldAliasMetadataBehavior() {
    return mc.getUseOldAliasMetadataBehavior();
  }
  
  public boolean getUseOldUTF8Behavior() {
    return mc.getUseOldUTF8Behavior();
  }
  
  public boolean getUseOnlyServerErrorMessages() {
    return mc.getUseOnlyServerErrorMessages();
  }
  
  public boolean getUseReadAheadInput() {
    return mc.getUseReadAheadInput();
  }
  
  public boolean getUseSSL() {
    return mc.getUseSSL();
  }
  
  public boolean getUseSSPSCompatibleTimezoneShift() {
    return mc.getUseSSPSCompatibleTimezoneShift();
  }
  
  public boolean getUseServerPrepStmts() {
    return mc.getUseServerPrepStmts();
  }
  
  public boolean getUseServerPreparedStmts() {
    return mc.getUseServerPreparedStmts();
  }
  
  public boolean getUseSqlStateCodes() {
    return mc.getUseSqlStateCodes();
  }
  
  public boolean getUseStreamLengthsInPrepStmts() {
    return mc.getUseStreamLengthsInPrepStmts();
  }
  
  public boolean getUseTimezone() {
    return mc.getUseTimezone();
  }
  
  public boolean getUseUltraDevWorkAround() {
    return mc.getUseUltraDevWorkAround();
  }
  
  public boolean getUseUnbufferedInput() {
    return mc.getUseUnbufferedInput();
  }
  
  public boolean getUseUnicode() {
    return mc.getUseUnicode();
  }
  
  public boolean getUseUsageAdvisor() {
    return mc.getUseUsageAdvisor();
  }
  
  public String getUtf8OutsideBmpExcludedColumnNamePattern() {
    return mc.getUtf8OutsideBmpExcludedColumnNamePattern();
  }
  
  public String getUtf8OutsideBmpIncludedColumnNamePattern() {
    return mc.getUtf8OutsideBmpIncludedColumnNamePattern();
  }
  
  public boolean getYearIsDateType() {
    return mc.getYearIsDateType();
  }
  
  public String getZeroDateTimeBehavior() {
    return mc.getZeroDateTimeBehavior();
  }
  
  public void setAllowLoadLocalInfile(boolean property) {
    mc.setAllowLoadLocalInfile(property);
  }
  
  public void setAllowMultiQueries(boolean property) {
    mc.setAllowMultiQueries(property);
  }
  
  public void setAllowNanAndInf(boolean flag) {
    mc.setAllowNanAndInf(flag);
  }
  
  public void setAllowUrlInLocalInfile(boolean flag) {
    mc.setAllowUrlInLocalInfile(flag);
  }
  
  public void setAlwaysSendSetIsolation(boolean flag) {
    mc.setAlwaysSendSetIsolation(flag);
  }
  
  public void setAutoClosePStmtStreams(boolean flag) {
    mc.setAutoClosePStmtStreams(flag);
  }
  
  public void setAutoDeserialize(boolean flag) {
    mc.setAutoDeserialize(flag);
  }
  
  public void setAutoGenerateTestcaseScript(boolean flag) {
    mc.setAutoGenerateTestcaseScript(flag);
  }
  
  public void setAutoReconnect(boolean flag) {
    mc.setAutoReconnect(flag);
  }
  
  public void setAutoReconnectForConnectionPools(boolean property) {
    mc.setAutoReconnectForConnectionPools(property);
  }
  
  public void setAutoReconnectForPools(boolean flag) {
    mc.setAutoReconnectForPools(flag);
  }
  
  public void setAutoSlowLog(boolean flag) {
    mc.setAutoSlowLog(flag);
  }
  
  public void setBlobSendChunkSize(String value) throws SQLException {
    mc.setBlobSendChunkSize(value);
  }
  
  public void setBlobsAreStrings(boolean flag) {
    mc.setBlobsAreStrings(flag);
  }
  
  public void setCacheCallableStatements(boolean flag) {
    mc.setCacheCallableStatements(flag);
  }
  
  public void setCacheCallableStmts(boolean flag) {
    mc.setCacheCallableStmts(flag);
  }
  
  public void setCachePrepStmts(boolean flag) {
    mc.setCachePrepStmts(flag);
  }
  
  public void setCachePreparedStatements(boolean flag) {
    mc.setCachePreparedStatements(flag);
  }
  
  public void setCacheResultSetMetadata(boolean property) {
    mc.setCacheResultSetMetadata(property);
  }
  
  public void setCacheServerConfiguration(boolean flag) {
    mc.setCacheServerConfiguration(flag);
  }
  
  public void setCallableStatementCacheSize(int size) throws SQLException {
    mc.setCallableStatementCacheSize(size);
  }
  
  public void setCallableStmtCacheSize(int cacheSize) throws SQLException {
    mc.setCallableStmtCacheSize(cacheSize);
  }
  
  public void setCapitalizeDBMDTypes(boolean property) {
    mc.setCapitalizeDBMDTypes(property);
  }
  
  public void setCapitalizeTypeNames(boolean flag) {
    mc.setCapitalizeTypeNames(flag);
  }
  
  public void setCharacterEncoding(String encoding) {
    mc.setCharacterEncoding(encoding);
  }
  
  public void setCharacterSetResults(String characterSet) {
    mc.setCharacterSetResults(characterSet);
  }
  
  public void setClientCertificateKeyStorePassword(String value) {
    mc.setClientCertificateKeyStorePassword(value);
  }
  
  public void setClientCertificateKeyStoreType(String value) {
    mc.setClientCertificateKeyStoreType(value);
  }
  
  public void setClientCertificateKeyStoreUrl(String value) {
    mc.setClientCertificateKeyStoreUrl(value);
  }
  
  public void setClientInfoProvider(String classname) {
    mc.setClientInfoProvider(classname);
  }
  
  public void setClobCharacterEncoding(String encoding) {
    mc.setClobCharacterEncoding(encoding);
  }
  
  public void setClobberStreamingResults(boolean flag) {
    mc.setClobberStreamingResults(flag);
  }
  
  public void setConnectTimeout(int timeoutMs) throws SQLException {
    mc.setConnectTimeout(timeoutMs);
  }
  
  public void setConnectionCollation(String collation) {
    mc.setConnectionCollation(collation);
  }
  
  public void setConnectionLifecycleInterceptors(String interceptors) {
    mc.setConnectionLifecycleInterceptors(interceptors);
  }
  
  public void setContinueBatchOnError(boolean property) {
    mc.setContinueBatchOnError(property);
  }
  
  public void setCreateDatabaseIfNotExist(boolean flag) {
    mc.setCreateDatabaseIfNotExist(flag);
  }
  
  public void setDefaultFetchSize(int n) throws SQLException {
    mc.setDefaultFetchSize(n);
  }
  
  public void setDetectServerPreparedStmts(boolean property) {
    mc.setDetectServerPreparedStmts(property);
  }
  
  public void setDontTrackOpenResources(boolean flag) {
    mc.setDontTrackOpenResources(flag);
  }
  
  public void setDumpMetadataOnColumnNotFound(boolean flag) {
    mc.setDumpMetadataOnColumnNotFound(flag);
  }
  
  public void setDumpQueriesOnException(boolean flag) {
    mc.setDumpQueriesOnException(flag);
  }
  
  public void setDynamicCalendars(boolean flag) {
    mc.setDynamicCalendars(flag);
  }
  
  public void setElideSetAutoCommits(boolean flag) {
    mc.setElideSetAutoCommits(flag);
  }
  
  public void setEmptyStringsConvertToZero(boolean flag) {
    mc.setEmptyStringsConvertToZero(flag);
  }
  
  public void setEmulateLocators(boolean property) {
    mc.setEmulateLocators(property);
  }
  
  public void setEmulateUnsupportedPstmts(boolean flag) {
    mc.setEmulateUnsupportedPstmts(flag);
  }
  
  public void setEnablePacketDebug(boolean flag) {
    mc.setEnablePacketDebug(flag);
  }
  
  public void setEnableQueryTimeouts(boolean flag) {
    mc.setEnableQueryTimeouts(flag);
  }
  
  public void setEncoding(String property) {
    mc.setEncoding(property);
  }
  
  public void setExplainSlowQueries(boolean flag) {
    mc.setExplainSlowQueries(flag);
  }
  
  public void setFailOverReadOnly(boolean flag) {
    mc.setFailOverReadOnly(flag);
  }
  
  public void setFunctionsNeverReturnBlobs(boolean flag) {
    mc.setFunctionsNeverReturnBlobs(flag);
  }
  
  public void setGatherPerfMetrics(boolean flag) {
    mc.setGatherPerfMetrics(flag);
  }
  
  public void setGatherPerformanceMetrics(boolean flag) {
    mc.setGatherPerformanceMetrics(flag);
  }
  
  public void setGenerateSimpleParameterMetadata(boolean flag) {
    mc.setGenerateSimpleParameterMetadata(flag);
  }
  
  public void setHoldResultsOpenOverStatementClose(boolean flag) {
    mc.setHoldResultsOpenOverStatementClose(flag);
  }
  
  public void setIgnoreNonTxTables(boolean property) {
    mc.setIgnoreNonTxTables(property);
  }
  
  public void setIncludeInnodbStatusInDeadlockExceptions(boolean flag) {
    mc.setIncludeInnodbStatusInDeadlockExceptions(flag);
  }
  
  public void setInitialTimeout(int property) throws SQLException {
    mc.setInitialTimeout(property);
  }
  
  public void setInteractiveClient(boolean property) {
    mc.setInteractiveClient(property);
  }
  
  public void setIsInteractiveClient(boolean property) {
    mc.setIsInteractiveClient(property);
  }
  
  public void setJdbcCompliantTruncation(boolean flag) {
    mc.setJdbcCompliantTruncation(flag);
  }
  
  public void setJdbcCompliantTruncationForReads(boolean jdbcCompliantTruncationForReads) {
    mc.setJdbcCompliantTruncationForReads(jdbcCompliantTruncationForReads);
  }
  
  public void setLargeRowSizeThreshold(String value) throws SQLException {
    mc.setLargeRowSizeThreshold(value);
  }
  
  public void setLoadBalanceStrategy(String strategy) {
    mc.setLoadBalanceStrategy(strategy);
  }
  
  public void setServerAffinityOrder(String hostsList) {
    mc.setServerAffinityOrder(hostsList);
  }
  
  public void setLocalSocketAddress(String address) {
    mc.setLocalSocketAddress(address);
  }
  
  public void setLocatorFetchBufferSize(String value) throws SQLException {
    mc.setLocatorFetchBufferSize(value);
  }
  
  public void setLogSlowQueries(boolean flag) {
    mc.setLogSlowQueries(flag);
  }
  
  public void setLogXaCommands(boolean flag) {
    mc.setLogXaCommands(flag);
  }
  
  public void setLogger(String property) {
    mc.setLogger(property);
  }
  
  public void setLoggerClassName(String className) {
    mc.setLoggerClassName(className);
  }
  
  public void setMaintainTimeStats(boolean flag) {
    mc.setMaintainTimeStats(flag);
  }
  
  public void setMaxQuerySizeToLog(int sizeInBytes) throws SQLException {
    mc.setMaxQuerySizeToLog(sizeInBytes);
  }
  
  public void setMaxReconnects(int property) throws SQLException {
    mc.setMaxReconnects(property);
  }
  
  public void setMaxRows(int property) throws SQLException {
    mc.setMaxRows(property);
  }
  
  public void setMetadataCacheSize(int value) throws SQLException {
    mc.setMetadataCacheSize(value);
  }
  
  public void setNetTimeoutForStreamingResults(int value) throws SQLException {
    mc.setNetTimeoutForStreamingResults(value);
  }
  
  public void setNoAccessToProcedureBodies(boolean flag) {
    mc.setNoAccessToProcedureBodies(flag);
  }
  
  public void setNoDatetimeStringSync(boolean flag) {
    mc.setNoDatetimeStringSync(flag);
  }
  
  public void setNoTimezoneConversionForTimeType(boolean flag) {
    mc.setNoTimezoneConversionForTimeType(flag);
  }
  
  public void setNoTimezoneConversionForDateType(boolean flag) {
    mc.setNoTimezoneConversionForDateType(flag);
  }
  
  public void setCacheDefaultTimezone(boolean flag) {
    mc.setCacheDefaultTimezone(flag);
  }
  
  public void setNullCatalogMeansCurrent(boolean value) {
    mc.setNullCatalogMeansCurrent(value);
  }
  
  public void setNullNamePatternMatchesAll(boolean value) {
    mc.setNullNamePatternMatchesAll(value);
  }
  
  public void setOverrideSupportsIntegrityEnhancementFacility(boolean flag) {
    mc.setOverrideSupportsIntegrityEnhancementFacility(flag);
  }
  
  public void setPacketDebugBufferSize(int size) throws SQLException {
    mc.setPacketDebugBufferSize(size);
  }
  
  public void setPadCharsWithSpace(boolean flag) {
    mc.setPadCharsWithSpace(flag);
  }
  
  public void setParanoid(boolean property) {
    mc.setParanoid(property);
  }
  
  public void setPedantic(boolean property) {
    mc.setPedantic(property);
  }
  
  public void setPinGlobalTxToPhysicalConnection(boolean flag) {
    mc.setPinGlobalTxToPhysicalConnection(flag);
  }
  
  public void setPopulateInsertRowWithDefaultValues(boolean flag) {
    mc.setPopulateInsertRowWithDefaultValues(flag);
  }
  
  public void setPrepStmtCacheSize(int cacheSize) throws SQLException {
    mc.setPrepStmtCacheSize(cacheSize);
  }
  
  public void setPrepStmtCacheSqlLimit(int sqlLimit) throws SQLException {
    mc.setPrepStmtCacheSqlLimit(sqlLimit);
  }
  
  public void setPreparedStatementCacheSize(int cacheSize) throws SQLException {
    mc.setPreparedStatementCacheSize(cacheSize);
  }
  
  public void setPreparedStatementCacheSqlLimit(int cacheSqlLimit) throws SQLException {
    mc.setPreparedStatementCacheSqlLimit(cacheSqlLimit);
  }
  
  public void setProcessEscapeCodesForPrepStmts(boolean flag) {
    mc.setProcessEscapeCodesForPrepStmts(flag);
  }
  
  public void setProfileSQL(boolean flag) {
    mc.setProfileSQL(flag);
  }
  
  public void setProfileSql(boolean property) {
    mc.setProfileSql(property);
  }
  
  public void setPropertiesTransform(String value) {
    mc.setPropertiesTransform(value);
  }
  
  public void setQueriesBeforeRetryMaster(int property) throws SQLException {
    mc.setQueriesBeforeRetryMaster(property);
  }
  
  public void setReconnectAtTxEnd(boolean property) {
    mc.setReconnectAtTxEnd(property);
  }
  
  public void setRelaxAutoCommit(boolean property) {
    mc.setRelaxAutoCommit(property);
  }
  
  public void setReportMetricsIntervalMillis(int millis) throws SQLException {
    mc.setReportMetricsIntervalMillis(millis);
  }
  
  public void setRequireSSL(boolean property) {
    mc.setRequireSSL(property);
  }
  
  public void setResourceId(String resourceId) {
    mc.setResourceId(resourceId);
  }
  
  public void setResultSetSizeThreshold(int threshold) throws SQLException {
    mc.setResultSetSizeThreshold(threshold);
  }
  
  public void setRetainStatementAfterResultSetClose(boolean flag) {
    mc.setRetainStatementAfterResultSetClose(flag);
  }
  
  public void setRewriteBatchedStatements(boolean flag) {
    mc.setRewriteBatchedStatements(flag);
  }
  
  public void setRollbackOnPooledClose(boolean flag) {
    mc.setRollbackOnPooledClose(flag);
  }
  
  public void setRoundRobinLoadBalance(boolean flag) {
    mc.setRoundRobinLoadBalance(flag);
  }
  
  public void setRunningCTS13(boolean flag) {
    mc.setRunningCTS13(flag);
  }
  
  public void setSecondsBeforeRetryMaster(int property) throws SQLException {
    mc.setSecondsBeforeRetryMaster(property);
  }
  
  public void setServerTimezone(String property) {
    mc.setServerTimezone(property);
  }
  
  public void setSessionVariables(String variables) {
    mc.setSessionVariables(variables);
  }
  
  public void setSlowQueryThresholdMillis(int millis) throws SQLException {
    mc.setSlowQueryThresholdMillis(millis);
  }
  
  public void setSlowQueryThresholdNanos(long nanos) throws SQLException {
    mc.setSlowQueryThresholdNanos(nanos);
  }
  
  public void setSocketFactory(String name) {
    mc.setSocketFactory(name);
  }
  
  public void setSocketFactoryClassName(String property) {
    mc.setSocketFactoryClassName(property);
  }
  
  public void setSocketTimeout(int property) throws SQLException {
    mc.setSocketTimeout(property);
  }
  
  public void setStatementInterceptors(String value) {
    mc.setStatementInterceptors(value);
  }
  
  public void setStrictFloatingPoint(boolean property) {
    mc.setStrictFloatingPoint(property);
  }
  
  public void setStrictUpdates(boolean property) {
    mc.setStrictUpdates(property);
  }
  
  public void setTcpKeepAlive(boolean flag) {
    mc.setTcpKeepAlive(flag);
  }
  
  public void setTcpNoDelay(boolean flag) {
    mc.setTcpNoDelay(flag);
  }
  
  public void setTcpRcvBuf(int bufSize) throws SQLException {
    mc.setTcpRcvBuf(bufSize);
  }
  
  public void setTcpSndBuf(int bufSize) throws SQLException {
    mc.setTcpSndBuf(bufSize);
  }
  
  public void setTcpTrafficClass(int classFlags) throws SQLException {
    mc.setTcpTrafficClass(classFlags);
  }
  
  public void setTinyInt1isBit(boolean flag) {
    mc.setTinyInt1isBit(flag);
  }
  
  public void setTraceProtocol(boolean flag) {
    mc.setTraceProtocol(flag);
  }
  
  public void setTransformedBitIsBoolean(boolean flag) {
    mc.setTransformedBitIsBoolean(flag);
  }
  
  public void setTreatUtilDateAsTimestamp(boolean flag) {
    mc.setTreatUtilDateAsTimestamp(flag);
  }
  
  public void setTrustCertificateKeyStorePassword(String value) {
    mc.setTrustCertificateKeyStorePassword(value);
  }
  
  public void setTrustCertificateKeyStoreType(String value) {
    mc.setTrustCertificateKeyStoreType(value);
  }
  
  public void setTrustCertificateKeyStoreUrl(String value) {
    mc.setTrustCertificateKeyStoreUrl(value);
  }
  
  public void setUltraDevHack(boolean flag) {
    mc.setUltraDevHack(flag);
  }
  
  public void setUseBlobToStoreUTF8OutsideBMP(boolean flag) {
    mc.setUseBlobToStoreUTF8OutsideBMP(flag);
  }
  
  public void setUseCompression(boolean property) {
    mc.setUseCompression(property);
  }
  
  public void setUseConfigs(String configs) {
    mc.setUseConfigs(configs);
  }
  
  public void setUseCursorFetch(boolean flag) {
    mc.setUseCursorFetch(flag);
  }
  
  public void setUseDirectRowUnpack(boolean flag) {
    mc.setUseDirectRowUnpack(flag);
  }
  
  public void setUseDynamicCharsetInfo(boolean flag) {
    mc.setUseDynamicCharsetInfo(flag);
  }
  
  public void setUseFastDateParsing(boolean flag) {
    mc.setUseFastDateParsing(flag);
  }
  
  public void setUseFastIntParsing(boolean flag) {
    mc.setUseFastIntParsing(flag);
  }
  
  public void setUseGmtMillisForDatetimes(boolean flag) {
    mc.setUseGmtMillisForDatetimes(flag);
  }
  
  public void setUseHostsInPrivileges(boolean property) {
    mc.setUseHostsInPrivileges(property);
  }
  
  public void setUseInformationSchema(boolean flag) {
    mc.setUseInformationSchema(flag);
  }
  
  public void setUseJDBCCompliantTimezoneShift(boolean flag) {
    mc.setUseJDBCCompliantTimezoneShift(flag);
  }
  
  public void setUseJvmCharsetConverters(boolean flag) {
    mc.setUseJvmCharsetConverters(flag);
  }
  
  public void setUseLocalSessionState(boolean flag) {
    mc.setUseLocalSessionState(flag);
  }
  
  public void setUseNanosForElapsedTime(boolean flag) {
    mc.setUseNanosForElapsedTime(flag);
  }
  
  public void setUseOldAliasMetadataBehavior(boolean flag) {
    mc.setUseOldAliasMetadataBehavior(flag);
  }
  
  public void setUseOldUTF8Behavior(boolean flag) {
    mc.setUseOldUTF8Behavior(flag);
  }
  
  public void setUseOnlyServerErrorMessages(boolean flag) {
    mc.setUseOnlyServerErrorMessages(flag);
  }
  
  public void setUseReadAheadInput(boolean flag) {
    mc.setUseReadAheadInput(flag);
  }
  
  public void setUseSSL(boolean property) {
    mc.setUseSSL(property);
  }
  
  public void setUseSSPSCompatibleTimezoneShift(boolean flag) {
    mc.setUseSSPSCompatibleTimezoneShift(flag);
  }
  
  public void setUseServerPrepStmts(boolean flag) {
    mc.setUseServerPrepStmts(flag);
  }
  
  public void setUseServerPreparedStmts(boolean flag) {
    mc.setUseServerPreparedStmts(flag);
  }
  
  public void setUseSqlStateCodes(boolean flag) {
    mc.setUseSqlStateCodes(flag);
  }
  
  public void setUseStreamLengthsInPrepStmts(boolean property) {
    mc.setUseStreamLengthsInPrepStmts(property);
  }
  
  public void setUseTimezone(boolean property) {
    mc.setUseTimezone(property);
  }
  
  public void setUseUltraDevWorkAround(boolean property) {
    mc.setUseUltraDevWorkAround(property);
  }
  
  public void setUseUnbufferedInput(boolean flag) {
    mc.setUseUnbufferedInput(flag);
  }
  
  public void setUseUnicode(boolean flag) {
    mc.setUseUnicode(flag);
  }
  
  public void setUseUsageAdvisor(boolean useUsageAdvisorFlag) {
    mc.setUseUsageAdvisor(useUsageAdvisorFlag);
  }
  
  public void setUtf8OutsideBmpExcludedColumnNamePattern(String regexPattern) {
    mc.setUtf8OutsideBmpExcludedColumnNamePattern(regexPattern);
  }
  
  public void setUtf8OutsideBmpIncludedColumnNamePattern(String regexPattern) {
    mc.setUtf8OutsideBmpIncludedColumnNamePattern(regexPattern);
  }
  
  public void setYearIsDateType(boolean flag) {
    mc.setYearIsDateType(flag);
  }
  
  public void setZeroDateTimeBehavior(String behavior) {
    mc.setZeroDateTimeBehavior(behavior);
  }
  
  public boolean useUnbufferedInput() {
    return mc.useUnbufferedInput();
  }
  
  public void initializeExtension(Extension ex) throws SQLException {
    mc.initializeExtension(ex);
  }
  
  public String getProfilerEventHandler() {
    return mc.getProfilerEventHandler();
  }
  
  public void setProfilerEventHandler(String handler) {
    mc.setProfilerEventHandler(handler);
  }
  
  public boolean getVerifyServerCertificate() {
    return mc.getVerifyServerCertificate();
  }
  
  public void setVerifyServerCertificate(boolean flag) {
    mc.setVerifyServerCertificate(flag);
  }
  
  public boolean getUseLegacyDatetimeCode() {
    return mc.getUseLegacyDatetimeCode();
  }
  
  public void setUseLegacyDatetimeCode(boolean flag) {
    mc.setUseLegacyDatetimeCode(flag);
  }
  
  public boolean getSendFractionalSeconds() {
    return mc.getSendFractionalSeconds();
  }
  
  public void setSendFractionalSeconds(boolean flag) {
    mc.setSendFractionalSeconds(flag);
  }
  
  public int getSelfDestructOnPingMaxOperations() {
    return mc.getSelfDestructOnPingMaxOperations();
  }
  
  public int getSelfDestructOnPingSecondsLifetime() {
    return mc.getSelfDestructOnPingSecondsLifetime();
  }
  
  public void setSelfDestructOnPingMaxOperations(int maxOperations) throws SQLException {
    mc.setSelfDestructOnPingMaxOperations(maxOperations);
  }
  
  public void setSelfDestructOnPingSecondsLifetime(int seconds) throws SQLException {
    mc.setSelfDestructOnPingSecondsLifetime(seconds);
  }
  
  public boolean getUseColumnNamesInFindColumn() {
    return mc.getUseColumnNamesInFindColumn();
  }
  
  public void setUseColumnNamesInFindColumn(boolean flag) {
    mc.setUseColumnNamesInFindColumn(flag);
  }
  
  public boolean getUseLocalTransactionState() {
    return mc.getUseLocalTransactionState();
  }
  
  public void setUseLocalTransactionState(boolean flag) {
    mc.setUseLocalTransactionState(flag);
  }
  
  public boolean getCompensateOnDuplicateKeyUpdateCounts() {
    return mc.getCompensateOnDuplicateKeyUpdateCounts();
  }
  
  public void setCompensateOnDuplicateKeyUpdateCounts(boolean flag) {
    mc.setCompensateOnDuplicateKeyUpdateCounts(flag);
  }
  
  public boolean getUseAffectedRows() {
    return mc.getUseAffectedRows();
  }
  
  public void setUseAffectedRows(boolean flag) {
    mc.setUseAffectedRows(flag);
  }
  
  public String getPasswordCharacterEncoding() {
    return mc.getPasswordCharacterEncoding();
  }
  
  public void setPasswordCharacterEncoding(String characterSet) {
    mc.setPasswordCharacterEncoding(characterSet);
  }
  
  public int getAutoIncrementIncrement() {
    return mc.getAutoIncrementIncrement();
  }
  
  public int getLoadBalanceBlacklistTimeout() {
    return mc.getLoadBalanceBlacklistTimeout();
  }
  
  public void setLoadBalanceBlacklistTimeout(int loadBalanceBlacklistTimeout) throws SQLException {
    mc.setLoadBalanceBlacklistTimeout(loadBalanceBlacklistTimeout);
  }
  
  public int getLoadBalancePingTimeout() {
    return mc.getLoadBalancePingTimeout();
  }
  
  public void setLoadBalancePingTimeout(int loadBalancePingTimeout) throws SQLException {
    mc.setLoadBalancePingTimeout(loadBalancePingTimeout);
  }
  
  public boolean getLoadBalanceValidateConnectionOnSwapServer() {
    return mc.getLoadBalanceValidateConnectionOnSwapServer();
  }
  
  public void setLoadBalanceValidateConnectionOnSwapServer(boolean loadBalanceValidateConnectionOnSwapServer) {
    mc.setLoadBalanceValidateConnectionOnSwapServer(loadBalanceValidateConnectionOnSwapServer);
  }
  
  public void setRetriesAllDown(int retriesAllDown) throws SQLException {
    mc.setRetriesAllDown(retriesAllDown);
  }
  
  public int getRetriesAllDown() {
    return mc.getRetriesAllDown();
  }
  
  public ExceptionInterceptor getExceptionInterceptor() {
    return pooledConnection.getExceptionInterceptor();
  }
  
  public String getExceptionInterceptors() {
    return mc.getExceptionInterceptors();
  }
  
  public void setExceptionInterceptors(String exceptionInterceptors) {
    mc.setExceptionInterceptors(exceptionInterceptors);
  }
  
  public boolean getQueryTimeoutKillsConnection() {
    return mc.getQueryTimeoutKillsConnection();
  }
  
  public void setQueryTimeoutKillsConnection(boolean queryTimeoutKillsConnection) {
    mc.setQueryTimeoutKillsConnection(queryTimeoutKillsConnection);
  }
  
  public boolean hasSameProperties(Connection c) {
    return mc.hasSameProperties(c);
  }
  
  public Properties getProperties() {
    return mc.getProperties();
  }
  
  public String getHost() {
    return mc.getHost();
  }
  
  public void setProxy(MySQLConnection conn) {
    mc.setProxy(conn);
  }
  
  public boolean getRetainStatementAfterResultSetClose() {
    return mc.getRetainStatementAfterResultSetClose();
  }
  
  public int getMaxAllowedPacket() {
    return mc.getMaxAllowedPacket();
  }
  
  public String getLoadBalanceConnectionGroup() {
    return mc.getLoadBalanceConnectionGroup();
  }
  
  public boolean getLoadBalanceEnableJMX() {
    return mc.getLoadBalanceEnableJMX();
  }
  
  public String getLoadBalanceExceptionChecker() {
    return mc.getLoadBalanceExceptionChecker();
  }
  
  public String getLoadBalanceSQLExceptionSubclassFailover() {
    return mc.getLoadBalanceSQLExceptionSubclassFailover();
  }
  
  public String getLoadBalanceSQLStateFailover() {
    return mc.getLoadBalanceSQLStateFailover();
  }
  
  public void setLoadBalanceConnectionGroup(String loadBalanceConnectionGroup) {
    mc.setLoadBalanceConnectionGroup(loadBalanceConnectionGroup);
  }
  
  public void setLoadBalanceEnableJMX(boolean loadBalanceEnableJMX)
  {
    mc.setLoadBalanceEnableJMX(loadBalanceEnableJMX);
  }
  
  public void setLoadBalanceExceptionChecker(String loadBalanceExceptionChecker)
  {
    mc.setLoadBalanceExceptionChecker(loadBalanceExceptionChecker);
  }
  
  public void setLoadBalanceSQLExceptionSubclassFailover(String loadBalanceSQLExceptionSubclassFailover)
  {
    mc.setLoadBalanceSQLExceptionSubclassFailover(loadBalanceSQLExceptionSubclassFailover);
  }
  
  public void setLoadBalanceSQLStateFailover(String loadBalanceSQLStateFailover)
  {
    mc.setLoadBalanceSQLStateFailover(loadBalanceSQLStateFailover);
  }
  
  public String getLoadBalanceAutoCommitStatementRegex()
  {
    return mc.getLoadBalanceAutoCommitStatementRegex();
  }
  
  public int getLoadBalanceAutoCommitStatementThreshold() {
    return mc.getLoadBalanceAutoCommitStatementThreshold();
  }
  
  public void setLoadBalanceAutoCommitStatementRegex(String loadBalanceAutoCommitStatementRegex) {
    mc.setLoadBalanceAutoCommitStatementRegex(loadBalanceAutoCommitStatementRegex);
  }
  
  public void setLoadBalanceAutoCommitStatementThreshold(int loadBalanceAutoCommitStatementThreshold) throws SQLException
  {
    mc.setLoadBalanceAutoCommitStatementThreshold(loadBalanceAutoCommitStatementThreshold);
  }
  
  public void setLoadBalanceHostRemovalGracePeriod(int loadBalanceHostRemovalGracePeriod) throws SQLException
  {
    mc.setLoadBalanceHostRemovalGracePeriod(loadBalanceHostRemovalGracePeriod);
  }
  
  public int getLoadBalanceHostRemovalGracePeriod() {
    return mc.getLoadBalanceHostRemovalGracePeriod();
  }
  
  public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
    checkClosed();
    try
    {
      mc.setTypeMap(map);
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
  }
  
  public boolean getIncludeThreadDumpInDeadlockExceptions() {
    return mc.getIncludeThreadDumpInDeadlockExceptions();
  }
  
  public void setIncludeThreadDumpInDeadlockExceptions(boolean flag) {
    mc.setIncludeThreadDumpInDeadlockExceptions(flag);
  }
  
  public boolean getIncludeThreadNamesAsStatementComment()
  {
    return mc.getIncludeThreadNamesAsStatementComment();
  }
  
  public void setIncludeThreadNamesAsStatementComment(boolean flag) {
    mc.setIncludeThreadNamesAsStatementComment(flag);
  }
  
  public boolean isServerLocal() throws SQLException {
    return mc.isServerLocal();
  }
  
  public void setAuthenticationPlugins(String authenticationPlugins) {
    mc.setAuthenticationPlugins(authenticationPlugins);
  }
  
  public String getAuthenticationPlugins() {
    return mc.getAuthenticationPlugins();
  }
  
  public void setDisabledAuthenticationPlugins(String disabledAuthenticationPlugins) {
    mc.setDisabledAuthenticationPlugins(disabledAuthenticationPlugins);
  }
  
  public String getDisabledAuthenticationPlugins() {
    return mc.getDisabledAuthenticationPlugins();
  }
  
  public void setDefaultAuthenticationPlugin(String defaultAuthenticationPlugin) {
    mc.setDefaultAuthenticationPlugin(defaultAuthenticationPlugin);
  }
  
  public String getDefaultAuthenticationPlugin()
  {
    return mc.getDefaultAuthenticationPlugin();
  }
  
  public void setParseInfoCacheFactory(String factoryClassname) {
    mc.setParseInfoCacheFactory(factoryClassname);
  }
  
  public String getParseInfoCacheFactory() {
    return mc.getParseInfoCacheFactory();
  }
  
  public void setSchema(String schema) throws SQLException {
    mc.setSchema(schema);
  }
  
  public String getSchema() throws SQLException {
    return mc.getSchema();
  }
  
  public void abort(Executor executor) throws SQLException {
    mc.abort(executor);
  }
  
  public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
    mc.setNetworkTimeout(executor, milliseconds);
  }
  
  public int getNetworkTimeout() throws SQLException {
    return mc.getNetworkTimeout();
  }
  
  public void setServerConfigCacheFactory(String factoryClassname) {
    mc.setServerConfigCacheFactory(factoryClassname);
  }
  
  public String getServerConfigCacheFactory() {
    return mc.getServerConfigCacheFactory();
  }
  
  public void setDisconnectOnExpiredPasswords(boolean disconnectOnExpiredPasswords) {
    mc.setDisconnectOnExpiredPasswords(disconnectOnExpiredPasswords);
  }
  
  public boolean getDisconnectOnExpiredPasswords() {
    return mc.getDisconnectOnExpiredPasswords();
  }
  
  public void setGetProceduresReturnsFunctions(boolean getProcedureReturnsFunctions) {
    mc.setGetProceduresReturnsFunctions(getProcedureReturnsFunctions);
  }
  
  public boolean getGetProceduresReturnsFunctions() {
    return mc.getGetProceduresReturnsFunctions();
  }
  
  public void abortInternal() throws SQLException {
    mc.abortInternal();
  }
  
  public Object getConnectionMutex() {
    return mc.getConnectionMutex();
  }
  
  public boolean getAllowMasterDownConnections() {
    return mc.getAllowMasterDownConnections();
  }
  
  public void setAllowMasterDownConnections(boolean connectIfMasterDown) {
    mc.setAllowMasterDownConnections(connectIfMasterDown);
  }
  
  public boolean getAllowSlaveDownConnections() {
    return mc.getAllowSlaveDownConnections();
  }
  
  public void setAllowSlaveDownConnections(boolean connectIfSlaveDown) {
    mc.setAllowSlaveDownConnections(connectIfSlaveDown);
  }
  
  public boolean getReadFromMasterWhenNoSlaves() {
    return mc.getReadFromMasterWhenNoSlaves();
  }
  
  public void setReadFromMasterWhenNoSlaves(boolean useMasterIfSlavesDown) {
    mc.setReadFromMasterWhenNoSlaves(useMasterIfSlavesDown);
  }
  
  public boolean getReplicationEnableJMX() {
    return mc.getReplicationEnableJMX();
  }
  
  public void setReplicationEnableJMX(boolean replicationEnableJMX) {
    mc.setReplicationEnableJMX(replicationEnableJMX);
  }
  
  public String getConnectionAttributes() throws SQLException
  {
    return mc.getConnectionAttributes();
  }
  
  public void setDetectCustomCollations(boolean detectCustomCollations) {
    mc.setDetectCustomCollations(detectCustomCollations);
  }
  
  public boolean getDetectCustomCollations() {
    return mc.getDetectCustomCollations();
  }
  
  public int getSessionMaxRows() {
    return mc.getSessionMaxRows();
  }
  
  public void setSessionMaxRows(int max) throws SQLException {
    mc.setSessionMaxRows(max);
  }
  
  public String getServerRSAPublicKeyFile() {
    return mc.getServerRSAPublicKeyFile();
  }
  
  public void setServerRSAPublicKeyFile(String serverRSAPublicKeyFile) throws SQLException {
    mc.setServerRSAPublicKeyFile(serverRSAPublicKeyFile);
  }
  
  public boolean getAllowPublicKeyRetrieval() {
    return mc.getAllowPublicKeyRetrieval();
  }
  
  public void setAllowPublicKeyRetrieval(boolean allowPublicKeyRetrieval) throws SQLException {
    mc.setAllowPublicKeyRetrieval(allowPublicKeyRetrieval);
  }
  
  public void setDontCheckOnDuplicateKeyUpdateInSQL(boolean dontCheckOnDuplicateKeyUpdateInSQL) {
    mc.setDontCheckOnDuplicateKeyUpdateInSQL(dontCheckOnDuplicateKeyUpdateInSQL);
  }
  
  public boolean getDontCheckOnDuplicateKeyUpdateInSQL() {
    return mc.getDontCheckOnDuplicateKeyUpdateInSQL();
  }
  
  public void setSocksProxyHost(String socksProxyHost) {
    mc.setSocksProxyHost(socksProxyHost);
  }
  
  public String getSocksProxyHost() {
    return mc.getSocksProxyHost();
  }
  
  public void setSocksProxyPort(int socksProxyPort) throws SQLException {
    mc.setSocksProxyPort(socksProxyPort);
  }
  
  public int getSocksProxyPort() {
    return mc.getSocksProxyPort();
  }
  
  public boolean getReadOnlyPropagatesToServer() {
    return mc.getReadOnlyPropagatesToServer();
  }
  
  public void setReadOnlyPropagatesToServer(boolean flag) {
    mc.setReadOnlyPropagatesToServer(flag);
  }
  
  public String getEnabledSSLCipherSuites() {
    return mc.getEnabledSSLCipherSuites();
  }
  
  public void setEnabledSSLCipherSuites(String cipherSuites) {
    mc.setEnabledSSLCipherSuites(cipherSuites);
  }
  
  public String getEnabledTLSProtocols() {
    return mc.getEnabledTLSProtocols();
  }
  
  public void setEnabledTLSProtocols(String protocols) {
    mc.setEnabledTLSProtocols(protocols);
  }
  
  public boolean getEnableEscapeProcessing() {
    return mc.getEnableEscapeProcessing();
  }
  
  public void setEnableEscapeProcessing(boolean flag) {
    mc.setEnableEscapeProcessing(flag);
  }
  
  public boolean isUseSSLExplicit() {
    return mc.isUseSSLExplicit();
  }
}
