package com.mysql.fabric.jdbc;

import com.mysql.fabric.FabricConnection;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.JDBC4ClientInfoProvider;
import com.mysql.jdbc.JDBC4MySQLConnection;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;


































































public class JDBC4FabricMySQLConnectionProxy
  extends FabricMySQLConnectionProxy
  implements JDBC4FabricMySQLConnection, FabricMySQLConnectionProperties
{
  private static final long serialVersionUID = 5845485979107347258L;
  private FabricConnection fabricConnection;
  
  public JDBC4FabricMySQLConnectionProxy(Properties props)
    throws SQLException
  {
    super(props);
  }
  
  public Blob createBlob() {
    try {
      transactionBegun();
      return getActiveConnection().createBlob();
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }
  
  public Clob createClob() {
    try {
      transactionBegun();
      return getActiveConnection().createClob();
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }
  
  public NClob createNClob() {
    try {
      transactionBegun();
      return getActiveConnection().createNClob();
    } catch (SQLException ex) {
      throw new RuntimeException(ex);
    }
  }
  
  public SQLXML createSQLXML() throws SQLException {
    transactionBegun();
    return getActiveConnection().createSQLXML();
  }
  
  public void setClientInfo(Properties properties) throws SQLClientInfoException {
    for (Connection c : serverConnections.values())
      c.setClientInfo(properties);
  }
  
  public void setClientInfo(String name, String value) throws SQLClientInfoException {
    for (Connection c : serverConnections.values())
      c.setClientInfo(name, value);
  }
  
  public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
    return getActiveConnection().createArrayOf(typeName, elements);
  }
  
  public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
    transactionBegun();
    return getActiveConnection().createStruct(typeName, attributes);
  }
  
  public JDBC4ClientInfoProvider getClientInfoProviderImpl() throws SQLException {
    return ((JDBC4MySQLConnection)getActiveConnection()).getClientInfoProviderImpl();
  }
}
