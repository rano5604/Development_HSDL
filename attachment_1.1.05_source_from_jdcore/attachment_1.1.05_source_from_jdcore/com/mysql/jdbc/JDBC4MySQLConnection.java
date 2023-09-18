package com.mysql.jdbc;

import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Struct;
import java.util.Properties;

public abstract interface JDBC4MySQLConnection
  extends MySQLConnection
{
  public abstract SQLXML createSQLXML()
    throws SQLException;
  
  public abstract Array createArrayOf(String paramString, Object[] paramArrayOfObject)
    throws SQLException;
  
  public abstract Struct createStruct(String paramString, Object[] paramArrayOfObject)
    throws SQLException;
  
  public abstract Properties getClientInfo()
    throws SQLException;
  
  public abstract String getClientInfo(String paramString)
    throws SQLException;
  
  public abstract boolean isValid(int paramInt)
    throws SQLException;
  
  public abstract void setClientInfo(Properties paramProperties)
    throws SQLClientInfoException;
  
  public abstract void setClientInfo(String paramString1, String paramString2)
    throws SQLClientInfoException;
  
  public abstract boolean isWrapperFor(Class<?> paramClass)
    throws SQLException;
  
  public abstract <T> T unwrap(Class<T> paramClass)
    throws SQLException;
  
  public abstract Blob createBlob();
  
  public abstract Clob createClob();
  
  public abstract NClob createNClob();
  
  public abstract JDBC4ClientInfoProvider getClientInfoProviderImpl()
    throws SQLException;
}
