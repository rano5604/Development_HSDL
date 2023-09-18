package com.mysql.jdbc.jdbc2.optional;

import com.mysql.jdbc.ConnectionPropertiesImpl;
import com.mysql.jdbc.Messages;
import com.mysql.jdbc.NonRegisteringDriver;
import com.mysql.jdbc.SQLError;
import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.sql.DataSource;

























public class MysqlDataSource
  extends ConnectionPropertiesImpl
  implements DataSource, Referenceable, Serializable
{
  static final long serialVersionUID = -5515846944416881264L;
  protected static final NonRegisteringDriver mysqlDriver;
  
  static
  {
    try
    {
      mysqlDriver = new NonRegisteringDriver();
    } catch (Exception E) {
      throw new RuntimeException("Can not load Driver class com.mysql.jdbc.Driver");
    }
  }
  

  protected transient PrintWriter logWriter = null;
  

  protected String databaseName = null;
  

  protected String encoding = null;
  

  protected String hostName = null;
  

  protected String password = null;
  

  protected String profileSql = "false";
  

  protected String url = null;
  

  protected String user = null;
  

  protected boolean explicitUrl = false;
  

  protected int port = 3306;
  













  public Connection getConnection()
    throws SQLException
  {
    return getConnection(user, password);
  }
  











  public Connection getConnection(String userID, String pass)
    throws SQLException
  {
    Properties props = new Properties();
    
    if (userID != null) {
      props.setProperty("user", userID);
    }
    
    if (pass != null) {
      props.setProperty("password", pass);
    }
    
    exposeAsProperties(props);
    
    return getConnection(props);
  }
  





  public void setDatabaseName(String dbName)
  {
    databaseName = dbName;
  }
  




  public String getDatabaseName()
  {
    return databaseName != null ? databaseName : "";
  }
  



  public void setLogWriter(PrintWriter output)
    throws SQLException
  {
    logWriter = output;
  }
  




  public PrintWriter getLogWriter()
  {
    return logWriter;
  }
  












  public int getLoginTimeout()
  {
    return 0;
  }
  





  public void setPassword(String pass)
  {
    password = pass;
  }
  





  public void setPort(int p)
  {
    port = p;
  }
  




  public int getPort()
  {
    return port;
  }
  







  public void setPortNumber(int p)
  {
    setPort(p);
  }
  




  public int getPortNumber()
  {
    return getPort();
  }
  



  public void setPropertiesViaRef(Reference ref)
    throws SQLException
  {
    super.initializeFromRef(ref);
  }
  






  public Reference getReference()
    throws NamingException
  {
    String factoryName = "com.mysql.jdbc.jdbc2.optional.MysqlDataSourceFactory";
    Reference ref = new Reference(getClass().getName(), factoryName, null);
    ref.add(new StringRefAddr("user", getUser()));
    ref.add(new StringRefAddr("password", password));
    ref.add(new StringRefAddr("serverName", getServerName()));
    ref.add(new StringRefAddr("port", "" + getPort()));
    ref.add(new StringRefAddr("databaseName", getDatabaseName()));
    ref.add(new StringRefAddr("url", getUrl()));
    ref.add(new StringRefAddr("explicitUrl", String.valueOf(explicitUrl)));
    


    try
    {
      storeToRef(ref);
    } catch (SQLException sqlEx) {
      throw new NamingException(sqlEx.getMessage());
    }
    
    return ref;
  }
  





  public void setServerName(String serverName)
  {
    hostName = serverName;
  }
  




  public String getServerName()
  {
    return hostName != null ? hostName : "";
  }
  









  public void setURL(String url)
  {
    setUrl(url);
  }
  




  public String getURL()
  {
    return getUrl();
  }
  







  public void setUrl(String url)
  {
    this.url = url;
    explicitUrl = true;
  }
  




  public String getUrl()
  {
    if (!explicitUrl) {
      String builtUrl = "jdbc:mysql://";
      builtUrl = builtUrl + getServerName() + ":" + getPort() + "/" + getDatabaseName();
      
      return builtUrl;
    }
    
    return url;
  }
  





  public void setUser(String userID)
  {
    user = userID;
  }
  




  public String getUser()
  {
    return user;
  }
  









  protected Connection getConnection(Properties props)
    throws SQLException
  {
    String jdbcUrlToUse = null;
    
    if (!explicitUrl) {
      StringBuilder jdbcUrl = new StringBuilder("jdbc:mysql://");
      
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
    




    Properties urlProps = mysqlDriver.parseURL(jdbcUrlToUse, null);
    if (urlProps == null) {
      throw SQLError.createSQLException(Messages.getString("MysqlDataSource.BadUrl", new Object[] { jdbcUrlToUse }), "08006", null);
    }
    
    urlProps.remove("DBNAME");
    urlProps.remove("HOST");
    urlProps.remove("PORT");
    
    Iterator<Object> keys = urlProps.keySet().iterator();
    
    while (keys.hasNext()) {
      String key = (String)keys.next();
      
      props.setProperty(key, urlProps.getProperty(key));
    }
    
    return mysqlDriver.connect(jdbcUrlToUse, props);
  }
  







  public Properties exposeAsProperties(Properties props)
    throws SQLException
  {
    return exposeAsProperties(props, true);
  }
  
  public MysqlDataSource() {}
  
  public void setLoginTimeout(int seconds)
    throws SQLException
  {}
}
