package com.mysql.jdbc;

import com.mysql.jdbc.exceptions.MySQLStatementCancelledException;
import com.mysql.jdbc.exceptions.MySQLTimeoutException;
import com.mysql.jdbc.log.LogUtils;
import com.mysql.jdbc.profiler.ProfilerEvent;
import com.mysql.jdbc.profiler.ProfilerEventHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ParameterMetaData;
import java.sql.Ref;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;























public class ServerPreparedStatement
  extends PreparedStatement
{
  private static final Constructor<?> JDBC_4_SPS_CTOR;
  protected static final int BLOB_STREAM_READ_BUF_SIZE = 8192;
  
  static
  {
    if (Util.isJdbc4()) {
      try {
        String jdbc4ClassName = Util.isJdbc42() ? "com.mysql.jdbc.JDBC42ServerPreparedStatement" : "com.mysql.jdbc.JDBC4ServerPreparedStatement";
        JDBC_4_SPS_CTOR = Class.forName(jdbc4ClassName).getConstructor(new Class[] { MySQLConnection.class, String.class, String.class, Integer.TYPE, Integer.TYPE });
      }
      catch (SecurityException e) {
        throw new RuntimeException(e);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    } else {
      JDBC_4_SPS_CTOR = null;
    }
  }
  
  public static class BatchedBindValues
  {
    public ServerPreparedStatement.BindValue[] batchedParameterValues;
    
    BatchedBindValues(ServerPreparedStatement.BindValue[] paramVals)
    {
      int numParams = paramVals.length;
      
      batchedParameterValues = new ServerPreparedStatement.BindValue[numParams];
      
      for (int i = 0; i < numParams; i++) {
        batchedParameterValues[i] = new ServerPreparedStatement.BindValue(paramVals[i]);
      }
    }
  }
  
  public static class BindValue
  {
    public long boundBeforeExecutionNum = 0L;
    
    public long bindLength;
    
    public int bufferType;
    
    public double doubleBinding;
    
    public float floatBinding;
    
    public boolean isLongData;
    
    public boolean isNull;
    
    public boolean isSet = false;
    
    public long longBinding;
    
    public Object value;
    
    BindValue() {}
    
    BindValue(BindValue copyMe)
    {
      value = value;
      isSet = isSet;
      isLongData = isLongData;
      isNull = isNull;
      bufferType = bufferType;
      bindLength = bindLength;
      longBinding = longBinding;
      floatBinding = floatBinding;
      doubleBinding = doubleBinding;
    }
    
    void reset() {
      isNull = false;
      isSet = false;
      value = null;
      isLongData = false;
      
      longBinding = 0L;
      floatBinding = 0.0F;
      doubleBinding = 0.0D;
    }
    
    public String toString()
    {
      return toString(false);
    }
    
    public String toString(boolean quoteIfNeeded) {
      if (isLongData) {
        return "' STREAM DATA '";
      }
      
      if (isNull) {
        return "NULL";
      }
      
      switch (bufferType) {
      case 1: 
      case 2: 
      case 3: 
      case 8: 
        return String.valueOf(longBinding);
      case 4: 
        return String.valueOf(floatBinding);
      case 5: 
        return String.valueOf(doubleBinding);
      case 7: 
      case 10: 
      case 11: 
      case 12: 
      case 15: 
      case 253: 
      case 254: 
        if (quoteIfNeeded) {
          return "'" + String.valueOf(value) + "'";
        }
        return String.valueOf(value);
      }
      
      if ((value instanceof byte[])) {
        return "byte data";
      }
      if (quoteIfNeeded) {
        return "'" + String.valueOf(value) + "'";
      }
      return String.valueOf(value);
    }
    
    long getBoundLength()
    {
      if (isNull) {
        return 0L;
      }
      
      if (isLongData) {
        return bindLength;
      }
      
      switch (bufferType)
      {
      case 1: 
        return 1L;
      case 2: 
        return 2L;
      case 3: 
        return 4L;
      case 8: 
        return 8L;
      case 4: 
        return 4L;
      case 5: 
        return 8L;
      case 11: 
        return 9L;
      case 10: 
        return 7L;
      case 7: 
      case 12: 
        return 11L;
      case 0: 
      case 15: 
      case 246: 
      case 253: 
      case 254: 
        if ((value instanceof byte[])) {
          return ((byte[])value).length;
        }
        return ((String)value).length();
      }
      
      return 0L;
    }
  }
  
















  private boolean hasOnDuplicateKeyUpdate = false;
  
  private void storeTime(Buffer intoBuf, Time tm) throws SQLException
  {
    intoBuf.ensureCapacity(9);
    intoBuf.writeByte((byte)8);
    intoBuf.writeByte((byte)0);
    intoBuf.writeLong(0L);
    
    Calendar sessionCalendar = getCalendarInstanceForSessionOrNew();
    
    synchronized (sessionCalendar) {
      java.util.Date oldTime = sessionCalendar.getTime();
      try {
        sessionCalendar.setTime(tm);
        intoBuf.writeByte((byte)sessionCalendar.get(11));
        intoBuf.writeByte((byte)sessionCalendar.get(12));
        intoBuf.writeByte((byte)sessionCalendar.get(13));
      }
      finally
      {
        sessionCalendar.setTime(oldTime);
      }
    }
  }
  





  private boolean detectedLongParameterSwitch = false;
  



  private int fieldCount;
  


  private boolean invalid = false;
  

  private SQLException invalidationException;
  

  private Buffer outByteBuffer;
  

  private BindValue[] parameterBindings;
  

  private Field[] parameterFields;
  

  private Field[] resultFields;
  
  private boolean sendTypesToServer = false;
  

  private long serverStatementId;
  

  private int stringTypeCode = 254;
  



  private boolean serverNeedsResetBeforeEachExecution;
  



  protected static ServerPreparedStatement getInstance(MySQLConnection conn, String sql, String catalog, int resultSetType, int resultSetConcurrency)
    throws SQLException
  {
    if (!Util.isJdbc4()) {
      return new ServerPreparedStatement(conn, sql, catalog, resultSetType, resultSetConcurrency);
    }
    try
    {
      return (ServerPreparedStatement)JDBC_4_SPS_CTOR.newInstance(new Object[] { conn, sql, catalog, Integer.valueOf(resultSetType), Integer.valueOf(resultSetConcurrency) });
    }
    catch (IllegalArgumentException e) {
      throw new SQLException(e.toString(), "S1000");
    } catch (InstantiationException e) {
      throw new SQLException(e.toString(), "S1000");
    } catch (IllegalAccessException e) {
      throw new SQLException(e.toString(), "S1000");
    } catch (InvocationTargetException e) {
      Throwable target = e.getTargetException();
      
      if ((target instanceof SQLException)) {
        throw ((SQLException)target);
      }
      
      throw new SQLException(target.toString(), "S1000");
    }
  }
  











  protected ServerPreparedStatement(MySQLConnection conn, String sql, String catalog, int resultSetType, int resultSetConcurrency)
    throws SQLException
  {
    super(conn, catalog);
    
    checkNullOrEmptyQuery(sql);
    
    int startOfStatement = findStartOfStatement(sql);
    
    firstCharOfStmt = StringUtils.firstAlphaCharUc(sql, startOfStatement);
    
    hasOnDuplicateKeyUpdate = ((firstCharOfStmt == 'I') && (containsOnDuplicateKeyInString(sql)));
    
    if (connection.versionMeetsMinimum(5, 0, 0)) {
      serverNeedsResetBeforeEachExecution = (!connection.versionMeetsMinimum(5, 0, 3));
    } else {
      serverNeedsResetBeforeEachExecution = (!connection.versionMeetsMinimum(4, 1, 10));
    }
    
    useAutoSlowLog = connection.getAutoSlowLog();
    useTrueBoolean = connection.versionMeetsMinimum(3, 21, 23);
    
    String statementComment = connection.getStatementComment();
    
    originalSql = ("/* " + statementComment + " */ " + sql);
    
    if (connection.versionMeetsMinimum(4, 1, 2)) {
      stringTypeCode = 253;
    } else {
      stringTypeCode = 254;
    }
    try
    {
      serverPrepare(sql);
    } catch (SQLException sqlEx) {
      realClose(false, true);
      
      throw sqlEx;
    } catch (Exception ex) {
      realClose(false, true);
      
      SQLException sqlEx = SQLError.createSQLException(ex.toString(), "S1000", getExceptionInterceptor());
      sqlEx.initCause(ex);
      
      throw sqlEx;
    }
    
    setResultSetType(resultSetType);
    setResultSetConcurrency(resultSetConcurrency);
    
    parameterTypes = new int[parameterCount];
  }
  







  public void addBatch()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {
      if (batchedArgs == null) {
        batchedArgs = new ArrayList();
      }
      
      batchedArgs.add(new BatchedBindValues(parameterBindings));
    }
  }
  
  public String asSql(boolean quoteStreamsAndUnknowns)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {
      PreparedStatement pStmtForSub = null;
      try
      {
        pStmtForSub = PreparedStatement.getInstance(connection, originalSql, currentCatalog);
        
        int numParameters = parameterCount;
        int ourNumParameters = parameterCount;
        
        for (int i = 0; (i < numParameters) && (i < ourNumParameters); i++) {
          if (parameterBindings[i] != null) {
            if (parameterBindings[i].isNull) {
              pStmtForSub.setNull(i + 1, 0);
            } else {
              BindValue bindValue = parameterBindings[i];
              



              switch (bufferType)
              {
              case 1: 
                pStmtForSub.setByte(i + 1, (byte)(int)longBinding);
                break;
              case 2: 
                pStmtForSub.setShort(i + 1, (short)(int)longBinding);
                break;
              case 3: 
                pStmtForSub.setInt(i + 1, (int)longBinding);
                break;
              case 8: 
                pStmtForSub.setLong(i + 1, longBinding);
                break;
              case 4: 
                pStmtForSub.setFloat(i + 1, floatBinding);
                break;
              case 5: 
                pStmtForSub.setDouble(i + 1, doubleBinding);
                break;
              case 6: case 7: default: 
                pStmtForSub.setObject(i + 1, parameterBindings[i].value);
              }
              
            }
          }
        }
        
        i = pStmtForSub.asSql(quoteStreamsAndUnknowns);jsr 16;return i;
      } finally {
        jsr 6; } localObject2 = returnAddress; if (pStmtForSub != null) {
        try {
          pStmtForSub.close();
        } catch (SQLException sqlEx) {}
      }
      ret;
    }
  }
  






  protected MySQLConnection checkClosed()
    throws SQLException
  {
    if (invalid) {
      throw invalidationException;
    }
    
    return super.checkClosed();
  }
  


  public void clearParameters()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      clearParametersInternal(true);
    }
  }
  
  private void clearParametersInternal(boolean clearServerParameters) throws SQLException {
    boolean hadLongData = false;
    
    if (parameterBindings != null) {
      for (int i = 0; i < parameterCount; i++) {
        if ((parameterBindings[i] != null) && (parameterBindings[i].isLongData)) {
          hadLongData = true;
        }
        
        parameterBindings[i].reset();
      }
    }
    
    if ((clearServerParameters) && (hadLongData)) {
      serverResetStatement();
      
      detectedLongParameterSwitch = false;
    }
  }
  
  protected boolean isCached = false;
  
  private boolean useAutoSlowLog;
  
  private Calendar serverTzCalendar;
  private Calendar defaultTzCalendar;
  
  protected void setClosed(boolean flag)
  {
    isClosed = flag;
  }
  


  public void close()
    throws SQLException
  {
    MySQLConnection locallyScopedConn = connection;
    
    if (locallyScopedConn == null) {
      return;
    }
    
    synchronized (locallyScopedConn.getConnectionMutex()) {
      if ((isCached) && (isPoolable()) && (!isClosed)) {
        clearParameters();
        isClosed = true;
        connection.recachePreparedStatement(this);
        return;
      }
      
      isClosed = false;
      realClose(true, true);
    }
  }
  
  private void dumpCloseForTestcase() throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      StringBuilder buf = new StringBuilder();
      connection.generateConnectionCommentBlock(buf);
      buf.append("DEALLOCATE PREPARE debug_stmt_");
      buf.append(statementId);
      buf.append(";\n");
      
      connection.dumpTestcaseQuery(buf.toString());
    }
  }
  
  private void dumpExecuteForTestcase() throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      StringBuilder buf = new StringBuilder();
      
      for (int i = 0; i < parameterCount; i++) {
        connection.generateConnectionCommentBlock(buf);
        
        buf.append("SET @debug_stmt_param");
        buf.append(statementId);
        buf.append("_");
        buf.append(i);
        buf.append("=");
        
        if (parameterBindings[i].isNull) {
          buf.append("NULL");
        } else {
          buf.append(parameterBindings[i].toString(true));
        }
        
        buf.append(";\n");
      }
      
      connection.generateConnectionCommentBlock(buf);
      
      buf.append("EXECUTE debug_stmt_");
      buf.append(statementId);
      
      if (parameterCount > 0) {
        buf.append(" USING ");
        for (int i = 0; i < parameterCount; i++) {
          if (i > 0) {
            buf.append(", ");
          }
          
          buf.append("@debug_stmt_param");
          buf.append(statementId);
          buf.append("_");
          buf.append(i);
        }
      }
      

      buf.append(";\n");
      
      connection.dumpTestcaseQuery(buf.toString());
    }
  }
  
  private void dumpPrepareForTestcase() throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      StringBuilder buf = new StringBuilder(originalSql.length() + 64);
      
      connection.generateConnectionCommentBlock(buf);
      
      buf.append("PREPARE debug_stmt_");
      buf.append(statementId);
      buf.append(" FROM \"");
      buf.append(originalSql);
      buf.append("\";\n");
      
      connection.dumpTestcaseQuery(buf.toString());
    }
  }
  
  protected long[] executeBatchSerially(int batchTimeout) throws SQLException { MySQLConnection locallyScopedConn;
    BindValue[] oldBindValues;
    synchronized (checkClosed().getConnectionMutex()) {
      locallyScopedConn = connection;
      
      if (locallyScopedConn.isReadOnly()) {
        throw SQLError.createSQLException(Messages.getString("ServerPreparedStatement.2") + Messages.getString("ServerPreparedStatement.3"), "S1009", getExceptionInterceptor());
      }
      

      clearWarnings();
      


      oldBindValues = parameterBindings;
    }
    try {
      long[] updateCounts = null;
      
      if (batchedArgs != null) {
        nbrCommands = batchedArgs.size();
        updateCounts = new long[nbrCommands];
        
        if (retrieveGeneratedKeys) {
          batchedGeneratedKeys = new ArrayList(nbrCommands);
        }
        
        for (int i = 0; i < nbrCommands; i++) {
          updateCounts[i] = -3L;
        }
        
        SQLException sqlEx = null;
        
        int commandIndex = 0;
        
        BindValue[] previousBindValuesForBatch = null;
        
        StatementImpl.CancelTask timeoutTask = null;
        try
        {
          if ((locallyScopedConn.getEnableQueryTimeouts()) && (batchTimeout != 0) && (locallyScopedConn.versionMeetsMinimum(5, 0, 0))) {
            timeoutTask = new StatementImpl.CancelTask(this, this);
            locallyScopedConn.getCancelTimer().schedule(timeoutTask, batchTimeout);
          }
          
          for (commandIndex = 0; commandIndex < nbrCommands; commandIndex++) {
            Object arg = batchedArgs.get(commandIndex);
            try
            {
              if ((arg instanceof String)) {
                updateCounts[commandIndex] = executeUpdateInternal((String)arg, true, retrieveGeneratedKeys);
                

                getBatchedGeneratedKeys((results.getFirstCharOfQuery() == 'I') && (containsOnDuplicateKeyInString((String)arg)) ? 1 : 0);
              } else {
                parameterBindings = batchedParameterValues;
                


                if (previousBindValuesForBatch != null) {
                  for (int j = 0; j < parameterBindings.length; j++) {
                    if (parameterBindings[j].bufferType != bufferType) {
                      sendTypesToServer = true;
                      
                      break;
                    }
                  }
                }
                try
                {
                  updateCounts[commandIndex] = executeUpdateInternal(false, true);
                } finally {
                  previousBindValuesForBatch = parameterBindings;
                }
                

                getBatchedGeneratedKeys(containsOnDuplicateKeyUpdateInSQL() ? 1 : 0);
              }
            } catch (SQLException ex) {
              updateCounts[commandIndex] = -3L;
              
              if ((continueBatchOnError) && (!(ex instanceof MySQLTimeoutException)) && (!(ex instanceof MySQLStatementCancelledException)) && (!hasDeadlockOrTimeoutRolledBackTx(ex)))
              {
                sqlEx = ex;
              } else {
                long[] newUpdateCounts = new long[commandIndex];
                System.arraycopy(updateCounts, 0, newUpdateCounts, 0, commandIndex);
                
                throw SQLError.createBatchUpdateException(ex, newUpdateCounts, getExceptionInterceptor());
              }
            }
          }
        } finally {
          if (timeoutTask != null) {
            timeoutTask.cancel();
            
            locallyScopedConn.getCancelTimer().purge();
          }
          
          resetCancelledState();
        }
        
        if (sqlEx != null) {
          throw SQLError.createBatchUpdateException(sqlEx, updateCounts, getExceptionInterceptor());
        }
      }
      
      int nbrCommands = updateCounts != null ? updateCounts : new long[0];jsr 16;return nbrCommands;
    } finally {
      jsr 6; } localObject6 = returnAddress;parameterBindings = oldBindValues;
    sendTypesToServer = true;
    
    clearBatch();ret;
    
    localObject7 = finally;throw localObject7;
  }
  



  protected ResultSetInternalMethods executeInternal(int maxRowsToRetrieve, Buffer sendPacket, boolean createStreamingResultSet, boolean queryIsSelectOnly, Field[] metadataFromCache, boolean isBatch)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      numberOfExecutions += 1;
      
      try
      {
        return serverExecute(maxRowsToRetrieve, createStreamingResultSet, metadataFromCache);
      }
      catch (SQLException sqlEx) {
        if (connection.getEnablePacketDebug()) {
          connection.getIO().dumpPacketRingBuffer();
        }
        
        if (connection.getDumpQueriesOnException()) {
          String extractedSql = toString();
          StringBuilder messageBuf = new StringBuilder(extractedSql.length() + 32);
          messageBuf.append("\n\nQuery being executed when exception was thrown:\n");
          messageBuf.append(extractedSql);
          messageBuf.append("\n\n");
          
          sqlEx = ConnectionImpl.appendMessageToException(sqlEx, messageBuf.toString(), getExceptionInterceptor());
        }
        
        throw sqlEx;
      } catch (Exception ex) {
        if (connection.getEnablePacketDebug()) {
          connection.getIO().dumpPacketRingBuffer();
        }
        
        SQLException sqlEx = SQLError.createSQLException(ex.toString(), "S1000", getExceptionInterceptor());
        
        if (connection.getDumpQueriesOnException()) {
          String extractedSql = toString();
          StringBuilder messageBuf = new StringBuilder(extractedSql.length() + 32);
          messageBuf.append("\n\nQuery being executed when exception was thrown:\n");
          messageBuf.append(extractedSql);
          messageBuf.append("\n\n");
          
          sqlEx = ConnectionImpl.appendMessageToException(sqlEx, messageBuf.toString(), getExceptionInterceptor());
        }
        
        sqlEx.initCause(ex);
        
        throw sqlEx;
      }
    }
  }
  


  protected Buffer fillSendPacket()
    throws SQLException
  {
    return null;
  }
  



  protected Buffer fillSendPacket(byte[][] batchedParameterStrings, InputStream[] batchedParameterStreams, boolean[] batchedIsStream, int[] batchedStreamLengths)
    throws SQLException
  {
    return null;
  }
  








  protected BindValue getBinding(int parameterIndex, boolean forLongData)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {
      if (parameterBindings.length == 0) {
        throw SQLError.createSQLException(Messages.getString("ServerPreparedStatement.8"), "S1009", getExceptionInterceptor());
      }
      

      parameterIndex--;
      
      if ((parameterIndex < 0) || (parameterIndex >= parameterBindings.length)) {
        throw SQLError.createSQLException(Messages.getString("ServerPreparedStatement.9") + (parameterIndex + 1) + Messages.getString("ServerPreparedStatement.10") + parameterBindings.length, "S1009", getExceptionInterceptor());
      }
      


      if (parameterBindings[parameterIndex] == null) {
        parameterBindings[parameterIndex] = new BindValue();
      }
      else if ((parameterBindings[parameterIndex].isLongData) && (!forLongData)) {
        detectedLongParameterSwitch = true;
      }
      

      return parameterBindings[parameterIndex];
    }
  }
  






  public BindValue[] getParameterBindValues()
  {
    return parameterBindings;
  }
  

  byte[] getBytes(int parameterIndex)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      BindValue bindValue = getBinding(parameterIndex, false);
      
      if (isNull)
        return null;
      if (isLongData) {
        throw SQLError.createSQLFeatureNotSupportedException();
      }
      if (outByteBuffer == null) {
        outByteBuffer = new Buffer(connection.getNetBufferLength());
      }
      
      outByteBuffer.clear();
      
      int originalPosition = outByteBuffer.getPosition();
      
      storeBinding(outByteBuffer, bindValue, connection.getIO());
      
      int newPosition = outByteBuffer.getPosition();
      
      int length = newPosition - originalPosition;
      
      byte[] valueAsBytes = new byte[length];
      
      System.arraycopy(outByteBuffer.getByteBuffer(), originalPosition, valueAsBytes, 0, length);
      
      return valueAsBytes;
    }
  }
  



  public java.sql.ResultSetMetaData getMetaData()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {
      if (resultFields == null) {
        return null;
      }
      
      return new ResultSetMetaData(resultFields, connection.getUseOldAliasMetadataBehavior(), connection.getYearIsDateType(), getExceptionInterceptor());
    }
  }
  



  public ParameterMetaData getParameterMetaData()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {
      if (parameterMetaData == null) {
        parameterMetaData = new MysqlParameterMetadata(parameterFields, parameterCount, getExceptionInterceptor());
      }
      
      return parameterMetaData;
    }
  }
  



  boolean isNull(int paramIndex)
  {
    throw new IllegalArgumentException(Messages.getString("ServerPreparedStatement.7"));
  }
  








  protected void realClose(boolean calledExplicitly, boolean closeOpenResults)
    throws SQLException
  {
    MySQLConnection locallyScopedConn = connection;
    
    if (locallyScopedConn == null) {
      return;
    }
    
    synchronized (locallyScopedConn.getConnectionMutex())
    {
      if (connection != null) {
        if (connection.getAutoGenerateTestcaseScript()) {
          dumpCloseForTestcase();
        }
        







        SQLException exceptionDuringClose = null;
        
        if ((calledExplicitly) && (!connection.isClosed())) {
          synchronized (connection.getConnectionMutex())
          {
            try {
              MysqlIO mysql = connection.getIO();
              
              Buffer packet = mysql.getSharedSendPacket();
              
              packet.writeByte((byte)25);
              packet.writeLong(serverStatementId);
              
              mysql.sendCommand(25, null, packet, true, null, 0);
            } catch (SQLException sqlEx) {
              exceptionDuringClose = sqlEx;
            }
          }
        }
        
        if (isCached) {
          connection.decachePreparedStatement(this);
          isCached = false;
        }
        super.realClose(calledExplicitly, closeOpenResults);
        
        clearParametersInternal(false);
        parameterBindings = null;
        
        parameterFields = null;
        resultFields = null;
        
        if (exceptionDuringClose != null) {
          throw exceptionDuringClose;
        }
      }
    }
  }
  





  protected void rePrepare()
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      invalidationException = null;
      try
      {
        serverPrepare(originalSql);
      }
      catch (SQLException sqlEx) {
        invalidationException = sqlEx;
      } catch (Exception ex) {
        invalidationException = SQLError.createSQLException(ex.toString(), "S1000", getExceptionInterceptor());
        invalidationException.initCause(ex);
      }
      
      if (invalidationException != null) {
        invalid = true;
        
        parameterBindings = null;
        
        parameterFields = null;
        resultFields = null;
        
        if (results != null) {
          try {
            results.close();
          }
          catch (Exception ex) {}
        }
        
        if (generatedKeysResults != null) {
          try {
            generatedKeysResults.close();
          }
          catch (Exception ex) {}
        }
        try
        {
          closeAllOpenResults();
        }
        catch (Exception e) {}
        
        if ((connection != null) && 
          (!connection.getDontTrackOpenResources())) {
          connection.unregisterStatement(this);
        }
      }
    }
  }
  





  boolean isCursorRequired()
    throws SQLException
  {
    return (resultFields != null) && (connection.isCursorFetchEnabled()) && (getResultSetType() == 1003) && (getResultSetConcurrency() == 1007) && (getFetchSize() > 0);
  }
  





























  private ResultSetInternalMethods serverExecute(int maxRowsToRetrieve, boolean createStreamingResultSet, Field[] metadataFromCache)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      MysqlIO mysql = connection.getIO();
      
      if (mysql.shouldIntercept()) {
        ResultSetInternalMethods interceptedResults = mysql.invokeStatementInterceptorsPre(originalSql, this, true);
        
        if (interceptedResults != null) {
          return interceptedResults;
        }
      }
      
      if (detectedLongParameterSwitch)
      {
        boolean firstFound = false;
        long boundTimeToCheck = 0L;
        
        for (int i = 0; i < parameterCount - 1; i++) {
          if (parameterBindings[i].isLongData) {
            if ((firstFound) && (boundTimeToCheck != parameterBindings[i].boundBeforeExecutionNum)) {
              throw SQLError.createSQLException(Messages.getString("ServerPreparedStatement.11") + Messages.getString("ServerPreparedStatement.12"), "S1C00", getExceptionInterceptor());
            }
            

            firstFound = true;
            boundTimeToCheck = parameterBindings[i].boundBeforeExecutionNum;
          }
        }
        


        serverResetStatement();
      }
      

      for (int i = 0; i < parameterCount; i++) {
        if (!parameterBindings[i].isSet) {
          throw SQLError.createSQLException(Messages.getString("ServerPreparedStatement.13") + (i + 1) + Messages.getString("ServerPreparedStatement.14"), "S1009", getExceptionInterceptor());
        }
      }
      





      for (int i = 0; i < parameterCount; i++) {
        if (parameterBindings[i].isLongData) {
          serverLongData(i, parameterBindings[i]);
        }
      }
      
      if (connection.getAutoGenerateTestcaseScript()) {
        dumpExecuteForTestcase();
      }
      




      Buffer packet = mysql.getSharedSendPacket();
      
      packet.clear();
      packet.writeByte((byte)23);
      packet.writeLong(serverStatementId);
      
      if (connection.versionMeetsMinimum(4, 1, 2)) {
        if (isCursorRequired()) {
          packet.writeByte((byte)1);
        } else {
          packet.writeByte((byte)0);
        }
        
        packet.writeLong(1L);
      }
      

      int nullCount = (parameterCount + 7) / 8;
      



      int nullBitsPosition = packet.getPosition();
      
      for (int i = 0; i < nullCount; i++) {
        packet.writeByte((byte)0);
      }
      
      byte[] nullBitsBuffer = new byte[nullCount];
      

      packet.writeByte((byte)(sendTypesToServer ? 1 : 0));
      
      if (sendTypesToServer)
      {


        for (int i = 0; i < parameterCount; i++) {
          packet.writeInt(parameterBindings[i].bufferType);
        }
      }
      



      for (int i = 0; i < parameterCount; i++) {
        if (!parameterBindings[i].isLongData) {
          if (!parameterBindings[i].isNull) {
            storeBinding(packet, parameterBindings[i], mysql);
          } else {
            int tmp550_549 = (i / 8); byte[] tmp550_543 = nullBitsBuffer;tmp550_543[tmp550_549] = ((byte)(tmp550_543[tmp550_549] | 1 << (i & 0x7)));
          }
        }
      }
      



      int endPosition = packet.getPosition();
      packet.setPosition(nullBitsPosition);
      packet.writeBytesNoNull(nullBitsBuffer);
      packet.setPosition(endPosition);
      
      long begin = 0L;
      
      boolean logSlowQueries = connection.getLogSlowQueries();
      boolean gatherPerformanceMetrics = connection.getGatherPerformanceMetrics();
      
      if ((profileSQL) || (logSlowQueries) || (gatherPerformanceMetrics)) {
        begin = mysql.getCurrentTimeNanosOrMillis();
      }
      
      resetCancelledState();
      
      StatementImpl.CancelTask timeoutTask = null;
      
      try
      {
        String queryAsString = "";
        if ((profileSQL) || (logSlowQueries) || (gatherPerformanceMetrics)) {
          queryAsString = asSql(true);
        }
        
        if ((connection.getEnableQueryTimeouts()) && (timeoutInMillis != 0) && (connection.versionMeetsMinimum(5, 0, 0))) {
          timeoutTask = new StatementImpl.CancelTask(this, this);
          connection.getCancelTimer().schedule(timeoutTask, timeoutInMillis);
        }
        
        statementBegins();
        
        Buffer resultPacket = mysql.sendCommand(23, null, packet, false, null, 0);
        
        long queryEndTime = 0L;
        
        if ((logSlowQueries) || (gatherPerformanceMetrics) || (profileSQL)) {
          queryEndTime = mysql.getCurrentTimeNanosOrMillis();
        }
        
        if (timeoutTask != null) {
          timeoutTask.cancel();
          
          connection.getCancelTimer().purge();
          
          if (caughtWhileCancelling != null) {
            throw caughtWhileCancelling;
          }
          
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
        
        boolean queryWasSlow = false;
        
        if ((logSlowQueries) || (gatherPerformanceMetrics)) {
          long elapsedTime = queryEndTime - begin;
          
          if (logSlowQueries) {
            if (useAutoSlowLog) {
              queryWasSlow = elapsedTime > connection.getSlowQueryThresholdMillis();
            } else {
              queryWasSlow = connection.isAbonormallyLongQuery(elapsedTime);
              
              connection.reportQueryTime(elapsedTime);
            }
          }
          
          if (queryWasSlow)
          {
            StringBuilder mesgBuf = new StringBuilder(48 + originalSql.length());
            mesgBuf.append(Messages.getString("ServerPreparedStatement.15"));
            mesgBuf.append(mysql.getSlowQueryThreshold());
            mesgBuf.append(Messages.getString("ServerPreparedStatement.15a"));
            mesgBuf.append(elapsedTime);
            mesgBuf.append(Messages.getString("ServerPreparedStatement.16"));
            
            mesgBuf.append("as prepared: ");
            mesgBuf.append(originalSql);
            mesgBuf.append("\n\n with parameters bound:\n\n");
            mesgBuf.append(queryAsString);
            
            eventSink.consumeEvent(new ProfilerEvent((byte)6, "", currentCatalog, connection.getId(), getId(), 0, System.currentTimeMillis(), elapsedTime, mysql.getQueryTimingUnits(), null, LogUtils.findCallingClassAndMethod(new Throwable()), mesgBuf.toString()));
          }
          


          if (gatherPerformanceMetrics) {
            connection.registerQueryExecutionTime(elapsedTime);
          }
        }
        
        connection.incrementNumberOfPreparedExecutes();
        
        if (profileSQL) {
          eventSink = ProfilerEventHandlerFactory.getInstance(connection);
          
          eventSink.consumeEvent(new ProfilerEvent((byte)4, "", currentCatalog, connectionId, statementId, -1, System.currentTimeMillis(), mysql.getCurrentTimeNanosOrMillis() - begin, mysql.getQueryTimingUnits(), null, LogUtils.findCallingClassAndMethod(new Throwable()), truncateQueryToLog(queryAsString)));
        }
        


        ResultSetInternalMethods rs = mysql.readAllResults(this, maxRowsToRetrieve, resultSetType, resultSetConcurrency, createStreamingResultSet, currentCatalog, resultPacket, true, fieldCount, metadataFromCache);
        

        if (mysql.shouldIntercept()) {
          ResultSetInternalMethods interceptedResults = mysql.invokeStatementInterceptorsPost(originalSql, this, rs, true, null);
          
          if (interceptedResults != null) {
            rs = interceptedResults;
          }
        }
        long fetchEndTime;
        if (profileSQL) {
          fetchEndTime = mysql.getCurrentTimeNanosOrMillis();
          
          eventSink.consumeEvent(new ProfilerEvent((byte)5, "", currentCatalog, connection.getId(), getId(), 0, System.currentTimeMillis(), fetchEndTime - queryEndTime, mysql.getQueryTimingUnits(), null, LogUtils.findCallingClassAndMethod(new Throwable()), null));
        }
        






        if ((queryWasSlow) && (connection.getExplainSlowQueries())) {
          mysql.explainSlowQuery(StringUtils.getBytes(queryAsString), queryAsString);
        }
        
        if ((!createStreamingResultSet) && (serverNeedsResetBeforeEachExecution)) {
          serverResetStatement();
        }
        
        sendTypesToServer = false;
        results = rs;
        
        if (mysql.hadWarnings()) {
          mysql.scanForAndThrowDataTruncation();
        }
        
        ResultSetInternalMethods localResultSetInternalMethods1 = rs;jsr 45;return localResultSetInternalMethods1;
      } catch (SQLException sqlEx) {
        if (mysql.shouldIntercept()) {
          mysql.invokeStatementInterceptorsPost(originalSql, this, null, true, sqlEx);
        }
        
        throw sqlEx;
      } finally {
        jsr 6; } localObject3 = returnAddress;statementExecuting.set(false);
      
      if (timeoutTask != null) {
        timeoutTask.cancel();
        connection.getCancelTimer().purge(); } ret;
    }
  }
  

























  private void serverLongData(int parameterIndex, BindValue longData)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      MysqlIO mysql = connection.getIO();
      
      Buffer packet = mysql.getSharedSendPacket();
      
      Object value = value;
      
      if ((value instanceof byte[])) {
        packet.clear();
        packet.writeByte((byte)24);
        packet.writeLong(serverStatementId);
        packet.writeInt(parameterIndex);
        
        packet.writeBytesNoNull((byte[])value);
        
        mysql.sendCommand(24, null, packet, true, null, 0);
      } else if ((value instanceof InputStream)) {
        storeStream(mysql, parameterIndex, packet, (InputStream)value);
      } else if ((value instanceof Blob)) {
        storeStream(mysql, parameterIndex, packet, ((Blob)value).getBinaryStream());
      } else if ((value instanceof Reader)) {
        storeReader(mysql, parameterIndex, packet, (Reader)value);
      } else {
        throw SQLError.createSQLException(Messages.getString("ServerPreparedStatement.18") + value.getClass().getName() + "'", "S1009", getExceptionInterceptor());
      }
    }
  }
  
  private void serverPrepare(String sql) throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      MysqlIO mysql = connection.getIO();
      
      if (connection.getAutoGenerateTestcaseScript()) {
        dumpPrepareForTestcase();
      }
      try
      {
        long begin = 0L;
        
        if (StringUtils.startsWithIgnoreCaseAndWs(sql, "LOAD DATA")) {
          isLoadDataQuery = true;
        } else {
          isLoadDataQuery = false;
        }
        
        if (connection.getProfileSql()) {
          begin = System.currentTimeMillis();
        }
        
        String characterEncoding = null;
        String connectionEncoding = connection.getEncoding();
        
        if ((!isLoadDataQuery) && (connection.getUseUnicode()) && (connectionEncoding != null)) {
          characterEncoding = connectionEncoding;
        }
        
        Buffer prepareResultPacket = mysql.sendCommand(22, sql, null, false, characterEncoding, 0);
        
        if (connection.versionMeetsMinimum(4, 1, 1))
        {
          prepareResultPacket.setPosition(1);
        }
        else {
          prepareResultPacket.setPosition(0);
        }
        
        serverStatementId = prepareResultPacket.readLong();
        fieldCount = prepareResultPacket.readInt();
        parameterCount = prepareResultPacket.readInt();
        parameterBindings = new BindValue[parameterCount];
        
        for (int i = 0; i < parameterCount; i++) {
          parameterBindings[i] = new BindValue();
        }
        
        connection.incrementNumberOfPrepares();
        
        if (profileSQL) {
          eventSink.consumeEvent(new ProfilerEvent((byte)2, "", currentCatalog, connectionId, statementId, -1, System.currentTimeMillis(), mysql.getCurrentTimeNanosOrMillis() - begin, mysql.getQueryTimingUnits(), null, LogUtils.findCallingClassAndMethod(new Throwable()), truncateQueryToLog(sql)));
        }
        


        boolean checkEOF = !mysql.isEOFDeprecated();
        
        if ((parameterCount > 0) && (connection.versionMeetsMinimum(4, 1, 2)) && (!mysql.isVersion(5, 0, 0))) {
          parameterFields = new Field[parameterCount];
          

          for (int i = 0; i < parameterCount; i++) {
            Buffer metaDataPacket = mysql.readPacket();
            parameterFields[i] = mysql.unpackField(metaDataPacket, false);
          }
          if (checkEOF) {
            mysql.readPacket();
          }
        }
        

        if (fieldCount > 0) {
          resultFields = new Field[fieldCount];
          

          for (int i = 0; i < fieldCount; i++) {
            Buffer fieldPacket = mysql.readPacket();
            resultFields[i] = mysql.unpackField(fieldPacket, false);
          }
          if (checkEOF) {
            mysql.readPacket();
          }
        }
      } catch (SQLException sqlEx) {
        if (connection.getDumpQueriesOnException()) {
          StringBuilder messageBuf = new StringBuilder(originalSql.length() + 32);
          messageBuf.append("\n\nQuery being prepared when exception was thrown:\n\n");
          messageBuf.append(originalSql);
          
          sqlEx = ConnectionImpl.appendMessageToException(sqlEx, messageBuf.toString(), getExceptionInterceptor());
        }
        
        throw sqlEx;
      }
      finally {
        connection.getIO().clearInputStream();
      }
    }
  }
  
  private String truncateQueryToLog(String sql) throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      String query = null;
      
      if (sql.length() > connection.getMaxQuerySizeToLog()) {
        StringBuilder queryBuf = new StringBuilder(connection.getMaxQuerySizeToLog() + 12);
        queryBuf.append(sql.substring(0, connection.getMaxQuerySizeToLog()));
        queryBuf.append(Messages.getString("MysqlIO.25"));
        
        query = queryBuf.toString();
      } else {
        query = sql;
      }
      
      return query;
    }
  }
  
  private void serverResetStatement() throws SQLException {
    synchronized (checkClosed().getConnectionMutex())
    {
      MysqlIO mysql = connection.getIO();
      
      Buffer packet = mysql.getSharedSendPacket();
      
      packet.clear();
      packet.writeByte((byte)26);
      packet.writeLong(serverStatementId);
      try
      {
        mysql.sendCommand(26, null, packet, !connection.versionMeetsMinimum(4, 1, 2), null, 0);
      } catch (SQLException sqlEx) {
        throw sqlEx;
      } catch (Exception ex) {
        SQLException sqlEx = SQLError.createSQLException(ex.toString(), "S1000", getExceptionInterceptor());
        sqlEx.initCause(ex);
        
        throw sqlEx;
      } finally {
        mysql.clearInputStream();
      }
    }
  }
  


  public void setArray(int i, Array x)
    throws SQLException
  {
    throw SQLError.createSQLFeatureNotSupportedException();
  }
  


  public void setAsciiStream(int parameterIndex, InputStream x, int length)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (x == null) {
        setNull(parameterIndex, -2);
      } else {
        BindValue binding = getBinding(parameterIndex, true);
        resetToType(binding, 252);
        
        value = x;
        isLongData = true;
        
        if (connection.getUseStreamLengthsInPrepStmts()) {
          bindLength = length;
        } else {
          bindLength = -1L;
        }
      }
    }
  }
  


  public void setBigDecimal(int parameterIndex, BigDecimal x)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {
      if (x == null) {
        setNull(parameterIndex, 3);
      }
      else {
        BindValue binding = getBinding(parameterIndex, false);
        
        if (connection.versionMeetsMinimum(5, 0, 3)) {
          resetToType(binding, 246);
        } else {
          resetToType(binding, stringTypeCode);
        }
        
        value = StringUtils.fixDecimalExponent(StringUtils.consistentToString(x));
      }
    }
  }
  


  public void setBinaryStream(int parameterIndex, InputStream x, int length)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {
      if (x == null) {
        setNull(parameterIndex, -2);
      } else {
        BindValue binding = getBinding(parameterIndex, true);
        resetToType(binding, 252);
        
        value = x;
        isLongData = true;
        
        if (connection.getUseStreamLengthsInPrepStmts()) {
          bindLength = length;
        } else {
          bindLength = -1L;
        }
      }
    }
  }
  


  public void setBlob(int parameterIndex, Blob x)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {
      if (x == null) {
        setNull(parameterIndex, -2);
      } else {
        BindValue binding = getBinding(parameterIndex, true);
        resetToType(binding, 252);
        
        value = x;
        isLongData = true;
        
        if (connection.getUseStreamLengthsInPrepStmts()) {
          bindLength = x.length();
        } else {
          bindLength = -1L;
        }
      }
    }
  }
  


  public void setBoolean(int parameterIndex, boolean x)
    throws SQLException
  {
    setByte(parameterIndex, (byte)(x ? 1 : 0));
  }
  


  public void setByte(int parameterIndex, byte x)
    throws SQLException
  {
    checkClosed();
    
    BindValue binding = getBinding(parameterIndex, false);
    resetToType(binding, 1);
    
    longBinding = x;
  }
  


  public void setBytes(int parameterIndex, byte[] x)
    throws SQLException
  {
    checkClosed();
    
    if (x == null) {
      setNull(parameterIndex, -2);
    } else {
      BindValue binding = getBinding(parameterIndex, false);
      resetToType(binding, 253);
      
      value = x;
    }
  }
  


  public void setCharacterStream(int parameterIndex, Reader reader, int length)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {
      if (reader == null) {
        setNull(parameterIndex, -2);
      } else {
        BindValue binding = getBinding(parameterIndex, true);
        resetToType(binding, 252);
        
        value = reader;
        isLongData = true;
        
        if (connection.getUseStreamLengthsInPrepStmts()) {
          bindLength = length;
        } else {
          bindLength = -1L;
        }
      }
    }
  }
  


  public void setClob(int parameterIndex, Clob x)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {
      if (x == null) {
        setNull(parameterIndex, -2);
      } else {
        BindValue binding = getBinding(parameterIndex, true);
        resetToType(binding, 252);
        
        value = x.getCharacterStream();
        isLongData = true;
        
        if (connection.getUseStreamLengthsInPrepStmts()) {
          bindLength = x.length();
        } else {
          bindLength = -1L;
        }
      }
    }
  }
  











  public void setDate(int parameterIndex, java.sql.Date x)
    throws SQLException
  {
    setDate(parameterIndex, x, null);
  }
  













  public void setDate(int parameterIndex, java.sql.Date x, Calendar cal)
    throws SQLException
  {
    if (x == null) {
      setNull(parameterIndex, 91);
    } else {
      BindValue binding = getBinding(parameterIndex, false);
      resetToType(binding, 10);
      
      value = x;
    }
  }
  


  public void setDouble(int parameterIndex, double x)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {
      if ((!connection.getAllowNanAndInf()) && ((x == Double.POSITIVE_INFINITY) || (x == Double.NEGATIVE_INFINITY) || (Double.isNaN(x)))) {
        throw SQLError.createSQLException("'" + x + "' is not a valid numeric or approximate numeric value", "S1009", getExceptionInterceptor());
      }
      


      BindValue binding = getBinding(parameterIndex, false);
      resetToType(binding, 5);
      
      doubleBinding = x;
    }
  }
  


  public void setFloat(int parameterIndex, float x)
    throws SQLException
  {
    checkClosed();
    
    BindValue binding = getBinding(parameterIndex, false);
    resetToType(binding, 4);
    
    floatBinding = x;
  }
  


  public void setInt(int parameterIndex, int x)
    throws SQLException
  {
    checkClosed();
    
    BindValue binding = getBinding(parameterIndex, false);
    resetToType(binding, 3);
    
    longBinding = x;
  }
  


  public void setLong(int parameterIndex, long x)
    throws SQLException
  {
    checkClosed();
    
    BindValue binding = getBinding(parameterIndex, false);
    resetToType(binding, 8);
    
    longBinding = x;
  }
  


  public void setNull(int parameterIndex, int sqlType)
    throws SQLException
  {
    checkClosed();
    
    BindValue binding = getBinding(parameterIndex, false);
    resetToType(binding, 6);
    
    isNull = true;
  }
  


  public void setNull(int parameterIndex, int sqlType, String typeName)
    throws SQLException
  {
    checkClosed();
    
    BindValue binding = getBinding(parameterIndex, false);
    resetToType(binding, 6);
    
    isNull = true;
  }
  


  public void setRef(int i, Ref x)
    throws SQLException
  {
    throw SQLError.createSQLFeatureNotSupportedException();
  }
  


  public void setShort(int parameterIndex, short x)
    throws SQLException
  {
    checkClosed();
    
    BindValue binding = getBinding(parameterIndex, false);
    resetToType(binding, 2);
    
    longBinding = x;
  }
  


  public void setString(int parameterIndex, String x)
    throws SQLException
  {
    checkClosed();
    
    if (x == null) {
      setNull(parameterIndex, 1);
    } else {
      BindValue binding = getBinding(parameterIndex, false);
      resetToType(binding, stringTypeCode);
      
      value = x;
    }
  }
  










  public void setTime(int parameterIndex, Time x)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      setTimeInternal(parameterIndex, x, null, connection.getDefaultTimeZone(), false);
    }
  }
  














  public void setTime(int parameterIndex, Time x, Calendar cal)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      setTimeInternal(parameterIndex, x, cal, cal.getTimeZone(), true);
    }
  }
  













  private void setTimeInternal(int parameterIndex, Time x, Calendar targetCalendar, TimeZone tz, boolean rollForward)
    throws SQLException
  {
    if (x == null) {
      setNull(parameterIndex, 92);
    } else {
      BindValue binding = getBinding(parameterIndex, false);
      resetToType(binding, 11);
      
      if (!useLegacyDatetimeCode) {
        value = x;
      } else {
        Calendar sessionCalendar = getCalendarInstanceForSessionOrNew();
        
        value = TimeUtil.changeTimezone(connection, sessionCalendar, targetCalendar, x, tz, connection.getServerTimezoneTZ(), rollForward);
      }
    }
  }
  












  public void setTimestamp(int parameterIndex, Timestamp x)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      setTimestampInternal(parameterIndex, x, null, connection.getDefaultTimeZone(), false);
    }
  }
  













  public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      setTimestampInternal(parameterIndex, x, cal, cal.getTimeZone(), true);
    }
  }
  
  private void setTimestampInternal(int parameterIndex, Timestamp x, Calendar targetCalendar, TimeZone tz, boolean rollForward) throws SQLException {
    if (x == null) {
      setNull(parameterIndex, 93);
    } else {
      BindValue binding = getBinding(parameterIndex, false);
      resetToType(binding, 12);
      
      if (!sendFractionalSeconds) {
        x = TimeUtil.truncateFractionalSeconds(x);
      }
      
      if (!useLegacyDatetimeCode) {
        value = x;
      } else {
        Calendar sessionCalendar = connection.getUseJDBCCompliantTimezoneShift() ? connection.getUtcCalendar() : getCalendarInstanceForSessionOrNew();
        

        value = TimeUtil.changeTimezone(connection, sessionCalendar, targetCalendar, x, tz, connection.getServerTimezoneTZ(), rollForward);
      }
    }
  }
  


  protected void resetToType(BindValue oldValue, int bufferType)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex())
    {
      oldValue.reset();
      
      if ((bufferType != 6) || (bufferType == 0))
      {
        if (bufferType != bufferType) {
          sendTypesToServer = true;
          bufferType = bufferType;
        }
      }
      
      isSet = true;
      boundBeforeExecutionNum = numberOfExecutions;
    }
  }
  










  @Deprecated
  public void setUnicodeStream(int parameterIndex, InputStream x, int length)
    throws SQLException
  {
    checkClosed();
    
    throw SQLError.createSQLFeatureNotSupportedException();
  }
  


  public void setURL(int parameterIndex, URL x)
    throws SQLException
  {
    checkClosed();
    
    setString(parameterIndex, x.toString());
  }
  







  private void storeBinding(Buffer packet, BindValue bindValue, MysqlIO mysql)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      try {
        Object value = value;
        



        switch (bufferType)
        {
        case 1: 
          packet.writeByte((byte)(int)longBinding);
          return;
        case 2: 
          packet.ensureCapacity(2);
          packet.writeInt((int)longBinding);
          return;
        case 3: 
          packet.ensureCapacity(4);
          packet.writeLong((int)longBinding);
          return;
        case 8: 
          packet.ensureCapacity(8);
          packet.writeLongLong(longBinding);
          return;
        case 4: 
          packet.ensureCapacity(4);
          packet.writeFloat(floatBinding);
          return;
        case 5: 
          packet.ensureCapacity(8);
          packet.writeDouble(doubleBinding);
          return;
        case 11: 
          storeTime(packet, (Time)value);
          return;
        case 7: 
        case 10: 
        case 12: 
          storeDateTime(packet, (java.util.Date)value, mysql, bufferType);
          return;
        case 0: 
        case 15: 
        case 246: 
        case 253: 
        case 254: 
          if ((value instanceof byte[])) {
            packet.writeLenBytes((byte[])value);
          } else if (!isLoadDataQuery) {
            packet.writeLenString((String)value, charEncoding, connection.getServerCharset(), charConverter, connection.parserKnowsUnicode(), connection);
          }
          else {
            packet.writeLenBytes(StringUtils.getBytes((String)value));
          }
          
          return;
        }
      }
      catch (UnsupportedEncodingException uEE) {
        throw SQLError.createSQLException(Messages.getString("ServerPreparedStatement.22") + connection.getEncoding() + "'", "S1000", getExceptionInterceptor());
      }
    }
  }
  
  private void storeDateTime412AndOlder(Buffer intoBuf, java.util.Date dt, int bufferType) throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      Calendar sessionCalendar = null;
      
      if (!useLegacyDatetimeCode) {
        if (bufferType == 10) {
          sessionCalendar = getDefaultTzCalendar();
        } else {
          sessionCalendar = getServerTzCalendar();
        }
      } else {
        sessionCalendar = ((dt instanceof Timestamp)) && (connection.getUseJDBCCompliantTimezoneShift()) ? connection.getUtcCalendar() : getCalendarInstanceForSessionOrNew();
      }
      

      java.util.Date oldTime = sessionCalendar.getTime();
      try
      {
        intoBuf.ensureCapacity(8);
        intoBuf.writeByte((byte)7);
        
        sessionCalendar.setTime(dt);
        
        int year = sessionCalendar.get(1);
        int month = sessionCalendar.get(2) + 1;
        int date = sessionCalendar.get(5);
        
        intoBuf.writeInt(year);
        intoBuf.writeByte((byte)month);
        intoBuf.writeByte((byte)date);
        
        if ((dt instanceof java.sql.Date)) {
          intoBuf.writeByte((byte)0);
          intoBuf.writeByte((byte)0);
          intoBuf.writeByte((byte)0);
        } else {
          intoBuf.writeByte((byte)sessionCalendar.get(11));
          intoBuf.writeByte((byte)sessionCalendar.get(12));
          intoBuf.writeByte((byte)sessionCalendar.get(13));
        }
      } finally {
        sessionCalendar.setTime(oldTime);
      }
    }
  }
  





  private void storeDateTime(Buffer intoBuf, java.util.Date dt, MysqlIO mysql, int bufferType)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (connection.versionMeetsMinimum(4, 1, 3)) {
        storeDateTime413AndNewer(intoBuf, dt, bufferType);
      } else {
        storeDateTime412AndOlder(intoBuf, dt, bufferType);
      }
    }
  }
  
  private void storeDateTime413AndNewer(Buffer intoBuf, java.util.Date dt, int bufferType) throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      Calendar sessionCalendar = null;
      
      if (!useLegacyDatetimeCode) {
        if (bufferType == 10) {
          sessionCalendar = getDefaultTzCalendar();
        } else {
          sessionCalendar = getServerTzCalendar();
        }
      } else {
        sessionCalendar = ((dt instanceof Timestamp)) && (connection.getUseJDBCCompliantTimezoneShift()) ? connection.getUtcCalendar() : getCalendarInstanceForSessionOrNew();
      }
      

      java.util.Date oldTime = sessionCalendar.getTime();
      try
      {
        sessionCalendar.setTime(dt);
        
        if ((dt instanceof java.sql.Date)) {
          sessionCalendar.set(11, 0);
          sessionCalendar.set(12, 0);
          sessionCalendar.set(13, 0);
        }
        
        byte length = 7;
        
        if ((dt instanceof Timestamp)) {
          length = 11;
        }
        
        intoBuf.ensureCapacity(length);
        
        intoBuf.writeByte(length);
        
        int year = sessionCalendar.get(1);
        int month = sessionCalendar.get(2) + 1;
        int date = sessionCalendar.get(5);
        
        intoBuf.writeInt(year);
        intoBuf.writeByte((byte)month);
        intoBuf.writeByte((byte)date);
        
        if ((dt instanceof java.sql.Date)) {
          intoBuf.writeByte((byte)0);
          intoBuf.writeByte((byte)0);
          intoBuf.writeByte((byte)0);
        } else {
          intoBuf.writeByte((byte)sessionCalendar.get(11));
          intoBuf.writeByte((byte)sessionCalendar.get(12));
          intoBuf.writeByte((byte)sessionCalendar.get(13));
        }
        
        if (length == 11)
        {
          intoBuf.writeLong(((Timestamp)dt).getNanos() / 1000);
        }
      }
      finally {
        sessionCalendar.setTime(oldTime);
      }
    }
  }
  
  private Calendar getServerTzCalendar() throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      if (serverTzCalendar == null) {
        serverTzCalendar = new GregorianCalendar(connection.getServerTimezoneTZ());
      }
      
      return serverTzCalendar;
    }
  }
  
  private Calendar getDefaultTzCalendar() throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      if (defaultTzCalendar == null) {
        defaultTzCalendar = new GregorianCalendar(TimeZone.getDefault());
      }
      
      return defaultTzCalendar;
    }
  }
  

  private void storeReader(MysqlIO mysql, int parameterIndex, Buffer packet, Reader inStream)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      String forcedEncoding = connection.getClobCharacterEncoding();
      
      String clobEncoding = forcedEncoding == null ? connection.getEncoding() : forcedEncoding;
      
      int maxBytesChar = 2;
      
      if (clobEncoding != null) {
        if (!clobEncoding.equals("UTF-16")) {
          maxBytesChar = connection.getMaxBytesPerChar(clobEncoding);
          
          if (maxBytesChar == 1) {
            maxBytesChar = 2;
          }
        } else {
          maxBytesChar = 4;
        }
      }
      
      char[] buf = new char[8192 / maxBytesChar];
      
      int numRead = 0;
      
      int bytesInPacket = 0;
      int totalBytesRead = 0;
      int bytesReadAtLastSend = 0;
      int packetIsFullAt = connection.getBlobSendChunkSize();
      try
      {
        packet.clear();
        packet.writeByte((byte)24);
        packet.writeLong(serverStatementId);
        packet.writeInt(parameterIndex);
        
        boolean readAny = false;
        
        while ((numRead = inStream.read(buf)) != -1) {
          readAny = true;
          
          byte[] valueAsBytes = StringUtils.getBytes(buf, null, clobEncoding, connection.getServerCharset(), 0, numRead, connection.parserKnowsUnicode(), getExceptionInterceptor());
          

          packet.writeBytesNoNull(valueAsBytes, 0, valueAsBytes.length);
          
          bytesInPacket += valueAsBytes.length;
          totalBytesRead += valueAsBytes.length;
          
          if (bytesInPacket >= packetIsFullAt) {
            bytesReadAtLastSend = totalBytesRead;
            
            mysql.sendCommand(24, null, packet, true, null, 0);
            
            bytesInPacket = 0;
            packet.clear();
            packet.writeByte((byte)24);
            packet.writeLong(serverStatementId);
            packet.writeInt(parameterIndex);
          }
        }
        
        if (totalBytesRead != bytesReadAtLastSend) {
          mysql.sendCommand(24, null, packet, true, null, 0);
        }
        
        if (!readAny) {
          mysql.sendCommand(24, null, packet, true, null, 0);
        }
      } catch (IOException ioEx) {
        SQLException sqlEx = SQLError.createSQLException(Messages.getString("ServerPreparedStatement.24") + ioEx.toString(), "S1000", getExceptionInterceptor());
        
        sqlEx.initCause(ioEx);
        
        throw sqlEx;
      } finally {
        if ((connection.getAutoClosePStmtStreams()) && 
          (inStream != null)) {
          try {
            inStream.close();
          }
          catch (IOException ioEx) {}
        }
      }
    }
  }
  
  private void storeStream(MysqlIO mysql, int parameterIndex, Buffer packet, InputStream inStream)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      byte[] buf = new byte[' '];
      
      int numRead = 0;
      try
      {
        int bytesInPacket = 0;
        int totalBytesRead = 0;
        int bytesReadAtLastSend = 0;
        int packetIsFullAt = connection.getBlobSendChunkSize();
        
        packet.clear();
        packet.writeByte((byte)24);
        packet.writeLong(serverStatementId);
        packet.writeInt(parameterIndex);
        
        boolean readAny = false;
        
        while ((numRead = inStream.read(buf)) != -1)
        {
          readAny = true;
          
          packet.writeBytesNoNull(buf, 0, numRead);
          bytesInPacket += numRead;
          totalBytesRead += numRead;
          
          if (bytesInPacket >= packetIsFullAt) {
            bytesReadAtLastSend = totalBytesRead;
            
            mysql.sendCommand(24, null, packet, true, null, 0);
            
            bytesInPacket = 0;
            packet.clear();
            packet.writeByte((byte)24);
            packet.writeLong(serverStatementId);
            packet.writeInt(parameterIndex);
          }
        }
        
        if (totalBytesRead != bytesReadAtLastSend) {
          mysql.sendCommand(24, null, packet, true, null, 0);
        }
        
        if (!readAny) {
          mysql.sendCommand(24, null, packet, true, null, 0);
        }
      } catch (IOException ioEx) {
        SQLException sqlEx = SQLError.createSQLException(Messages.getString("ServerPreparedStatement.25") + ioEx.toString(), "S1000", getExceptionInterceptor());
        
        sqlEx.initCause(ioEx);
        
        throw sqlEx;
      } finally {
        if ((connection.getAutoClosePStmtStreams()) && 
          (inStream != null)) {
          try {
            inStream.close();
          }
          catch (IOException ioEx) {}
        }
      }
    }
  }
  





  public String toString()
  {
    StringBuilder toStringBuf = new StringBuilder();
    
    toStringBuf.append("com.mysql.jdbc.ServerPreparedStatement[");
    toStringBuf.append(serverStatementId);
    toStringBuf.append("] - ");
    try
    {
      toStringBuf.append(asSql());
    } catch (SQLException sqlEx) {
      toStringBuf.append(Messages.getString("ServerPreparedStatement.6"));
      toStringBuf.append(sqlEx);
    }
    
    return toStringBuf.toString();
  }
  
  protected long getServerStatementId() {
    return serverStatementId;
  }
  
  private boolean hasCheckedRewrite = false;
  private boolean canRewrite = false;
  
  public boolean canRewriteAsMultiValueInsertAtSqlLevel() throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (!hasCheckedRewrite) {
        hasCheckedRewrite = true;
        canRewrite = canRewrite(originalSql, isOnDuplicateKeyUpdate(), getLocationOfOnDuplicateKeyUpdate(), 0);
        
        parseInfo = new PreparedStatement.ParseInfo(originalSql, connection, connection.getMetaData(), charEncoding, charConverter);
      }
      
      return canRewrite;
    }
  }
  
  public boolean canRewriteAsMultivalueInsertStatement() throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      if (!canRewriteAsMultiValueInsertAtSqlLevel()) {
        return false;
      }
      
      BindValue[] currentBindValues = null;
      BindValue[] previousBindValues = null;
      
      int nbrCommands = batchedArgs.size();
      


      for (int commandIndex = 0; commandIndex < nbrCommands; commandIndex++) {
        Object arg = batchedArgs.get(commandIndex);
        
        if (!(arg instanceof String))
        {
          currentBindValues = batchedParameterValues;
          


          if (previousBindValues != null) {
            for (int j = 0; j < parameterBindings.length; j++) {
              if (bufferType != bufferType) {
                return false;
              }
            }
          }
        }
      }
      
      return true;
    }
  }
  
  private int locationOfOnDuplicateKeyUpdate = -2;
  
  protected int getLocationOfOnDuplicateKeyUpdate() throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      if (locationOfOnDuplicateKeyUpdate == -2) {
        locationOfOnDuplicateKeyUpdate = getOnDuplicateKeyLocation(originalSql, connection.getDontCheckOnDuplicateKeyUpdateInSQL(), connection.getRewriteBatchedStatements(), connection.isNoBackslashEscapesSet());
      }
      

      return locationOfOnDuplicateKeyUpdate;
    }
  }
  
  protected boolean isOnDuplicateKeyUpdate() throws SQLException {
    synchronized (checkClosed().getConnectionMutex()) {
      return getLocationOfOnDuplicateKeyUpdate() != -1;
    }
  }
  





  protected long[] computeMaxParameterSetSizeAndBatchSize(int numBatchedArgs)
    throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      long sizeOfEntireBatch = 10L;
      long maxSizeOfParameterSet = 0L;
      
      for (int i = 0; i < numBatchedArgs; i++) {
        BindValue[] paramArg = batchedArgs.get(i)).batchedParameterValues;
        
        long sizeOfParameterSet = 0L;
        
        sizeOfParameterSet += (parameterCount + 7) / 8;
        
        sizeOfParameterSet += parameterCount * 2;
        
        for (int j = 0; j < parameterBindings.length; j++) {
          if (!isNull)
          {
            long size = paramArg[j].getBoundLength();
            
            if (isLongData) {
              if (size != -1L) {
                sizeOfParameterSet += size;
              }
            } else {
              sizeOfParameterSet += size;
            }
          }
        }
        
        sizeOfEntireBatch += sizeOfParameterSet;
        
        if (sizeOfParameterSet > maxSizeOfParameterSet) {
          maxSizeOfParameterSet = sizeOfParameterSet;
        }
      }
      
      return new long[] { maxSizeOfParameterSet, sizeOfEntireBatch };
    }
  }
  
  protected int setOneBatchedParameterSet(java.sql.PreparedStatement batchedStatement, int batchedParamIndex, Object paramSet) throws SQLException
  {
    BindValue[] paramArg = batchedParameterValues;
    
    for (int j = 0; j < paramArg.length; j++) {
      if (isNull) {
        batchedStatement.setNull(batchedParamIndex++, 0);
      }
      else if (isLongData) {
        Object value = value;
        
        if ((value instanceof InputStream)) {
          batchedStatement.setBinaryStream(batchedParamIndex++, (InputStream)value, (int)bindLength);
        } else {
          batchedStatement.setCharacterStream(batchedParamIndex++, (Reader)value, (int)bindLength);
        }
      }
      else {
        switch (bufferType)
        {
        case 1: 
          batchedStatement.setByte(batchedParamIndex++, (byte)(int)longBinding);
          break;
        case 2: 
          batchedStatement.setShort(batchedParamIndex++, (short)(int)longBinding);
          break;
        case 3: 
          batchedStatement.setInt(batchedParamIndex++, (int)longBinding);
          break;
        case 8: 
          batchedStatement.setLong(batchedParamIndex++, longBinding);
          break;
        case 4: 
          batchedStatement.setFloat(batchedParamIndex++, floatBinding);
          break;
        case 5: 
          batchedStatement.setDouble(batchedParamIndex++, doubleBinding);
          break;
        case 11: 
          batchedStatement.setTime(batchedParamIndex++, (Time)value);
          break;
        case 10: 
          batchedStatement.setDate(batchedParamIndex++, (java.sql.Date)value);
          break;
        case 7: 
        case 12: 
          batchedStatement.setTimestamp(batchedParamIndex++, (Timestamp)value);
          break;
        case 0: 
        case 15: 
        case 246: 
        case 253: 
        case 254: 
          Object value = value;
          
          if ((value instanceof byte[])) {
            batchedStatement.setBytes(batchedParamIndex, (byte[])value);
          } else {
            batchedStatement.setString(batchedParamIndex, (String)value);
          }
          


          if ((batchedStatement instanceof ServerPreparedStatement)) {
            BindValue asBound = ((ServerPreparedStatement)batchedStatement).getBinding(batchedParamIndex, false);
            bufferType = bufferType;
          }
          
          batchedParamIndex++;
          
          break;
        default: 
          throw new IllegalArgumentException("Unknown type when re-binding parameter into batched statement for parameter index " + batchedParamIndex);
        }
        
      }
    }
    

    return batchedParamIndex;
  }
  
  protected boolean containsOnDuplicateKeyUpdateInSQL()
  {
    return hasOnDuplicateKeyUpdate;
  }
  
  protected PreparedStatement prepareBatchedInsertSQL(MySQLConnection localConn, int numBatches) throws SQLException
  {
    synchronized (checkClosed().getConnectionMutex()) {
      try {
        PreparedStatement pstmt = (PreparedStatement)((Wrapper)localConn.prepareStatement(parseInfo.getSqlForBatch(numBatches), resultSetType, resultSetConcurrency)).unwrap(PreparedStatement.class);
        
        pstmt.setRetrieveGeneratedKeys(retrieveGeneratedKeys);
        
        return pstmt;
      } catch (UnsupportedEncodingException e) {
        SQLException sqlEx = SQLError.createSQLException("Unable to prepare batch statement", "S1000", getExceptionInterceptor());
        
        sqlEx.initCause(e);
        
        throw sqlEx;
      }
    }
  }
  
  public void setPoolable(boolean poolable) throws SQLException
  {
    if (!poolable) {
      connection.decachePreparedStatement(this);
    }
    super.setPoolable(poolable);
  }
}
