package com.mysql.jdbc;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;






















public class ServerAffinityStrategy
  extends RandomBalanceStrategy
{
  public static final String AFFINITY_ORDER = "serverAffinityOrder";
  public String[] affinityOrderedServers = null;
  
  public ServerAffinityStrategy() {}
  
  public void init(Connection conn, Properties props) throws SQLException { super.init(conn, props);
    String hosts = props.getProperty("serverAffinityOrder");
    if (!StringUtils.isNullOrEmpty(hosts)) {
      affinityOrderedServers = hosts.split(",");
    }
  }
  
  public ConnectionImpl pickConnection(LoadBalancedConnectionProxy proxy, List<String> configuredHosts, Map<String, ConnectionImpl> liveConnections, long[] responseTimes, int numRetries)
    throws SQLException
  {
    if (affinityOrderedServers == null) {
      return super.pickConnection(proxy, configuredHosts, liveConnections, responseTimes, numRetries);
    }
    Map<String, Long> blackList = proxy.getGlobalBlacklist();
    
    for (String host : affinityOrderedServers) {
      if ((configuredHosts.contains(host)) && (!blackList.containsKey(host))) {
        ConnectionImpl conn = (ConnectionImpl)liveConnections.get(host);
        if (conn != null) {
          return conn;
        }
        try {
          return proxy.createConnectionForHost(host);
        }
        catch (SQLException sqlEx) {
          if (proxy.shouldExceptionTriggerConnectionSwitch(sqlEx)) {
            proxy.addToGlobalBlacklist(host);
          }
        }
      }
    }
    

    return super.pickConnection(proxy, configuredHosts, liveConnections, responseTimes, numRetries);
  }
}
