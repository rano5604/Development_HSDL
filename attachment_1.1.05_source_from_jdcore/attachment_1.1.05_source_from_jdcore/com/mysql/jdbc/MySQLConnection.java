package com.mysql.jdbc;

import com.mysql.jdbc.log.Log;
import com.mysql.jdbc.profiler.ProfilerEventHandler;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.Timer;

public abstract interface MySQLConnection
  extends Connection, ConnectionProperties
{
  public abstract boolean isProxySet();
  
  public abstract void createNewIO(boolean paramBoolean)
    throws SQLException;
  
  public abstract void dumpTestcaseQuery(String paramString);
  
  public abstract Connection duplicate()
    throws SQLException;
  
  public abstract ResultSetInternalMethods execSQL(StatementImpl paramStatementImpl, String paramString1, int paramInt1, Buffer paramBuffer, int paramInt2, int paramInt3, boolean paramBoolean, String paramString2, Field[] paramArrayOfField)
    throws SQLException;
  
  public abstract ResultSetInternalMethods execSQL(StatementImpl paramStatementImpl, String paramString1, int paramInt1, Buffer paramBuffer, int paramInt2, int paramInt3, boolean paramBoolean1, String paramString2, Field[] paramArrayOfField, boolean paramBoolean2)
    throws SQLException;
  
  public abstract String extractSqlFromPacket(String paramString, Buffer paramBuffer, int paramInt)
    throws SQLException;
  
  public abstract StringBuilder generateConnectionCommentBlock(StringBuilder paramStringBuilder);
  
  public abstract int getActiveStatementCount();
  
  public abstract int getAutoIncrementIncrement();
  
  public abstract CachedResultSetMetaData getCachedMetaData(String paramString);
  
  public abstract Calendar getCalendarInstanceForSessionOrNew();
  
  public abstract Timer getCancelTimer();
  
  public abstract String getCharacterSetMetadata();
  
  public abstract SingleByteCharsetConverter getCharsetConverter(String paramString)
    throws SQLException;
  
  @Deprecated
  public abstract String getCharsetNameForIndex(int paramInt)
    throws SQLException;
  
  public abstract String getEncodingForIndex(int paramInt)
    throws SQLException;
  
  public abstract TimeZone getDefaultTimeZone();
  
  public abstract String getErrorMessageEncoding();
  
  public abstract ExceptionInterceptor getExceptionInterceptor();
  
  public abstract String getHost();
  
  public abstract String getHostPortPair();
  
  public abstract long getId();
  
  public abstract long getIdleFor();
  
  public abstract MysqlIO getIO()
    throws SQLException;
  
  public abstract Log getLog()
    throws SQLException;
  
  public abstract int getMaxBytesPerChar(String paramString)
    throws SQLException;
  
  public abstract int getMaxBytesPerChar(Integer paramInteger, String paramString)
    throws SQLException;
  
  public abstract java.sql.Statement getMetadataSafeStatement()
    throws SQLException;
  
  public abstract int getNetBufferLength();
  
  public abstract Properties getProperties();
  
  public abstract boolean getRequiresEscapingEncoder();
  
  public abstract String getServerCharset();
  
  public abstract int getServerMajorVersion();
  
  public abstract int getServerMinorVersion();
  
  public abstract int getServerSubMinorVersion();
  
  public abstract TimeZone getServerTimezoneTZ();
  
  public abstract String getServerVariable(String paramString);
  
  public abstract String getServerVersion();
  
  public abstract Calendar getSessionLockedCalendar();
  
  public abstract String getStatementComment();
  
  public abstract List<StatementInterceptorV2> getStatementInterceptorsInstances();
  
  public abstract String getURL();
  
  public abstract String getUser();
  
  public abstract Calendar getUtcCalendar();
  
  public abstract void incrementNumberOfPreparedExecutes();
  
  public abstract void incrementNumberOfPrepares();
  
  public abstract void incrementNumberOfResultSetsCreated();
  
  public abstract void initializeResultsMetadataFromCache(String paramString, CachedResultSetMetaData paramCachedResultSetMetaData, ResultSetInternalMethods paramResultSetInternalMethods)
    throws SQLException;
  
  public abstract void initializeSafeStatementInterceptors()
    throws SQLException;
  
  public abstract boolean isAbonormallyLongQuery(long paramLong);
  
  public abstract boolean isClientTzUTC();
  
  public abstract boolean isCursorFetchEnabled()
    throws SQLException;
  
  public abstract boolean isReadInfoMsgEnabled();
  
  public abstract boolean isReadOnly()
    throws SQLException;
  
  public abstract boolean isReadOnly(boolean paramBoolean)
    throws SQLException;
  
  public abstract boolean isRunningOnJDK13();
  
  public abstract boolean isServerTzUTC();
  
  public abstract boolean lowerCaseTableNames();
  
  public abstract void pingInternal(boolean paramBoolean, int paramInt)
    throws SQLException;
  
  public abstract void realClose(boolean paramBoolean1, boolean paramBoolean2, boolean paramBoolean3, Throwable paramThrowable)
    throws SQLException;
  
  public abstract void recachePreparedStatement(ServerPreparedStatement paramServerPreparedStatement)
    throws SQLException;
  
  public abstract void decachePreparedStatement(ServerPreparedStatement paramServerPreparedStatement)
    throws SQLException;
  
  public abstract void registerQueryExecutionTime(long paramLong);
  
  public abstract void registerStatement(Statement paramStatement);
  
  public abstract void reportNumberOfTablesAccessed(int paramInt);
  
  public abstract boolean serverSupportsConvertFn()
    throws SQLException;
  
  public abstract void setProxy(MySQLConnection paramMySQLConnection);
  
  public abstract void setReadInfoMsgEnabled(boolean paramBoolean);
  
  public abstract void setReadOnlyInternal(boolean paramBoolean)
    throws SQLException;
  
  public abstract void shutdownServer()
    throws SQLException;
  
  public abstract boolean storesLowerCaseTableName();
  
  public abstract void throwConnectionClosedException()
    throws SQLException;
  
  public abstract void transactionBegun()
    throws SQLException;
  
  public abstract void transactionCompleted()
    throws SQLException;
  
  public abstract void unregisterStatement(Statement paramStatement);
  
  public abstract void unSafeStatementInterceptors()
    throws SQLException;
  
  public abstract boolean useAnsiQuotedIdentifiers();
  
  public abstract String getConnectionAttributes()
    throws SQLException;
  
  @Deprecated
  public abstract MySQLConnection getLoadBalanceSafeProxy();
  
  public abstract MySQLConnection getMultiHostSafeProxy();
  
  public abstract MySQLConnection getActiveMySQLConnection();
  
  public abstract ProfilerEventHandler getProfilerEventHandlerInstance();
  
  public abstract void setProfilerEventHandlerInstance(ProfilerEventHandler paramProfilerEventHandler);
}
