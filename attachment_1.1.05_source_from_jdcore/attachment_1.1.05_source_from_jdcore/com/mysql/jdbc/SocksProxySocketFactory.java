package com.mysql.jdbc;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.Socket;
import java.util.Properties;
























public class SocksProxySocketFactory
  extends StandardSocketFactory
{
  public static int SOCKS_DEFAULT_PORT = 1080;
  
  public SocksProxySocketFactory() {}
  
  protected Socket createSocket(Properties props) { String socksProxyHost = props.getProperty("socksProxyHost");
    String socksProxyPortString = props.getProperty("socksProxyPort", String.valueOf(SOCKS_DEFAULT_PORT));
    int socksProxyPort = SOCKS_DEFAULT_PORT;
    try {
      socksProxyPort = Integer.valueOf(socksProxyPortString).intValue();
    }
    catch (NumberFormatException ex) {}
    

    return new Socket(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(socksProxyHost, socksProxyPort)));
  }
}
