package com.mysql.jdbc;

import com.mysql.jdbc.exceptions.MySQLStatementCancelledException;
import com.mysql.jdbc.exceptions.MySQLTimeoutException;
import com.mysql.jdbc.log.LogUtils;
import com.mysql.jdbc.profiler.ProfilerEvent;
import com.mysql.jdbc.profiler.ProfilerEventHandler;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.sql.BatchUpdateException;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;































public class StatementImpl
  implements Statement
{
  protected static final String PING_MARKER = "/* ping */";
  protected static final String[] ON_DUPLICATE_KEY_UPDATE_CLAUSE = { "ON", "DUPLICATE", "KEY", "UPDATE" };
  



  class CancelTask
    extends TimerTask
  {
    SQLException caughtWhileCancelling = null;
    StatementImpl toCancel;
    Properties origConnProps = null;
    String origConnURL = "";
    long origConnId = 0L;
    
    CancelTask(StatementImpl cancellee) throws SQLException {
      toCancel = cancellee;
      origConnProps = new Properties();
      
      Properties props = connection.getProperties();
      
      Enumeration<?> keys = props.propertyNames();
      
      while (keys.hasMoreElements()) {
        String key = keys.nextElement().toString();
        origConnProps.setProperty(key, props.getProperty(key));
      }
      
      origConnURL = connection.getURL();
      origConnId = connection.getId();
    }
    

    public void run()
    {
      Thread cancelThread = new Thread()
      {

        public void run()
        {
          Connection cancelConn = null;
          java.sql.Statement cancelStmt = null;
          try
          {
            MySQLConnection physicalConn = (MySQLConnection)physicalConnection.get();
            if (physicalConn != null) {
              if (physicalConn.getQueryTimeoutKillsConnection()) {
                toCancel.wasCancelled = true;
                toCancel.wasCancelledByTimeout = true;
                physicalConn.realClose(false, false, true, new MySQLStatementCancelledException(Messages.getString("Statement.ConnectionKilledDueToTimeout")));
              }
              else {
                synchronized (cancelTimeoutMutex) {
                  if (origConnURL.equals(physicalConn.getURL()))
                  {
                    cancelConn = physicalConn.duplicate();
                    cancelStmt = cancelConn.createStatement();
                    cancelStmt.execute("KILL QUERY " + physicalConn.getId());
                  } else {
                    try {
                      cancelConn = (Connection)DriverManager.getConnection(origConnURL, origConnProps);
                      cancelStmt = cancelConn.createStatement();
                      cancelStmt.execute("KILL QUERY " + origConnId);
                    }
                    catch (NullPointerException npe) {}
                  }
                  
                  toCancel.wasCancelled = true;
                  toCancel.wasCancelledByTimeout = true;
                }
              }
            }
          } catch (SQLException sqlEx) {
            caughtWhileCancelling = sqlEx;

          }
          catch (NullPointerException npe) {}finally
          {

            if (cancelStmt != null) {
              try {
                cancelStmt.close();
              } catch (SQLException sqlEx) {
                throw new RuntimeException(sqlEx.toString());
              }
            }
            
            if (cancelConn != null) {
              try {
                cancelConn.close();
              } catch (SQLException sqlEx) {
                throw new RuntimeException(sqlEx.toString());
              }
            }
            
            toCancel = null;
            origConnProps = null;
            origConnURL = null;
          }
          
        }
      };
      cancelThread.start();
    }
  }
  





  protected Object cancelTimeoutMutex = new Object();
  

  static int statementCounter = 1;
  
  public static final byte USES_VARIABLES_FALSE = 0;
  
  public static final byte USES_VARIABLES_TRUE = 1;
  
  public static final byte USES_VARIABLES_UNKNOWN = -1;
  
  protected boolean wasCancelled = false;
  protected boolean wasCancelledByTimeout = false;
  

  protected List<Object> batchedArgs;
  

  protected SingleByteCharsetConverter charConverter = null;
  

  protected String charEncoding = null;
  

  protected volatile MySQLConnection connection = null;
  

  protected Reference<MySQLConnection> physicalConnection = null;
  
  protected long connectionId = 0L;
  

  protected String currentCatalog = null;
  

  protected boolean doEscapeProcessing = true;
  

  protected ProfilerEventHandler eventSink = null;
  

  private int fetchSize = 0;
  

  protected boolean isClosed = false;
  

  protected long lastInsertId = -1L;
  

  protected int maxFieldSize = MysqlIO.getMaxBuf();
  




  protected int maxRows = -1;
  

  protected Set<ResultSetInternalMethods> openResults = new HashSet();
  

  protected boolean pedantic = false;
  



  protected String pointOfOrigin;
  


  protected boolean profileSQL = false;
  

  protected ResultSetInternalMethods results = null;
  
  protected ResultSetInternalMethods generatedKeysResults = null;
  

  protected int resultSetConcurrency = 0;
  

  protected int resultSetType = 0;
  

  protected int statementId;
  

  protected int timeoutInMillis = 0;
  

  protected long updateCount = -1L;
  

  protected boolean useUsageAdvisor = false;
  

  protected SQLWarning warningChain = null;
  

  protected boolean clearWarningsCalled = false;
  




  protected boolean holdResultsOpenOverClose = false;
  
  protected ArrayList<ResultSetRow> batchedGeneratedKeys = null;
  
  protected boolean retrieveGeneratedKeys = false;
  
  protected boolean continueBatchOnError = false;
  
  protected PingTarget pingTarget = null;
  

  protected boolean useLegacyDatetimeCode;
  
  protected boolean sendFractionalSeconds;
  
  private ExceptionInterceptor exceptionInterceptor;
  
  protected boolean lastQueryIsOnDupKeyUpdate = false;
  

  protected final AtomicBoolean statementExecuting = new AtomicBoolean(false);
  

  private boolean isImplicitlyClosingResults = false;
  









  public StatementImpl(MySQLConnection c, String catalog)
    throws SQLException
  {
    if ((c == null) || (c.isClosed())) {
      throw SQLError.createSQLException(Messages.getString("Statement.0"), "08003", null);
    }
    
    connection = c;
    connectionId = connection.getId();
    exceptionInterceptor = connection.getExceptionInterceptor();
    
    currentCatalog = catalog;
    pedantic = connection.getPedantic();
    continueBatchOnError = connection.getContinueBatchOnError();
    useLegacyDatetimeCode = connection.getUseLegacyDatetimeCode();
    sendFractionalSeconds = connection.getSendFractionalSeconds();
    doEscapeProcessing = connection.getEnableEscapeProcessing();
    
    if (!connection.getDontTrackOpenResources()) {
      connection.registerStatement(this);
    }
    
    maxFieldSize = connection.getMaxAllowedPacket();
    
    int defaultFetchSize = connection.getDefaultFetchSize();
    if (defaultFetchSize != 0) {
      setFetchSize(defaultFetchSize);
    }
    
    if (connection.getUseUnicode()) {
      charEncoding = connection.getEncoding();
      charConverter = connection.getCharsetConverter(charEncoding);
    }
    
    boolean profiling = (connection.getProfileSql()) || (connection.getUseUsageAdvisor()) || (connection.getLogSlowQueries());
    if ((connection.getAutoGenerateTestcaseScript()) || (profiling)) {
      statementId = (statementCounter++);
    }
    if (profiling) {
      pointOfOrigin = LogUtils.findCallingClassAndMethod(new Throwable());
      profileSQL = connection.getProfileSql();
      useUsageAdvisor = connection.getUseUsageAdvisor();
      eventSink = ProfilerEventHandlerFactory.getInstance(connection);
    }
    
    int maxRowsConn = connection.getMaxRows();
    if (maxRowsConn != -1) {
      setMaxRows(maxRowsConn);
    }
    
    holdResultsOpenOverClose = connection.getHoldResultsOpenOverStatementClose();
    
    version5013OrNewer = connection.versionMeetsMinimum(5, 0, 13);
  }
  



  public void addBatch(String sql)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (batchedArgs == null) {
        batchedArgs = new ArrayList();
      }
      
      if (sql != null) {
        batchedArgs.add(sql);
      }
    }
  }
  







  public List<Object> getBatchedArgs()
  {
    return batchedArgs == null ? null : Collections.unmodifiableList(batchedArgs);
  }
  



  public void cancel()
    throws SQLException
  {
    if (!statementExecuting.get()) {
      return;
    }
    
    if ((!isClosed) && (connection != null) && (connection.versionMeetsMinimum(5, 0, 0))) {
      Connection cancelConn = null;
      java.sql.Statement cancelStmt = null;
      try
      {
        cancelConn = connection.duplicate();
        cancelStmt = cancelConn.createStatement();
        cancelStmt.execute("KILL QUERY " + connection.getIO().getThreadId());
        wasCancelled = true;
      } finally {
        if (cancelStmt != null) {
          cancelStmt.close();
        }
        
        if (cancelConn != null) {
          cancelConn.close();
        }
      }
    }
  }
  







  protected MySQLConnection checkClosed()
    throws SQLException
  {
    MySQLConnection c = connection;
    
    if (c == null) {
      throw SQLError.createSQLException(Messages.getString("Statement.49"), "S1009", getExceptionInterceptor());
    }
    
    return c;
  }
  










  protected void checkForDml(String sql, char firstStatementChar)
    throws SQLException
  {
    if ((firstStatementChar == 'I') || (firstStatementChar == 'U') || (firstStatementChar == 'D') || (firstStatementChar == 'A') || (firstStatementChar == 'C') || (firstStatementChar == 'T') || (firstStatementChar == 'R'))
    {
      String noCommentSql = StringUtils.stripComments(sql, "'\"", "'\"", true, false, true, true);
      
      if ((StringUtils.startsWithIgnoreCaseAndWs(noCommentSql, "INSERT")) || (StringUtils.startsWithIgnoreCaseAndWs(noCommentSql, "UPDATE")) || (StringUtils.startsWithIgnoreCaseAndWs(noCommentSql, "DELETE")) || (StringUtils.startsWithIgnoreCaseAndWs(noCommentSql, "DROP")) || (StringUtils.startsWithIgnoreCaseAndWs(noCommentSql, "CREATE")) || (StringUtils.startsWithIgnoreCaseAndWs(noCommentSql, "ALTER")) || (StringUtils.startsWithIgnoreCaseAndWs(noCommentSql, "TRUNCATE")) || (StringUtils.startsWithIgnoreCaseAndWs(noCommentSql, "RENAME")))
      {


        throw SQLError.createSQLException(Messages.getString("Statement.57"), "S1009", getExceptionInterceptor());
      }
    }
  }
  







  protected void checkNullOrEmptyQuery(String sql)
    throws SQLException
  {
    if (sql == null) {
      throw SQLError.createSQLException(Messages.getString("Statement.59"), "S1009", getExceptionInterceptor());
    }
    
    if (sql.length() == 0) {
      throw SQLError.createSQLException(Messages.getString("Statement.61"), "S1009", getExceptionInterceptor());
    }
  }
  






  public void clearBatch()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (batchedArgs != null) {
        batchedArgs.clear();
      }
    }
  }
  





  public void clearWarnings()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      clearWarningsCalled = true;
      warningChain = null;
    }
  }
  












  public void close()
    throws SQLException
  {
    realClose(true, true);
  }
  

  protected void closeAllOpenResults()
    throws SQLException
  {
    MySQLConnection locallyScopedConn = connection;
    
    if (locallyScopedConn == null) {
      return;
    }
    
    synchronized (locallyScopedConn.getConnectionMutex()) {
      if (openResults != null) {
        for (ResultSetInternalMethods element : openResults) {
          try {
            element.realClose(false);
          } catch (SQLException sqlEx) {
            AssertionFailedException.shouldNotHappen(sqlEx);
          }
        }
        
        openResults.clear();
      }
    }
  }
  

  protected void implicitlyCloseAllOpenResults()
    throws SQLException
  {
    isImplicitlyClosingResults = true;
    try {
      if ((!connection.getHoldResultsOpenOverStatementClose()) && (!connection.getDontTrackOpenResources()) && (!holdResultsOpenOverClose)) {
        if (results != null) {
          results.realClose(false);
        }
        if (generatedKeysResults != null) {
          generatedKeysResults.realClose(false);
        }
        closeAllOpenResults();
      }
    } finally {
      isImplicitlyClosingResults = false;
    }
  }
  
  public void removeOpenResultSet(ResultSetInternalMethods rs) {
    try {
      synchronized (checkClosed().getConnectionMutex()) {
        if (openResults != null) {
          openResults.remove(rs);
        }
        
        boolean hasMoreResults = rs.getNextResultSet() != null;
        

        if ((results == rs) && (!hasMoreResults)) {
          results = null;
        }
        if (generatedKeysResults == rs) {
          generatedKeysResults = null;
        }
        



        if ((!isImplicitlyClosingResults) && (!hasMoreResults)) {
          checkAndPerformCloseOnCompletionAction();
        }
      }
    }
    catch (SQLException e) {}
  }
  
  public int getOpenResultSetCount()
  {
    try {
      synchronized (checkClosed().getConnectionMutex()) {
        if (openResults != null) {
          return openResults.size();
        }
        
        return 0;
      }
      


      return 0;
    }
    catch (SQLException e) {}
  }
  

  private void checkAndPerformCloseOnCompletionAction()
  {
    try
    {
      synchronized (checkClosed().getConnectionMutex()) {
        if ((isCloseOnCompletion()) && (!connection.getDontTrackOpenResources()) && (getOpenResultSetCount() == 0) && ((results == null) || (!results.reallyResult()) || (results.isClosed())) && ((generatedKeysResults == null) || (!generatedKeysResults.reallyResult()) || (generatedKeysResults.isClosed())))
        {

          realClose(false, false);
        }
      }
    }
    catch (SQLException e) {}
  }
  

  private ResultSetInternalMethods createResultSetUsingServerFetch(String sql)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      java.sql.PreparedStatement pStmt = connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
      
      pStmt.setFetchSize(fetchSize);
      
      if (maxRows > -1) {
        pStmt.setMaxRows(maxRows);
      }
      
      statementBegins();
      
      pStmt.execute();
      



      ResultSetInternalMethods rs = ((StatementImpl)pStmt).getResultSetInternal();
      
      rs.setStatementUsedForFetchingRows((PreparedStatement)pStmt);
      
      results = rs;
      
      return rs;
    }
  }
  






  protected boolean createStreamingResultSet()
  {
    return (resultSetType == 1003) && (resultSetConcurrency == 1007) && (fetchSize == Integer.MIN_VALUE);
  }
  

  private int originalResultSetType = 0;
  private int originalFetchSize = 0;
  



  public void enableStreamingResults()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      originalResultSetType = resultSetType;
      originalFetchSize = fetchSize;
      
      setFetchSize(Integer.MIN_VALUE);
      setResultSetType(1003);
    }
  }
  
  public void disableStreamingResults() throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      if ((fetchSize == Integer.MIN_VALUE) && (resultSetType == 1003)) {
        setFetchSize(originalFetchSize);
        setResultSetType(originalResultSetType);
      }
    }
  }
  





  protected void setupStreamingTimeout(MySQLConnection con)
    throws SQLException
  {
    if ((createStreamingResultSet()) && (con.getNetTimeoutForStreamingResults() > 0)) {
      executeSimpleNonQuery(con, "SET net_write_timeout=" + con.getNetTimeoutForStreamingResults());
    }
  }
  












  public boolean execute(String sql)
    throws SQLException
  {
    return executeInternal(sql, false);
  }
  
  private boolean executeInternal(String sql, boolean returnGeneratedKeys) throws SQLException {
    MySQLConnection locallyScopedConn = checkClosed();
    char firstNonWsChar;
    boolean maybeSelect; boolean readInfoMsgState; synchronized (locallyScopedConn.getConnectionMutex()) {
      checkClosed();
      
      checkNullOrEmptyQuery(sql);
      
      resetCancelledState();
      
      implicitlyCloseAllOpenResults();
      
      if ((sql.charAt(0) == '/') && 
        (sql.startsWith("/* ping */"))) {
        doPingInstead();
        
        return true;
      }
      

      firstNonWsChar = StringUtils.firstAlphaCharUc(sql, findStartOfStatement(sql));
      maybeSelect = firstNonWsChar == 'S';
      
      retrieveGeneratedKeys = returnGeneratedKeys;
      
      lastQueryIsOnDupKeyUpdate = ((returnGeneratedKeys) && (firstNonWsChar == 'I') && (containsOnDuplicateKeyInString(sql)));
      
      if ((!maybeSelect) && (locallyScopedConn.isReadOnly())) {
        throw SQLError.createSQLException(Messages.getString("Statement.27") + Messages.getString("Statement.28"), "S1009", getExceptionInterceptor());
      }
      

      readInfoMsgState = locallyScopedConn.isReadInfoMsgEnabled();
      if ((returnGeneratedKeys) && (firstNonWsChar == 'R'))
      {

        locallyScopedConn.setReadInfoMsgEnabled(true);
      }
    }
    try {
      setupStreamingTimeout(locallyScopedConn);
      
      if (doEscapeProcessing) {
        Object escapedSqlResult = EscapeProcessor.escapeSQL(sql, locallyScopedConn.serverSupportsConvertFn(), locallyScopedConn);
        
        if ((escapedSqlResult instanceof String)) {
          sql = (String)escapedSqlResult;
        } else {
          sql = escapedSql;
        }
      }
      
      CachedResultSetMetaData cachedMetaData = null;
      
      ResultSetInternalMethods rs = null;
      
      batchedGeneratedKeys = null;
      
      if (useServerFetch()) {
        rs = createResultSetUsingServerFetch(sql);
      } else {
        timeoutTask = null;
        
        String oldCatalog = null;
        try
        {
          if ((locallyScopedConn.getEnableQueryTimeouts()) && (timeoutInMillis != 0) && (locallyScopedConn.versionMeetsMinimum(5, 0, 0))) {
            timeoutTask = new CancelTask(this);
            locallyScopedConn.getCancelTimer().schedule(timeoutTask, timeoutInMillis);
          }
          
          if (!locallyScopedConn.getCatalog().equals(currentCatalog)) {
            oldCatalog = locallyScopedConn.getCatalog();
            locallyScopedConn.setCatalog(currentCatalog);
          }
          




          Field[] cachedFields = null;
          
          if (locallyScopedConn.getCacheResultSetMetadata()) {
            cachedMetaData = locallyScopedConn.getCachedMetaData(sql);
            
            if (cachedMetaData != null) {
              cachedFields = fields;
            }
          }
          



          locallyScopedConn.setSessionMaxRows(maybeSelect ? maxRows : -1);
          
          statementBegins();
          
          rs = locallyScopedConn.execSQL(this, sql, maxRows, null, resultSetType, resultSetConcurrency, createStreamingResultSet(), currentCatalog, cachedFields);
          

          if (timeoutTask != null) {
            if (caughtWhileCancelling != null) {
              throw caughtWhileCancelling;
            }
            
            timeoutTask.cancel();
            timeoutTask = null;
          }
          
          synchronized (cancelTimeoutMutex) {
            if (wasCancelled) {
              SQLException cause = null;
              
              if (wasCancelledByTimeout) {
                cause = new MySQLTimeoutException();
              } else {
                cause = new MySQLStatementCancelledException();
              }
              
              resetCancelledState();
              
              throw cause;
            }
          }
        } finally {
          if (timeoutTask != null) {
            timeoutTask.cancel();
            locallyScopedConn.getCancelTimer().purge();
          }
          
          if (oldCatalog != null) {
            locallyScopedConn.setCatalog(oldCatalog);
          }
        }
      }
      
      if (rs != null) {
        lastInsertId = rs.getUpdateID();
        
        results = rs;
        
        rs.setFirstCharOfQuery(firstNonWsChar);
        
        if (rs.reallyResult()) {
          if (cachedMetaData != null) {
            locallyScopedConn.initializeResultsMetadataFromCache(sql, cachedMetaData, results);
          }
          else if (connection.getCacheResultSetMetadata()) {
            locallyScopedConn.initializeResultsMetadataFromCache(sql, null, results);
          }
        }
      }
      

      CancelTask timeoutTask = (rs != null) && (rs.reallyResult()) ? 1 : 0;jsr 17;return timeoutTask;
    } finally {
      jsr 6; } localObject5 = returnAddress;locallyScopedConn.setReadInfoMsgEnabled(readInfoMsgState);
    
    statementExecuting.set(false);ret;
    
    localObject6 = finally;throw localObject6;
  }
  
  protected void statementBegins() {
    clearWarningsCalled = false;
    statementExecuting.set(true);
    
    MySQLConnection physicalConn = connection.getMultiHostSafeProxy().getActiveMySQLConnection();
    while (!(physicalConn instanceof ConnectionImpl)) {
      physicalConn = physicalConn.getMultiHostSafeProxy().getActiveMySQLConnection();
    }
    physicalConnection = new WeakReference(physicalConn);
  }
  
  protected void resetCancelledState() throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      if (cancelTimeoutMutex == null) {
        return;
      }
      
      synchronized (cancelTimeoutMutex) {
        wasCancelled = false;
        wasCancelledByTimeout = false;
      }
    }
  }
  

  public boolean execute(String sql, int returnGeneratedKeys)
    throws SQLException
  {
    return executeInternal(sql, returnGeneratedKeys == 1);
  }
  

  public boolean execute(String sql, int[] generatedKeyIndices)
    throws SQLException
  {
    return executeInternal(sql, (generatedKeyIndices != null) && (generatedKeyIndices.length > 0));
  }
  

  public boolean execute(String sql, String[] generatedKeyNames)
    throws SQLException
  {
    return executeInternal(sql, (generatedKeyNames != null) && (generatedKeyNames.length > 0));
  }
  











  public int[] executeBatch()
    throws SQLException
  {
    return Util.truncateAndConvertToInt(executeBatchInternal());
  }
  
  protected long[] executeBatchInternal() throws SQLException {
    MySQLConnection locallyScopedConn = checkClosed();
    
    synchronized (locallyScopedConn.getConnectionMutex()) {
      if (locallyScopedConn.isReadOnly()) {
        throw SQLError.createSQLException(Messages.getString("Statement.34") + Messages.getString("Statement.35"), "S1009", getExceptionInterceptor());
      }
      

      implicitlyCloseAllOpenResults();
      
      if ((batchedArgs == null) || (batchedArgs.size() == 0)) {
        return new long[0];
      }
      

      int individualStatementTimeout = timeoutInMillis;
      timeoutInMillis = 0;
      
      CancelTask timeoutTask = null;
      try
      {
        resetCancelledState();
        
        statementBegins();
        try
        {
          retrieveGeneratedKeys = true;
          
          long[] updateCounts = null;
          
          if (batchedArgs != null) {
            nbrCommands = batchedArgs.size();
            
            batchedGeneratedKeys = new ArrayList(batchedArgs.size());
            
            boolean multiQueriesEnabled = locallyScopedConn.getAllowMultiQueries();
            
            if ((locallyScopedConn.versionMeetsMinimum(4, 1, 1)) && ((multiQueriesEnabled) || ((locallyScopedConn.getRewriteBatchedStatements()) && (nbrCommands > 4))))
            {
              long[] arrayOfLong1 = executeBatchUsingMultiQueries(multiQueriesEnabled, nbrCommands, individualStatementTimeout);return arrayOfLong1;
            }
            
            if ((locallyScopedConn.getEnableQueryTimeouts()) && (individualStatementTimeout != 0) && (locallyScopedConn.versionMeetsMinimum(5, 0, 0))) {
              timeoutTask = new CancelTask(this);
              locallyScopedConn.getCancelTimer().schedule(timeoutTask, individualStatementTimeout);
            }
            
            updateCounts = new long[nbrCommands];
            
            for (int i = 0; i < nbrCommands; i++) {
              updateCounts[i] = -3L;
            }
            
            SQLException sqlEx = null;
            
            int commandIndex = 0;
            
            for (commandIndex = 0; commandIndex < nbrCommands; commandIndex++) {
              try {
                String sql = (String)batchedArgs.get(commandIndex);
                updateCounts[commandIndex] = executeUpdateInternal(sql, true, true);
                
                getBatchedGeneratedKeys((results.getFirstCharOfQuery() == 'I') && (containsOnDuplicateKeyInString(sql)) ? 1 : 0);
              } catch (SQLException ex) {
                updateCounts[commandIndex] = -3L;
                
                if ((continueBatchOnError) && (!(ex instanceof MySQLTimeoutException)) && (!(ex instanceof MySQLStatementCancelledException)) && (!hasDeadlockOrTimeoutRolledBackTx(ex)))
                {
                  sqlEx = ex;
                } else {
                  long[] newUpdateCounts = new long[commandIndex];
                  
                  if (hasDeadlockOrTimeoutRolledBackTx(ex)) {
                    for (int i = 0; i < newUpdateCounts.length; i++) {
                      newUpdateCounts[i] = -3L;
                    }
                  } else {
                    System.arraycopy(updateCounts, 0, newUpdateCounts, 0, commandIndex);
                  }
                  
                  throw SQLError.createBatchUpdateException(ex, newUpdateCounts, getExceptionInterceptor());
                }
              }
            }
            
            if (sqlEx != null) {
              throw SQLError.createBatchUpdateException(sqlEx, updateCounts, getExceptionInterceptor());
            }
          }
          
          if (timeoutTask != null) {
            if (caughtWhileCancelling != null) {
              throw caughtWhileCancelling;
            }
            
            timeoutTask.cancel();
            
            locallyScopedConn.getCancelTimer().purge();
            timeoutTask = null;
          }
          
          int nbrCommands = updateCounts != null ? updateCounts : new long[0];return nbrCommands;
        } finally {
          statementExecuting.set(false);
        }
        

        localObject4 = returnAddress; } finally { jsr 6; } if (timeoutTask != null) {
        timeoutTask.cancel();
        
        locallyScopedConn.getCancelTimer().purge();
      }
      
      resetCancelledState();
      
      timeoutInMillis = individualStatementTimeout;
      
      clearBatch();ret;
    }
  }
  
  protected final boolean hasDeadlockOrTimeoutRolledBackTx(SQLException ex)
  {
    int vendorCode = ex.getErrorCode();
    
    switch (vendorCode) {
    case 1206: 
    case 1213: 
      return true;
    case 1205: 
      return !version5013OrNewer;
    }
    return false;
  }
  








  private long[] executeBatchUsingMultiQueries(boolean multiQueriesEnabled, int nbrCommands, int individualStatementTimeout)
    throws SQLException
  {
    MySQLConnection locallyScopedConn = checkClosed();
    
    synchronized (locallyScopedConn.getConnectionMutex()) {
      if (!multiQueriesEnabled) {
        locallyScopedConn.getIO().enableMultiQueries();
      }
      
      java.sql.Statement batchStmt = null;
      
      CancelTask timeoutTask = null;
      try
      {
        long[] updateCounts = new long[nbrCommands];
        
        for (int i = 0; i < nbrCommands; i++) {
          updateCounts[i] = -3L;
        }
        
        int commandIndex = 0;
        
        StringBuilder queryBuf = new StringBuilder();
        
        batchStmt = locallyScopedConn.createStatement();
        
        if ((locallyScopedConn.getEnableQueryTimeouts()) && (individualStatementTimeout != 0) && (locallyScopedConn.versionMeetsMinimum(5, 0, 0))) {
          timeoutTask = new CancelTask((StatementImpl)batchStmt);
          locallyScopedConn.getCancelTimer().schedule(timeoutTask, individualStatementTimeout);
        }
        
        int counter = 0;
        
        int numberOfBytesPerChar = 1;
        
        String connectionEncoding = locallyScopedConn.getEncoding();
        
        if (StringUtils.startsWithIgnoreCase(connectionEncoding, "utf")) {
          numberOfBytesPerChar = 3;
        } else if (CharsetMapping.isMultibyteCharset(connectionEncoding)) {
          numberOfBytesPerChar = 2;
        }
        
        int escapeAdjust = 1;
        
        batchStmt.setEscapeProcessing(doEscapeProcessing);
        
        if (doEscapeProcessing) {
          escapeAdjust = 2;
        }
        
        SQLException sqlEx = null;
        
        int argumentSetsInBatchSoFar = 0;
        
        for (commandIndex = 0; commandIndex < nbrCommands; commandIndex++) {
          String nextQuery = (String)batchedArgs.get(commandIndex);
          
          if (((queryBuf.length() + nextQuery.length()) * numberOfBytesPerChar + 1 + 4) * escapeAdjust + 32 > connection.getMaxAllowedPacket())
          {
            try {
              batchStmt.execute(queryBuf.toString(), 1);
            } catch (SQLException ex) {
              sqlEx = handleExceptionForBatch(commandIndex, argumentSetsInBatchSoFar, updateCounts, ex);
            }
            
            counter = processMultiCountsAndKeys((StatementImpl)batchStmt, counter, updateCounts);
            
            queryBuf = new StringBuilder();
            argumentSetsInBatchSoFar = 0;
          }
          
          queryBuf.append(nextQuery);
          queryBuf.append(";");
          argumentSetsInBatchSoFar++;
        }
        
        if (queryBuf.length() > 0) {
          try {
            batchStmt.execute(queryBuf.toString(), 1);
          } catch (SQLException ex) {
            sqlEx = handleExceptionForBatch(commandIndex - 1, argumentSetsInBatchSoFar, updateCounts, ex);
          }
          
          counter = processMultiCountsAndKeys((StatementImpl)batchStmt, counter, updateCounts);
        }
        
        if (timeoutTask != null) {
          if (caughtWhileCancelling != null) {
            throw caughtWhileCancelling;
          }
          
          timeoutTask.cancel();
          
          locallyScopedConn.getCancelTimer().purge();
          
          timeoutTask = null;
        }
        
        if (sqlEx != null) {
          throw SQLError.createBatchUpdateException(sqlEx, updateCounts, getExceptionInterceptor());
        }
        
        ex = updateCounts != null ? updateCounts : new long[0];jsr 17;return ex;
      } finally {
        jsr 6; } localObject2 = returnAddress; if (timeoutTask != null) {
        timeoutTask.cancel();
        
        locallyScopedConn.getCancelTimer().purge();
      }
      
      resetCancelledState();
      try
      {
        if (batchStmt != null) {
          batchStmt.close();
        }
      } finally {
        if (!multiQueriesEnabled)
          locallyScopedConn.getIO().disableMultiQueries(); } } ret;
    


    localObject5 = finally;throw localObject5;
  }
  
  protected int processMultiCountsAndKeys(StatementImpl batchedStatement, int updateCountCounter, long[] updateCounts) throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      updateCounts[(updateCountCounter++)] = batchedStatement.getLargeUpdateCount();
      
      boolean doGenKeys = batchedGeneratedKeys != null;
      
      byte[][] row = (byte[][])null;
      
      if (doGenKeys) {
        long generatedKey = batchedStatement.getLastInsertID();
        
        row = new byte[1][];
        row[0] = StringUtils.getBytes(Long.toString(generatedKey));
        batchedGeneratedKeys.add(new ByteArrayRow(row, getExceptionInterceptor()));
      }
      
      while ((batchedStatement.getMoreResults()) || (batchedStatement.getLargeUpdateCount() != -1L)) {
        updateCounts[(updateCountCounter++)] = batchedStatement.getLargeUpdateCount();
        
        if (doGenKeys) {
          long generatedKey = batchedStatement.getLastInsertID();
          
          row = new byte[1][];
          row[0] = StringUtils.getBytes(Long.toString(generatedKey));
          batchedGeneratedKeys.add(new ByteArrayRow(row, getExceptionInterceptor()));
        }
      }
      
      return updateCountCounter;
    }
  }
  
  protected SQLException handleExceptionForBatch(int endOfBatchIndex, int numValuesPerBatch, long[] updateCounts, SQLException ex) throws BatchUpdateException, SQLException
  {
    for (int j = endOfBatchIndex; j > endOfBatchIndex - numValuesPerBatch; j--) {
      updateCounts[j] = -3L;
    }
    
    if ((continueBatchOnError) && (!(ex instanceof MySQLTimeoutException)) && (!(ex instanceof MySQLStatementCancelledException)) && (!hasDeadlockOrTimeoutRolledBackTx(ex)))
    {
      return ex;
    }
    
    long[] newUpdateCounts = new long[endOfBatchIndex];
    System.arraycopy(updateCounts, 0, newUpdateCounts, 0, endOfBatchIndex);
    
    throw SQLError.createBatchUpdateException(ex, newUpdateCounts, getExceptionInterceptor());
  }
  









  public ResultSet executeQuery(String sql)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      MySQLConnection locallyScopedConn = connection;
      
      retrieveGeneratedKeys = false;
      
      checkNullOrEmptyQuery(sql);
      
      resetCancelledState();
      
      implicitlyCloseAllOpenResults();
      
      if ((sql.charAt(0) == '/') && 
        (sql.startsWith("/* ping */"))) {
        doPingInstead();
        
        return results;
      }
      

      setupStreamingTimeout(locallyScopedConn);
      
      if (doEscapeProcessing) {
        Object escapedSqlResult = EscapeProcessor.escapeSQL(sql, locallyScopedConn.serverSupportsConvertFn(), connection);
        
        if ((escapedSqlResult instanceof String)) {
          sql = (String)escapedSqlResult;
        } else {
          sql = escapedSql;
        }
      }
      
      char firstStatementChar = StringUtils.firstAlphaCharUc(sql, findStartOfStatement(sql));
      
      checkForDml(sql, firstStatementChar);
      
      CachedResultSetMetaData cachedMetaData = null;
      
      if (useServerFetch()) {
        results = createResultSetUsingServerFetch(sql);
        
        return results;
      }
      
      CancelTask timeoutTask = null;
      
      String oldCatalog = null;
      try
      {
        if ((locallyScopedConn.getEnableQueryTimeouts()) && (timeoutInMillis != 0) && (locallyScopedConn.versionMeetsMinimum(5, 0, 0))) {
          timeoutTask = new CancelTask(this);
          locallyScopedConn.getCancelTimer().schedule(timeoutTask, timeoutInMillis);
        }
        
        if (!locallyScopedConn.getCatalog().equals(currentCatalog)) {
          oldCatalog = locallyScopedConn.getCatalog();
          locallyScopedConn.setCatalog(currentCatalog);
        }
        




        Field[] cachedFields = null;
        
        if (locallyScopedConn.getCacheResultSetMetadata()) {
          cachedMetaData = locallyScopedConn.getCachedMetaData(sql);
          
          if (cachedMetaData != null) {
            cachedFields = fields;
          }
        }
        
        locallyScopedConn.setSessionMaxRows(maxRows);
        
        statementBegins();
        
        results = locallyScopedConn.execSQL(this, sql, maxRows, null, resultSetType, resultSetConcurrency, createStreamingResultSet(), currentCatalog, cachedFields);
        

        if (timeoutTask != null) {
          if (caughtWhileCancelling != null) {
            throw caughtWhileCancelling;
          }
          
          timeoutTask.cancel();
          
          locallyScopedConn.getCancelTimer().purge();
          
          timeoutTask = null;
        }
        
        synchronized (cancelTimeoutMutex) {
          if (wasCancelled) {
            SQLException cause = null;
            
            if (wasCancelledByTimeout) {
              cause = new MySQLTimeoutException();
            } else {
              cause = new MySQLStatementCancelledException();
            }
            
            resetCancelledState();
            
            throw cause;
          }
        }
      } finally {
        statementExecuting.set(false);
        
        if (timeoutTask != null) {
          timeoutTask.cancel();
          
          locallyScopedConn.getCancelTimer().purge();
        }
        
        if (oldCatalog != null) {
          locallyScopedConn.setCatalog(oldCatalog);
        }
      }
      
      lastInsertId = results.getUpdateID();
      
      if (cachedMetaData != null) {
        locallyScopedConn.initializeResultsMetadataFromCache(sql, cachedMetaData, results);
      }
      else if (connection.getCacheResultSetMetadata()) {
        locallyScopedConn.initializeResultsMetadataFromCache(sql, null, results);
      }
      

      return results;
    }
  }
  
  protected void doPingInstead() throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      if (pingTarget != null) {
        pingTarget.doPing();
      } else {
        connection.ping();
      }
      
      ResultSetInternalMethods fakeSelectOneResultSet = generatePingResultSet();
      results = fakeSelectOneResultSet;
    }
  }
  
  protected ResultSetInternalMethods generatePingResultSet() throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      Field[] fields = { new Field(null, "1", -5, 1) };
      ArrayList<ResultSetRow> rows = new ArrayList();
      byte[] colVal = { 49 };
      
      rows.add(new ByteArrayRow(new byte[][] { colVal }, getExceptionInterceptor()));
      
      return (ResultSetInternalMethods)DatabaseMetaData.buildResultSet(fields, rows, connection);
    }
  }
  
  protected void executeSimpleNonQuery(MySQLConnection c, String nonQuery) throws SQLException {
    c.execSQL(this, nonQuery, -1, null, 1003, 1007, false, currentCatalog, null, false).close();
  }
  









  public int executeUpdate(String sql)
    throws SQLException
  {
    return Util.truncateAndConvertToInt(executeLargeUpdate(sql));
  }
  
  protected long executeUpdateInternal(String sql, boolean isBatch, boolean returnGeneratedKeys) throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      MySQLConnection locallyScopedConn = connection;
      
      checkNullOrEmptyQuery(sql);
      
      resetCancelledState();
      
      char firstStatementChar = StringUtils.firstAlphaCharUc(sql, findStartOfStatement(sql));
      
      retrieveGeneratedKeys = returnGeneratedKeys;
      
      lastQueryIsOnDupKeyUpdate = ((returnGeneratedKeys) && (firstStatementChar == 'I') && (containsOnDuplicateKeyInString(sql)));
      
      ResultSetInternalMethods rs = null;
      
      if (doEscapeProcessing) {
        Object escapedSqlResult = EscapeProcessor.escapeSQL(sql, connection.serverSupportsConvertFn(), connection);
        
        if ((escapedSqlResult instanceof String)) {
          sql = (String)escapedSqlResult;
        } else {
          sql = escapedSql;
        }
      }
      
      if (locallyScopedConn.isReadOnly(false)) {
        throw SQLError.createSQLException(Messages.getString("Statement.42") + Messages.getString("Statement.43"), "S1009", getExceptionInterceptor());
      }
      

      if (StringUtils.startsWithIgnoreCaseAndWs(sql, "select")) {
        throw SQLError.createSQLException(Messages.getString("Statement.46"), "01S03", getExceptionInterceptor());
      }
      
      implicitlyCloseAllOpenResults();
      


      CancelTask timeoutTask = null;
      
      String oldCatalog = null;
      
      boolean readInfoMsgState = locallyScopedConn.isReadInfoMsgEnabled();
      if ((returnGeneratedKeys) && (firstStatementChar == 'R'))
      {

        locallyScopedConn.setReadInfoMsgEnabled(true);
      }
      try
      {
        if ((locallyScopedConn.getEnableQueryTimeouts()) && (timeoutInMillis != 0) && (locallyScopedConn.versionMeetsMinimum(5, 0, 0))) {
          timeoutTask = new CancelTask(this);
          locallyScopedConn.getCancelTimer().schedule(timeoutTask, timeoutInMillis);
        }
        
        if (!locallyScopedConn.getCatalog().equals(currentCatalog)) {
          oldCatalog = locallyScopedConn.getCatalog();
          locallyScopedConn.setCatalog(currentCatalog);
        }
        



        locallyScopedConn.setSessionMaxRows(-1);
        
        statementBegins();
        

        rs = locallyScopedConn.execSQL(this, sql, -1, null, 1003, 1007, false, currentCatalog, null, isBatch);
        

        if (timeoutTask != null) {
          if (caughtWhileCancelling != null) {
            throw caughtWhileCancelling;
          }
          
          timeoutTask.cancel();
          
          locallyScopedConn.getCancelTimer().purge();
          
          timeoutTask = null;
        }
        
        synchronized (cancelTimeoutMutex) {
          if (wasCancelled) {
            SQLException cause = null;
            
            if (wasCancelledByTimeout) {
              cause = new MySQLTimeoutException();
            } else {
              cause = new MySQLStatementCancelledException();
            }
            
            resetCancelledState();
            
            throw cause;
          }
        }
      } finally {
        locallyScopedConn.setReadInfoMsgEnabled(readInfoMsgState);
        
        if (timeoutTask != null) {
          timeoutTask.cancel();
          
          locallyScopedConn.getCancelTimer().purge();
        }
        
        if (oldCatalog != null) {
          locallyScopedConn.setCatalog(oldCatalog);
        }
        
        if (!isBatch) {
          statementExecuting.set(false);
        }
      }
      
      results = rs;
      
      rs.setFirstCharOfQuery(firstStatementChar);
      
      updateCount = rs.getUpdateCount();
      
      lastInsertId = rs.getUpdateID();
      
      return updateCount;
    }
  }
  

  public int executeUpdate(String sql, int autoGeneratedKeys)
    throws SQLException
  {
    return Util.truncateAndConvertToInt(executeLargeUpdate(sql, autoGeneratedKeys));
  }
  

  public int executeUpdate(String sql, int[] columnIndexes)
    throws SQLException
  {
    return Util.truncateAndConvertToInt(executeLargeUpdate(sql, columnIndexes));
  }
  

  public int executeUpdate(String sql, String[] columnNames)
    throws SQLException
  {
    return Util.truncateAndConvertToInt(executeLargeUpdate(sql, columnNames));
  }
  


  protected Calendar getCalendarInstanceForSessionOrNew()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (connection != null) {
        return connection.getCalendarInstanceForSessionOrNew();
      }
      
      return new GregorianCalendar();
    }
  }
  






  public java.sql.Connection getConnection()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      return connection;
    }
  }
  






  public int getFetchDirection()
    throws SQLException
  {
    return 1000;
  }
  






  public int getFetchSize()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      return fetchSize;
    }
  }
  

  public ResultSet getGeneratedKeys()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (!retrieveGeneratedKeys) {
        throw SQLError.createSQLException(Messages.getString("Statement.GeneratedKeysNotRequested"), "S1009", getExceptionInterceptor());
      }
      

      if (batchedGeneratedKeys == null) {
        if (lastQueryIsOnDupKeyUpdate) {
          return this.generatedKeysResults = getGeneratedKeysInternal(1L);
        }
        return this.generatedKeysResults = getGeneratedKeysInternal();
      }
      
      Field[] fields = new Field[1];
      fields[0] = new Field("", "GENERATED_KEY", -5, 20);
      fields[0].setConnection(connection);
      
      generatedKeysResults = ResultSetImpl.getInstance(currentCatalog, fields, new RowDataStatic(batchedGeneratedKeys), connection, this, false);
      

      return generatedKeysResults;
    }
  }
  



  protected ResultSetInternalMethods getGeneratedKeysInternal()
    throws SQLException
  {
    long numKeys = getLargeUpdateCount();
    return getGeneratedKeysInternal(numKeys);
  }
  
  protected ResultSetInternalMethods getGeneratedKeysInternal(long numKeys) throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      Field[] fields = new Field[1];
      fields[0] = new Field("", "GENERATED_KEY", -5, 20);
      fields[0].setConnection(connection);
      fields[0].setUseOldNameMetadata(true);
      
      ArrayList<ResultSetRow> rowSet = new ArrayList();
      
      long beginAt = getLastInsertID();
      
      if (beginAt < 0L) {
        fields[0].setUnsigned();
      }
      
      if (results != null) {
        String serverInfo = results.getServerInfo();
        



        if ((numKeys > 0L) && (results.getFirstCharOfQuery() == 'R') && (serverInfo != null) && (serverInfo.length() > 0)) {
          numKeys = getRecordCountFromInfo(serverInfo);
        }
        
        if ((beginAt != 0L) && (numKeys > 0L)) {
          for (int i = 0; i < numKeys; i++) {
            byte[][] row = new byte[1][];
            if (beginAt > 0L) {
              row[0] = StringUtils.getBytes(Long.toString(beginAt));
            } else {
              byte[] asBytes = new byte[8];
              asBytes[7] = ((byte)(int)(beginAt & 0xFF));
              asBytes[6] = ((byte)(int)(beginAt >>> 8));
              asBytes[5] = ((byte)(int)(beginAt >>> 16));
              asBytes[4] = ((byte)(int)(beginAt >>> 24));
              asBytes[3] = ((byte)(int)(beginAt >>> 32));
              asBytes[2] = ((byte)(int)(beginAt >>> 40));
              asBytes[1] = ((byte)(int)(beginAt >>> 48));
              asBytes[0] = ((byte)(int)(beginAt >>> 56));
              
              BigInteger val = new BigInteger(1, asBytes);
              
              row[0] = val.toString().getBytes();
            }
            rowSet.add(new ByteArrayRow(row, getExceptionInterceptor()));
            beginAt += connection.getAutoIncrementIncrement();
          }
        }
      }
      
      ResultSetImpl gkRs = ResultSetImpl.getInstance(currentCatalog, fields, new RowDataStatic(rowSet), connection, this, false);
      

      return gkRs;
    }
  }
  




  protected int getId()
  {
    return statementId;
  }
  
  /* Error */
  public long getLastInsertID()
  {
    // Byte code:
    //   0: aload_0
    //   1: invokevirtual 89	com/mysql/jdbc/StatementImpl:checkClosed	()Lcom/mysql/jdbc/MySQLConnection;
    //   4: invokeinterface 90 1 0
    //   9: dup
    //   10: astore_1
    //   11: monitorenter
    //   12: aload_0
    //   13: getfield 18	com/mysql/jdbc/StatementImpl:lastInsertId	J
    //   16: aload_1
    //   17: monitorexit
    //   18: lreturn
    //   19: astore_2
    //   20: aload_1
    //   21: monitorexit
    //   22: aload_2
    //   23: athrow
    //   24: astore_1
    //   25: new 303	java/lang/RuntimeException
    //   28: dup
    //   29: aload_1
    //   30: invokespecial 304	java/lang/RuntimeException:<init>	(Ljava/lang/Throwable;)V
    //   33: athrow
    // Line number table:
    //   Java source line #1805	-> byte code offset #0
    //   Java source line #1806	-> byte code offset #12
    //   Java source line #1807	-> byte code offset #19
    //   Java source line #1808	-> byte code offset #24
    //   Java source line #1809	-> byte code offset #25
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	34	0	this	StatementImpl
    //   24	6	1	e	SQLException
    //   19	4	2	localObject1	Object
    // Exception table:
    //   from	to	target	type
    //   12	18	19	finally
    //   19	22	19	finally
    //   0	18	24	java/sql/SQLException
    //   19	24	24	java/sql/SQLException
  }
  
  /* Error */
  public long getLongUpdateCount()
  {
    // Byte code:
    //   0: aload_0
    //   1: invokevirtual 89	com/mysql/jdbc/StatementImpl:checkClosed	()Lcom/mysql/jdbc/MySQLConnection;
    //   4: invokeinterface 90 1 0
    //   9: dup
    //   10: astore_1
    //   11: monitorenter
    //   12: aload_0
    //   13: getfield 27	com/mysql/jdbc/StatementImpl:results	Lcom/mysql/jdbc/ResultSetInternalMethods;
    //   16: ifnonnull +9 -> 25
    //   19: ldc2_w 16
    //   22: aload_1
    //   23: monitorexit
    //   24: lreturn
    //   25: aload_0
    //   26: getfield 27	com/mysql/jdbc/StatementImpl:results	Lcom/mysql/jdbc/ResultSetInternalMethods;
    //   29: invokeinterface 145 1 0
    //   34: ifeq +9 -> 43
    //   37: ldc2_w 16
    //   40: aload_1
    //   41: monitorexit
    //   42: lreturn
    //   43: aload_0
    //   44: getfield 32	com/mysql/jdbc/StatementImpl:updateCount	J
    //   47: aload_1
    //   48: monitorexit
    //   49: lreturn
    //   50: astore_2
    //   51: aload_1
    //   52: monitorexit
    //   53: aload_2
    //   54: athrow
    //   55: astore_1
    //   56: new 303	java/lang/RuntimeException
    //   59: dup
    //   60: aload_1
    //   61: invokespecial 304	java/lang/RuntimeException:<init>	(Ljava/lang/Throwable;)V
    //   64: athrow
    // Line number table:
    //   Java source line #1826	-> byte code offset #0
    //   Java source line #1827	-> byte code offset #12
    //   Java source line #1828	-> byte code offset #19
    //   Java source line #1831	-> byte code offset #25
    //   Java source line #1832	-> byte code offset #37
    //   Java source line #1835	-> byte code offset #43
    //   Java source line #1836	-> byte code offset #50
    //   Java source line #1837	-> byte code offset #55
    //   Java source line #1838	-> byte code offset #56
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	65	0	this	StatementImpl
    //   55	6	1	e	SQLException
    //   50	4	2	localObject1	Object
    // Exception table:
    //   from	to	target	type
    //   12	24	50	finally
    //   25	42	50	finally
    //   43	49	50	finally
    //   50	53	50	finally
    //   0	24	55	java/sql/SQLException
    //   25	42	55	java/sql/SQLException
    //   43	49	55	java/sql/SQLException
    //   50	55	55	java/sql/SQLException
  }
  
  public int getMaxFieldSize()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      return maxFieldSize;
    }
  }
  








  public int getMaxRows()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (maxRows <= 0) {
        return 0;
      }
      
      return maxRows;
    }
  }
  







  public boolean getMoreResults()
    throws SQLException
  {
    return getMoreResults(1);
  }
  

  public boolean getMoreResults(int current)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (results == null) {
        return false;
      }
      
      boolean streamingMode = createStreamingResultSet();
      
      while ((streamingMode) && 
        (results.reallyResult()) && 
        (results.next())) {}
      




      ResultSetInternalMethods nextResultSet = results.getNextResultSet();
      
      switch (current)
      {
      case 1: 
        if (results != null) {
          if ((!streamingMode) && (!connection.getDontTrackOpenResources())) {
            results.realClose(false);
          }
          
          results.clearNextResult();
        }
        


        break;
      case 3: 
        if (results != null) {
          if ((!streamingMode) && (!connection.getDontTrackOpenResources())) {
            results.realClose(false);
          }
          
          results.clearNextResult();
        }
        
        closeAllOpenResults();
        
        break;
      
      case 2: 
        if (!connection.getDontTrackOpenResources()) {
          openResults.add(results);
        }
        
        results.clearNextResult();
        
        break;
      
      default: 
        throw SQLError.createSQLException(Messages.getString("Statement.19"), "S1009", getExceptionInterceptor());
      }
      
      results = nextResultSet;
      
      if (results == null) {
        updateCount = -1L;
        lastInsertId = -1L;
      } else if (results.reallyResult()) {
        updateCount = -1L;
        lastInsertId = -1L;
      } else {
        updateCount = results.getUpdateCount();
        lastInsertId = results.getUpdateID();
      }
      
      boolean moreResults = (results != null) && (results.reallyResult());
      if (!moreResults) {
        checkAndPerformCloseOnCompletionAction();
      }
      return moreResults;
    }
  }
  








  public int getQueryTimeout()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      return timeoutInMillis / 1000;
    }
  }
  




  private long getRecordCountFromInfo(String serverInfo)
  {
    StringBuilder recordsBuf = new StringBuilder();
    long recordsCount = 0L;
    long duplicatesCount = 0L;
    
    char c = '\000';
    
    int length = serverInfo.length();
    for (int i = 0; 
        
        i < length; i++) {
      c = serverInfo.charAt(i);
      
      if (Character.isDigit(c)) {
        break;
      }
    }
    
    recordsBuf.append(c);
    i++;
    for (; 
        i < length; i++) {
      c = serverInfo.charAt(i);
      
      if (!Character.isDigit(c)) {
        break;
      }
      
      recordsBuf.append(c);
    }
    
    recordsCount = Long.parseLong(recordsBuf.toString());
    
    StringBuilder duplicatesBuf = new StringBuilder();
    for (; 
        i < length; i++) {
      c = serverInfo.charAt(i);
      
      if (Character.isDigit(c)) {
        break;
      }
    }
    
    duplicatesBuf.append(c);
    i++;
    for (; 
        i < length; i++) {
      c = serverInfo.charAt(i);
      
      if (!Character.isDigit(c)) {
        break;
      }
      
      duplicatesBuf.append(c);
    }
    
    duplicatesCount = Long.parseLong(duplicatesBuf.toString());
    
    return recordsCount - duplicatesCount;
  }
  







  public ResultSet getResultSet()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      return (results != null) && (results.reallyResult()) ? results : null;
    }
  }
  






  public int getResultSetConcurrency()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      return resultSetConcurrency;
    }
  }
  

  public int getResultSetHoldability()
    throws SQLException
  {
    return 1;
  }
  
  protected ResultSetInternalMethods getResultSetInternal() {
    try {
      synchronized (checkClosed().getConnectionMutex()) {
        return results;
      }
      
      return results;
    }
    catch (SQLException e) {}
  }
  





  public int getResultSetType()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      return resultSetType;
    }
  }
  








  public int getUpdateCount()
    throws SQLException
  {
    return Util.truncateAndConvertToInt(getLargeUpdateCount());
  }
  

















  public SQLWarning getWarnings()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {
      if (clearWarningsCalled) {
        return null;
      }
      
      if (connection.versionMeetsMinimum(4, 1, 0)) {
        SQLWarning pendingWarningsFromServer = SQLError.convertShowWarningsToSQLWarnings(connection);
        
        if (warningChain != null) {
          warningChain.setNextWarning(pendingWarningsFromServer);
        } else {
          warningChain = pendingWarningsFromServer;
        }
        
        return warningChain;
      }
      
      return warningChain;
    }
  }
  







  protected void realClose(boolean calledExplicitly, boolean closeOpenResults)
    throws SQLException
  {
    MySQLConnection locallyScopedConn = connection;
    
    if ((locallyScopedConn == null) || (isClosed)) {
      return;
    }
    

    if (!locallyScopedConn.getDontTrackOpenResources()) {
      locallyScopedConn.unregisterStatement(this);
    }
    
    if ((useUsageAdvisor) && 
      (!calledExplicitly)) {
      String message = Messages.getString("Statement.63") + Messages.getString("Statement.64");
      
      eventSink.consumeEvent(new ProfilerEvent((byte)0, "", currentCatalog, connectionId, getId(), -1, System.currentTimeMillis(), 0L, Constants.MILLIS_I18N, null, pointOfOrigin, message));
    }
    


    if (closeOpenResults) {
      closeOpenResults = (!holdResultsOpenOverClose) && (!connection.getDontTrackOpenResources());
    }
    
    if (closeOpenResults) {
      if (results != null) {
        try
        {
          results.close();
        }
        catch (Exception ex) {}
      }
      
      if (generatedKeysResults != null) {
        try
        {
          generatedKeysResults.close();
        }
        catch (Exception ex) {}
      }
      
      closeAllOpenResults();
    }
    
    isClosed = true;
    
    results = null;
    generatedKeysResults = null;
    connection = null;
    warningChain = null;
    openResults = null;
    batchedGeneratedKeys = null;
    localInfileInputStream = null;
    pingTarget = null;
  }
  













  public void setCursorName(String name)
    throws SQLException
  {}
  












  public void setEscapeProcessing(boolean enable)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      doEscapeProcessing = enable;
    }
  }
  











  public void setFetchDirection(int direction)
    throws SQLException
  {
    switch (direction)
    {
    case 1000: 
    case 1001: 
    case 1002: 
      break;
    default: 
      throw SQLError.createSQLException(Messages.getString("Statement.5"), "S1009", getExceptionInterceptor());
    }
    
  }
  











  public void setFetchSize(int rows)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (((rows < 0) && (rows != Integer.MIN_VALUE)) || ((maxRows > 0) && (rows > getMaxRows()))) {
        throw SQLError.createSQLException(Messages.getString("Statement.7"), "S1009", getExceptionInterceptor());
      }
      
      fetchSize = rows;
    }
  }
  
  public void setHoldResultsOpenOverClose(boolean holdResultsOpenOverClose) {
    try {
      synchronized (checkClosed().getConnectionMutex()) {
        this.holdResultsOpenOverClose = holdResultsOpenOverClose;
      }
    }
    catch (SQLException e) {}
  }
  








  public void setMaxFieldSize(int max)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (max < 0) {
        throw SQLError.createSQLException(Messages.getString("Statement.11"), "S1009", getExceptionInterceptor());
      }
      
      int maxBuf = connection != null ? connection.getMaxAllowedPacket() : MysqlIO.getMaxBuf();
      
      if (max > maxBuf) {
        throw SQLError.createSQLException(Messages.getString("Statement.13", new Object[] { Long.valueOf(maxBuf) }), "S1009", getExceptionInterceptor());
      }
      

      maxFieldSize = max;
    }
  }
  









  public void setMaxRows(int max)
    throws SQLException
  {
    setLargeMaxRows(max);
  }
  








  public void setQueryTimeout(int seconds)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (seconds < 0) {
        throw SQLError.createSQLException(Messages.getString("Statement.21"), "S1009", getExceptionInterceptor());
      }
      
      timeoutInMillis = (seconds * 1000);
    }
  }
  



  void setResultSetConcurrency(int concurrencyFlag)
  {
    try
    {
      synchronized (checkClosed().getConnectionMutex()) {
        resultSetConcurrency = concurrencyFlag;
      }
    }
    catch (SQLException e) {}
  }
  




  void setResultSetType(int typeFlag)
  {
    try
    {
      synchronized (checkClosed().getConnectionMutex()) {
        resultSetType = typeFlag;
      }
    }
    catch (SQLException e) {}
  }
  
  protected void getBatchedGeneratedKeys(java.sql.Statement batchedStatement) throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (retrieveGeneratedKeys) {
        ResultSet rs = null;
        try
        {
          rs = batchedStatement.getGeneratedKeys();
          
          while (rs.next()) {
            batchedGeneratedKeys.add(new ByteArrayRow(new byte[][] { rs.getBytes(1) }, getExceptionInterceptor()));
          }
        } finally {
          if (rs != null) {
            rs.close();
          }
        }
      }
    }
  }
  
  protected void getBatchedGeneratedKeys(int maxKeys) throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      if (retrieveGeneratedKeys) {
        ResultSet rs = null;
        try
        {
          if (maxKeys == 0) {
            rs = getGeneratedKeysInternal();
          } else {
            rs = getGeneratedKeysInternal(maxKeys);
          }
          
          while (rs.next()) {
            batchedGeneratedKeys.add(new ByteArrayRow(new byte[][] { rs.getBytes(1) }, getExceptionInterceptor()));
          }
        } finally {
          isImplicitlyClosingResults = true;
        }
        



        ret;
      }
    }
  }
  
  private boolean useServerFetch()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      return (connection.isCursorFetchEnabled()) && (fetchSize > 0) && (resultSetConcurrency == 1007) && (resultSetType == 1003);
    }
  }
  
  public boolean isClosed() throws SQLException
  {
    MySQLConnection locallyScopedConn = connection;
    if (locallyScopedConn == null) {
      return true;
    }
    synchronized (locallyScopedConn.getConnectionMutex()) {
      return isClosed;
    }
  }
  
  private boolean isPoolable = true;
  private InputStream localInfileInputStream;
  
  public boolean isPoolable() throws SQLException { return isPoolable; }
  
  public void setPoolable(boolean poolable) throws SQLException
  {
    isPoolable = poolable;
  }
  

  public boolean isWrapperFor(Class<?> iface)
    throws SQLException
  {
    checkClosed();
    

    return iface.isInstance(this);
  }
  

  public <T> T unwrap(Class<T> iface)
    throws SQLException
  {
    try
    {
      return iface.cast(this);
    } catch (ClassCastException cce) {
      throw SQLError.createSQLException("Unable to unwrap to " + iface.toString(), "S1009", getExceptionInterceptor());
    }
  }
  
  protected static int findStartOfStatement(String sql) {
    int statementStartPos = 0;
    
    if (StringUtils.startsWithIgnoreCaseAndWs(sql, "/*")) {
      statementStartPos = sql.indexOf("*/");
      
      if (statementStartPos == -1) {
        statementStartPos = 0;
      } else {
        statementStartPos += 2;
      }
    } else if ((StringUtils.startsWithIgnoreCaseAndWs(sql, "--")) || (StringUtils.startsWithIgnoreCaseAndWs(sql, "#"))) {
      statementStartPos = sql.indexOf('\n');
      
      if (statementStartPos == -1) {
        statementStartPos = sql.indexOf('\r');
        
        if (statementStartPos == -1) {
          statementStartPos = 0;
        }
      }
    }
    
    return statementStartPos;
  }
  

  protected final boolean version5013OrNewer;
  
  public InputStream getLocalInfileInputStream()
  {
    return localInfileInputStream;
  }
  
  public void setLocalInfileInputStream(InputStream stream) {
    localInfileInputStream = stream;
  }
  
  public void setPingTarget(PingTarget pingTarget) {
    this.pingTarget = pingTarget;
  }
  
  public ExceptionInterceptor getExceptionInterceptor() {
    return exceptionInterceptor;
  }
  
  protected boolean containsOnDuplicateKeyInString(String sql) {
    return getOnDuplicateKeyLocation(sql, connection.getDontCheckOnDuplicateKeyUpdateInSQL(), connection.getRewriteBatchedStatements(), connection.isNoBackslashEscapesSet()) != -1;
  }
  

  protected static int getOnDuplicateKeyLocation(String sql, boolean dontCheckOnDuplicateKeyUpdateInSQL, boolean rewriteBatchedStatements, boolean noBackslashEscapes)
  {
    return (dontCheckOnDuplicateKeyUpdateInSQL) && (!rewriteBatchedStatements) ? -1 : StringUtils.indexOfIgnoreCase(0, sql, ON_DUPLICATE_KEY_UPDATE_CLAUSE, "\"'`", "\"'`", noBackslashEscapes ? StringUtils.SEARCH_MODE__MRK_COM_WS : StringUtils.SEARCH_MODE__ALL);
  }
  

  private boolean closeOnCompletion = false;
  
  public void closeOnCompletion() throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      closeOnCompletion = true;
    }
  }
  
  public boolean isCloseOnCompletion() throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      return closeOnCompletion;
    }
  }
  


  public long[] executeLargeBatch()
    throws SQLException
  {
    return executeBatchInternal();
  }
  


  public long executeLargeUpdate(String sql)
    throws SQLException
  {
    return executeUpdateInternal(sql, false, false);
  }
  


  public long executeLargeUpdate(String sql, int autoGeneratedKeys)
    throws SQLException
  {
    return executeUpdateInternal(sql, false, autoGeneratedKeys == 1);
  }
  


  public long executeLargeUpdate(String sql, int[] columnIndexes)
    throws SQLException
  {
    return executeUpdateInternal(sql, false, (columnIndexes != null) && (columnIndexes.length > 0));
  }
  


  public long executeLargeUpdate(String sql, String[] columnNames)
    throws SQLException
  {
    return executeUpdateInternal(sql, false, (columnNames != null) && (columnNames.length > 0));
  }
  



  public long getLargeMaxRows()
    throws SQLException
  {
    return getMaxRows();
  }
  


  public long getLargeUpdateCount()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (results == null) {
        return -1L;
      }
      
      if (results.reallyResult()) {
        return -1L;
      }
      
      return results.getUpdateCount();
    }
  }
  


  public void setLargeMaxRows(long max)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if ((max > 50000000L) || (max < 0L)) {
        throw SQLError.createSQLException(Messages.getString("Statement.15") + max + " > " + 50000000 + ".", "S1009", getExceptionInterceptor());
      }
      

      if (max == 0L) {
        max = -1L;
      }
      
      maxRows = ((int)max);
    }
  }
  
  boolean isCursorRequired() throws SQLException {
    return false;
  }
}
