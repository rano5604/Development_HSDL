package com.mysql.jdbc;

import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

























public class JDBC4CommentClientInfoProvider
  implements JDBC4ClientInfoProvider
{
  private Properties clientInfo;
  
  public JDBC4CommentClientInfoProvider() {}
  
  public synchronized void initialize(java.sql.Connection conn, Properties configurationProps)
    throws SQLException
  {
    clientInfo = new Properties();
  }
  
  public synchronized void destroy() throws SQLException {
    clientInfo = null;
  }
  
  public synchronized Properties getClientInfo(java.sql.Connection conn) throws SQLException {
    return clientInfo;
  }
  
  public synchronized String getClientInfo(java.sql.Connection conn, String name) throws SQLException {
    return clientInfo.getProperty(name);
  }
  
  public synchronized void setClientInfo(java.sql.Connection conn, Properties properties) throws SQLClientInfoException {
    clientInfo = new Properties();
    
    Enumeration<?> propNames = properties.propertyNames();
    
    while (propNames.hasMoreElements()) {
      String name = (String)propNames.nextElement();
      
      clientInfo.put(name, properties.getProperty(name));
    }
    
    setComment(conn);
  }
  
  public synchronized void setClientInfo(java.sql.Connection conn, String name, String value) throws SQLClientInfoException {
    clientInfo.setProperty(name, value);
    setComment(conn);
  }
  
  private synchronized void setComment(java.sql.Connection conn) {
    StringBuilder commentBuf = new StringBuilder();
    Iterator<Map.Entry<Object, Object>> elements = clientInfo.entrySet().iterator();
    
    while (elements.hasNext()) {
      if (commentBuf.length() > 0) {
        commentBuf.append(", ");
      }
      
      Map.Entry<Object, Object> entry = (Map.Entry)elements.next();
      commentBuf.append("" + entry.getKey());
      commentBuf.append("=");
      commentBuf.append("" + entry.getValue());
    }
    
    ((Connection)conn).setStatementComment(commentBuf.toString());
  }
}
