package com.mysql.jdbc;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Properties;



















public class JDBC4ClientInfoProviderSP
  implements JDBC4ClientInfoProvider
{
  PreparedStatement setClientInfoSp;
  PreparedStatement getClientInfoSp;
  PreparedStatement getClientInfoBulkSp;
  
  public JDBC4ClientInfoProviderSP() {}
  
  public synchronized void initialize(java.sql.Connection conn, Properties configurationProps)
    throws SQLException
  {
    String identifierQuote = conn.getMetaData().getIdentifierQuoteString();
    String setClientInfoSpName = configurationProps.getProperty("clientInfoSetSPName", "setClientInfo");
    String getClientInfoSpName = configurationProps.getProperty("clientInfoGetSPName", "getClientInfo");
    String getClientInfoBulkSpName = configurationProps.getProperty("clientInfoGetBulkSPName", "getClientInfoBulk");
    String clientInfoCatalog = configurationProps.getProperty("clientInfoCatalog", "");
    

    String catalog = "".equals(clientInfoCatalog) ? conn.getCatalog() : clientInfoCatalog;
    
    setClientInfoSp = ((Connection)conn).clientPrepareStatement("CALL " + identifierQuote + catalog + identifierQuote + "." + identifierQuote + setClientInfoSpName + identifierQuote + "(?, ?)");
    

    getClientInfoSp = ((Connection)conn).clientPrepareStatement("CALL" + identifierQuote + catalog + identifierQuote + "." + identifierQuote + getClientInfoSpName + identifierQuote + "(?)");
    

    getClientInfoBulkSp = ((Connection)conn).clientPrepareStatement("CALL " + identifierQuote + catalog + identifierQuote + "." + identifierQuote + getClientInfoBulkSpName + identifierQuote + "()");
  }
  
  public synchronized void destroy() throws SQLException
  {
    if (setClientInfoSp != null) {
      setClientInfoSp.close();
      setClientInfoSp = null;
    }
    
    if (getClientInfoSp != null) {
      getClientInfoSp.close();
      getClientInfoSp = null;
    }
    
    if (getClientInfoBulkSp != null) {
      getClientInfoBulkSp.close();
      getClientInfoBulkSp = null;
    }
  }
  
  public synchronized Properties getClientInfo(java.sql.Connection conn) throws SQLException {
    ResultSet rs = null;
    
    Properties props = new Properties();
    try
    {
      getClientInfoBulkSp.execute();
      
      rs = getClientInfoBulkSp.getResultSet();
      
      while (rs.next()) {
        props.setProperty(rs.getString(1), rs.getString(2));
      }
    } finally {
      if (rs != null) {
        rs.close();
      }
    }
    
    return props;
  }
  
  public synchronized String getClientInfo(java.sql.Connection conn, String name) throws SQLException {
    ResultSet rs = null;
    
    String clientInfo = null;
    try
    {
      getClientInfoSp.setString(1, name);
      getClientInfoSp.execute();
      
      rs = getClientInfoSp.getResultSet();
      
      if (rs.next()) {
        clientInfo = rs.getString(1);
      }
    } finally {
      if (rs != null) {
        rs.close();
      }
    }
    
    return clientInfo;
  }
  
  public synchronized void setClientInfo(java.sql.Connection conn, Properties properties) throws SQLClientInfoException {
    try {
      Enumeration<?> propNames = properties.propertyNames();
      
      while (propNames.hasMoreElements()) {
        String name = (String)propNames.nextElement();
        String value = properties.getProperty(name);
        
        setClientInfo(conn, name, value);
      }
    } catch (SQLException sqlEx) {
      SQLClientInfoException clientInfoEx = new SQLClientInfoException();
      clientInfoEx.initCause(sqlEx);
      
      throw clientInfoEx;
    }
  }
  
  public synchronized void setClientInfo(java.sql.Connection conn, String name, String value) throws SQLClientInfoException {
    try {
      setClientInfoSp.setString(1, name);
      setClientInfoSp.setString(2, value);
      setClientInfoSp.execute();
    } catch (SQLException sqlEx) {
      SQLClientInfoException clientInfoEx = new SQLClientInfoException();
      clientInfoEx.initCause(sqlEx);
      
      throw clientInfoEx;
    }
  }
}
