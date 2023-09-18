package com.mysql.jdbc;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;























public class ConnectionGroup
{
  private String groupName;
  private long connections = 0L;
  private long activeConnections = 0L;
  private HashMap<Long, LoadBalancedConnectionProxy> connectionProxies = new HashMap();
  private Set<String> hostList = new HashSet();
  private boolean isInitialized = false;
  private long closedProxyTotalPhysicalConnections = 0L;
  private long closedProxyTotalTransactions = 0L;
  private int activeHosts = 0;
  private Set<String> closedHosts = new HashSet();
  
  ConnectionGroup(String groupName) {
    this.groupName = groupName;
  }
  
  public long registerConnectionProxy(LoadBalancedConnectionProxy proxy, List<String> localHostList)
  {
    long currentConnectionId;
    synchronized (this) {
      if (!isInitialized) {
        hostList.addAll(localHostList);
        isInitialized = true;
        activeHosts = localHostList.size();
      }
      currentConnectionId = ++connections;
      connectionProxies.put(Long.valueOf(currentConnectionId), proxy);
    }
    activeConnections += 1L;
    
    return currentConnectionId;
  }
  
  public String getGroupName() {
    return groupName;
  }
  
  public Collection<String> getInitialHosts() {
    return hostList;
  }
  
  public int getActiveHostCount() {
    return activeHosts;
  }
  
  public Collection<String> getClosedHosts() {
    return closedHosts;
  }
  
  public long getTotalLogicalConnectionCount() {
    return connections;
  }
  
  public long getActiveLogicalConnectionCount() {
    return activeConnections;
  }
  
  public long getActivePhysicalConnectionCount() {
    long result = 0L;
    Map<Long, LoadBalancedConnectionProxy> proxyMap = new HashMap();
    synchronized (connectionProxies) {
      proxyMap.putAll(connectionProxies);
    }
    for (LoadBalancedConnectionProxy proxy : proxyMap.values()) {
      result += proxy.getActivePhysicalConnectionCount();
    }
    return result;
  }
  
  public long getTotalPhysicalConnectionCount() {
    long allConnections = closedProxyTotalPhysicalConnections;
    Map<Long, LoadBalancedConnectionProxy> proxyMap = new HashMap();
    synchronized (connectionProxies) {
      proxyMap.putAll(connectionProxies);
    }
    for (LoadBalancedConnectionProxy proxy : proxyMap.values()) {
      allConnections += proxy.getTotalPhysicalConnectionCount();
    }
    return allConnections;
  }
  
  public long getTotalTransactionCount()
  {
    long transactions = closedProxyTotalTransactions;
    Map<Long, LoadBalancedConnectionProxy> proxyMap = new HashMap();
    synchronized (connectionProxies) {
      proxyMap.putAll(connectionProxies);
    }
    for (LoadBalancedConnectionProxy proxy : proxyMap.values()) {
      transactions += proxy.getTransactionCount();
    }
    return transactions;
  }
  
  public void closeConnectionProxy(LoadBalancedConnectionProxy proxy) {
    activeConnections -= 1L;
    connectionProxies.remove(Long.valueOf(proxy.getConnectionGroupProxyID()));
    closedProxyTotalPhysicalConnections += proxy.getTotalPhysicalConnectionCount();
    closedProxyTotalTransactions += proxy.getTransactionCount();
  }
  





  public void removeHost(String hostPortPair)
    throws SQLException
  {
    removeHost(hostPortPair, false);
  }
  







  public void removeHost(String hostPortPair, boolean removeExisting)
    throws SQLException
  {
    removeHost(hostPortPair, removeExisting, true);
  }
  










  public synchronized void removeHost(String hostPortPair, boolean removeExisting, boolean waitForGracefulFailover)
    throws SQLException
  {
    if (activeHosts == 1) {
      throw SQLError.createSQLException("Cannot remove host, only one configured host active.", null);
    }
    
    if (hostList.remove(hostPortPair)) {
      activeHosts -= 1;
    } else {
      throw SQLError.createSQLException("Host is not configured: " + hostPortPair, null);
    }
    
    if (removeExisting)
    {
      Map<Long, LoadBalancedConnectionProxy> proxyMap = new HashMap();
      synchronized (connectionProxies) {
        proxyMap.putAll(connectionProxies);
      }
      
      for (LoadBalancedConnectionProxy proxy : proxyMap.values()) {
        if (waitForGracefulFailover) {
          proxy.removeHostWhenNotInUse(hostPortPair);
        } else {
          proxy.removeHost(hostPortPair);
        }
      }
    }
    closedHosts.add(hostPortPair);
  }
  






  public void addHost(String hostPortPair)
  {
    addHost(hostPortPair, false);
  }
  







  public void addHost(String hostPortPair, boolean forExisting)
  {
    synchronized (this) {
      if (hostList.add(hostPortPair)) {
        activeHosts += 1;
      }
    }
    
    if (!forExisting) {
      return;
    }
    

    Map<Long, LoadBalancedConnectionProxy> proxyMap = new HashMap();
    synchronized (connectionProxies) {
      proxyMap.putAll(connectionProxies);
    }
    
    for (LoadBalancedConnectionProxy proxy : proxyMap.values()) {
      proxy.addHost(hostPortPair);
    }
  }
}
