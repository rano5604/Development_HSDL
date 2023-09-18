package com.mysql.jdbc;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;



























public class ReplicationConnectionGroup
{
  private String groupName;
  private long connections = 0L;
  private long slavesAdded = 0L;
  private long slavesRemoved = 0L;
  private long slavesPromoted = 0L;
  private long activeConnections = 0L;
  private HashMap<Long, ReplicationConnection> replicationConnections = new HashMap();
  private Set<String> slaveHostList = new CopyOnWriteArraySet();
  private boolean isInitialized = false;
  private Set<String> masterHostList = new CopyOnWriteArraySet();
  
  ReplicationConnectionGroup(String groupName) {
    this.groupName = groupName;
  }
  
  public long getConnectionCount() {
    return connections;
  }
  
  public long registerReplicationConnection(ReplicationConnection conn, List<String> localMasterList, List<String> localSlaveList)
  {
    long currentConnectionId;
    synchronized (this) {
      if (!isInitialized) {
        if (localMasterList != null) {
          masterHostList.addAll(localMasterList);
        }
        if (localSlaveList != null) {
          slaveHostList.addAll(localSlaveList);
        }
        isInitialized = true;
      }
      currentConnectionId = ++connections;
      replicationConnections.put(Long.valueOf(currentConnectionId), conn);
    }
    activeConnections += 1L;
    
    return currentConnectionId;
  }
  
  public String getGroupName() {
    return groupName;
  }
  
  public Collection<String> getMasterHosts() {
    return masterHostList;
  }
  
  public Collection<String> getSlaveHosts() {
    return slaveHostList;
  }
  












  public void addSlaveHost(String hostPortPair)
    throws SQLException
  {
    if (slaveHostList.add(hostPortPair)) {
      slavesAdded += 1L;
      

      for (ReplicationConnection c : replicationConnections.values()) {
        c.addSlaveHost(hostPortPair);
      }
    }
  }
  
  public void handleCloseConnection(ReplicationConnection conn) {
    replicationConnections.remove(Long.valueOf(conn.getConnectionGroupId()));
    activeConnections -= 1L;
  }
  












  public void removeSlaveHost(String hostPortPair, boolean closeGently)
    throws SQLException
  {
    if (slaveHostList.remove(hostPortPair)) {
      slavesRemoved += 1L;
      

      for (ReplicationConnection c : replicationConnections.values()) {
        c.removeSlave(hostPortPair, closeGently);
      }
    }
  }
  












  public void promoteSlaveToMaster(String hostPortPair)
    throws SQLException
  {
    if ((slaveHostList.remove(hostPortPair) | masterHostList.add(hostPortPair))) {
      slavesPromoted += 1L;
      
      for (ReplicationConnection c : replicationConnections.values()) {
        c.promoteSlaveToMaster(hostPortPair);
      }
    }
  }
  



  public void removeMasterHost(String hostPortPair)
    throws SQLException
  {
    removeMasterHost(hostPortPair, true);
  }
  












  public void removeMasterHost(String hostPortPair, boolean closeGently)
    throws SQLException
  {
    if (masterHostList.remove(hostPortPair))
    {
      for (ReplicationConnection c : replicationConnections.values()) {
        c.removeMasterHost(hostPortPair, closeGently);
      }
    }
  }
  
  public int getConnectionCountWithHostAsSlave(String hostPortPair) {
    int matched = 0;
    
    for (ReplicationConnection c : replicationConnections.values()) {
      if (c.isHostSlave(hostPortPair)) {
        matched++;
      }
    }
    return matched;
  }
  
  public int getConnectionCountWithHostAsMaster(String hostPortPair) {
    int matched = 0;
    
    for (ReplicationConnection c : replicationConnections.values()) {
      if (c.isHostMaster(hostPortPair)) {
        matched++;
      }
    }
    return matched;
  }
  
  public long getNumberOfSlavesAdded() {
    return slavesAdded;
  }
  
  public long getNumberOfSlavesRemoved() {
    return slavesRemoved;
  }
  
  public long getNumberOfSlavePromotions() {
    return slavesPromoted;
  }
  
  public long getTotalConnectionCount() {
    return connections;
  }
  
  public long getActiveConnectionCount() {
    return activeConnections;
  }
  
  public String toString()
  {
    return "ReplicationConnectionGroup[groupName=" + groupName + ",masterHostList=" + masterHostList + ",slaveHostList=" + slaveHostList + "]";
  }
}
