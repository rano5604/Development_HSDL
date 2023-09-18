package com.mysql.jdbc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;





































public class StandardSocketFactory
  implements SocketFactory, SocketMetadata
{
  public static final String TCP_NO_DELAY_PROPERTY_NAME = "tcpNoDelay";
  public static final String TCP_KEEP_ALIVE_DEFAULT_VALUE = "true";
  public static final String TCP_KEEP_ALIVE_PROPERTY_NAME = "tcpKeepAlive";
  public static final String TCP_RCV_BUF_PROPERTY_NAME = "tcpRcvBuf";
  public static final String TCP_SND_BUF_PROPERTY_NAME = "tcpSndBuf";
  public static final String TCP_TRAFFIC_CLASS_PROPERTY_NAME = "tcpTrafficClass";
  public static final String TCP_RCV_BUF_DEFAULT_VALUE = "0";
  public static final String TCP_SND_BUF_DEFAULT_VALUE = "0";
  public static final String TCP_TRAFFIC_CLASS_DEFAULT_VALUE = "0";
  public static final String TCP_NO_DELAY_DEFAULT_VALUE = "true";
  protected String host = null;
  

  protected int port = 3306;
  

  protected Socket rawSocket = null;
  

  protected int loginTimeoutCountdown = DriverManager.getLoginTimeout() * 1000;
  

  protected long loginTimeoutCheckTimestamp = System.currentTimeMillis();
  

  protected int socketTimeoutBackup = 0;
  




  public StandardSocketFactory() {}
  



  public Socket afterHandshake()
    throws SocketException, IOException
  {
    resetLoginTimeCountdown();
    rawSocket.setSoTimeout(socketTimeoutBackup);
    return rawSocket;
  }
  









  public Socket beforeHandshake()
    throws SocketException, IOException
  {
    resetLoginTimeCountdown();
    socketTimeoutBackup = rawSocket.getSoTimeout();
    rawSocket.setSoTimeout(getRealTimeout(socketTimeoutBackup));
    return rawSocket;
  }
  





  protected Socket createSocket(Properties props)
  {
    return new Socket();
  }
  






  private void configureSocket(Socket sock, Properties props)
    throws SocketException, IOException
  {
    sock.setTcpNoDelay(Boolean.valueOf(props.getProperty("tcpNoDelay", "true")).booleanValue());
    
    String keepAlive = props.getProperty("tcpKeepAlive", "true");
    
    if ((keepAlive != null) && (keepAlive.length() > 0)) {
      sock.setKeepAlive(Boolean.valueOf(keepAlive).booleanValue());
    }
    
    int receiveBufferSize = Integer.parseInt(props.getProperty("tcpRcvBuf", "0"));
    
    if (receiveBufferSize > 0) {
      sock.setReceiveBufferSize(receiveBufferSize);
    }
    
    int sendBufferSize = Integer.parseInt(props.getProperty("tcpSndBuf", "0"));
    
    if (sendBufferSize > 0) {
      sock.setSendBufferSize(sendBufferSize);
    }
    
    int trafficClass = Integer.parseInt(props.getProperty("tcpTrafficClass", "0"));
    
    if (trafficClass > 0) {
      sock.setTrafficClass(trafficClass);
    }
  }
  


  public Socket connect(String hostname, int portNumber, Properties props)
    throws SocketException, IOException
  {
    if (props != null) {
      host = hostname;
      
      port = portNumber;
      
      String localSocketHostname = props.getProperty("localSocketAddress");
      InetSocketAddress localSockAddr = null;
      if ((localSocketHostname != null) && (localSocketHostname.length() > 0)) {
        localSockAddr = new InetSocketAddress(InetAddress.getByName(localSocketHostname), 0);
      }
      
      String connectTimeoutStr = props.getProperty("connectTimeout");
      
      int connectTimeout = 0;
      
      if (connectTimeoutStr != null) {
        try {
          connectTimeout = Integer.parseInt(connectTimeoutStr);
        } catch (NumberFormatException nfe) {
          throw new SocketException("Illegal value '" + connectTimeoutStr + "' for connectTimeout");
        }
      }
      
      if (host != null) {
        InetAddress[] possibleAddresses = InetAddress.getAllByName(host);
        
        if (possibleAddresses.length == 0) {
          throw new SocketException("No addresses for host");
        }
        

        SocketException lastException = null;
        


        for (int i = 0; i < possibleAddresses.length; i++) {
          try {
            rawSocket = createSocket(props);
            
            configureSocket(rawSocket, props);
            
            InetSocketAddress sockAddr = new InetSocketAddress(possibleAddresses[i], port);
            
            if (localSockAddr != null) {
              rawSocket.bind(localSockAddr);
            }
            
            rawSocket.connect(sockAddr, getRealTimeout(connectTimeout));
          }
          catch (SocketException ex)
          {
            lastException = ex;
            resetLoginTimeCountdown();
            rawSocket = null;
          }
        }
        
        if ((rawSocket == null) && (lastException != null)) {
          throw lastException;
        }
        
        resetLoginTimeCountdown();
        
        return rawSocket;
      }
    }
    
    throw new SocketException("Unable to create socket");
  }
  
  public boolean isLocallyConnected(ConnectionImpl conn) throws SQLException {
    return SocketMetadata.Helper.isLocallyConnected(conn);
  }
  




  protected void resetLoginTimeCountdown()
    throws SocketException
  {
    if (loginTimeoutCountdown > 0) {
      long now = System.currentTimeMillis();
      loginTimeoutCountdown = ((int)(loginTimeoutCountdown - (now - loginTimeoutCheckTimestamp)));
      if (loginTimeoutCountdown <= 0) {
        throw new SocketException(Messages.getString("Connection.LoginTimeout"));
      }
      loginTimeoutCheckTimestamp = now;
    }
  }
  






  protected int getRealTimeout(int expectedTimeout)
  {
    if ((loginTimeoutCountdown > 0) && ((expectedTimeout == 0) || (expectedTimeout > loginTimeoutCountdown))) {
      return loginTimeoutCountdown;
    }
    return expectedTimeout;
  }
}
