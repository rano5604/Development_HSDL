package com.mysql.jdbc;

import java.sql.Array;
import java.sql.NClob;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Struct;
import java.util.Properties;



























public class JDBC4Connection
  extends ConnectionImpl
  implements JDBC4MySQLConnection
{
  private static final long serialVersionUID = 2877471301981509475L;
  private JDBC4ClientInfoProvider infoProvider;
  
  public JDBC4Connection(String hostToConnectTo, int portToConnectTo, Properties info, String databaseToConnectTo, String url)
    throws SQLException
  {
    super(hostToConnectTo, portToConnectTo, info, databaseToConnectTo, url);
  }
  
  public SQLXML createSQLXML() throws SQLException {
    return new JDBC4MysqlSQLXML(getExceptionInterceptor());
  }
  
  public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
    throw SQLError.createSQLFeatureNotSupportedException();
  }
  
  public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
    throw SQLError.createSQLFeatureNotSupportedException();
  }
  
  public Properties getClientInfo() throws SQLException {
    return getClientInfoProviderImpl().getClientInfo(this);
  }
  
  public String getClientInfo(String name) throws SQLException {
    return getClientInfoProviderImpl().getClientInfo(this, name);
  }
  



















  public boolean isValid(int timeout)
    throws SQLException
  {
    synchronized (getConnectionMutex()) {
      if (isClosed()) {
        return false;
      }
      try
      {
        try {
          pingInternal(false, timeout * 1000);
        } catch (Throwable t) {
          try {
            abortInternal();
          }
          catch (Throwable localThrowable1) {}
          

          return false;
        }
      }
      catch (Throwable t) {
        return false;
      }
      
      return true;
    }
  }
  
  public void setClientInfo(Properties properties) throws SQLClientInfoException {
    try {
      getClientInfoProviderImpl().setClientInfo(this, properties);
    } catch (SQLClientInfoException ciEx) {
      throw ciEx;
    } catch (SQLException sqlEx) {
      SQLClientInfoException clientInfoEx = new SQLClientInfoException();
      clientInfoEx.initCause(sqlEx);
      
      throw clientInfoEx;
    }
  }
  
  public void setClientInfo(String name, String value) throws SQLClientInfoException {
    try {
      getClientInfoProviderImpl().setClientInfo(this, name, value);
    } catch (SQLClientInfoException ciEx) {
      throw ciEx;
    } catch (SQLException sqlEx) {
      SQLClientInfoException clientInfoEx = new SQLClientInfoException();
      clientInfoEx.initCause(sqlEx);
      
      throw clientInfoEx;
    }
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
  


  public java.sql.Blob createBlob()
  {
    return new Blob(getExceptionInterceptor());
  }
  


  public java.sql.Clob createClob()
  {
    return new Clob(getExceptionInterceptor());
  }
  


  public NClob createNClob()
  {
    return new JDBC4NClob(getExceptionInterceptor());
  }
  
  public JDBC4ClientInfoProvider getClientInfoProviderImpl() throws SQLException {
    synchronized (getConnectionMutex()) {
      if (infoProvider == null) {
        try {
          try {
            infoProvider = ((JDBC4ClientInfoProvider)Util.getInstance(getClientInfoProvider(), new Class[0], new Object[0], 
              getExceptionInterceptor()));
          } catch (SQLException sqlEx) {
            if ((sqlEx.getCause() instanceof ClassCastException))
            {
              infoProvider = ((JDBC4ClientInfoProvider)Util.getInstance("com.mysql.jdbc." + getClientInfoProvider(), new Class[0], new Object[0], 
                getExceptionInterceptor()));
            }
          }
        } catch (ClassCastException cce) {
          throw SQLError.createSQLException(Messages.getString("JDBC4Connection.ClientInfoNotImplemented", new Object[] { getClientInfoProvider() }), "S1009", 
            getExceptionInterceptor());
        }
        
        infoProvider.initialize(this, props);
      }
      
      return infoProvider;
    }
  }
}
