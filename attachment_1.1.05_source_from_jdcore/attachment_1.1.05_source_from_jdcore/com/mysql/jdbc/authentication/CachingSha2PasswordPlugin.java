package com.mysql.jdbc.authentication;

import com.mysql.jdbc.Buffer;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.Messages;
import com.mysql.jdbc.MySQLConnection;
import com.mysql.jdbc.MysqlIO;
import com.mysql.jdbc.SQLError;
import com.mysql.jdbc.Security;
import com.mysql.jdbc.StringUtils;
import java.io.UnsupportedEncodingException;
import java.security.DigestException;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;























public class CachingSha2PasswordPlugin
  extends Sha256PasswordPlugin
{
  public static String PLUGIN_NAME = "caching_sha2_password";
  public CachingSha2PasswordPlugin() {}
  
  public static enum AuthStage { FAST_AUTH_SEND_SCRAMBLE,  FAST_AUTH_READ_RESULT,  FAST_AUTH_COMPLETE,  FULL_AUTH;
    
    private AuthStage() {} }
  private AuthStage stage = AuthStage.FAST_AUTH_SEND_SCRAMBLE;
  
  public void init(Connection conn, Properties props) throws SQLException
  {
    super.init(conn, props);
    stage = AuthStage.FAST_AUTH_SEND_SCRAMBLE;
  }
  
  public void destroy()
  {
    stage = AuthStage.FAST_AUTH_SEND_SCRAMBLE;
    super.destroy();
  }
  
  public String getProtocolPluginName()
  {
    return PLUGIN_NAME;
  }
  
  public boolean nextAuthenticationStep(Buffer fromServer, List<Buffer> toServer) throws SQLException
  {
    toServer.clear();
    
    if ((password == null) || (password.length() == 0) || (fromServer == null))
    {
      Buffer bresp = new Buffer(new byte[] { 0 });
      toServer.add(bresp);
    }
    else {
      if (stage == AuthStage.FAST_AUTH_SEND_SCRAMBLE)
      {
        seed = fromServer.readString();
        try {
          toServer.add(new Buffer(Security.scrambleCachingSha2(StringUtils.getBytes(password, connection.getPasswordCharacterEncoding()), seed.getBytes())));
        }
        catch (DigestException e) {
          throw SQLError.createSQLException(e.getMessage(), "S1000", e, null);
        } catch (UnsupportedEncodingException e) {
          throw SQLError.createSQLException(e.getMessage(), "S1000", e, null);
        }
        stage = AuthStage.FAST_AUTH_READ_RESULT;
        return true;
      }
      if (stage == AuthStage.FAST_AUTH_READ_RESULT) {
        int fastAuthResult = fromServer.getByteBuffer()[0];
        switch (fastAuthResult) {
        case 3: 
          stage = AuthStage.FAST_AUTH_COMPLETE;
          return true;
        case 4: 
          stage = AuthStage.FULL_AUTH;
          break;
        default: 
          throw SQLError.createSQLException("Unknown server response after fast auth.", "08001", connection.getExceptionInterceptor());
        }
        
      }
      
      if (((MySQLConnection)connection).getIO().isSSLEstablished())
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
          Buffer bresp = new Buffer(new byte[] { 2 });
          toServer.add(bresp);
          publicKeyRequested = true;
        }
      }
    }
    return true;
  }
  
  protected byte[] encryptPassword() throws SQLException
  {
    if (connection.versionMeetsMinimum(8, 0, 5)) {
      return super.encryptPassword();
    }
    return super.encryptPassword("RSA/ECB/PKCS1Padding");
  }
  
  public void reset()
  {
    stage = AuthStage.FAST_AUTH_SEND_SCRAMBLE;
  }
}
