package com.mysql.jdbc.authentication;

import com.mysql.jdbc.AuthenticationPlugin;
import com.mysql.jdbc.Buffer;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.ExportControlled;
import com.mysql.jdbc.Messages;
import com.mysql.jdbc.MySQLConnection;
import com.mysql.jdbc.MysqlIO;
import com.mysql.jdbc.SQLError;
import com.mysql.jdbc.Security;
import com.mysql.jdbc.StringUtils;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
























public class Sha256PasswordPlugin
  implements AuthenticationPlugin
{
  public Sha256PasswordPlugin() {}
  
  public static String PLUGIN_NAME = "sha256_password";
  protected Connection connection;
  
  protected String password = null;
  protected String seed = null;
  protected boolean publicKeyRequested = false;
  protected String publicKeyString = null;
  
  public void init(Connection conn, Properties props) throws SQLException {
    connection = conn;
    
    String pkURL = connection.getServerRSAPublicKeyFile();
    if (pkURL != null) {
      publicKeyString = readRSAKey(connection, pkURL);
    }
  }
  
  public void destroy() {
    password = null;
    seed = null;
    publicKeyRequested = false;
  }
  
  public String getProtocolPluginName() {
    return PLUGIN_NAME;
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
  
  public boolean nextAuthenticationStep(Buffer fromServer, List<Buffer> toServer) throws SQLException {
    toServer.clear();
    
    if ((password == null) || (password.length() == 0) || (fromServer == null))
    {
      Buffer bresp = new Buffer(new byte[] { 0 });
      toServer.add(bresp);
    }
    else if (((MySQLConnection)connection).getIO().isSSLEstablished())
    {
      Buffer bresp;
      try {
        bresp = new Buffer(StringUtils.getBytes(password, connection.getPasswordCharacterEncoding()));
      } catch (UnsupportedEncodingException e) {
        throw SQLError.createSQLException(Messages.getString("Sha256PasswordPlugin.3", new Object[] { connection.getPasswordCharacterEncoding() }), "S1000", null);
      }
      
      bresp.setPosition(bresp.getBufLength());
      int oldBufLength = bresp.getBufLength();
      bresp.writeByte((byte)0);
      bresp.setBufLength(oldBufLength + 1);
      bresp.setPosition(0);
      toServer.add(bresp);
    }
    else if (connection.getServerRSAPublicKeyFile() != null)
    {
      seed = fromServer.readString();
      Buffer bresp = new Buffer(encryptPassword());
      toServer.add(bresp);
    }
    else {
      if (!connection.getAllowPublicKeyRetrieval()) {
        throw SQLError.createSQLException(Messages.getString("Sha256PasswordPlugin.2"), "08001", connection.getExceptionInterceptor());
      }
      


      if ((publicKeyRequested) && (fromServer.getBufLength() > 20))
      {



        publicKeyString = fromServer.readString();
        Buffer bresp = new Buffer(encryptPassword());
        toServer.add(bresp);
        publicKeyRequested = false;
      }
      else {
        seed = fromServer.readString();
        Buffer bresp = new Buffer(new byte[] { 1 });
        toServer.add(bresp);
        publicKeyRequested = true;
      }
    }
    return true;
  }
  
  protected byte[] encryptPassword() throws SQLException {
    return encryptPassword("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
  }
  
  protected byte[] encryptPassword(String transformation) throws SQLException {
    byte[] input = null;
    try {
      input = new byte[] { password != null ? StringUtils.getBytesNullTerminated(password, connection.getPasswordCharacterEncoding()) : 0 };
    }
    catch (UnsupportedEncodingException e) {
      throw SQLError.createSQLException(Messages.getString("Sha256PasswordPlugin.3", new Object[] { connection.getPasswordCharacterEncoding() }), "S1000", null);
    }
    
    byte[] mysqlScrambleBuff = new byte[input.length];
    Security.xorString(input, mysqlScrambleBuff, seed.getBytes(), input.length);
    return ExportControlled.encryptWithRSAPublicKey(mysqlScrambleBuff, ExportControlled.decodeRSAPublicKey(publicKeyString, connection.getExceptionInterceptor()), transformation, connection.getExceptionInterceptor());
  }
  
  private static String readRSAKey(Connection connection, String pkPath)
    throws SQLException
  {
    String res = null;
    byte[] fileBuf = new byte['ࠀ'];
    
    BufferedInputStream fileIn = null;
    try
    {
      File f = new File(pkPath);
      String canonicalPath = f.getCanonicalPath();
      fileIn = new BufferedInputStream(new FileInputStream(canonicalPath));
      
      int bytesRead = 0;
      
      StringBuilder sb = new StringBuilder();
      while ((bytesRead = fileIn.read(fileBuf)) != -1) {
        sb.append(StringUtils.toAsciiString(fileBuf, 0, bytesRead));
      }
      res = sb.toString();
    }
    catch (IOException ioEx)
    {
      if (connection.getParanoid()) {
        throw SQLError.createSQLException(Messages.getString("Sha256PasswordPlugin.0", new Object[] { "" }), "S1009", connection.getExceptionInterceptor());
      }
      
      throw SQLError.createSQLException(Messages.getString("Sha256PasswordPlugin.0", new Object[] { "'" + pkPath + "'" }), "S1009", ioEx, connection.getExceptionInterceptor());
    }
    finally
    {
      if (fileIn != null) {
        try {
          fileIn.close();
        } catch (Exception ex) {
          SQLException sqlEx = SQLError.createSQLException(Messages.getString("Sha256PasswordPlugin.1"), "S1000", ex, connection.getExceptionInterceptor());
          

          throw sqlEx;
        }
      }
    }
    
    return res;
  }
  
  public void reset() {}
}
