package com.mysql.jdbc;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

























public class RandomBalanceStrategy
  implements BalanceStrategy
{
  public RandomBalanceStrategy() {}
  
  public void destroy() {}
  
  public void init(Connection conn, Properties props)
    throws SQLException
  {}
  
  public ConnectionImpl pickConnection(LoadBalancedConnectionProxy proxy, List<String> configuredHosts, Map<String, ConnectionImpl> liveConnections, long[] responseTimes, int numRetries)
    throws SQLException
  {
    int numHosts = configuredHosts.size();
    
    SQLException ex = null;
    
    List<String> whiteList = new ArrayList(numHosts);
    whiteList.addAll(configuredHosts);
    
    Map<String, Long> blackList = proxy.getGlobalBlacklist();
    
    whiteList.removeAll(blackList.keySet());
    
    Map<String, Integer> whiteListMap = getArrayIndexMap(whiteList);
    
    int attempts = 0; ConnectionImpl conn; for (;;) { if (attempts >= numRetries) break label291;
      int random = (int)Math.floor(Math.random() * whiteList.size());
      if (whiteList.size() == 0) {
        throw SQLError.createSQLException("No hosts configured", null);
      }
      
      String hostPortSpec = (String)whiteList.get(random);
      
      conn = (ConnectionImpl)liveConnections.get(hostPortSpec);
      
      if (conn == null)
        try {
          conn = proxy.createConnectionForHost(hostPortSpec);
        } catch (SQLException sqlEx) {
          ex = sqlEx;
          
          if (proxy.shouldExceptionTriggerConnectionSwitch(sqlEx))
          {
            Integer whiteListIndex = (Integer)whiteListMap.get(hostPortSpec);
            

            if (whiteListIndex != null) {
              whiteList.remove(whiteListIndex.intValue());
              whiteListMap = getArrayIndexMap(whiteList);
            }
            proxy.addToGlobalBlacklist(hostPortSpec);
            
            if (whiteList.size() == 0) {
              attempts++;
              try {
                Thread.sleep(250L);
              }
              catch (InterruptedException e) {}
              

              whiteListMap = new HashMap(numHosts);
              whiteList.addAll(configuredHosts);
              blackList = proxy.getGlobalBlacklist();
              
              whiteList.removeAll(blackList.keySet());
              whiteListMap = getArrayIndexMap(whiteList);
            }
            
          }
          else
          {
            throw sqlEx;
          }
        }
    }
    return conn;
    
    label291:
    if (ex != null) {
      throw ex;
    }
    
    return null;
  }
  
  private Map<String, Integer> getArrayIndexMap(List<String> l) {
    Map<String, Integer> m = new HashMap(l.size());
    for (int i = 0; i < l.size(); i++) {
      m.put(l.get(i), Integer.valueOf(i));
    }
    return m;
  }
}
