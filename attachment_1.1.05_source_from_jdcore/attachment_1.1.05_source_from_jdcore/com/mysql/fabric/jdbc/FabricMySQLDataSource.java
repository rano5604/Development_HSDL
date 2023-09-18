package com.mysql.fabric.jdbc;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;






















public class FabricMySQLDataSource
  extends MysqlDataSource
  implements FabricMySQLConnectionProperties
{
  private static final long serialVersionUID = 1L;
  private static final Driver driver;
  private String fabricShardKey;
  private String fabricShardTable;
  private String fabricServerGroup;
  
  static
  {
    try
    {
      driver = new FabricMySQLDriver();
    } catch (Exception ex) {
      throw new RuntimeException("Can create driver", ex);
    }
  }
  













  protected Connection getConnection(Properties props)
    throws SQLException
  {
    String jdbcUrlToUse = null;
    
    if (!explicitUrl) {
      StringBuilder jdbcUrl = new StringBuilder("jdbc:mysql:fabric://");
      
      if (hostName != null) {
        jdbcUrl.append(hostName);
      }
      
      jdbcUrl.append(":");
      jdbcUrl.append(port);
      jdbcUrl.append("/");
      
      if (databaseName != null) {
        jdbcUrl.append(databaseName);
      }
      
      jdbcUrlToUse = jdbcUrl.toString();
    } else {
      jdbcUrlToUse = url;
    }
    




    Properties urlProps = ((FabricMySQLDriver)driver).parseFabricURL(jdbcUrlToUse, null);
    urlProps.remove("DBNAME");
    urlProps.remove("HOST");
    urlProps.remove("PORT");
    
    Iterator<Object> keys = urlProps.keySet().iterator();
    
    while (keys.hasNext()) {
      String key = (String)keys.next();
      
      props.setProperty(key, urlProps.getProperty(key));
    }
    
    if (fabricShardKey != null) {
      props.setProperty("fabricShardKey", fabricShardKey);
    }
    if (fabricShardTable != null) {
      props.setProperty("fabricShardTable", fabricShardTable);
    }
    if (fabricServerGroup != null) {
      props.setProperty("fabricServerGroup", fabricServerGroup);
    }
    props.setProperty("fabricProtocol", fabricProtocol);
    if (fabricUsername != null) {
      props.setProperty("fabricUsername", fabricUsername);
    }
    if (fabricPassword != null) {
      props.setProperty("fabricPassword", fabricPassword);
    }
    props.setProperty("fabricReportErrors", Boolean.toString(fabricReportErrors));
    
    return driver.connect(jdbcUrlToUse, props);
  }
  



  private String fabricProtocol = "http";
  private String fabricUsername;
  private String fabricPassword;
  private boolean fabricReportErrors = false;
  
  public void setFabricShardKey(String value) {
    fabricShardKey = value;
  }
  
  public String getFabricShardKey() {
    return fabricShardKey;
  }
  
  public void setFabricShardTable(String value) {
    fabricShardTable = value;
  }
  
  public String getFabricShardTable() {
    return fabricShardTable;
  }
  
  public void setFabricServerGroup(String value) {
    fabricServerGroup = value;
  }
  
  public String getFabricServerGroup() {
    return fabricServerGroup;
  }
  
  public void setFabricProtocol(String value) {
    fabricProtocol = value;
  }
  
  public String getFabricProtocol() {
    return fabricProtocol;
  }
  
  public void setFabricUsername(String value) {
    fabricUsername = value;
  }
  
  public String getFabricUsername() {
    return fabricUsername;
  }
  
  public void setFabricPassword(String value) {
    fabricPassword = value;
  }
  
  public String getFabricPassword() {
    return fabricPassword;
  }
  
  public void setFabricReportErrors(boolean value) {
    fabricReportErrors = value;
  }
  
  public boolean getFabricReportErrors() {
    return fabricReportErrors;
  }
  
  public FabricMySQLDataSource() {}
}
