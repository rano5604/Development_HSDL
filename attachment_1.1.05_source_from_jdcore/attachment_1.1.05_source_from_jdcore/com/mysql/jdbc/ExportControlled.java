package com.mysql.jdbc;

import com.mysql.jdbc.util.Base64Decoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorResult;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.crypto.Cipher;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;




























public class ExportControlled
{
  private static final String SQL_STATE_BAD_SSL_PARAMS = "08000";
  private static final String TLSv1 = "TLSv1";
  private static final String TLSv1_1 = "TLSv1.1";
  private static final String TLSv1_2 = "TLSv1.2";
  private static final String[] TLS_PROTOCOLS = { "TLSv1.2", "TLSv1.1", "TLSv1" };
  
  protected static boolean enabled()
  {
    return true;
  }
  











  protected static void transformSocketToSSLSocket(MysqlIO mysqlIO)
    throws SQLException
  {
    SocketFactory sslFact = new StandardSSLSocketFactory(getSSLSocketFactoryDefaultOrConfigured(mysqlIO), socketFactory, mysqlConnection);
    try
    {
      mysqlConnection = sslFact.connect(host, port, null);
      
      String[] tryProtocols = null;
      





      String enabledTLSProtocols = connection.getEnabledTLSProtocols();
      if ((enabledTLSProtocols != null) && (enabledTLSProtocols.length() > 0)) {
        tryProtocols = enabledTLSProtocols.split("\\s*,\\s*");
      } else if ((mysqlIO.versionMeetsMinimum(8, 0, 4)) || ((mysqlIO.versionMeetsMinimum(5, 6, 0)) && (Util.isEnterpriseEdition(mysqlIO.getServerVersion()))))
      {
        tryProtocols = TLS_PROTOCOLS;
      }
      else {
        tryProtocols = new String[] { "TLSv1.1", "TLSv1" };
      }
      

      List<String> configuredProtocols = new ArrayList(Arrays.asList(tryProtocols));
      List<String> jvmSupportedProtocols = Arrays.asList(((SSLSocket)mysqlConnection).getSupportedProtocols());
      List<String> allowedProtocols = new ArrayList();
      for (String protocol : TLS_PROTOCOLS) {
        if ((jvmSupportedProtocols.contains(protocol)) && (configuredProtocols.contains(protocol))) {
          allowedProtocols.add(protocol);
        }
      }
      ((SSLSocket)mysqlConnection).setEnabledProtocols((String[])allowedProtocols.toArray(new String[0]));
      

      String enabledSSLCipherSuites = connection.getEnabledSSLCipherSuites();
      boolean overrideCiphers = (enabledSSLCipherSuites != null) && (enabledSSLCipherSuites.length() > 0);
      
      List<String> allowedCiphers = null;
      if (overrideCiphers)
      {

        allowedCiphers = new ArrayList();
        List<String> availableCiphers = Arrays.asList(((SSLSocket)mysqlConnection).getEnabledCipherSuites());
        for (String cipher : enabledSSLCipherSuites.split("\\s*,\\s*")) {
          if (availableCiphers.contains(cipher)) {
            allowedCiphers.add(cipher);
          }
        }
      }
      else
      {
        boolean disableDHAlgorithm = false;
        if (((mysqlIO.versionMeetsMinimum(5, 5, 45)) && (!mysqlIO.versionMeetsMinimum(5, 6, 0))) || ((mysqlIO.versionMeetsMinimum(5, 6, 26)) && (!mysqlIO.versionMeetsMinimum(5, 7, 0))) || (mysqlIO.versionMeetsMinimum(5, 7, 6)))
        {




          if (Util.getJVMVersion() < 8) {
            disableDHAlgorithm = true;
          }
        } else if (Util.getJVMVersion() >= 8)
        {

          disableDHAlgorithm = true;
        }
        
        if (disableDHAlgorithm) {
          allowedCiphers = new ArrayList();
          for (String cipher : ((SSLSocket)mysqlConnection).getEnabledCipherSuites()) {
            if ((!disableDHAlgorithm) || ((cipher.indexOf("_DHE_") <= -1) && (cipher.indexOf("_DH_") <= -1))) {
              allowedCiphers.add(cipher);
            }
          }
        }
      }
      

      if (allowedCiphers != null) {
        ((SSLSocket)mysqlConnection).setEnabledCipherSuites((String[])allowedCiphers.toArray(new String[0]));
      }
      
      ((SSLSocket)mysqlConnection).startHandshake();
      
      if (connection.getUseUnbufferedInput()) {
        mysqlInput = mysqlConnection.getInputStream();
      } else {
        mysqlInput = new BufferedInputStream(mysqlConnection.getInputStream(), 16384);
      }
      
      mysqlOutput = new BufferedOutputStream(mysqlConnection.getOutputStream(), 16384);
      
      mysqlOutput.flush();
      
      socketFactory = sslFact;
    }
    catch (IOException ioEx) {
      throw SQLError.createCommunicationsException(connection, mysqlIO.getLastPacketSentTimeMs(), mysqlIO.getLastPacketReceivedTimeMs(), ioEx, mysqlIO.getExceptionInterceptor());
    }
  }
  
  private ExportControlled() {}
  
  public static class StandardSSLSocketFactory
    implements SocketFactory, SocketMetadata
  {
    private SSLSocket rawSocket = null;
    private final SSLSocketFactory sslFact;
    private final SocketFactory existingSocketFactory;
    private final Socket existingSocket;
    
    public StandardSSLSocketFactory(SSLSocketFactory sslFact, SocketFactory existingSocketFactory, Socket existingSocket) {
      this.sslFact = sslFact;
      this.existingSocketFactory = existingSocketFactory;
      this.existingSocket = existingSocket;
    }
    
    public Socket afterHandshake() throws SocketException, IOException {
      existingSocketFactory.afterHandshake();
      return rawSocket;
    }
    
    public Socket beforeHandshake() throws SocketException, IOException {
      return rawSocket;
    }
    
    public Socket connect(String host, int portNumber, Properties props) throws SocketException, IOException {
      rawSocket = ((SSLSocket)sslFact.createSocket(existingSocket, host, portNumber, true));
      return rawSocket;
    }
    
    public boolean isLocallyConnected(ConnectionImpl conn) throws SQLException {
      return SocketMetadata.Helper.isLocallyConnected(conn);
    }
  }
  






  public static class X509TrustManagerWrapper
    implements X509TrustManager
  {
    private X509TrustManager origTm = null;
    private boolean verifyServerCert = false;
    private CertificateFactory certFactory = null;
    private PKIXParameters validatorParams = null;
    private CertPathValidator validator = null;
    
    public X509TrustManagerWrapper(X509TrustManager tm, boolean verifyServerCertificate) throws CertificateException {
      origTm = tm;
      verifyServerCert = verifyServerCertificate;
      
      if (verifyServerCertificate) {
        try {
          Set<TrustAnchor> anch = new HashSet();
          for (X509Certificate cert : tm.getAcceptedIssuers()) {
            anch.add(new TrustAnchor(cert, null));
          }
          validatorParams = new PKIXParameters(anch);
          validatorParams.setRevocationEnabled(false);
          validator = CertPathValidator.getInstance("PKIX");
          certFactory = CertificateFactory.getInstance("X.509");
        } catch (Exception e) {
          throw new CertificateException(e);
        }
      }
    }
    
    public X509TrustManagerWrapper() {}
    
    public X509Certificate[] getAcceptedIssuers()
    {
      return origTm != null ? origTm.getAcceptedIssuers() : new X509Certificate[0];
    }
    
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
      for (int i = 0; i < chain.length; i++) {
        chain[i].checkValidity();
      }
      
      if (validatorParams != null)
      {
        X509CertSelector certSelect = new X509CertSelector();
        certSelect.setSerialNumber(chain[0].getSerialNumber());
        try
        {
          CertPath certPath = certFactory.generateCertPath(Arrays.asList(chain));
          
          CertPathValidatorResult result = validator.validate(certPath, validatorParams);
          
          ((PKIXCertPathValidatorResult)result).getTrustAnchor().getTrustedCert().checkValidity();
        }
        catch (InvalidAlgorithmParameterException e) {
          throw new CertificateException(e);
        } catch (CertPathValidatorException e) {
          throw new CertificateException(e);
        }
      }
      
      if (verifyServerCert) {
        origTm.checkServerTrusted(chain, authType);
      }
    }
    
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
      origTm.checkClientTrusted(chain, authType);
    }
  }
  
  private static SSLSocketFactory getSSLSocketFactoryDefaultOrConfigured(MysqlIO mysqlIO) throws SQLException {
    String clientCertificateKeyStoreUrl = connection.getClientCertificateKeyStoreUrl();
    String clientCertificateKeyStorePassword = connection.getClientCertificateKeyStorePassword();
    String clientCertificateKeyStoreType = connection.getClientCertificateKeyStoreType();
    String trustCertificateKeyStoreUrl = connection.getTrustCertificateKeyStoreUrl();
    String trustCertificateKeyStorePassword = connection.getTrustCertificateKeyStorePassword();
    String trustCertificateKeyStoreType = connection.getTrustCertificateKeyStoreType();
    
    if (StringUtils.isNullOrEmpty(clientCertificateKeyStoreUrl)) {
      clientCertificateKeyStoreUrl = System.getProperty("javax.net.ssl.keyStore");
      clientCertificateKeyStorePassword = System.getProperty("javax.net.ssl.keyStorePassword");
      clientCertificateKeyStoreType = System.getProperty("javax.net.ssl.keyStoreType");
      if (StringUtils.isNullOrEmpty(clientCertificateKeyStoreType)) {
        clientCertificateKeyStoreType = "JKS";
      }
      
      if (!StringUtils.isNullOrEmpty(clientCertificateKeyStoreUrl)) {
        try {
          new URL(clientCertificateKeyStoreUrl);
        } catch (MalformedURLException e) {
          clientCertificateKeyStoreUrl = "file:" + clientCertificateKeyStoreUrl;
        }
      }
    }
    
    if (StringUtils.isNullOrEmpty(trustCertificateKeyStoreUrl)) {
      trustCertificateKeyStoreUrl = System.getProperty("javax.net.ssl.trustStore");
      trustCertificateKeyStorePassword = System.getProperty("javax.net.ssl.trustStorePassword");
      trustCertificateKeyStoreType = System.getProperty("javax.net.ssl.trustStoreType");
      if (StringUtils.isNullOrEmpty(trustCertificateKeyStoreType)) {
        trustCertificateKeyStoreType = "JKS";
      }
      
      if (!StringUtils.isNullOrEmpty(trustCertificateKeyStoreUrl)) {
        try {
          new URL(trustCertificateKeyStoreUrl);
        } catch (MalformedURLException e) {
          trustCertificateKeyStoreUrl = "file:" + trustCertificateKeyStoreUrl;
        }
      }
    }
    
    TrustManagerFactory tmf = null;
    KeyManagerFactory kmf = null;
    
    KeyManager[] kms = null;
    List<TrustManager> tms = new ArrayList();
    try
    {
      tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    } catch (NoSuchAlgorithmException nsae) {
      throw SQLError.createSQLException("Default algorithm definitions for TrustManager and/or KeyManager are invalid.  Check java security properties file.", "08000", 0, false, mysqlIO.getExceptionInterceptor());
    }
    


    if (!StringUtils.isNullOrEmpty(clientCertificateKeyStoreUrl)) {
      InputStream ksIS = null;
      try {
        if (!StringUtils.isNullOrEmpty(clientCertificateKeyStoreType)) {
          KeyStore clientKeyStore = KeyStore.getInstance(clientCertificateKeyStoreType);
          URL ksURL = new URL(clientCertificateKeyStoreUrl);
          char[] password = clientCertificateKeyStorePassword == null ? new char[0] : clientCertificateKeyStorePassword.toCharArray();
          ksIS = ksURL.openStream();
          clientKeyStore.load(ksIS, password);
          kmf.init(clientKeyStore, password);
          kms = kmf.getKeyManagers();
        }
      } catch (UnrecoverableKeyException uke) {
        throw SQLError.createSQLException("Could not recover keys from client keystore.  Check password?", "08000", 0, false, mysqlIO.getExceptionInterceptor());
      }
      catch (NoSuchAlgorithmException nsae) {
        throw SQLError.createSQLException("Unsupported keystore algorithm [" + nsae.getMessage() + "]", "08000", 0, false, mysqlIO.getExceptionInterceptor());
      }
      catch (KeyStoreException kse) {
        throw SQLError.createSQLException("Could not create KeyStore instance [" + kse.getMessage() + "]", "08000", 0, false, mysqlIO.getExceptionInterceptor());
      }
      catch (CertificateException nsae) {
        throw SQLError.createSQLException("Could not load client" + clientCertificateKeyStoreType + " keystore from " + clientCertificateKeyStoreUrl, mysqlIO.getExceptionInterceptor());
      }
      catch (MalformedURLException mue) {
        throw SQLError.createSQLException(clientCertificateKeyStoreUrl + " does not appear to be a valid URL.", "08000", 0, false, mysqlIO.getExceptionInterceptor());
      }
      catch (IOException ioe) {
        SQLException sqlEx = SQLError.createSQLException("Cannot open " + clientCertificateKeyStoreUrl + " [" + ioe.getMessage() + "]", "08000", 0, false, mysqlIO.getExceptionInterceptor());
        
        sqlEx.initCause(ioe);
        
        throw sqlEx;
      } finally {
        if (ksIS != null) {
          try {
            ksIS.close();
          }
          catch (IOException e) {}
        }
      }
    }
    

    InputStream trustStoreIS = null;
    try {
      KeyStore trustKeyStore = null;
      
      if ((!StringUtils.isNullOrEmpty(trustCertificateKeyStoreUrl)) && (!StringUtils.isNullOrEmpty(trustCertificateKeyStoreType))) {
        trustStoreIS = new URL(trustCertificateKeyStoreUrl).openStream();
        char[] trustStorePassword = trustCertificateKeyStorePassword == null ? new char[0] : trustCertificateKeyStorePassword.toCharArray();
        
        trustKeyStore = KeyStore.getInstance(trustCertificateKeyStoreType);
        trustKeyStore.load(trustStoreIS, trustStorePassword);
      }
      
      tmf.init(trustKeyStore);
      

      TrustManager[] origTms = tmf.getTrustManagers();
      boolean verifyServerCert = connection.getVerifyServerCertificate();
      
      for (TrustManager tm : origTms)
      {
        tms.add((tm instanceof X509TrustManager) ? new X509TrustManagerWrapper((X509TrustManager)tm, verifyServerCert) : tm);
      }
    }
    catch (MalformedURLException e) {
      throw SQLError.createSQLException(trustCertificateKeyStoreUrl + " does not appear to be a valid URL.", "08000", 0, false, mysqlIO.getExceptionInterceptor());
    }
    catch (KeyStoreException e) {
      throw SQLError.createSQLException("Could not create KeyStore instance [" + e.getMessage() + "]", "08000", 0, false, mysqlIO.getExceptionInterceptor());
    }
    catch (NoSuchAlgorithmException e) {
      throw SQLError.createSQLException("Unsupported keystore algorithm [" + e.getMessage() + "]", "08000", 0, false, mysqlIO.getExceptionInterceptor());
    }
    catch (CertificateException e) {
      throw SQLError.createSQLException("Could not load trust" + trustCertificateKeyStoreType + " keystore from " + trustCertificateKeyStoreUrl, "08000", 0, false, mysqlIO.getExceptionInterceptor());
    }
    catch (IOException e) {
      SQLException sqlEx = SQLError.createSQLException("Cannot open " + trustCertificateKeyStoreType + " [" + e.getMessage() + "]", "08000", 0, false, mysqlIO.getExceptionInterceptor());
      
      sqlEx.initCause(e);
      throw sqlEx;
    } finally {
      if (trustStoreIS != null) {
        try {
          trustStoreIS.close();
        }
        catch (IOException e) {}
      }
    }
    


    if (tms.size() == 0) {
      tms.add(new X509TrustManagerWrapper());
    }
    try
    {
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(kms, (TrustManager[])tms.toArray(new TrustManager[tms.size()]), null);
      return sslContext.getSocketFactory();
    }
    catch (NoSuchAlgorithmException nsae) {
      throw SQLError.createSQLException("TLS is not a valid SSL protocol.", "08000", 0, false, mysqlIO.getExceptionInterceptor());
    } catch (KeyManagementException kme) {
      throw SQLError.createSQLException("KeyManagementException: " + kme.getMessage(), "08000", 0, false, mysqlIO.getExceptionInterceptor());
    }
  }
  
  public static boolean isSSLEstablished(MysqlIO mysqlIO)
  {
    return SSLSocket.class.isAssignableFrom(mysqlConnection.getClass());
  }
  
  public static RSAPublicKey decodeRSAPublicKey(String key, ExceptionInterceptor interceptor) throws SQLException
  {
    try {
      if (key == null) {
        throw new SQLException("key parameter is null");
      }
      
      int offset = key.indexOf("\n") + 1;
      int len = key.indexOf("-----END PUBLIC KEY-----") - offset;
      

      byte[] certificateData = Base64Decoder.decode(key.getBytes(), offset, len);
      
      X509EncodedKeySpec spec = new X509EncodedKeySpec(certificateData);
      KeyFactory kf = KeyFactory.getInstance("RSA");
      return (RSAPublicKey)kf.generatePublic(spec);
    } catch (Exception ex) {
      throw SQLError.createSQLException("Unable to decode public key", "S1009", ex, interceptor);
    }
  }
  
  public static byte[] encryptWithRSAPublicKey(byte[] source, RSAPublicKey key, String transformation, ExceptionInterceptor interceptor) throws SQLException {
    try {
      Cipher cipher = Cipher.getInstance(transformation);
      cipher.init(1, key);
      return cipher.doFinal(source);
    } catch (Exception ex) {
      throw SQLError.createSQLException(ex.getMessage(), "S1009", ex, interceptor);
    }
  }
}
