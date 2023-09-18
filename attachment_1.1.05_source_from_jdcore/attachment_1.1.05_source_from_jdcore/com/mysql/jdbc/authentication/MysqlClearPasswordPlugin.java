package com.mysql.jdbc.authentication;

import com.mysql.jdbc.AuthenticationPlugin;
import com.mysql.jdbc.Buffer;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.Messages;
import com.mysql.jdbc.SQLError;
import com.mysql.jdbc.StringUtils;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
























public class MysqlClearPasswordPlugin
  implements AuthenticationPlugin
{
  private Connection connection;
  
  public MysqlClearPasswordPlugin() {}
  
  private String password = null;
  
  public void init(Connection conn, Properties props) throws SQLException {
    connection = conn;
  }
  
  public void destroy() {
    password = null;
  }
  
  public String getProtocolPluginName() {
    return "mysql_clear_password";
  }
  
  public boolean requiresConfidentiality() {
    return true;
  }
  
  public boolean isReusable() {
    return true;
  }
  
  public void setAuthenticationParameters(String user, String password) {
    this.password = password;
  }
  
  public boolean nextAuthenticationStep(Buffer fromServer, List<Buffer> toServer) throws SQLException {
    toServer.clear();
    Buffer bresp;
    try
    {
      String encoding = connection.versionMeetsMinimum(5, 7, 6) ? connection.getPasswordCharacterEncoding() : "UTF-8";
      bresp = new Buffer(StringUtils.getBytes(password != null ? password : "", encoding));
    } catch (UnsupportedEncodingException e) {
      throw SQLError.createSQLException(Messages.getString("MysqlClearPasswordPlugin.1", new Object[] { connection.getPasswordCharacterEncoding() }), "S1000", null);
    }
    

    bresp.setPosition(bresp.getBufLength());
    int oldBufLength = bresp.getBufLength();
    
    bresp.writeByte((byte)0);
    
    bresp.setBufLength(oldBufLength + 1);
    bresp.setPosition(0);
    
    toServer.add(bresp);
    return true;
  }
  
  public void reset() {}
}
