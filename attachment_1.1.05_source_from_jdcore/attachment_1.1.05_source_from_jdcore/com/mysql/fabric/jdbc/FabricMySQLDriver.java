package com.mysql.fabric.jdbc;

import com.mysql.jdbc.NonRegisteringDriver;
import com.mysql.jdbc.Util;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;



























public class FabricMySQLDriver
  extends NonRegisteringDriver
  implements Driver
{
  public static final String FABRIC_URL_PREFIX = "jdbc:mysql:fabric://";
  public static final String FABRIC_SHARD_KEY_PROPERTY_KEY = "fabricShardKey";
  public static final String FABRIC_SHARD_TABLE_PROPERTY_KEY = "fabricShardTable";
  public static final String FABRIC_SERVER_GROUP_PROPERTY_KEY = "fabricServerGroup";
  public static final String FABRIC_PROTOCOL_PROPERTY_KEY = "fabricProtocol";
  public static final String FABRIC_USERNAME_PROPERTY_KEY = "fabricUsername";
  public static final String FABRIC_PASSWORD_PROPERTY_KEY = "fabricPassword";
  public static final String FABRIC_REPORT_ERRORS_PROPERTY_KEY = "fabricReportErrors";
  
  static
  {
    try
    {
      DriverManager.registerDriver(new FabricMySQLDriver());
    } catch (SQLException ex) {
      throw new RuntimeException("Can't register driver", ex);
    }
  }
  


  public Connection connect(String url, Properties info)
    throws SQLException
  {
    Properties parsedProps = parseFabricURL(url, info);
    
    if (parsedProps == null) {
      return null;
    }
    
    parsedProps.setProperty("fabricProtocol", "http");
    if (Util.isJdbc4()) {
      try {
        Constructor<?> jdbc4proxy = Class.forName("com.mysql.fabric.jdbc.JDBC4FabricMySQLConnectionProxy").getConstructor(new Class[] { Properties.class });
        
        return (Connection)Util.handleNewInstance(jdbc4proxy, new Object[] { parsedProps }, null);
      } catch (Exception e) {
        throw ((SQLException)new SQLException(e.getMessage()).initCause(e));
      }
    }
    
    return new FabricMySQLConnectionProxy(parsedProps);
  }
  



  public boolean acceptsURL(String url)
    throws SQLException
  {
    return parseFabricURL(url, null) != null;
  }
  
  Properties parseFabricURL(String url, Properties defaults) throws SQLException {
    if (!url.startsWith("jdbc:mysql:fabric://")) {
      return null;
    }
    


    return super.parseURL(url.replaceAll("fabric:", ""), defaults);
  }
  
  public Logger getParentLogger() throws SQLException {
    throw new SQLException("no logging");
  }
  
  public FabricMySQLDriver()
    throws SQLException
  {}
}
