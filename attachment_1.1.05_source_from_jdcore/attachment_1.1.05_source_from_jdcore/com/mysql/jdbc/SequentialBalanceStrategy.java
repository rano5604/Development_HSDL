package com.mysql.jdbc;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;



























public class SequentialBalanceStrategy
  implements BalanceStrategy
{
  private int currentHostIndex = -1;
  

  public SequentialBalanceStrategy() {}
  

  public void destroy() {}
  
  public void init(Connection conn, Properties props)
    throws SQLException
  {}
  
  public ConnectionImpl pickConnection(LoadBalancedConnectionProxy proxy, List<String> configuredHosts, Map<String, ConnectionImpl> liveConnections, long[] responseTimes, int numRetries)
    throws SQLException
  {
    int numHosts = configuredHosts.size();
    
    SQLException ex = null;
    
    Map<String, Long> blackList = proxy.getGlobalBlacklist();
    
    int attempts = 0; ConnectionImpl conn; for (;;) { if (attempts >= numRetries) break label405;
      if (numHosts == 1) {
        currentHostIndex = 0;
      } else if (currentHostIndex == -1) {
        int random = (int)Math.floor(Math.random() * numHosts);
        
        for (int i = random; i < numHosts; i++) {
          if (!blackList.containsKey(configuredHosts.get(i))) {
            currentHostIndex = i;
            break;
          }
        }
        
        if (currentHostIndex == -1) {
          for (int i = 0; i < random; i++) {
            if (!blackList.containsKey(configuredHosts.get(i))) {
              currentHostIndex = i;
              break;
            }
          }
        }
        
        if (currentHostIndex == -1) {
          blackList = proxy.getGlobalBlacklist();
          
          try
          {
            Thread.sleep(250L);
          }
          catch (InterruptedException e) {}
          
          continue;
        }
      }
      else {
        int i = currentHostIndex + 1;
        boolean foundGoodHost = false;
        for (; 
            i < numHosts; i++) {
          if (!blackList.containsKey(configuredHosts.get(i))) {
            currentHostIndex = i;
            foundGoodHost = true;
            break;
          }
        }
        
        if (!foundGoodHost) {
          for (i = 0; i < currentHostIndex; i++) {
            if (!blackList.containsKey(configuredHosts.get(i))) {
              currentHostIndex = i;
              foundGoodHost = true;
              break;
            }
          }
        }
        
        if (!foundGoodHost) {
          blackList = proxy.getGlobalBlacklist();
          
          try
          {
            Thread.sleep(250L);
          }
          catch (InterruptedException e) {}
          
          continue;
        }
      }
      
      String hostPortSpec = (String)configuredHosts.get(currentHostIndex);
      
      conn = (ConnectionImpl)liveConnections.get(hostPortSpec);
      
      if (conn == null)
        try {
          conn = proxy.createConnectionForHost(hostPortSpec);
        } catch (SQLException sqlEx) {
          ex = sqlEx;
          
          if (proxy.shouldExceptionTriggerConnectionSwitch(sqlEx))
          {
            proxy.addToGlobalBlacklist(hostPortSpec);
            try
            {
              Thread.sleep(250L);
            }
            catch (InterruptedException e) {}
          }
          else
          {
            throw sqlEx;
          }
        }
    }
    return conn;
    
    label405:
    if (ex != null) {
      throw ex;
    }
    
    return null;
  }
}
