package com.mysql.jdbc;

import com.mysql.jdbc.log.Log;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.Executor;

public abstract interface Connection
  extends java.sql.Connection, ConnectionProperties
{
  public abstract void changeUser(String paramString1, String paramString2)
    throws SQLException;
  
  @Deprecated
  public abstract void clearHasTriedMaster();
  
  public abstract PreparedStatement clientPrepareStatement(String paramString)
    throws SQLException;
  
  public abstract PreparedStatement clientPrepareStatement(String paramString, int paramInt)
    throws SQLException;
  
  public abstract PreparedStatement clientPrepareStatement(String paramString, int paramInt1, int paramInt2)
    throws SQLException;
  
  public abstract PreparedStatement clientPrepareStatement(String paramString, int[] paramArrayOfInt)
    throws SQLException;
  
  public abstract PreparedStatement clientPrepareStatement(String paramString, int paramInt1, int paramInt2, int paramInt3)
    throws SQLException;
  
  public abstract PreparedStatement clientPrepareStatement(String paramString, String[] paramArrayOfString)
    throws SQLException;
  
  public abstract int getActiveStatementCount();
  
  public abstract long getIdleFor();
  
  public abstract Log getLog()
    throws SQLException;
  
  @Deprecated
  public abstract String getServerCharacterEncoding();
  
  public abstract String getServerCharset();
  
  public abstract TimeZone getServerTimezoneTZ();
  
  public abstract String getStatementComment();
  
  @Deprecated
  public abstract boolean hasTriedMaster();
  
  public abstract boolean isInGlobalTx();
  
  public abstract void setInGlobalTx(boolean paramBoolean);
  
  public abstract boolean isMasterConnection();
  
  public abstract boolean isNoBackslashEscapesSet();
  
  public abstract boolean isSameResource(Connection paramConnection);
  
  public abstract boolean lowerCaseTableNames();
  
  public abstract boolean parserKnowsUnicode();
  
  public abstract void ping()
    throws SQLException;
  
  public abstract void resetServerState()
    throws SQLException;
  
  public abstract PreparedStatement serverPrepareStatement(String paramString)
    throws SQLException;
  
  public abstract PreparedStatement serverPrepareStatement(String paramString, int paramInt)
    throws SQLException;
  
  public abstract PreparedStatement serverPrepareStatement(String paramString, int paramInt1, int paramInt2)
    throws SQLException;
  
  public abstract PreparedStatement serverPrepareStatement(String paramString, int paramInt1, int paramInt2, int paramInt3)
    throws SQLException;
  
  public abstract PreparedStatement serverPrepareStatement(String paramString, int[] paramArrayOfInt)
    throws SQLException;
  
  public abstract PreparedStatement serverPrepareStatement(String paramString, String[] paramArrayOfString)
    throws SQLException;
  
  public abstract void setFailedOver(boolean paramBoolean);
  
  @Deprecated
  public abstract void setPreferSlaveDuringFailover(boolean paramBoolean);
  
  public abstract void setStatementComment(String paramString);
  
  public abstract void shutdownServer()
    throws SQLException;
  
  public abstract boolean supportsIsolationLevel();
  
  public abstract boolean supportsQuotedIdentifiers();
  
  public abstract boolean supportsTransactions();
  
  public abstract boolean versionMeetsMinimum(int paramInt1, int paramInt2, int paramInt3)
    throws SQLException;
  
  public abstract void reportQueryTime(long paramLong);
  
  public abstract boolean isAbonormallyLongQuery(long paramLong);
  
  public abstract void initializeExtension(Extension paramExtension)
    throws SQLException;
  
  public abstract int getAutoIncrementIncrement();
  
  public abstract boolean hasSameProperties(Connection paramConnection);
  
  public abstract Properties getProperties();
  
  public abstract String getHost();
  
  public abstract void setProxy(MySQLConnection paramMySQLConnection);
  
  public abstract boolean isServerLocal()
    throws SQLException;
  
  public abstract int getSessionMaxRows();
  
  public abstract void setSessionMaxRows(int paramInt)
    throws SQLException;
  
  public abstract void setSchema(String paramString)
    throws SQLException;
  
  public abstract String getSchema()
    throws SQLException;
  
  public abstract void abort(Executor paramExecutor)
    throws SQLException;
  
  public abstract void setNetworkTimeout(Executor paramExecutor, int paramInt)
    throws SQLException;
  
  public abstract int getNetworkTimeout()
    throws SQLException;
  
  public abstract void abortInternal()
    throws SQLException;
  
  public abstract void checkClosed()
    throws SQLException;
  
  public abstract Object getConnectionMutex();
}
