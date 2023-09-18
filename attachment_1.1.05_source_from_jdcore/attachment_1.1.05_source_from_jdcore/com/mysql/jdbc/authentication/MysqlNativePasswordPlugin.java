package com.mysql.jdbc.authentication;

import com.mysql.jdbc.AuthenticationPlugin;
import com.mysql.jdbc.Buffer;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.Messages;
import com.mysql.jdbc.SQLError;
import com.mysql.jdbc.Security;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
























public class MysqlNativePasswordPlugin
  implements AuthenticationPlugin
{
  private Connection connection;
  
  public MysqlNativePasswordPlugin() {}
  
  private String password = null;
  
  public void init(Connection conn, Properties props) throws SQLException {
    connection = conn;
  }
  
  public void destroy() {
    password = null;
  }
  
  public String getProtocolPluginName() {
    return "mysql_native_password";
  }
  
  public boolean requiresConfidentiality() {
    return false;
  }
  
  public boolean isReusable() {
    return true;
  }
  
  public void setAuthenticationParameters(String user, String password) {
    this.password = password;
  }
  
  public boolean nextAuthenticationStep(Buffer fromServer, List<Buffer> toServer) throws SQLException
  {
    try {
      toServer.clear();
      
      Buffer bresp = null;
      
      String pwd = password;
      
      if ((fromServer == null) || (pwd == null) || (pwd.length() == 0)) {
        bresp = new Buffer(new byte[0]);
      } else {
        bresp = new Buffer(Security.scramble411(pwd, fromServer.readString(), connection.getPasswordCharacterEncoding()));
      }
      toServer.add(bresp);
    }
    catch (NoSuchAlgorithmException nse) {
      throw SQLError.createSQLException(Messages.getString("MysqlIO.91") + Messages.getString("MysqlIO.92"), "S1000", null);
    } catch (UnsupportedEncodingException e) {
      throw SQLError.createSQLException(Messages.getString("MysqlNativePasswordPlugin.1", new Object[] { connection.getPasswordCharacterEncoding() }), "S1000", null);
    }
    


    return true;
  }
  
  public void reset() {}
}
