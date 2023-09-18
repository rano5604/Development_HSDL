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


























public class JDBC4LoadBalancedMySQLConnection
  extends LoadBalancedMySQLConnection
  implements JDBC4MySQLConnection
{
  public JDBC4LoadBalancedMySQLConnection(LoadBalancedConnectionProxy proxy)
    throws SQLException
  {
    super(proxy);
  }
  
  private JDBC4MySQLConnection getJDBC4Connection() {
    return (JDBC4MySQLConnection)getActiveMySQLConnection();
  }
  
  public SQLXML createSQLXML() throws SQLException {
    return getJDBC4Connection().createSQLXML();
  }
  
  public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
    return getJDBC4Connection().createArrayOf(typeName, elements);
  }
  
  public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
    return getJDBC4Connection().createStruct(typeName, attributes);
  }
  
  public Properties getClientInfo() throws SQLException {
    return getJDBC4Connection().getClientInfo();
  }
  
  public String getClientInfo(String name) throws SQLException {
    return getJDBC4Connection().getClientInfo(name);
  }
  
  public boolean isValid(int timeout) throws SQLException {
    return getJDBC4Connection().isValid(timeout);
  }
  
  public void setClientInfo(Properties properties) throws SQLClientInfoException {
    getJDBC4Connection().setClientInfo(properties);
  }
  
  public void setClientInfo(String name, String value) throws SQLClientInfoException {
    getJDBC4Connection().setClientInfo(name, value);
  }
  
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    checkClosed();
    

    return iface.isInstance(this);
  }
  
  public <T> T unwrap(Class<T> iface) throws SQLException
  {
    try {
      return iface.cast(this);
    } catch (ClassCastException cce) {
      throw SQLError.createSQLException("Unable to unwrap to " + iface.toString(), "S1009", getExceptionInterceptor());
    }
  }
  


  public Blob createBlob()
  {
    return getJDBC4Connection().createBlob();
  }
  


  public Clob createClob()
  {
    return getJDBC4Connection().createClob();
  }
  


  public NClob createNClob()
  {
    return getJDBC4Connection().createNClob();
  }
  
  public JDBC4ClientInfoProvider getClientInfoProviderImpl() throws SQLException {
    synchronized (getThisAsProxy()) {
      return getJDBC4Connection().getClientInfoProviderImpl();
    }
  }
}
