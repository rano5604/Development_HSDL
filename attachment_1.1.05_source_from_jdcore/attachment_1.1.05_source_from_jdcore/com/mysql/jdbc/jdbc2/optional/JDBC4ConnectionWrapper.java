package com.mysql.jdbc.jdbc2.optional;

import com.mysql.jdbc.SQLError;
import java.lang.reflect.Proxy;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Struct;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;










































public class JDBC4ConnectionWrapper
  extends ConnectionWrapper
{
  public JDBC4ConnectionWrapper(MysqlPooledConnection mysqlPooledConnection, com.mysql.jdbc.Connection mysqlConnection, boolean forXa)
    throws SQLException
  {
    super(mysqlPooledConnection, mysqlConnection, forXa);
  }
  
  public void close() throws SQLException {
    try {
      super.close();
      
      unwrappedInterfaces = null; } finally { unwrappedInterfaces = null;
    }
  }
  
  public SQLXML createSQLXML() throws SQLException {
    checkClosed();
    try
    {
      return mc.createSQLXML();
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  
  public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
    checkClosed();
    try
    {
      return mc.createArrayOf(typeName, elements);
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  
  public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
    checkClosed();
    try
    {
      return mc.createStruct(typeName, attributes);
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  
  public Properties getClientInfo() throws SQLException {
    checkClosed();
    try
    {
      return mc.getClientInfo();
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  
  public String getClientInfo(String name) throws SQLException {
    checkClosed();
    try
    {
      return mc.getClientInfo(name);
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  



















  public synchronized boolean isValid(int timeout)
    throws SQLException
  {
    try
    {
      return mc.isValid(timeout);
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return false;
  }
  
  public void setClientInfo(Properties properties) throws SQLClientInfoException {
    try {
      checkClosed();
      
      mc.setClientInfo(properties);
    } catch (SQLException sqlException) {
      try {
        checkAndFireConnectionError(sqlException);
      } catch (SQLException sqlEx2) {
        SQLClientInfoException clientEx = new SQLClientInfoException();
        clientEx.initCause(sqlEx2);
        
        throw clientEx;
      }
    }
  }
  
  public void setClientInfo(String name, String value) throws SQLClientInfoException {
    try {
      checkClosed();
      
      mc.setClientInfo(name, value);
    } catch (SQLException sqlException) {
      try {
        checkAndFireConnectionError(sqlException);
      } catch (SQLException sqlEx2) {
        SQLClientInfoException clientEx = new SQLClientInfoException();
        clientEx.initCause(sqlEx2);
        
        throw clientEx;
      }
    }
  }
  


















  public boolean isWrapperFor(Class<?> iface)
    throws SQLException
  {
    checkClosed();
    
    boolean isInstance = iface.isInstance(this);
    
    if (isInstance) {
      return true;
    }
    
    return (iface.getName().equals("com.mysql.jdbc.Connection")) || (iface.getName().equals("com.mysql.jdbc.ConnectionProperties"));
  }
  
















  public synchronized <T> T unwrap(Class<T> iface)
    throws SQLException
  {
    try
    {
      if (("java.sql.Connection".equals(iface.getName())) || ("java.sql.Wrapper.class".equals(iface.getName()))) {
        return iface.cast(this);
      }
      
      if (unwrappedInterfaces == null) {
        unwrappedInterfaces = new HashMap();
      }
      
      Object cachedUnwrapped = unwrappedInterfaces.get(iface);
      
      if (cachedUnwrapped == null) {
        cachedUnwrapped = Proxy.newProxyInstance(mc.getClass().getClassLoader(), new Class[] { iface }, new WrapperBase.ConnectionErrorFiringInvocationHandler(this, mc));
        
        unwrappedInterfaces.put(iface, cachedUnwrapped);
      }
      
      return iface.cast(cachedUnwrapped);
    } catch (ClassCastException cce) {
      throw SQLError.createSQLException("Unable to unwrap to " + iface.toString(), "S1009", exceptionInterceptor);
    }
  }
  

  public Blob createBlob()
    throws SQLException
  {
    checkClosed();
    try
    {
      return mc.createBlob();
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  

  public Clob createClob()
    throws SQLException
  {
    checkClosed();
    try
    {
      return mc.createClob();
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
  

  public NClob createNClob()
    throws SQLException
  {
    checkClosed();
    try
    {
      return mc.createNClob();
    } catch (SQLException sqlException) {
      checkAndFireConnectionError(sqlException);
    }
    
    return null;
  }
}
